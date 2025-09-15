package com.service.studioasinc.AI;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.synapse.social.studioasinc.R;

public class Gemini {
    
    private static final String TAG = "GeminiAPI";
    
    // Default configuration
    private List<String> apiKeys;
    private String model = "gemini-1.5-flash";
    private String responseType = "text";
    private String tone = "normal";
    private String size = "normal";
    private int maxTokens = 2500;
    private double temperature = 1.0;
    private boolean showThinking = false;
    private String thinkingText = "Thinking...";
    private String systemInstruction = "Your name is Synapse AI, you are an AI made for Synapse (social media) assistance";
    
    private Context context;
    private TextView responseTextView;
    private Random random;
    
    // API endpoints
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    
    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
        void onThinking();
    }
    
    public static class Builder {
        private Context context;
        private List<String> apiKeys;
        private String model;
        private String responseType;
        private String tone;
        private String size;
        private int maxTokens;
        private double temperature;
        private boolean showThinking;
        private String thinkingText;
        private String systemInstruction;
        private TextView responseTextView;
        
        public Builder(Context context) {
            this.context = context;
            // Load API keys from JSON file
            this.apiKeys = loadApiKeysFromRaw(context);
            
            // Default values
            this.model = "gemini-1.5-flash";
            this.responseType = "text";
            this.tone = "normal";
            this.size = "normal";
            this.maxTokens = 2500;
            this.temperature = 1.0;
            this.showThinking = false;
            this.thinkingText = "Thinking...";
            this.systemInstruction = "Your name is Synapse AI, you are an AI made for Synapse (social media) assistance";
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder responseType(String responseType) {
            this.responseType = responseType;
            return this;
        }
        
        public Builder tone(String tone) {
            this.tone = tone;
            return this;
        }
        
        public Builder size(String size) {
            this.size = size;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder showThinking(boolean showThinking) {
            this.showThinking = showThinking;
            return this;
        }
        
        public Builder thinkingText(String thinkingText) {
            this.thinkingText = thinkingText;
            return this;
        }
        
        public Builder systemInstruction(String systemInstruction) {
            this.systemInstruction = systemInstruction;
            return this;
        }
        
        public Builder responseTextView(TextView textView) {
            this.responseTextView = textView;
            return this;
        }
        
        public Gemini build() {
            return new Gemini(this);
        }
        
        private List<String> loadApiKeysFromRaw(Context context) {
            List<String> keys = new ArrayList<>();
            try {
                InputStream is = context.getResources().openRawResource(R.raw.gemini_api);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                
                JSONArray jsonArray = new JSONArray(sb.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    keys.add(jsonArray.getString(i));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading API keys: " + e.getMessage());
            }
            return keys;
        }
    }
    
    private Gemini(Builder builder) {
        this.apiKeys = builder.apiKeys;
        this.model = builder.model;
        this.responseType = builder.responseType;
        this.tone = builder.tone;
        this.size = builder.size;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.showThinking = builder.showThinking;
        this.thinkingText = builder.thinkingText;
        this.systemInstruction = builder.systemInstruction;
        this.context = builder.context;
        this.responseTextView = builder.responseTextView;
        this.random = new Random();
    }
    
    public void sendPrompt(String prompt) {
        sendPrompt(prompt, null);
    }
    
    public void sendPrompt(String prompt, GeminiCallback callback) {
        if (prompt == null || prompt.trim().isEmpty()) {
            handleError("Prompt is empty!", callback);
            return;
        }
        
        if (apiKeys == null || apiKeys.isEmpty()) {
            handleError("No API keys available!", callback);
            return;
        }
        
        if (showThinking) {
            if (callback != null) {
                callback.onThinking();
            } else if (responseTextView != null) {
                responseTextView.post(() -> responseTextView.setText(thinkingText));
            }
        }
        
        new Thread(() -> {
            try {
                // Randomly select an API key
                String selectedApiKey = apiKeys.get(random.nextInt(apiKeys.size()));
                String response = sendGeminiRequest(prompt, selectedApiKey);
                
                if (responseTextView != null) {
                    responseTextView.post(() -> responseTextView.setText(response));
                }
                
                if (callback != null) {
                    callback.onSuccess(response);
                }
                
            } catch (Exception e) {
                String error = "Error: " + e.getMessage();
                Log.e(TAG, error, e);
                handleError(error, callback);
            }
        }).start();
    }
    
    private String sendGeminiRequest(String prompt, String apiKey) throws Exception {
        String urlString = BASE_URL + model + ":generateContent?key=" + apiKey;
        HttpURLConnection conn = null;
        
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            
            JSONObject payload = buildPayload(prompt);
            String body = payload.toString();
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input);
                os.flush();
            }
            
            int code = conn.getResponseCode();
            InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                br.close();
            }
            
            String rawResponse = sb.toString().trim();
            Log.d(TAG, "HTTP code=" + code + " rawResponse=" + rawResponse);
            
            if (code >= 200 && code < 300) {
                return extractTextFromGeminiResponse(rawResponse);
            } else {
                throw new Exception("HTTP " + code + " error: " + rawResponse);
            }
            
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    
    private JSONObject buildPayload(String prompt) {
        JSONObject payload = new JSONObject();
        try {
            // Build complete system instruction with all customizations
            String fullSystemInstruction = buildFullSystemInstruction();
            if (fullSystemInstruction != null && !fullSystemInstruction.isEmpty()) {
                JSONObject systemInstructionObj = new JSONObject();
                JSONArray sysParts = new JSONArray();
                sysParts.put(new JSONObject().put("text", fullSystemInstruction));
                systemInstructionObj.put("parts", sysParts);
                payload.put("system_instruction", systemInstructionObj);
            }
            
            JSONArray contents = new JSONArray();
            JSONObject contentItem = new JSONObject();
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", prompt));
            contentItem.put("parts", parts);
            contents.put(contentItem);
            payload.put("contents", contents);
            
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", temperature);
            generationConfig.put("maxOutputTokens", maxTokens);
            payload.put("generationConfig", generationConfig);
            
        } catch (JSONException e) {
            Log.e(TAG, "buildPayload JSONException: " + e.getMessage());
        }
        return payload;
    }
    
    private String buildFullSystemInstruction() {
        StringBuilder instruction = new StringBuilder(systemInstruction);
        
        if (!"normal".equals(tone)) {
            instruction.append(" Respond in a ").append(tone).append(" tone.");
        }
        
        if (!"normal".equals(size)) {
            instruction.append(" Make the response ").append(size).append(" in length.");
        }
        
        if (!"text".equals(responseType)) {
            instruction.append(" Format the response as ").append(responseType).append(".");
        }
        
        return instruction.toString();
    }
    
    private String extractTextFromGeminiResponse(String raw) {
        if (raw == null || raw.isEmpty()) return "Empty response";
        try {
            JSONObject root = new JSONObject(raw);
            
            if (root.has("candidates")) {
                JSONArray candidates = root.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    JSONObject c0 = candidates.getJSONObject(0);
                    if (c0.has("content")) {
                        JSONObject content = c0.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                return parts.getJSONObject(0).optString("text", "No text found");
                            }
                        }
                    }
                }
            }
            
            return raw.length() > 1000 ? raw.substring(0, 1000) + "..." : raw;
            
        } catch (JSONException e) {
            Log.w(TAG, "extractTextFromGeminiResponse JSON parse error: " + e.getMessage());
            return raw;
        }
    }
    
    private void handleError(String error, GeminiCallback callback) {
        if (callback != null) {
            callback.onError(error);
        } else if (responseTextView != null) {
            responseTextView.post(() -> responseTextView.setText(error));
        }
    }
    
    // Getters and setters for all customization options
    public void setModel(String model) { this.model = model; }
    public void setResponseType(String responseType) { this.responseType = responseType; }
    public void setTone(String tone) { this.tone = tone; }
    public void setSize(String size) { this.size = size; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setShowThinking(boolean showThinking) { this.showThinking = showThinking; }
    public void setThinkingText(String thinkingText) { this.thinkingText = thinkingText; }
    public void setSystemInstruction(String systemInstruction) { this.systemInstruction = systemInstruction; }
    public void setResponseTextView(TextView responseTextView) { this.responseTextView = responseTextView; }
    
    public String getModel() { return model; }
    public String getResponseType() { return responseType; }
    public String getTone() { return tone; }
    public String getSize() { return size; }
    public int getMaxTokens() { return maxTokens; }
    public double getTemperature() { return temperature; }
    public boolean isShowThinking() { return showThinking; }
    public String getThinkingText() { return thinkingText; }
    public String getSystemInstruction() { return systemInstruction; }
}