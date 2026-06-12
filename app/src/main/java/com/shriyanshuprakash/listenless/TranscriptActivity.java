package com.shriyanshuprakash.listenless;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class TranscriptActivity extends AppCompatActivity {

    private TextView fileNameText;
    private EditText transcriptContent; // Changed from TextView to EditText
    private Button shareButton;

    private String transcriptText = "";
    private String currentFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transcript);

        fileNameText = findViewById(R.id.fileNameText);
        transcriptContent = findViewById(R.id.transcriptContent); // Now an EditText
        shareButton = findViewById(R.id.shareButton);

        currentFileName = getIntent().getStringExtra("fileName");

        // Set display title
        refreshTitle();

        // Load transcript content from file
        try {
            File file = new File(getFilesDir(), currentFileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line);
                content.append("\n");
            }
            reader.close();

            transcriptText = content.toString();
            transcriptContent.setText(transcriptText);

        } catch (Exception e) {
            transcriptContent.setText("Failed to load transcript.");
            e.printStackTrace();
        }

        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Tap title to rename
        fileNameText.setOnClickListener(v -> showRenameDialog());

        // Share button — saves edits first, then shares as a named file
        shareButton.setOnClickListener(v -> {
            saveTranscriptContent();
            try {
                String customTitle = TitleManager.getTitle(this, currentFileName);
                String displayName = (customTitle != null) ? customTitle : currentFileName.replace(".txt", "");
                String safeName = displayName.replaceAll("[\\/:*?\"<>|]", "_").trim();

                File shareDir = new File(getCacheDir(), "share");
                shareDir.mkdirs();
                File[] old = shareDir.listFiles();
                if (old != null) for (File f : old) f.delete();

                File shareFile = new File(shareDir, safeName + ".txt");
                FileWriter writer = new FileWriter(shareFile);
                writer.write(transcriptContent.getText().toString());
                writer.close();

                Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        this, getPackageName() + ".provider", shareFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share Transcript"));
            } catch (Exception e) {
                Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    // Save edited transcript content back to file when leaving the screen
    @Override
    protected void onPause() {
        super.onPause();
        saveTranscriptContent();
    }

    private void saveTranscriptContent() {
        if (currentFileName == null) return;
        try {
            File file = new File(getFilesDir(), currentFileName);
            FileWriter writer = new FileWriter(file, false);
            writer.write(transcriptContent.getText().toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshTitle() {
        String customTitle = TitleManager.getTitle(this, currentFileName);
        if (customTitle != null) {
            fileNameText.setText(customTitle);
        } else {
            if (currentFileName != null) {
                fileNameText.setText(currentFileName.replace(".txt", ""));
            } else {
                fileNameText.setText("Unknown Transcript");
            }
        }
    }

    private void showRenameDialog() {
        EditText input = new EditText(this);
        input.setText(fileNameText.getText()); // Pre-fill with current displayed title

        new AlertDialog.Builder(this)
                .setTitle("Rename Transcript")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = input.getText().toString().trim();

                    if (newTitle.isEmpty()) {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Save title + rename physical file; get back the new filename
                    currentFileName = TitleManager.saveTitle(this, currentFileName, newTitle);

                    // Update the title shown in this screen immediately
                    fileNameText.setText(newTitle);

                    // Signal to the list activity that a rename happened
                    setResult(RESULT_OK);

                    Toast.makeText(this, "Renamed ✓", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}