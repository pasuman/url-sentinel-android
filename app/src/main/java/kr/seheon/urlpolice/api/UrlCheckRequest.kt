package kr.seheon.urlpolice.api

data class UrlCheckRequest(
    val url: String,
    val clientObservation: ClientObservation? = null
)

data class ClientObservation(
    val certificateChain: List<String>? = null,
    val serverIp: String? = null,
    val serverAsn: Int? = null,
    val tlsProtocol: String? = null,
    val cipherSuite: String? = null,
    val alpnProtocol: String? = null
)
