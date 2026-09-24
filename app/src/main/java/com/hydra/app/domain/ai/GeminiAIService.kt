package com.hydra.app.domain.ai

import com.hydra.app.data.model.ParsedInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAIService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : AIService {

    override suspend fun parseNaturalLanguage(input: String, apiKey: String): Result<ParsedInput> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing"))
            }

            try {
                val prompt = buildString {
                    append("You are an assistant parsing daily health events for an app called Hydra.\n")
                    append("Convert this user log into a single JSON object without any markdown tags or backticks:\n")
                    append("Input: \"$input\"\n")
                    append("Output Schema:\n")
                    append("{\n")
                    append("  \"type\": \"WATER\" | \"FOOD\" | \"CREATINE\" | \"NOTE\",\n")
                    append("  \"description\": string or null,\n")
                    append("  \"amount\": number or null (e.g. 500 for water ml),\n")
                    append("  \"unit\": string or null (e.g. \"ml\" or \"g\"),\n")
                    append("  \"calories\": integer or null (approximate),\n")
                    append("  \"protein\": number or null (approximate grams),\n")
                    append("  \"carbs\": number or null (approximate grams),\n")
                    append("  \"fat\": number or null (approximate grams),\n")
                    append("  \"confidence\": number between 0.0 and 1.0\n")
                    append("}\n")
                    append("Output pure JSON only.")
                }

                val requestJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)

                    val generationConfig = JSONObject().apply {
                        put("temperature", 0.1)
                        put("response_mime_type", "application/json")
                    }
                    put("generationConfig", generationConfig)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestJson.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "HTTP ${response.code}"
                    return@withContext Result.failure(Exception("Gemini API error (${response.code}): $errBody"))
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No response candidates from Gemini"))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

                AIJsonParser.parse(textOutput, input)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun testConnection(apiKey: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("مفتاح API غير مدخل"))
            }

            try {
                val requestJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                put(JSONObject().put("text", "ping"))
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = requestJson.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    Result.failure(Exception("فشل الاتصال: $err"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
