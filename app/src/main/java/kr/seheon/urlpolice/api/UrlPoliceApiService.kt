package kr.seheon.urlpolice.api

import retrofit2.http.Body
import retrofit2.http.POST

interface UrlPoliceApiService {
    @POST("/api/v1/url/check")
    suspend fun checkUrl(@Body request: UrlCheckRequest): UrlCheckResponse
}
