package com.shriyanshuprakash.listenless;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;

public class NotesActivity extends AppCompatActivity {

    private ListView notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        notesList = findViewById(R.id.notesList);

        File folder = getFilesDir();

        File[] files = folder.listFiles();

        ArrayList<String> fileNames = new ArrayList<>();

        if (files != null) {

            for (File file : files) {

                // Only show transcript files
                if (file.getName().startsWith("Transcript_")) {

                    fileNames.add(file.getName());
                }
            }
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        fileNames
                );

        notesList.setAdapter(adapter);

        notesList.setOnItemClickListener((parent, view, position, id) -> {

            String fileName = fileNames.get(position);

            Intent intent =
                    new Intent(
                            NotesActivity.this,
                            TranscriptActivity.class);

            intent.putExtra("fileName", fileName);

            startActivity(intent);
        });
    }
}