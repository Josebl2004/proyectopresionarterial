package com.example.proyectopresionarterial;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Estructura de respuesta de OpenAI chat/completions API
 */
public class OpenAIResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("object")
    private String object;

    @SerializedName("created")
    private long created;

    @SerializedName("model")
    private String model;

    @SerializedName("choices")
    private List<Choice> choices;

    @SerializedName("usage")
    private Usage usage;

    // Adaptador para convertir respuesta de OpenAI a nuestro formato ApiResponse
    public ApiResponse toApiResponse() {
        ApiResponse response = new ApiResponse();
        if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
            String content = choices.get(0).getMessage().getContent();
            // Dejar solo el cuerpo de recomendación; la clasificación se calcula localmente
            if (content != null) {
                String trimmed = content.trim();
                response.setRecommendation(trimmed.isEmpty() ? "No se pudo obtener una recomendación." : trimmed);
            } else {
                response.setRecommendation("No se pudo obtener una recomendación.");
            }
            // No inferir clasificación desde el texto de IA para evitar falsos positivos
            response.setClassification(null);
        } else {
            response.setRecommendation("No se pudo obtener una recomendación.");
            response.setClassification(null);
        }
        return response;
    }

    public static class Choice {
        @SerializedName("index")
        private int index;

        @SerializedName("message")
        private Message message;

        @SerializedName("finish_reason")
        private String finishReason;

        public Message getMessage() {
            return message;
        }
    }

    public static class Message {
        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private String content;

        public String getContent() {
            return content;
        }
    }

    public static class Usage {
        @SerializedName("prompt_tokens")
        private int promptTokens;

        @SerializedName("completion_tokens")
        private int completionTokens;

        @SerializedName("total_tokens")
        private int totalTokens;
    }
}
