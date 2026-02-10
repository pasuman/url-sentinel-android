package kr.seheon.urlpolice

import android.net.Uri
import kr.seheon.urlpolice.api.ApiClient
import kr.seheon.urlpolice.api.UrlCheckRequest
import kr.seheon.urlpolice.api.UrlCheckResponse

class URLValidationService {

    companion object {
        private const val VERDICT_ALLOW = "ALLOW"
        private const val VERDICT_REJECT = "REJECT"

        private const val SEVERITY_CRITICAL = "CRITICAL"
        private const val SEVERITY_MAJOR = "MAJOR"

        // Rule codes from server
        private const val IP_ADDRESS_DOMAIN = "IP_ADDRESS_DOMAIN"
        private const val SUSPICIOUS_KEYWORD = "SUSPICIOUS_KEYWORD"
        private const val URL_SHORTENER = "URL_SHORTENER"
    }

    private val apiService = ApiClient.urlPoliceService

    suspend fun validateUrl(uri: Uri): URLValidationResult {
        val url = uri.toString()

        return try {
            // Call API without client observation (simplified)
            val response = apiService.analyzeUrl(
                UrlCheckRequest(
                    url = url,
                    clientObservation = null
                )
            )
            mapResponseToResult(url, response)
        } catch (e: Exception) {
            // Fallback to error result if API fails
            createErrorResult(url, e)
        }
    }

    private fun mapResponseToResult(
        url: String,
        response: UrlCheckResponse
    ): URLValidationResult {
        val urlCheck = response.urlCheck

        // If no URL check result, return error
        if (urlCheck == null) {
            return URLValidationResult(
                url = url,
                securityLevel = SecurityLevel.WARNING,
                threatType = ThreatType.SUSPICIOUS,
                confidence = 0.5f,
                message = "URL 분석 결과를 받을 수 없습니다."
            )
        }

        // Determine security level based on URL check verdict
        val securityLevel = when (urlCheck.verdict) {
            VERDICT_ALLOW -> SecurityLevel.SAFE
            VERDICT_REJECT -> {
                // Check severity of reasons
                val hasCritical = urlCheck.reasons.any { it.severity == SEVERITY_CRITICAL }
                if (hasCritical) SecurityLevel.DANGER else SecurityLevel.WARNING
            }
            else -> SecurityLevel.WARNING
        }

        // Determine threat type from URL checks
        val threatType = determineThreatType(urlCheck.reasons)

        // Calculate confidence
        val confidence = calculateConfidence(urlCheck.riskScore)

        // Build message
        val message = buildMessage(securityLevel, urlCheck.reasons, urlCheck.riskScore)

        return URLValidationResult(
            url = url,
            securityLevel = securityLevel,
            threatType = threatType,
            confidence = confidence,
            message = message
        )
    }

    private fun calculateConfidence(riskScore: Int): Float {
        // Convert risk score (0-100) to confidence (0.0-1.0)
        // Higher risk = lower confidence
        return (100 - riskScore) / 100f
    }

    private fun determineThreatType(reasons: List<kr.seheon.urlpolice.api.ReasonDetail>): ThreatType? {
        if (reasons.isEmpty()) return null

        // Prioritize based on severity and codes
        val criticalReasons = reasons.filter { it.severity == SEVERITY_CRITICAL }
        val majorReasons = reasons.filter { it.severity == SEVERITY_MAJOR }

        return when {
            criticalReasons.any { it.code == IP_ADDRESS_DOMAIN } -> ThreatType.PHISHING
            majorReasons.any { it.code == SUSPICIOUS_KEYWORD } -> ThreatType.PHISHING
            majorReasons.any { it.code == URL_SHORTENER } -> ThreatType.SUSPICIOUS
            reasons.isNotEmpty() -> ThreatType.SUSPICIOUS
            else -> null
        }
    }

    private fun buildMessage(
        securityLevel: SecurityLevel,
        reasons: List<kr.seheon.urlpolice.api.ReasonDetail>,
        riskScore: Int
    ): String {
        return when (securityLevel) {
            SecurityLevel.SAFE -> {
                "위협이 감지되지 않았습니다. 이 URL은 안전한 것으로 보입니다."
            }
            SecurityLevel.WARNING -> {
                if (reasons.isEmpty()) {
                    "이 URL은 잠재적으로 안전하지 않을 수 있습니다. 주의하세요."
                } else {
                    val primaryReason = reasons
                        .sortedByDescending {
                            when (it.severity) {
                                SEVERITY_CRITICAL -> 3
                                SEVERITY_MAJOR -> 2
                                else -> 1
                            }
                        }
                        .firstOrNull()

                    "경고: ${primaryReason?.message ?: "의심스러운 URL입니다."} (위험 점수: $riskScore/100)"
                }
            }
            SecurityLevel.DANGER -> {
                if (reasons.isEmpty()) {
                    "이 URL은 위험합니다. 열지 마세요!"
                } else {
                    val primaryReasons = reasons
                        .sortedByDescending {
                            when (it.severity) {
                                SEVERITY_CRITICAL -> 3
                                SEVERITY_MAJOR -> 2
                                else -> 1
                            }
                        }
                        .take(2)
                        .joinToString(" ") { it.message }

                    "위험: $primaryReasons (위험 점수: $riskScore/100)"
                }
            }
        }
    }

    private fun createErrorResult(url: String, error: Exception): URLValidationResult {
        return URLValidationResult(
            url = url,
            securityLevel = SecurityLevel.WARNING,
            threatType = ThreatType.SUSPICIOUS,
            confidence = 0.5f,
            message = "URL을 검증할 수 없습니다: ${error.message ?: "네트워크 오류"}. 주의하여 진행하세요."
        )
    }
}
