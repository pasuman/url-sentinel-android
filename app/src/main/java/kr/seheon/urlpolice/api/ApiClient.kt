package kr.seheon.urlpolice.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {
    // Default to localhost for development - can be configured via BuildConfig
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(DEFAULT_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val urlPoliceService: UrlPoliceApiService by lazy {
        retrofit.create(UrlPoliceApiService::class.java)
    }
}
