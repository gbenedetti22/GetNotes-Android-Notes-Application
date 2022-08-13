package com.unipi.sam.getnotes;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.unipi.sam.getnotes.note.BlackboardView;
import com.unipi.sam.getnotes.note.EraserDialog;
import com.unipi.sam.getnotes.note.StylusStyleDialog;
import com.unipi.sam.getnotes.table.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocalDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "storage.db";
    private static final int DB_VERSION = 1;
    private int currentFolderID;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
    private final String FILES_TABLE_NAME = "Files";
    private static final String CREATE_FILES_TABLE_QUERY =
            "CREATE TABLE \"Files\" (\n" +
                    "\t\"id\"\tINTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "\t\"parent_folder\"\tINTEGER NOT NULL,\n" +
                    "\t\"name\"\tvarchar(255) NOT NULL,\n" +
                    "\t\"content\"\tTEXT,\n" +
                    "\t\"type\"\tvarchar(255) NOT NULL DEFAULT 'FILE',\n" +
                    "\t\"date\"\tTEXT\n" +
                    ");";

    private static final String CREATE_ROOT_FOLDER_QUERY = "INSERT INTO Files(id, parent_folder, name, type) VALUES(1, 0, \"root\", \"FOLDER\")";

    public enum SORTING_OPTIONS {
        NAME,
        DATE,
        TYPE
    }
    private SORTING_OPTIONS sortingOptions;
    private static final String SHARED_PREFERENCE_FILENAME = "UNIPI_GETNOTES_PREFERENCE_FILE";

    private String userId;
    private String name;
    private User.Info info;
    private SharedPreferences preferences;

    public static int ROOT_ID = 1;

    public LocalDatabase(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        currentFolderID = ROOT_ID;
        if(context == null) throw new NullPointerException("Context cannot be null");

        preferences = context.getSharedPreferences(SHARED_PREFERENCE_FILENAME, Context.MODE_PRIVATE);

        this.userId = preferences.getString("id", null);
        this.name = preferences.getString("name", null);
        String sortOptions = preferences.getString("sortingOptions", null);
        if(sortOptions == null)
            setSortingOptions(SORTING_OPTIONS.NAME);
        else {
            switch (sortOptions){
                case "NAME": sortingOptions = SORTING_OPTIONS.NAME; break;
                case "DATE": sortingOptions = SORTING_OPTIONS.DATE; break;
                case "TYPE": sortingOptions = SORTING_OPTIONS.TYPE; break;
            }
        }

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FILES_TABLE_QUERY);
        db.execSQL(CREATE_ROOT_FOLDER_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String USER_TABLE_NAME = "User";
        db.execSQL("DROP TABLE IF EXISTS " + FILES_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + USER_TABLE_NAME);
        onCreate(db);
    }

    public void saveEraserSize(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("eraser", value);
        editor.apply();
    }
    public int getEraserSize() {
        return preferences.getInt("eraser", EraserDialog.ERASER_DEFAULT_SIZE);
    }

    public void saveStrokeWidth(int strokeWidth) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("stroke", strokeWidth);
        editor.apply();
    }

    public int getStrokeWidth() {
        return preferences.getInt("stroke", StylusStyleDialog.DEFAULT_STROKE_WIDTH);
    }

    public int getCurrentColor() {
        return preferences.getInt("color", Color.BLACK);
    }

    public void saveColor(int color) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("color", color);
        editor.apply();
    }

    public BlackboardView.TOOL getCurrentInstrument() {
        String tool = preferences.getString("tool", BlackboardView.TOOL.NONE.name());
        return BlackboardView.TOOL.valueOf(tool);
    }

    public void saveCurrentInstrument(BlackboardView.TOOL tool) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("tool", tool.name());
        editor.apply();
    }

    public void setUser(@NonNull User u) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("id", u.getId());
        editor.putString("name", u.getName());
        editor.apply();
    }

    public String getUserId() {
        return userId;
    }

    public User.Info getUserPairInfo() {
        if (info == null) {
            info = new User.Info(userId, name);
        }

        return info;
    }

    public String getUserName() {
        return name;
    }

    public boolean userExist() {
        return userId != null && name != null;
    }

    public void removeCurrentUser() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("id");
        editor.remove("name");
        editor.apply();
    }

    public void clear() {
        onUpgrade(getWritableDatabase(), DB_VERSION, DB_VERSION);
    }

    public void setCurrentFolder(int currentFolderID) {
        this.currentFolderID = currentFolderID;
    }

    public String now() {
        return sdf.format(new Date());
    }
    public int addNote(String name, String content) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("parent_folder", currentFolderID);
        cv.put("name", name);
        if (content != null)
            cv.put("content", content);

        cv.put("type", "NOTE");
        cv.put("date", sdf.format(new Date()));
        if (db.insert(FILES_TABLE_NAME, null, cv) == -1) {
            throw new Exception("error on insert note");
        }

        db.close();
        return getPrimaryKeyOfLastInsertedFile();
    }

    public int addNote(String name) throws Exception {
        return addNote(name, null);
    }

    public void createFolder(String name) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("parent_folder", currentFolderID);
        cv.put("type", "FOLDER");
        cv.put("date", sdf.format(new Date()));
        long id = db.insert(FILES_TABLE_NAME, null, cv);
        if (id == -1) throw new Exception("error on creating new folder");
        db.close();
    }

    public Cursor getFiles() {
        return getFiles(sortingOptions);
    }

    public void setSortingOptions(SORTING_OPTIONS sortingOptions) {
        this.sortingOptions = sortingOptions;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("sortingOptions", sortingOptions.name());
        editor.apply();
    }

    public SORTING_OPTIONS getSortingOptions() {
        return sortingOptions;
    }

    private Cursor getFiles(SORTING_OPTIONS options) {
        switch (options) {
            case NAME: return getReadableDatabase().rawQuery("SELECT * FROM " + FILES_TABLE_NAME + " WHERE parent_folder = " + currentFolderID + " ORDER BY name", null);
            case DATE: return getReadableDatabase().rawQuery("SELECT * FROM " + FILES_TABLE_NAME + " WHERE parent_folder = " + currentFolderID + " ORDER BY date", null);
            case TYPE: return getReadableDatabase().rawQuery("SELECT * FROM " + FILES_TABLE_NAME + " WHERE parent_folder = " + currentFolderID + " ORDER BY type", null);
        }

        throw new IllegalArgumentException("sort must be: name, date or type");
    }

    private int getPrimaryKeyOfLastInsertedFile() {
        Cursor cursor = getFiles(SORTING_OPTIONS.DATE);
        int lastPK = getLastPK(cursor);
        cursor.close();
        return lastPK;
    }

    public void rename(int id, String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        db.update(FILES_TABLE_NAME, cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public void updateNoteContent(int id, String content) {
        SQLiteDatabase db = getWritableDatabase();
//        db.execSQL("UPDATE Files SET content = \"" + content + "\" WHERE id = " + id);
        ContentValues cv = new ContentValues();
        cv.put("content", content);
        db.update(FILES_TABLE_NAME, cv, "id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public String getNoteContent(int id) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor c = database.rawQuery("SELECT * FROM Files WHERE id = " + id, null);
        c.moveToPosition(0);

        if (c.isNull(3)) {
            c.close();
            database.close();
            return null;
        }

        String content = c.getString(3);
        c.close();
        database.close();
        return content;
    }

    private int getLastPK(Cursor c) {
        c.moveToLast();
        int id = c.getInt(0);
        c.close();
        return id;
    }
}
