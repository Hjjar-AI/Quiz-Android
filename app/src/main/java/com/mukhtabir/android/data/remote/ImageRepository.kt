package com.mukhtabir.android.data.remote

class ImageRepository(private val apiClient: ApiClient) : LearnerImages {
    override suspend fun load(url: String): ByteArray = apiClient.loadBytes(url)
}

