package com.example.sql;

// --- Keep existing imports ---
import android.database.Cursor;
import android.database.sqlite.SQLiteException; // Import SQLiteException
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.os.Handler; // Import Handler
import android.os.Looper; // Import Looper
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

import cn.com.newland.nle_sdk.responseEntity.SensorInfo;
import cn.com.newland.nle_sdk.responseEntity.base.BaseResponseEntity;
import cn.com.newland.nle_sdk.util.NCallBack;
import cn.com.newland.nle_sdk.util.NetWorkBusiness;

public class MainActivity extends AppCompatActivity {

    String wz = "http://www.nlecloud.com/"; // Consider using HTTPS: "https://www.nlecloud.com/"
    String token ="4A8B0A6C29C5F5BEC0FCA065A4D035A747B05C8206EB3B58E7BEC21C7A12857C22588F701C8953C10000BABD54CC870F73BDD2223937FFB8EB793A5119149C81C3A69EF58F6E14EA1584C5CC52277C3AD5B3F816DB3DE4E8D55E91CF0844A3A32E0B187F5F67EDCC243BDB8FC2E19E96403C8DD6C26D41A2BCC23E4D1DD7FB6D9BCE47B3B66741BE46367CB693E05D0A200D18DCCF2CAFA9CF3072B0CA0509CCF6DA7227BAB427DEB874EEEEE8183B2824C3CCA38AE2413F446ED1539662298D1CD9F1D50C47C14EC1661CD7C75697151EB278FD321377DD11FBD79F2385B32D"; // *** IMPORTANT: Replace with your actual token! ***
    String wd = "", sd = ""; // Keep as empty strings initially
    NetWorkBusiness netWorkBusiness;
    TextView textView1, textView2;
    // --- Removed unused variables: thread, state, wd_1, sd_1 ---
    // Thread thread;
    // boolean state;
    // Double wd_1,sd_1;

    private DatabaseManager dbManager;
    private Handler uiHandler; // Handler for UI updates
    private Thread sensorFetchingThread; // Reference to background thread
    private volatile boolean isRunning = true; // Flag to control the thread

    private static final String TAG = "MainActivity"; // Add TAG for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        textView1 = findViewById(R.id.wd_2);
        textView2 = findViewById(R.id.sd_2);
        uiHandler = new Handler(Looper.getMainLooper()); // Initialize Handler

        // *** IMPORTANT: Replace "YOUR_NLE_TOKEN" ***
        if (token.equals("YOUR_NLE_TOKEN")) {
            Log.e(TAG, "Please replace 'YOUR_NLE_TOKEN' with your actual NLE Cloud token!");
            Toast.makeText(this, "请替换 NLE Cloud Token!", Toast.LENGTH_LONG).show();
            // Handle error, maybe return or disable functionality
        }
        netWorkBusiness = new NetWorkBusiness(token, wz);

        // Initialize and open database
        dbManager = new DatabaseManager(this);
        try {
            dbManager.open();
            Log.d(TAG, "Database opened in onCreate.");
            // Load initial history right after opening DB
            loadAndDisplayHistory();
        } catch (SQLiteException e) {
            Log.e(TAG, "Failed to open database on create.", e);
            Toast.makeText(this, "数据库错误", Toast.LENGTH_SHORT).show();
            // Handle error, maybe disable features relying on DB
        }

