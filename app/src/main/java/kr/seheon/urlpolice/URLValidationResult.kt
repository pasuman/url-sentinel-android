package kr.seheon.urlpolice

enum class ThreatType {
    PHISHING,
    MALWARE,
    SCAM,
    SUSPICIOUS
}

data class URLValidationResult(
    val url: String,
    val isSafe: Boolean,
    val threatType: ThreatType? = null,
    val confidence: Float,
    val message: String
)
