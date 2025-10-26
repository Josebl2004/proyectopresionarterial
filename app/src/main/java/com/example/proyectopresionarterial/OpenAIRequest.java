package com.example.proyectopresionarterial;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Estructura de petición para OpenAI chat/completions API
 */
public class OpenAIRequest {
    @SerializedName("model")
    private String model = "gpt-3.5-turbo";

    @SerializedName("messages")
    private List<Message> messages;

    @SerializedName("temperature")
    private double temperature = 0.7;

    @SerializedName("max_tokens")
    private int maxTokens = 300;

    public OpenAIRequest(String prompt) {
        this.messages = new ArrayList<>();
        this.messages.add(new Message("system", "Eres un asistente médico especializado en hipertensión arterial. Ofrece recomendaciones precisas, concisas y personalizadas basadas en los datos de presión arterial. Adapta tus consejos al nivel de presión, condición física, edad, género y otra información proporcionada. Responde en español, usando lenguaje claro pero profesional. Limita tu respuesta a 3-4 frases específicas."));
        this.messages.add(new Message("user", prompt));
    }

    // Nuevo: constructor sobrecargado para controlar temperatura, tokens y system prompt
    public OpenAIRequest(String prompt, double temperature, int maxTokens, String systemPrompt) {
        this.messages = new ArrayList<>();
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = "Eres un asistente médico que genera recomendaciones en español basadas en múltiples mediciones de presión arterial. Sé claro y profesional.";
        }
        this.messages.add(new Message("system", systemPrompt));
        this.messages.add(new Message("user", prompt));
    }

    public static class Message {
        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
