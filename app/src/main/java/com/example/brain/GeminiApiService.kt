package com.example.brain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    @field:Json(name = "contents") val contents: List<GeminiContentDto>,
    @field:Json(name = "systemInstruction") val systemInstruction: GeminiContentDto? = null,
    @field:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContentDto(
    @field:Json(name = "role") val role: String? = null,
    @field:Json(name = "parts") val parts: List<GeminiPartDto>
)

@JsonClass(generateAdapter = true)
data class GeminiPartDto(
    @field:Json(name = "text") val text: String? = null,
    @field:Json(name = "inlineData") val inlineData: GeminiInlineDataDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineDataDto(
    @field:Json(name = "mimeType") val mimeType: String,
    @field:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfigDto(
    @field:Json(name = "temperature") val temperature: Double? = null,
    @field:Json(name = "topP") val topP: Double? = null,
    @field:Json(name = "topK") val topK: Int? = null,
    @field:Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @field:Json(name = "speechConfig") val speechConfig: GeminiSpeechConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSpeechConfigDto(
    @field:Json(name = "voiceConfig") val voiceConfig: GeminiVoiceConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiVoiceConfigDto(
    @field:Json(name = "prebuiltVoiceConfig") val prebuiltVoiceConfig: GeminiPrebuiltVoiceConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPrebuiltVoiceConfigDto(
    @field:Json(name = "voiceName") val voiceName: String = "Kore"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    @field:Json(name = "candidates") val candidates: List<GeminiCandidateDto>? = null,
    @field:Json(name = "promptFeedback") val promptFeedback: GeminiPromptFeedbackDto? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateDto(
    @field:Json(name = "content") val content: GeminiContentDto? = null,
    @field:Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPromptFeedbackDto(
    @field:Json(name = "blockReason") val blockReason: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String = "gemini-3.5-flash",
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): Response<GeminiGenerateContentResponse>
}