        // Start the background thread
        startSensorFetchingThread();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View container = findViewById(R.id.main); // Assuming RelativeLayout is the one needing padding
            container.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Removed database query and UI update from here ---
        // They should happen *after* data is fetched or loaded from DB initially.
    }

    private void startSensorFetchingThread() {
        isRunning = true;
        sensorFetchingThread = new Thread(() -> {
            while (isRunning) {
                try {
                    // Fetch data
                    fetchTemperatureData();
                    fetchHumidityData();

                    // Wait before next cycle
                    Thread.sleep(1000); // Fetch every 10 seconds
                    Log.d("SensorThread", "Looping...");
                } catch (InterruptedException e) {
                    Log.w("SensorThread", "Thread interrupted, stopping.");
                    Thread.currentThread().interrupt();
                    isRunning = false;
                } catch (Exception e) {
                    Log.e("SensorThread", "Error in thread loop: " + e.getMessage(), e);
                    try { Thread.sleep(30000); } catch (InterruptedException ie) { Thread.currentThread().interrupt();}
                }
            }
            Log.d("SensorThread", "Thread finished.");
        });
        sensorFetchingThread.start();
    }


    private void fetchTemperatureData() {
        if (netWorkBusiness == null) return;
        netWorkBusiness.getSensor("1205414", "wendu", new NCallBack<BaseResponseEntity<SensorInfo>>(getApplicationContext()) {
            @Override
            protected void onResponse(BaseResponseEntity<SensorInfo> response) {
                if (response != null) {
                    Log.d("Network", "Temp Response Status: " + response.getStatus() + ", Msg: " + response.getMsg());
                    if (response.getResultObj() != null && response.getResultObj().getValue() != null) {
                        final String newWd = response.getResultObj().getValue();
                        Log.d("Network", "Got Temperature: " + newWd);

                        // Update global variable only if needed
                        if (!newWd.isEmpty() && !newWd.equals(wd)) {
                            wd = newWd; // Update the class member variable
                            uiHandler.post(() -> {
                                Log.d("UI Update", "Updating Temperature UI: " + wd);
                                textView1.setText(wd + "°C"); // Add unit
                            });
                            // *** Insert data AFTER getting a valid value ***
                            insertAndRefreshHistory(wd, sd); // Use current 'wd' and latest known 'sd'
                        } else if (newWd.isEmpty()){
                            Log.w("Network", "Temperature value received is empty.");
                            // Optionally update UI to show empty/error state
                            // uiHandler.post(() -> textView1.setText("温度 N/A"));
                        }
                    } else {
                        Log.e("Network Error", "Temperature response invalid. ResultObj or Value is null.");
                        uiHandler.post(() -> textView1.setText("温度数据无效"));
                    }
                } else {
                    Log.e("Network Error", "Temperature response object is null.");
                    uiHandler.post(() -> textView1.setText("温度响应错误"));
                }
            }


        });
    }

    private void fetchHumidityData() {
        if (netWorkBusiness == null) return;
        netWorkBusiness.getSensor("1205414", "shidu", new NCallBack<BaseResponseEntity<SensorInfo>>(getApplicationContext()) {
            @Override
            protected void onResponse(BaseResponseEntity<SensorInfo> response) {
                if (response != null) {
                    Log.d("Network", "Humidity Response Status: " + response.getStatus() + ", Msg: " + response.getMsg());
                    if (response.getResultObj() != null && response.getResultObj().getValue() != null) {
                        final String newSd = response.getResultObj().getValue();
                        Log.d("Network", "Got Humidity: " + newSd);

                        if (!newSd.isEmpty() && !newSd.equals(sd)) {
                            sd = newSd; // Update the class member variable
                            uiHandler.post(() -> {
                                Log.d("UI Update", "Updating Humidity UI: " + sd);
                                textView2.setText(sd + "%"); // Add unit
                            });
                            // *** Insert data AFTER getting a valid value ***
                            insertAndRefreshHistory(wd, sd); // Use current 'sd' and latest known 'wd'
                        } else if (newSd.isEmpty()){
                            Log.w("Network", "Humidity value received is empty.");
                            // uiHandler.post(() -> textView2.setText("湿度 N/A"));
                        }
                    } else {
                        Log.e("Network Error", "Humidity response invalid. ResultObj or Value is null.");
                        uiHandler.post(() -> textView2.setText("湿度数据无效"));
                    }
                } else {
                    Log.e("Network Error", "Humidity response object is null.");
                    uiHandler.post(() -> textView2.setText("湿度响应错误"));
                }
            }


        });
    }

    // Helper method to insert data and refresh history display
    private void insertAndRefreshHistory(final String tempToInsert, final String humToInsert) {
        // Only insert if we have at least one valid value (optional, depends on requirement)
        // if ((tempToInsert == null || tempToInsert.isEmpty()) && (humToInsert == null || humToInsert.isEmpty())) {
        //    Log.d(TAG,"Skipping insert, both values are invalid/empty.");
        //    return;
        // }

        new Thread(() -> {
            if (dbManager != null) {
                // Pass the *latest known values* of both temp and humidity
                long result = dbManager.insertSensorData(
                        (tempToInsert != null && !tempToInsert.isEmpty()) ? tempToInsert : "N/A",
                        (humToInsert != null && !humToInsert.isEmpty()) ? humToInsert : "N/A"
                );
                if (result != -1) {
                    Log.d("Database", "Sensor data inserted via helper, ID: " + result);
                    // Refresh history display on UI thread
                    loadAndDisplayHistory(); // Reload history after insert
                } else {
                    Log.e("Database", "Sensor data insertion failed via helper.");
                }
            } else {
                Log.e("Database", "dbManager is null in insertAndRefreshHistory");
            }
        }).start();
    }


    // Method to load and display history
    private void loadAndDisplayHistory() {
        new Thread(() -> {
            final StringBuilder historyBuilder = new StringBuilder();
            Cursor cursor = null;
            try {
                if (dbManager != null) {
                    cursor = dbManager.getLatestData(10); // Get latest 10
                    if (cursor != null && cursor.moveToFirst()) {
                        Log.d("Database", "Loading history data...");
                        do {
                            String temp = "N/A";
                            String hum = "N/A";
                            String timestamp = "N/A";
                            try { // Add inner try-catch for potential column errors
                                temp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TEMPERATURE));
                                hum = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HUMIDITY));
                                timestamp = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP));
                            } catch (IllegalArgumentException e){
                                Log.e("Database", "Error getting column index in cursor loop.", e);
                                // Continue to next record if one column fails
                            }

                            historyBuilder.append(String.format(Locale.getDefault(),
                                    "%s - T: %s°C, H: %s%%\n", // Concise format
                                    timestamp,
                                    (temp == null || temp.equalsIgnoreCase("N/A")) ? "--" : temp, // Handle N/A display
                                    (hum == null || hum.equalsIgnoreCase("N/A")) ? "--" : hum
                            ));

                        } while (cursor.moveToNext());
                        Log.d("Database", "History data loaded.");
                    } else {
                        Log.d("Database", "No history data found or cursor is null.");
                        historyBuilder.append("暂无历史记录");
                    }
                } else {
                    Log.e("Database", "dbManager is null in loadAndDisplayHistory");
                    historyBuilder.append("数据库错误");
                }
            } catch (SQLiteException e) {
                Log.e("Database", "Error querying history data.", e);
                historyBuilder.append("查询历史记录出错");
            } finally {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

            // Update the UI on the main thread
            uiHandler.post(() -> {
                TextView historyTextView = findViewById(R.id.history_textview); // Find it again just in case
                if (historyTextView != null) {
                    historyTextView.setText(historyBuilder.toString());
                    Log.d("UI Update", "History TextView updated.");
                } else {
                    Log.e("UI Update", "historyTextView is null when trying to update!");
                }
            });
        }).start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called.");
        isRunning = false; // Signal thread to stop
        if (sensorFetchingThread != null) {
            sensorFetchingThread.interrupt(); // Interrupt sleep
            Log.d(TAG, "Sensor fetching thread interrupted.");
        }
        if (dbManager != null) {
            dbManager.close();
            Log.d(TAG, "Database manager closed.");
        }
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null); // Clean up handler
        }
    }
}