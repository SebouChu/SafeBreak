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
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorEventListener rvSensorListener;
    private Vibrator mVibrator;

    private int[] combination = new int[3];
    private int combinationState;

    private TextView zValueText;
    private Chronometer gameChrono;
    private ImageView[] imageStates = new ImageView[3];

    private Button unlockBtn;

    private TextView endText;
    private Button finishBtn;

    private int elapsedSeconds;
    private int previousZ = 200;

    public static final String EXTRA_SCORE = "com.sebastiengaya.safebreak.SCORE";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        generateCombination();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        getUIElements();

        unlockBtn.setVisibility(View.INVISIBLE);
        unlockBtn.setEnabled(false);
        endText.setVisibility(View.INVISIBLE);
        finishBtn.setVisibility(View.INVISIBLE);
        finishBtn.setEnabled(false);

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

                zValueText.setText(String.format(Locale.getDefault(), "%01d°", zValue));
                checkCombination(zValue);
                previousZ = zValue;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        mSensorManager.registerListener(rvSensorListener, mSensor, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(rvSensorListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(rvSensorListener, mSensor, 500);
    }

    private void checkCombination(Integer z) {

        // On vérifie qu'on a trouvé le premier nombre
        if (combinationState > 0) {
            int previousCombination = combinationState - 1;
            // Si on dépasse le nombre trouvé précédemment
            if ((combination[previousCombination] < 0 && z < combination[previousCombination]) || (combination[previousCombination] > 0 && z > combination[previousCombination]) || (combinationState == combination.length && z != combination[previousCombination])) {
                unlockBtn.setVisibility(View.INVISIBLE);
                unlockBtn.setEnabled(false);
                // On reset le combinationState
                combinationState = 0;
                for (int j = 0; j < combination.length; j++) {
                    // On réinitialise les images
                    imageStates[j].setImageDrawable(getResources().getDrawable(R.drawable.cancel));
                }
                return;
            }
        }

        // On vérifie qu'on cherche un nombre
        if (combinationState != combination.length) {
            // Si previousZ a été initialisé ET que la position actuelle de Z correspond au nombre à trouver ET le Z précédent correspond au sens de lecture
            if(previousZ != 200 && z == combination[combinationState] && ((previousZ > z && combination[combinationState] < 0) || (previousZ < z && combination[combinationState] > 0))) {
                // On valide le nombre
                imageStates[combinationState].setImageDrawable(getResources().getDrawable(R.drawable.checked));
                combinationState++;
                mVibrator.vibrate(200);

                // Si denier nombre trouvé
                if (combinationState == combination.length) {
                    // On fait apparaître le bouton de déverrouillage
                    unlockBtn.setVisibility(View.VISIBLE);
                    unlockBtn.setEnabled(true);
                }
            }
        }

    }

    public void onClickUnlockBtn(View view) {
        endGame();
    }

    public void onClickHomeBtn(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_SCORE, elapsedSeconds);
        startActivity(intent);
    }

    private void generateCombination() {
        Double randomNum = Math.random();
        for (int i = 0 ; i < combination.length ; i++) {
            combination[i] = (int) (Math.random() * 128);
        }
        if (randomNum < 0.5) {
            // + , - , +
            combination[1] = - combination[1];
        } else {
            // - , + , -
            combination[0] = - combination[0];
            combination[2] = - combination[2];
        }
        combinationState = 0;
        // Log.d("GENERATED", "Combination generated : "+combination[0]+", "+combination[1]+", "+combination[2]);
    }

    private void getUIElements() {
        zValueText = findViewById(R.id.valuesText);
        gameChrono = findViewById(R.id.gameChrono);
        unlockBtn = findViewById(R.id.unlockBtn);
        endText = findViewById(R.id.endText);
        finishBtn = findViewById(R.id.finishBtn);
        for (int i = 0 ; i < combination.length ; i++) {
            int resId = getResources().getIdentifier("imageState" + (i+1), "id", getPackageName());
            imageStates[i] = findViewById(resId);
        }
    }

    private void endGame() {
        gameChrono.stop();
        elapsedSeconds = (int) ((SystemClock.elapsedRealtime() - gameChrono.getBase()) / 1000);
        mSensorManager.unregisterListener(rvSensorListener);
        unlockBtn.setEnabled(false);
        unlockBtn.setVisibility(View.INVISIBLE);
        endText.setVisibility(View.VISIBLE);
        finishBtn.setEnabled(true);
        finishBtn.setVisibility(View.VISIBLE);
    }


}
