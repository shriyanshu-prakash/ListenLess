package com.shriyanshuprakash.listenless;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView transcriptText;
    private TextView statusText;
    private Button startButton;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    private boolean isListening = false;

    private String fullTranscript = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        transcriptText = findViewById(R.id.transcriptText);
        statusText = findViewById(R.id.statusText);
        startButton = findViewById(R.id.startButton);

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                statusText.setText("● Listening");
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

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

                ArrayList<String> data =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (data != null && !data.isEmpty()) {

                    String newText = data.get(0);

                    fullTranscript += newText + "\n\n";

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

                ArrayList<String> data =
                        partialResults.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (data != null && !data.isEmpty()) {

                    transcriptText.setText(
                            fullTranscript + data.get(0)
                    );
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
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
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}