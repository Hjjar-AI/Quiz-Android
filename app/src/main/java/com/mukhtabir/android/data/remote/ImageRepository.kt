package com.mukhtabir.android.data.remote

class ImageRepository(private val apiClient: ApiClient) {
    suspend fun load(url: String): ByteArray = apiClient.loadBytes(url)
}

