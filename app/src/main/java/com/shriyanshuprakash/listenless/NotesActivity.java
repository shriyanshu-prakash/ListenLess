package com.shriyanshuprakash.listenless;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NotesActivity extends AppCompatActivity {

    private static final int REQUEST_TRANSCRIPT = 1001;

    private ListView notesList;
    private LinearLayout normalHeader;
    private LinearLayout selectionHeader;
    private LinearLayout selectionActionBar;
    private TextView selectionCountText;
    private Button cancelSelectionButton;
    private Button newNoteButton;
    private Button renameSelectedButton;
    private Button shareSelectedButton;
    private Button deleteSelectedButton;

    private ArrayList<String> fileNames = new ArrayList<>();
    private Set<Integer> selectedPositions = new HashSet<>();
    private boolean isSelectionMode = false;
    private NoteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        notesList             = findViewById(R.id.notesList);
        normalHeader          = findViewById(R.id.normalHeader);
        selectionHeader       = findViewById(R.id.selectionHeader);
        selectionActionBar    = findViewById(R.id.selectionActionBar);
        selectionCountText    = findViewById(R.id.selectionCountText);
        cancelSelectionButton = findViewById(R.id.cancelSelectionButton);
        renameSelectedButton  = findViewById(R.id.renameSelectedButton);
        shareSelectedButton   = findViewById(R.id.shareSelectedButton);
        deleteSelectedButton  = findViewById(R.id.deleteSelectedButton);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        newNoteButton = findViewById(R.id.newNoteButton);
        newNoteButton.setOnClickListener(v -> createNewNote());

        loadFiles();
        adapter = new NoteAdapter();
        notesList.setAdapter(adapter);

        // Single tap: open transcript or toggle selection
        notesList.setOnItemClickListener((parent, view, position, id) -> {
            if (isSelectionMode) {
                toggleSelection(position);
            } else {
                String fileName = fileNames.get(position);
                Intent intent = new Intent(NotesActivity.this, TranscriptActivity.class);
                intent.putExtra("fileName", fileName);
                startActivityForResult(intent, REQUEST_TRANSCRIPT);
            }
        });

        // Long press: enter selection mode with this item selected
        notesList.setOnItemLongClickListener((parent, view, position, id) -> {
            if (!isSelectionMode) enterSelectionMode();
            toggleSelection(position);
            return true;
        });

        cancelSelectionButton.setOnClickListener(v -> exitSelectionMode());
        renameSelectedButton.setOnClickListener(v -> renameSelected());
        shareSelectedButton.setOnClickListener(v -> shareSelected());
        deleteSelectedButton.setOnClickListener(v -> confirmBulkDelete());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TRANSCRIPT) {
            loadFiles();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFiles();
        adapter.notifyDataSetChanged();
    }

    // ─── New Note ─────────────────────────────────────────────────────────────

    private void createNewNote() {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "Transcript_" + timestamp + ".txt";
            File file = new File(getFilesDir(), fileName);
            file.createNewFile(); // empty file

            // Open it straight into TranscriptActivity
            Intent intent = new Intent(NotesActivity.this, TranscriptActivity.class);
            intent.putExtra("fileName", fileName);
            startActivityForResult(intent, REQUEST_TRANSCRIPT);
        } catch (Exception e) {
            Toast.makeText(this, "Could not create note", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ─── File Loading ─────────────────────────────────────────────────────────

    private void loadFiles() {
        fileNames.clear();
        File folder = getFilesDir();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".txt") && !name.equals("note_titles.json")) {
                    fileNames.add(name);
                }
            }
        }
        fileNames.sort((a, b) -> b.compareTo(a));
    }

    // ─── Selection Mode ───────────────────────────────────────────────────────

    private void enterSelectionMode() {
        isSelectionMode = true;
        normalHeader.setVisibility(View.GONE);
        selectionHeader.setVisibility(View.VISIBLE);
        selectionActionBar.setVisibility(View.VISIBLE);
        updateSelectionCount();
        adapter.notifyDataSetChanged();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedPositions.clear();
        normalHeader.setVisibility(View.VISIBLE);
        selectionHeader.setVisibility(View.GONE);
        selectionActionBar.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        if (selectedPositions.isEmpty()) {
            exitSelectionMode();
            return;
        }
        updateSelectionCount();
        adapter.notifyDataSetChanged();
    }

    private void updateSelectionCount() {
        int count = selectedPositions.size();
        selectionCountText.setText(count + " selected");
        // Show Rename only when exactly 1 item is selected
        renameSelectedButton.setVisibility(count == 1 ? View.VISIBLE : View.GONE);
    }

    // ─── Rename ───────────────────────────────────────────────────────────────

    private void renameSelected() {
        if (selectedPositions.size() != 1) return;
        int position = selectedPositions.iterator().next();
        String fileName = fileNames.get(position);

        EditText input = new EditText(this);
        String currentTitle = TitleManager.getTitle(this, fileName);
        input.setText(currentTitle != null ? currentTitle : fileName.replace(".txt", ""));

        new AlertDialog.Builder(this)
                .setTitle("Rename Transcript")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = input.getText().toString().trim();
                    if (newTitle.isEmpty()) {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String newFileName = TitleManager.saveTitle(this, fileName, newTitle);
                    fileNames.set(position, newFileName);
                    exitSelectionMode();
                    Toast.makeText(this, "Renamed ✓", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Bulk Delete ──────────────────────────────────────────────────────────

    private void confirmBulkDelete() {
        int count = selectedPositions.size();
        String message = count == 1 ? "Delete 1 transcript?" : "Delete " + count + " transcripts?";
        new AlertDialog.Builder(this)
                .setTitle("Delete Transcripts")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> bulkDelete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void bulkDelete() {
        ArrayList<Integer> sorted = new ArrayList<>(selectedPositions);
        sorted.sort((a, b) -> b - a);
        int deleted = 0;
        for (int position : sorted) {
            File file = new File(getFilesDir(), fileNames.get(position));
            if (file.delete()) {
                fileNames.remove(position);
                deleted++;
            }
        }
        exitSelectionMode();
        adapter.notifyDataSetChanged();
        String msg = deleted == 1 ? "1 transcript deleted" : deleted + " transcripts deleted";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ─── Share ────────────────────────────────────────────────────────────────

    private String resolveDisplayName(String fileName) {
        String customTitle = TitleManager.getTitle(this, fileName);
        if (customTitle != null) return customTitle;
        String timestamp = TitleManager.getTimestamp(this, fileName);
        if (timestamp != null) return timestamp;
        try {
            String stripped = fileName.replace("Transcript_", "").replace(".txt", "");
            Date date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).parse(stripped);
            return new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return fileName.replace(".txt", "");
        }
    }

    private File buildShareFile(String fileName, File shareDir) throws Exception {
        String safeName = resolveDisplayName(fileName).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        File shareFile = new File(shareDir, safeName + ".txt");
        BufferedReader reader = new BufferedReader(new FileReader(new File(getFilesDir(), fileName)));
        FileWriter writer = new FileWriter(shareFile);
        String line;
        while ((line = reader.readLine()) != null) { writer.write(line); writer.write("\n"); }
        reader.close();
        writer.close();
        return shareFile;
    }

    private File prepareShareDir() {
        File shareDir = new File(getCacheDir(), "share");
        shareDir.mkdirs();
        File[] old = shareDir.listFiles();
        if (old != null) for (File f : old) f.delete();
        return shareDir;
    }

    private void shareSelected() {
        ArrayList<Integer> sorted = new ArrayList<>(selectedPositions);
        sorted.sort(Integer::compareTo);
        if (sorted.size() == 1) {
            shareSingleFile(fileNames.get(sorted.get(0)));
        } else {
            shareMultipleFiles(sorted);
        }
    }

    private void shareSingleFile(String fileName) {
        try {
            File shareDir = prepareShareDir();
            File shareFile = buildShareFile(fileName, shareDir);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", shareFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Transcript"));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void shareMultipleFiles(ArrayList<Integer> positions) {
        try {
            File shareDir = prepareShareDir();
            ArrayList<Uri> uris = new ArrayList<>();
            for (int position : positions) {
                File shareFile = buildShareFile(fileNames.get(position), shareDir);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", shareFile);
                uris.add(uri);
            }
            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("text/plain");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Transcripts"));
        } catch (Exception e) {
            Toast.makeText(this, "Share failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // ─── Adapter ──────────────────────────────────────────────────────────────

    private class NoteAdapter extends ArrayAdapter<String> {

        NoteAdapter() {
            super(NotesActivity.this, R.layout.item_note, fileNames);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_note, parent, false);
            }

            String fileName = fileNames.get(position);

            TextView titleText = convertView.findViewById(R.id.noteTitleText);
            TextView dateText  = convertView.findViewById(R.id.noteDateText);
            CheckBox checkbox  = convertView.findViewById(R.id.noteCheckbox);
            View     accentBar = convertView.findViewById(R.id.accentBar);
            TextView chevron   = convertView.findViewById(R.id.noteChevron);
            View     root      = convertView.findViewById(R.id.noteItemRoot);

            // Title: custom name if set, else parse from filename
            String customTitle = TitleManager.getTitle(NotesActivity.this, fileName);
            String displayTitle;
            if (customTitle != null) {
                displayTitle = customTitle;
            } else {
                try {
                    String stripped = fileName.replace("Transcript_", "").replace(".txt", "");
                    Date date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).parse(stripped);
                    displayTitle = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date);
                } catch (Exception e) {
                    displayTitle = fileName.replace(".txt", "");
                }
            }

            // Date: try TitleManager first (survives renames), then parse from filename directly
            String displayDate = TitleManager.getTimestamp(NotesActivity.this, fileName);
            if (displayDate == null) {
                try {
                    String stripped = fileName.replace("Transcript_", "").replace(".txt", "");
                    Date date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).parse(stripped);
                    displayDate = new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(date);
                } catch (Exception e) {
                    displayDate = "";
                }
            }

            titleText.setText(displayTitle);
            dateText.setText(displayDate);

            boolean selected = selectedPositions.contains(position);

            if (isSelectionMode) {
                checkbox.setVisibility(View.VISIBLE);
                checkbox.setChecked(selected);
                accentBar.setVisibility(View.GONE);
                chevron.setVisibility(View.GONE);
                root.setBackgroundColor(selected ? 0xFF1A1500 : 0xFF0A0A0A);
            } else {
                checkbox.setVisibility(View.GONE);
                accentBar.setVisibility(View.VISIBLE);
                chevron.setVisibility(View.VISIBLE);
                root.setBackgroundColor(0x00000000);
            }

            return convertView;
        }
    }
}