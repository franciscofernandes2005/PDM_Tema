package com.example.devicefeatures;

import android.Manifest;
import android.content.Context;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String PREFERENCES_NAME = "step_counter_preferences";
    private static final String KEY_PERMISSION_REQUESTED = "activity_recognition_requested";
    private static final String RESULT_FILE_PREFIX = "resultado_passos_";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private TextView stepCountValue;
    private TextView statusMessage;
    private MaterialButton startButton;
    private MaterialButton saveButton;
    private boolean counting;
    private boolean baselineSet;
    private float stepBaseline;
    private int steps;

    private final ActivityResultLauncher<String> activityRecognitionPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCounting();
                } else {
                    counting = false;
                    updateCountButton();
                    statusMessage.setText(R.string.permission_denied);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepCountValue = findViewById(R.id.stepCountValue);
        statusMessage = findViewById(R.id.statusMessage);
        startButton = findViewById(R.id.startButton);
        saveButton = findViewById(R.id.saveButton);

        if (savedInstanceState != null) {
            counting = savedInstanceState.getBoolean("counting");
            baselineSet = savedInstanceState.getBoolean("baselineSet");
            stepBaseline = savedInstanceState.getFloat("stepBaseline");
            steps = savedInstanceState.getInt("steps");
            updateCountDisplay();
            updateCountButton();
        }

        if (stepSensor == null) {
            statusMessage.setText(R.string.step_sensor_unavailable);
            startButton.setEnabled(false);
        }
        startButton.setOnClickListener(view -> onCountButtonClicked());
        saveButton.setOnClickListener(view -> saveResult());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (counting && hasActivityRecognitionPermission()) {
            registerStepSensor();
        } else if (counting) {
            counting = false;
            updateCountButton();
            statusMessage.setText(R.string.permission_denied);
        }
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this, stepSensor);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("counting", counting);
        outState.putBoolean("baselineSet", baselineSet);
        outState.putFloat("stepBaseline", stepBaseline);
        outState.putInt("steps", steps);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER || !counting) {
            return;
        }
        if (!baselineSet) {
            stepBaseline = event.values[0];
            baselineSet = true;
        }
        steps = Math.max(0, Math.round(event.values[0] - stepBaseline));
        updateCountDisplay();
        statusMessage.setText(R.string.counting_steps);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action is needed when step sensor accuracy changes.
    }

    private void onCountButtonClicked() {
        if (counting) {
            stopCounting();
        } else if (stepSensor == null) {
            statusMessage.setText(R.string.step_sensor_unavailable);
        } else if (hasActivityRecognitionPermission()) {
            startCounting();
        } else {
            requestActivityRecognitionPermission();
        }
    }

    private void requestActivityRecognitionPermission() {
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        boolean permissionWasRequested =
                preferences.getBoolean(KEY_PERMISSION_REQUESTED, false);
        if (permissionWasRequested
                && !shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)) {
            showPermissionSettingsDialog();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_title)
                .setMessage(R.string.permission_explanation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.permission_continue, (dialog, which) -> {
                    preferences.edit().putBoolean(KEY_PERMISSION_REQUESTED, true).apply();
                    activityRecognitionPermissionLauncher.launch(
                            Manifest.permission.ACTIVITY_RECOGNITION);
                })
                .show();
    }

    private boolean hasActivityRecognitionPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCounting() {
        if (!hasActivityRecognitionPermission()) {
            statusMessage.setText(R.string.permission_denied);
            return;
        }
        steps = 0;
        baselineSet = false;
        counting = true;
        updateCountDisplay();
        updateCountButton();
        statusMessage.setText(R.string.waiting_for_sensor);
        registerStepSensor();
    }

    private void registerStepSensor() {
        if (counting && stepSensor != null) {
            boolean registered = sensorManager.registerListener(
                    this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            if (!registered) {
                counting = false;
                updateCountButton();
                statusMessage.setText(R.string.sensor_registration_failed);
            }
        }
    }

    private void stopCounting() {
        sensorManager.unregisterListener(this, stepSensor);
        counting = false;
        updateCountButton();
        statusMessage.setText(R.string.counting_stopped);
    }

    private void updateCountDisplay() {
        stepCountValue.setText(getString(R.string.step_count, steps));
    }

    private void updateCountButton() {
        startButton.setText(counting ? R.string.stop_counting : R.string.start_counting);
    }

    private void saveResult() {
        Date now = new Date();
        String timestamp = DateFormat.getDateTimeInstance().format(now);
        String fileName = RESULT_FILE_PREFIX
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(now)
                + ".txt";
        String content = getString(R.string.saved_result_content, timestamp, steps);
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri resultUri = null;
        try {
            resultUri = getContentResolver().insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (resultUri == null) {
                throw new IOException("O Android não conseguiu criar o ficheiro em Downloads.");
            }
            OutputStream output = getContentResolver().openOutputStream(resultUri);
            if (output == null) {
                throw new IOException("O Android não conseguiu abrir o ficheiro em Downloads.");
            }
            try (OutputStream stream = output) {
                stream.write(content.getBytes(StandardCharsets.UTF_8));
            }
            statusMessage.setText(getString(R.string.result_saved, fileName));
        } catch (IOException | SecurityException exception) {
            if (resultUri != null) {
                try {
                    getContentResolver().delete(resultUri, null, null);
                } catch (SecurityException cleanupException) {
                    exception.addSuppressed(cleanupException);
                }
            }
            statusMessage.setText(getString(
                    R.string.result_save_failed, exception.getLocalizedMessage()));
        }
    }

    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_settings_title)
                .setMessage(R.string.permission_settings_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.open_settings, (dialog, which) -> {
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .show();
    }
}
