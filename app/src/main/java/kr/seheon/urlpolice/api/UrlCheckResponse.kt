package kr.seheon.urlpolice.api

data class UrlCheckResponse(
    val verdict: String,
    val reasons: List<ReasonDetail>,
    val riskScore: Int
)

data class ReasonDetail(
    val code: String,
    val severity: String,
    val message: String
)
