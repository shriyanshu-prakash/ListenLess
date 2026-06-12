package com.shriyanshuprakash.listenless;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TitleManager {

    private static final String TITLES_FILE     = "note_titles.json";
    private static final String TIMESTAMPS_FILE = "note_timestamps.json";

    // ─── JSON helpers ─────────────────────────────────────────────────────────

    private static JSONObject loadJson(Context context, String fileName) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            if (!file.exists()) return new JSONObject();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) content.append(line);
            reader.close();
            return new JSONObject(content.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    private static void saveJson(Context context, String fileName, JSONObject json) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(json.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Renames the physical .txt file to match the new title, migrates both
     * the title and timestamp JSON entries to the new filename key.
     * Returns the new filename, or the original if rename failed.
     */
    public static String saveTitle(Context context, String fileName, String title) {
        try {
            String safeTitle  = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            String newFileName = safeTitle + ".txt";

            File oldFile = new File(context.getFilesDir(), fileName);
            File newFile = new File(context.getFilesDir(), newFileName);

            if (!fileName.equals(newFileName) && oldFile.exists() && !newFile.exists()) {
                oldFile.renameTo(newFile);
            } else if (newFile.exists() && !fileName.equals(newFileName)) {
                newFileName = fileName; // collision — keep original filename
            }

            // Migrate title entry
            JSONObject titles = loadJson(context, TITLES_FILE);
            titles.remove(fileName);
            titles.put(newFileName, title);
            saveJson(context, TITLES_FILE, titles);

            // Migrate (or create) timestamp entry so it survives the rename
            JSONObject timestamps = loadJson(context, TIMESTAMPS_FILE);
            String existingTs = timestamps.optString(fileName, null);
            if (existingTs == null) {
                // First rename — extract timestamp from the original filename
                existingTs = parseTimestampFromFileName(fileName);
            }
            if (existingTs != null) {
                timestamps.remove(fileName);
                timestamps.put(newFileName, existingTs);
                saveJson(context, TIMESTAMPS_FILE, timestamps);
            }

            return newFileName;

        } catch (Exception e) {
            e.printStackTrace();
            return fileName;
        }
    }

    /** Returns the custom display title for a file, or null if none set. */
    public static String getTitle(Context context, String fileName) {
        try {
            JSONObject json = loadJson(context, TITLES_FILE);
            if (json.has(fileName)) return json.getString(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a human-readable timestamp string for a file (e.g. "Jun 10, 2026 · 23:09").
     * Works even after the file has been renamed, by reading from the timestamps store.
     * Falls back to parsing the current filename if no stored timestamp exists.
     */
    public static String getTimestamp(Context context, String fileName) {
        try {
            JSONObject timestamps = loadJson(context, TIMESTAMPS_FILE);
            if (timestamps.has(fileName)) return timestamps.getString(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Fallback: try to parse from filename directly
        return parseTimestampFromFileName(fileName);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    /** Tries to extract a readable "MMM d, yyyy · HH:mm" string from a
     *  Transcript_yyyy-MM-dd_HH-mm-ss.txt filename. Returns null if it can't. */
    private static String parseTimestampFromFileName(String fileName) {
        try {
            String stripped = fileName.replace("Transcript_", "").replace(".txt", "");
            SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            Date date = parseFormat.parse(stripped);
            return new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return null;
        }
    }
}