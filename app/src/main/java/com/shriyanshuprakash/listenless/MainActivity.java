package com.shriyanshuprakash.listenless;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView transcriptText;
    private TextView statusText;

    private Button startButton;
    private Button saveButton;
    private Button notesButton;
    private Button importButton;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    private boolean isListening = false;
    private String fullTranscript = "";

    private GroqTranscriptionService transcriptionService;

    // File picker launcher
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                String fileName = getFileNameFromUri(uri);
                                startTranscription(uri, fileName);
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transcriptText = findViewById(R.id.transcriptText);
        statusText     = findViewById(R.id.statusText);
        startButton    = findViewById(R.id.startButton);
        saveButton     = findViewById(R.id.saveButton);
        notesButton    = findViewById(R.id.notesButton);
        importButton   = findViewById(R.id.importButton);

        transcriptionService = new GroqTranscriptionService(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { statusText.setText("● Listening"); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                if (isListening) {
                    speechRecognizer.startListening(speechIntent);
                } else {
                    statusText.setText("● Ready");
                    startButton.setText("🎤 Start Listening");
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null && !data.isEmpty()) {
                    fullTranscript += data.get(0) + "\n\n";
                    transcriptText.setText(fullTranscript);
                }
                if (isListening) {
                    speechRecognizer.startListening(speechIntent);
                } else {
                    statusText.setText("● Ready");
                    startButton.setText("🎤 Start Listening");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null && !data.isEmpty()) {
                    transcriptText.setText(fullTranscript + data.get(0));
                }
            }

            @Override public void onEvent(int eventType, Bundle params) {}
        });

        startButton.setOnClickListener(v -> {
            if (!isListening) {
                isListening = true;
                speechRecognizer.startListening(speechIntent);
                statusText.setText("● Listening");
                startButton.setText("⏹ Stop Listening");
            } else {
                isListening = false;
                speechRecognizer.stopListening();
                speechRecognizer.cancel();
                statusText.setText("● Stopped");
                startButton.setText("🎤 Start Listening");
            }
        });

        saveButton.setOnClickListener(v -> {
            try {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
                String fileName = "Transcript_" + timestamp + ".txt";
                File file = new File(getFilesDir(), fileName);
                FileWriter writer = new FileWriter(file);
                writer.write(fullTranscript);
                writer.close();
                Toast.makeText(this, "Transcript Saved ✓", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        notesButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NotesActivity.class)));

        // Import button: open file picker for audio files
        importButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Audio File"));
        });
    }

    // ─── Transcription ────────────────────────────────────────────────────────

    private void startTranscription(Uri audioUri, String originalFileName) {
        // Show progress dialog while transcribing
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Transcribing...")
                .setMessage("Processing \"" + originalFileName + "\"\nThis may take a moment.")
                .setCancelable(false)
                .create();
        progressDialog.show();

        statusText.setText("● Transcribing import...");

        transcriptionService.transcribe(audioUri, originalFileName, new GroqTranscriptionService.TranscriptionCallback() {
            @Override
            public void onSuccess(String transcribedText) {
                progressDialog.dismiss();
                statusText.setText("● Ready");
                saveImportedTranscript(originalFileName, transcribedText);
            }

            @Override
            public void onError(String errorMessage) {
                progressDialog.dismiss();
                statusText.setText("● Ready");
                Toast.makeText(MainActivity.this, "Transcription failed: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveImportedTranscript(String originalFileName, String transcribedText) {
        try {
            // Strip audio extension from original name to use as transcript title
            String baseName = originalFileName;
            int dot = baseName.lastIndexOf('.');
            if (dot > 0) baseName = baseName.substring(0, dot);

            // Use timestamp filename on disk, store original name as custom title
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "Transcript_" + timestamp + ".txt";

            File file = new File(getFilesDir(), fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(transcribedText);
            writer.close();

            // Save the original audio filename as the display title
            TitleManager.saveTitle(this, fileName, baseName);

            Toast.makeText(this, "\"" + baseName + "\" transcribed and saved ✓", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String getFileNameFromUri(Uri uri) {
        // Try to get the actual filename from the content resolver
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
            if (result == null) result = "audio_import.mp3";
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}