package com.sebastiengaya.safebreak;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.TextView;

public class GameActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorEventListener rvSensorListener;
    private Vibrator mVibrator;

    private int[] combination = { 66, -30, 42 };
    private Boolean[] combinationState = new Boolean[combination.length];

    private TextView zValueText;
    private Chronometer gameChrono;

    public static final String EXTRA_SCORE = "com.sebastiengaya.safebreak.SCORE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        for (int i = 0 ; i < combinationState.length ; i++) {
            combinationState[i] = false;
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        zValueText = findViewById(R.id.valuesText);
        gameChrono = findViewById(R.id.gameChrono);

        gameChrono.start();

        if (mSensor == null) {
            Log.e("SENSOR_ERROR", "No gyroscope found");
            finish();
        }

        rvSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float[] rotationMatrix = new float[16];
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

                // Remap coordinate system
                float[] remappedRotationMatrix = new float[16];
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedRotationMatrix);

                // Convert to orientations
                float[] orientations = new float[3];

                SensorManager.getOrientation(remappedRotationMatrix, orientations);

                Integer zValue = (int) (Math.toDegrees(orientations[2]));

                zValueText.setText(Integer.toString(zValue));

                checkCombination(zValue);
            }

            private void checkCombination(Integer z) {
                for (int i = 0 ; i < combination.length ; i++) {
                    // If number not checked
                    if (!combinationState[i]) {
                        if(z == combination[i]) {
                            combinationState[i] = true;
                            mVibrator.vibrate(200);

                            // Si denier nombre trouvé
                            if (i == combination.length - 1) {
                                endGame();
                            }
                        }
                        return;
                    }
                }
            }



            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        mSensorManager.registerListener(rvSensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void endGame() {
        gameChrono.stop();
        int elapsedSeconds = (int) ((SystemClock.elapsedRealtime() - gameChrono.getBase()) / 1000);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_SCORE, elapsedSeconds);
        startActivity(intent);
    }
}
