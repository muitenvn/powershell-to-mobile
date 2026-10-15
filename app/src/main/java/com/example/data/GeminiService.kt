package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiService {
    private val client = OkHttpClient()

    suspend fun askPowerShellCopilot(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API Key is not configured. Please set GEMINI_API_KEY in the Secrets panel in AI Studio.\n\nSimulated AI Answer: To achieve your task in PowerShell, you can use:\nGet-ChildItem -Recurse | Where-Object { \$_.Extension -eq '.txt' }"
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", "You are PowerShell AI Copilot, an expert in PowerShell scripting and automation. Provide concise, accurate PowerShell commands and script examples.\n\nUser Question: $prompt")
                    ))
                ))
            }

            val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Error: HTTP ${response.code} - ${response.message}"
                }
                val responseString = response.body?.string() ?: return@withContext "Empty response"
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text found")
                    }
                }
                "No valid response structure from Gemini API."
            }
        } catch (e: Exception) {
            "Error calling Gemini AI: ${e.localizedMessage}\n\nSuggested PowerShell snippet:\nGet-Process | Sort-Object WorkingSet -Descending | Select-Object -First 10"
        }
    }
}
