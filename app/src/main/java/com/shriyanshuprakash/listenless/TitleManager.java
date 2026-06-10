package com.shriyanshuprakash.listenless;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class TitleManager {

    private static final String FILE_NAME = "note_titles.json";

    private static JSONObject loadJson(Context context) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
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

    private static void saveJson(Context context, JSONObject json) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            FileWriter writer = new FileWriter(file);
            writer.write(json.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Renames the physical .txt file to match the new title and updates the JSON map.
     * Returns the new filename so callers can update their reference, or the original
     * fileName if the rename failed.
     */
    public static String saveTitle(Context context, String fileName, String title) {
        try {
            // Sanitize title for use as a filename (replace chars that are invalid on Android)
            String safeTitle = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
            String newFileName = safeTitle + ".txt";

            File oldFile = new File(context.getFilesDir(), fileName);
            File newFile = new File(context.getFilesDir(), newFileName);

            // Only rename if the name actually changed and the new file doesn't already exist
            if (!fileName.equals(newFileName) && oldFile.exists() && !newFile.exists()) {
                oldFile.renameTo(newFile);
            } else if (newFile.exists() && !fileName.equals(newFileName)) {
                // Target already exists — keep old file, don't overwrite another transcript
                // Just store the title against the original filename instead
                newFileName = fileName;
            }

            // Update JSON: remove old key, store title under new filename
            JSONObject json = loadJson(context);
            json.remove(fileName);
            json.put(newFileName, title);
            saveJson(context, json);

            return newFileName;

        } catch (Exception e) {
            e.printStackTrace();
            return fileName;
        }
    }

    public static String getTitle(Context context, String fileName) {
        try {
            JSONObject json = loadJson(context);
            if (json.has(fileName)) return json.getString(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}