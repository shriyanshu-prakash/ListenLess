package com.shriyanshuprakash.listenless;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class TranscriptActivity extends AppCompatActivity {

    private TextView fileNameText;
    private TextView transcriptContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcript);

        fileNameText = findViewById(R.id.fileNameText);
        transcriptContent = findViewById(R.id.transcriptContent);

        String fileName =
                getIntent().getStringExtra("fileName");

        fileNameText.setText(fileName);

        try {

            File file =
                    new File(getFilesDir(), fileName);

            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(file));

            StringBuilder content =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {

                content.append(line);
                content.append("\n");
            }

            reader.close();

            transcriptContent.setText(content.toString());

        } catch (Exception e) {

            transcriptContent.setText(
                    "Failed to load transcript."
            );

            e.printStackTrace();
        }
    }
}