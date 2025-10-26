package com.example.proyectopresionarterial;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface BloodPressureApiService {
    @POST("chat/completions")
    Call<OpenAIResponse> classify(@Body OpenAIRequest request);
}
