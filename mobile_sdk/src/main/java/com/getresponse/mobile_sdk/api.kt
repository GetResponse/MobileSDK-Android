package com.getresponse.mobile_sdk

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import retrofit2.Response as retrofit2_Response

class StandardHeadersInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder()
                .header("Content-Type", "application/json")
                .header("X-Sdk-Version", "1.0")
                .header("X-Sdk-Platform", "Android")
                .build()

        )
    }
}

class AuthHeaderInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.run {
        proceed(
            request()
                .newBuilder()
                .header("Authorization", "Bearer $token")
                .build()

        )
    }
}

interface GrMobileApiService {

    @POST("consents")
    suspend fun consent(@Body consentData: ConsentModel): retrofit2_Response<Unit>

    @POST("consents")
    fun consentCall(@Body consentData: ConsentModel): Call<retrofit2_Response<Unit>>

    @DELETE("consents")
    suspend fun consentDelete(): retrofit2_Response<Unit>

    @DELETE("consents")
    fun consentDeleteCall(): Call<retrofit2_Response<Unit>>

}

interface GrMobileStatsApiService {
    @GET
    suspend fun stats(@Url url: String): retrofit2_Response<Unit>
}

object GetResponseMobileSdkClient {
    fun getApi(endPoint: String, token: String, enableDebug: Boolean): GrMobileApiService {

        val httpClient = OkHttpClient().newBuilder()
            .addInterceptor(StandardHeadersInterceptor())
            .addInterceptor(AuthHeaderInterceptor(token))
            .addNetworkInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (enableDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                }).build()

        return Retrofit.Builder()
            .baseUrl(endPoint.replace(Regex("/$"), "") + "/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
            .create(GrMobileApiService::class.java)
    }

    fun getStatsApi(enableDebug: Boolean): GrMobileStatsApiService {
        val httpClient = OkHttpClient().newBuilder()
            .addInterceptor(StandardHeadersInterceptor())
            .addNetworkInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (enableDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
                }).build()

        return Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .baseUrl("https://api.getresponse.com") //this url is ignored
            .build()
            .create(GrMobileStatsApiService::class.java)
    }
}