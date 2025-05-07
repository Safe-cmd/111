package com.example.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException; // Import SQLiteException
import android.util.Log;

public class DatabaseManager {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private static final String TAG = "DatabaseManager"; // Add TAG for logging

    public DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLiteException { // Throw exception if open fails
        try {
            database = dbHelper.getWritableDatabase();
            Log.d(TAG, "Database opened successfully.");
        } catch (SQLiteException e) {
            Log.e(TAG, "Error opening database.", e);
            throw e; // Re-throw exception
        }
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close(); // Close helper which closes the database
            Log.d(TAG, "Database helper closed.");
        }
        // Setting database variable to null after closing is good practice
        database = null;
    }

    // This method seems redundant given insertSensorData, maybe remove?
    // public long insertData(String name) { ... }

    public long insertSensorData(String temperature, String humidity) {
        if (database == null || !database.isOpen()) {
            Log.e(TAG, "Database is not open. Cannot insert data.");
            return -1;
        }
        ContentValues values = new ContentValues();
        // Use default value if input is null/empty to avoid storing bad data
        values.put(DatabaseHelper.COLUMN_TEMPERATURE, (temperature != null && !temperature.isEmpty()) ? temperature : "N/A");
        values.put(DatabaseHelper.COLUMN_HUMIDITY, (humidity != null && !humidity.isEmpty()) ? humidity : "N/A");
        // Timestamp is added automatically by the database schema (DEFAULT CURRENT_TIMESTAMP)

        long result = -1;
        try {
            result = database.insert(DatabaseHelper.TABLE_NAME, null, values);
            if (result != -1) {
                Log.d(TAG, "Data inserted successfully. Row ID: " + result);
            } else {
                Log.e(TAG, "Failed to insert data.");
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error inserting data.", e);
        }
        return result;
    }

    public Cursor getAllData() {
        if (database == null || !database.isOpen()) {
            Log.e(TAG, "Database is not open. Cannot query all data.");
            return null;
        }
        try {
            // Order by ID descending to get latest first by default in this view too
            return database.query(
                    DatabaseHelper.TABLE_NAME,
                    null, // All columns
                    null, // No WHERE clause
                    null, // No WHERE args
                    null, // No GROUP BY
                    null, // No HAVING
                    DatabaseHelper.COLUMN_ID + " DESC" // Order by ID descending
            );
        } catch (SQLiteException e) {
            Log.e(TAG, "Error querying all data.", e);
            return null;
        }
    }

    /**
     * Gets the latest 'limit' number of sensor data records.
     * @param limit The maximum number of records to retrieve.
     * @return A Cursor containing the latest records, or null if error.
     */
    public Cursor getLatestData(int limit) {
        if (database == null || !database.isOpen()) {
            Log.e(TAG, "Database is not open. Cannot query latest data.");
            return null;
        }
        if (limit <= 0) {
            limit = 10; // Default limit if invalid value passed
        }
        try {
            return database.query(
                    DatabaseHelper.TABLE_NAME,
                    null, // All columns
                    null, // No WHERE clause
                    null, // No WHERE args
                    null, // No GROUP BY
                    null, // No HAVING
                    DatabaseHelper.COLUMN_ID + " DESC", // Order by ID descending to get the latest
                    String.valueOf(limit) // LIMIT clause
            );
        } catch (SQLiteException e) {
            Log.e(TAG, "Error querying latest data.", e);
            return null;
        }
    }
}