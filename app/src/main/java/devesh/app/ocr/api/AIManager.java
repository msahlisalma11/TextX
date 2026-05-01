package devesh.app.ocr.api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIManager {
    private static final String TAG = "AIManager";

    // Groq API Key
    private final String groqApiKey = "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";
    
    // Groq API Endpoint (OpenAI compatible)
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient();

    public interface AIListener {
        void onSuccess(String summary, String keywords);
        void onFailure(String error);
    }

    public void analyzeText(String text, AIListener listener) {
        if (text == null || text.trim().isEmpty()) {
            listener.onFailure("No text provided for analysis.");
            return;
        }

        executor.execute(() -> {
            try {
                // Create a clear prompt for the model
                String prompt = "You are an AI assistant helping to analyze scanned OCR text. " +
                        "Below is the text extracted from an image. Please perform the following tasks:\n" +
                        "1. Summarize the text in exactly 2 sentences.\n" +
                        "2. Provide 3 relevant keywords separated by commas.\n\n" +
                        "Separate the summary and the keywords with a double newline (\\n\\n).\n\n" +
                        "TEXT:\n" + text;

                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", prompt);

                JSONArray messagesArray = new JSONArray();
                messagesArray.put(message);

                JSONObject jsonBody = new JSONObject();
                // Using llama-3.3-70b-versatile for high quality results
                jsonBody.put("model", "llama-3.3-70b-versatile");
                jsonBody.put("messages", messagesArray);

                RequestBody body = RequestBody.create(
                        jsonBody.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + groqApiKey)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String aiResponse = jsonResponse.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        Log.d(TAG, "AI Response: " + aiResponse);
                        parseAndReturn(aiResponse, listener);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Empty error body";
                        Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
                        mainHandler.post(() -> listener.onFailure("AI Error: " + response.code()));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Network/Parsing Error: ", e);
                mainHandler.post(() -> listener.onFailure("Error: " + e.getMessage()));
            }
        });
    }

    private void parseAndReturn(String aiResponse, AIListener listener) {
        mainHandler.post(() -> {
            String summary = aiResponse;
            String keywords = "";

            if (aiResponse.contains("\n\n")) {
                String[] parts = aiResponse.split("\n\n", 2);
                summary = parts[0].trim();
                keywords = parts[1].trim();
            } else if (aiResponse.contains("\n")) {
                String[] parts = aiResponse.split("\n", 2);
                summary = parts[0].trim();
                keywords = parts[1].trim();
            }

            listener.onSuccess(summary, keywords);
        });
    }
}