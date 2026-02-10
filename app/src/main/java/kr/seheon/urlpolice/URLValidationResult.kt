package kr.seheon.urlpolice

enum class ThreatType {
    PHISHING,
    MALWARE,
    SCAM,
    SUSPICIOUS,
    DNS_HIJACKING
}

enum class SecurityLevel {
    SAFE,       // URL is safe to open
    WARNING,    // URL has some suspicious indicators
    DANGER      // URL is dangerous, should not be opened
}

data class URLValidationResult(
    val url: String,
    val securityLevel: SecurityLevel,
    val threatType: ThreatType? = null,
    val confidence: Float,
    val message: String
) {
    // Backward compatibility property
    val isSafe: Boolean
        get() = securityLevel == SecurityLevel.SAFE
}
