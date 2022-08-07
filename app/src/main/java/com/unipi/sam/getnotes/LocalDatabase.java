package com.unipi.sam.getnotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.unipi.sam.getnotes.table.User;

public class LocalDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "storage.db";
    private static final int DB_VERSION = 1;
    private int currentFolderID;
    private static final String FILES_TABLE_NAME = "Files";
    private static final String CREATE_FILES_TABLE_QUERY =
            "CREATE TABLE \"Files\" (\n" +
                    "\t\"id\"\tINTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "\t\"parent_folder\"\tINTEGER NOT NULL,\n" +
                    "\t\"name\"\tvarchar(255) NOT NULL,\n" +
                    "\t\"content\"\tTEXT,\n" +
                    "\t\"type\"\tvarchar(255) NOT NULL DEFAULT 'FILE'\n" +
                    ");";

    private static final String CREATE_ROOT_FOLDER_QUERY = "INSERT INTO Files(id, parent_folder, name, type) VALUES(1, 0, \"root\", \"FOLDER\")";
    public static User currentUser;

    public LocalDatabase(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        currentFolderID = 1;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FILES_TABLE_QUERY);
        db.execSQL(CREATE_ROOT_FOLDER_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FILES_TABLE_NAME);
        onCreate(db);
    }

    public void clear() {
        onUpgrade(getWritableDatabase(), DB_VERSION, DB_VERSION);
    }

    public void setCurrentFolder(int currentFolderID) {
        this.currentFolderID = currentFolderID;
    }

    public int addNote(String name, String content) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("parent_folder", currentFolderID);
        cv.put("name", name);
        if (content != null)
            cv.put("content", content);

        cv.put("type", "NOTE");
        if (db.insert(FILES_TABLE_NAME, null, cv) == -1) {
            throw new Exception("error on insert note");
        }

        return getPrimaryKeyOfLastInsertedFile();
    }

    public int addNote(String name) throws Exception {
        return addNote(name, null);
    }

    public int createFolder(String name) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("parent_folder", currentFolderID);
        cv.put("type", "FOLDER");
        long id = db.insert(FILES_TABLE_NAME, null, cv);
        if (id == -1) throw new Exception("error on creating new folder");
        return getPrimaryKeyOfLastInsertedFile();
    }

    public Cursor getFiles() {
        return getReadableDatabase().rawQuery("SELECT * FROM " + FILES_TABLE_NAME + " WHERE parent_folder = " + currentFolderID, null);
    }

    private int getPrimaryKeyOfLastInsertedFile() {
        return getLastPK(getFiles());
    }

    public void updateNoteContent(int id, String content) {
        getWritableDatabase().execSQL("UPDATE Files SET content = \""+ content + "\" WHERE id = " + id);
    }

    public String getNoteContent(int id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM Files WHERE id = " + id, null);
        c.moveToPosition(0);

        if(c.isNull(3)) {
            c.close();
            return null;
        }

        String content = c.getString(3);
        c.close();
        return content;
    }

    private int getLastPK(Cursor c) {
        c.moveToLast();
        int id = c.getInt(0);
        c.close();
        return id;
    }
}
