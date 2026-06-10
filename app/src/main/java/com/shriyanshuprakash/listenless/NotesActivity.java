package com.shriyanshuprakash.listenless;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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

        final ArrayAdapter<String> adapter =
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

        notesList.setOnItemLongClickListener((parent, view, position, id) -> {

            String fileName = fileNames.get(position);

            new AlertDialog.Builder(NotesActivity.this)
                    .setTitle("Delete Transcript")
                    .setMessage("Delete " + fileName + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {

                        File file =
                                new File(getFilesDir(), fileName);

                        if (file.delete()) {

                            fileNames.remove(position);

                            adapter.notifyDataSetChanged();

                            Toast.makeText(
                                    NotesActivity.this,
                                    "Transcript Deleted",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    NotesActivity.this,
                                    "Delete Failed",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });
    }
}