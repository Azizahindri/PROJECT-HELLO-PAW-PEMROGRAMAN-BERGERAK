package com.example.projecthellopaw.utils

import android.util.Log
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApiService(
    private val apiKey: String
) {
    companion object {
        private const val TAG = "GeminiApiService"

        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(prompt: String): String? {
        return try {
            val escapedPrompt = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

            val jsonBody = """
            {
                "contents": [
                    {
                        "parts": [
                            {
                                "text": "$escapedPrompt"
                            }
                        ]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.3,
                    "topK": 40,
                    "topP": 0.85,
                    "maxOutputTokens": 1000
                }
            }
            """.trimIndent()

            Log.d(TAG, "=== REQUEST ===")
            Log.d(TAG, "URL: $BASE_URL?key=${apiKey.take(10)}...")
            Log.d(TAG, "Body: $jsonBody")

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            Log.d(TAG, "=== RESPONSE ===")
            Log.d(TAG, "Code: ${response.code}")

            val responseBody = response.body?.string()
            Log.d(TAG, "Body: $responseBody")

            handleResponse(response, responseBody)

        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            null
        }
    }

    private fun handleResponse(response: okhttp3.Response, responseBody: String?): String? {
        return try {
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                Log.d(TAG, "Parsing response...")

                try {
                    val jsonObject = JsonParser.parseString(responseBody).asJsonObject
                    val candidates = jsonObject.get("candidates")?.asJsonArray

                    if (candidates != null && candidates.size() > 0) {
                        val firstCandidate = candidates[0].asJsonObject
                        val content = firstCandidate.get("content")?.asJsonObject

                        if (content != null) {
                            val parts = content.get("parts")?.asJsonArray

                            if (parts != null && parts.size() > 0) {
                                val part = parts[0].asJsonObject
                                val text = part.get("text")?.asString
                                Log.d(TAG, "Extracted text: $text")
                                return text
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON: ${e.message}", e)
                    return responseBody
                }

                Log.e(TAG, "No text found in response")
                return null
            } else {
                Log.e(TAG, "API Error: ${response.code} - $responseBody")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling response: ${e.message}", e)
            null
        } finally {
            response.close()
        }
    }
}