package com.example.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "MyDatabase.db";
    private static final int DATABASE_VERSION = 2; // 增加版本号以强制更新
    public static final String TABLE_NAME = "sensor_data"; // 修正表名
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_HUMIDITY = "humidity";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TEMPERATURE + " TEXT, " +
                    COLUMN_HUMIDITY + " TEXT, " +
                    COLUMN_TIMESTAMP + " TEXT DEFAULT CURRENT_TIMESTAMP);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        android.util.Log.d("DatabaseHelper", "Table created: " + TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
        android.util.Log.d("DatabaseHelper", "Database upgraded, table dropped and recreated.");
    }
}