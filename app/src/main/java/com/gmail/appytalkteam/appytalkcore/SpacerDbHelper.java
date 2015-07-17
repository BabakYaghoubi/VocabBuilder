package com.gmail.appytalkteam.appytalkcore;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by derwin on 17/07/15.
 */
public class SpacerDbHelper extends SQLiteOpenHelper {
    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "userwords";

    // Contacts table name
//    private static final String TABLE_ = "words";

    // Contacts Table Columns names
    private static final String KEY_IMG = "img";
    private static final String KEY_TEXT = "name";
    private static final String KEY_AUDIO = "audio";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_SCORE = "score";
    private static final String KEY_BOX = "box";
    private static final String KEY_SEEN = "seen";

    public SpacerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String[] locales = {"en","es","fr","ru"};
        for(String locale: locales) {
            String CREATE_CONTACTS_TABLE = "CREATE TABLE " +
                    locale +
                    "(" + KEY_IMG + " TEXT PRIMARY KEY," +
                    KEY_TEXT + " TEXT,"
                    + KEY_AUDIO + " TEXT" +
                    KEY_CATEGORY + " TEXT" +
                    KEY_SCORE + " INTEGER" +
                    KEY_BOX + " INTEGER" +
                    KEY_SEEN + " INTEGER" + ")";
            db.execSQL(CREATE_CONTACTS_TABLE);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String[] locales = {"en","es","fr","ru"};
        for(String locale: locales)
        db.execSQL("DROP TABLE IF EXISTS " + locale);

        onCreate(db);
    }

    // Adding new contact
    public void addWord(UserWord wrd, String language) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_IMG, wrd.getImageLocation());
        values.put(KEY_TEXT, wrd.getWordText());
        values.put(KEY_AUDIO, wrd.getAudioLocation());
        values.put(KEY_CATEGORY, wrd.getCategory());
        values.put(KEY_SCORE, wrd.score);
        values.put(KEY_BOX, wrd.box);
        values.put(KEY_SEEN, wrd.seen);

        // Inserting Row
        db.insert(language, null, values);
        db.close(); // Closing database connection
    }

    public ArrayList<UserWord> getAllWords(String language) {
        ArrayList<UserWord> wordList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + language;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                UserWord word = new UserWord(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getString(3),Integer.parseInt(cursor.getString(4)),Integer.parseInt(cursor.getString(5)),Integer.parseInt(cursor.getString(6)));
                wordList.add(word);
            } while (cursor.moveToNext());
        }

        // return contact list
        return wordList;
    }
}
