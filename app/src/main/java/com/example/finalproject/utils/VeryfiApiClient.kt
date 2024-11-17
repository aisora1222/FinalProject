package com.example.finalproject.utils

import okhttp3.*
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object VeryfiApiClient {

    fun processDocumentWithVeryfi(
        imageFile: File,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val client = OkHttpClient()
        val url = "https://api.veryfi.com/api/v8/partner/documents/"

        val mediaType = "image/jpeg".toMediaTypeOrNull()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", imageFile.name, RequestBody.create(mediaType, imageFile))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "multipart/form-data")
            .addHeader("Accept", "application/json")
            .addHeader("Client-Id", VeryfiCredentials.CLIENT_ID)
            .addHeader("Authorization", "apikey ${VeryfiCredentials.USERNAME}:${VeryfiCredentials.API_KEY}")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onError(IOException("Unexpected code $it"))
                    } else {
                        val responseBody = it.body
                        if (responseBody != null) {
                            onSuccess(responseBody.string())
                        } else {
                            onError(IOException("Response body is null"))
                        }
                    }
                }
            }
        })
    }
}
