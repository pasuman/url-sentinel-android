package kr.seheon.urlpolice

import android.net.Uri
import kr.seheon.urlpolice.api.ApiClient
import kr.seheon.urlpolice.api.ReasonDetail
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
            val response = apiService.checkUrl(UrlCheckRequest(url))
            mapResponseToResult(url, response)
        } catch (e: Exception) {
            // Fallback to error result if API fails
            createErrorResult(url, e)
        }
    }

    private fun mapResponseToResult(url: String, response: UrlCheckResponse): URLValidationResult {
        val isSafe = response.verdict == VERDICT_ALLOW
        val confidence = calculateConfidence(response.riskScore)
        val threatType = determineThreatType(response.reasons)
        val message = buildMessage(isSafe, response.reasons, response.riskScore)

        return URLValidationResult(
            url = url,
            isSafe = isSafe,
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

    private fun determineThreatType(reasons: List<ReasonDetail>): ThreatType? {
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

    private fun buildMessage(isSafe: Boolean, reasons: List<ReasonDetail>, riskScore: Int): String {
        if (isSafe) {
            return "위협이 감지되지 않았습니다. 이 URL은 안전한 것으로 보입니다."
        }

        if (reasons.isEmpty()) {
            return "이 URL은 잠재적으로 안전하지 않은 것으로 표시되었습니다."
        }

        // Build message from most severe reasons
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

        return "$primaryReasons (위험 점수: $riskScore/100)"
    }

    private fun createErrorResult(url: String, error: Exception): URLValidationResult {
        return URLValidationResult(
            url = url,
            isSafe = false,
            threatType = ThreatType.SUSPICIOUS,
            confidence = 0.5f,
            message = "URL을 검증할 수 없습니다: ${error.message ?: "네트워크 오류"}. 주의하여 진행하세요."
        )
    }
}
