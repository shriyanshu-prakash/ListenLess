package com.shriyanshuprakash.listenless;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class GroqTranscriptionService {

    private static final String GROQ_API_KEY = "YOUR_GROQ_API_KEY_HERE";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/audio/transcriptions";

    public interface TranscriptionCallback {
        void onSuccess(String transcribedText);
        void onError(String errorMessage);
    }

    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;

    public GroqTranscriptionService(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS) // audio files can take time
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Transcribes an audio file from a URI using Groq Whisper.
     * Runs on a background thread; callback is delivered on the main thread.
     */
    public void transcribe(Uri audioUri, String originalFileName, TranscriptionCallback callback) {
        new Thread(() -> {
            try {
                // Copy URI content to a temp file so OkHttp can read it
                File tempFile = copyUriToTempFile(audioUri, originalFileName);
                if (tempFile == null) {
                    deliverError(callback, "Could not read audio file");
                    return;
                }

                // Determine MIME type from extension
                String mimeType = getMimeType(originalFileName);

                RequestBody fileBody = RequestBody.create(tempFile, MediaType.parse(mimeType));

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", originalFileName, fileBody)
                        .addFormDataPart("model", "whisper-large-v3")
                        .addFormDataPart("response_format", "text")
                        .build();

                Request request = new Request.Builder()
                        .url(GROQ_API_URL)
                        .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String transcribedText = response.body().string().trim();
                        tempFile.delete();
                        deliverSuccess(callback, transcribedText);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        tempFile.delete();
                        // Try to parse Groq error message
                        try {
                            JSONObject json = new JSONObject(errorBody);
                            String msg = json.getJSONObject("error").getString("message");
                            deliverError(callback, msg);
                        } catch (Exception e) {
                            deliverError(callback, "Transcription failed: " + response.code());
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                deliverError(callback, "Error: " + e.getMessage());
            }
        }).start();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private File copyUriToTempFile(Uri uri, String originalFileName) {
        try {
            String ext = getExtension(originalFileName);
            File tempFile = new File(context.getCacheDir(), "import_audio" + ext);
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : ".mp3";
    }

    private String getMimeType(String fileName) {
        String ext = getExtension(fileName).toLowerCase();
        switch (ext) {
            case ".mp3":  return "audio/mpeg";
            case ".mp4":
            case ".m4a":  return "audio/mp4";
            case ".wav":  return "audio/wav";
            case ".ogg":  return "audio/ogg";
            case ".flac": return "audio/flac";
            case ".webm": return "audio/webm";
            default:      return "audio/mpeg";
        }
    }

    private void deliverSuccess(TranscriptionCallback callback, String text) {
        mainHandler.post(() -> callback.onSuccess(text));
    }

    private void deliverError(TranscriptionCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}