package kr.seheon.urlpolice.api

data class UrlCheckResponse(
    val sslVerification: SslVerificationResult? = null,
    val urlCheck: UrlCheckResult? = null
)

data class SslVerificationResult(
    val verdict: String,
    val totalScore: Int,
    val checks: List<VerificationCheck>,
    val serverCertificate: ServerCertificate
)

data class VerificationCheck(
    val name: String,
    val passed: Boolean,
    val score: Int,
    val message: String
)

data class ServerCertificate(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val expired: Boolean,
    val spki: String
)

data class UrlCheckResult(
    val verdict: String,
    val reasons: List<ReasonDetail>,
    val riskScore: Int
)

data class ReasonDetail(
    val code: String,
    val severity: String,
    val message: String
)
