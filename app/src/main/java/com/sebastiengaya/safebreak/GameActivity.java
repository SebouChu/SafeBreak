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
    private Boolean[] combinationState = new Boolean[3];

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
        endText.setVisibility(View.INVISIBLE);
        finishBtn.setVisibility(View.INVISIBLE);

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
        for (int i = 0 ; i <= combination.length ; i++) {
            // On vérifie si le nombre trouvé précédemment est dépassé ou si le dernier chiffre a été trouvé mais Z est à une autre position
            if (i > 0) {
                if ((combination[i - 1] < 0 && z < combination[i - 1]) || (combination[i - 1] > 0 && z > combination[i - 1]) || (i == combination.length && z != combination[i-1])) {
                    unlockBtn.setVisibility(View.INVISIBLE);
                    for (int j = 0; j < combination.length; j++) {
                        // On reset le combinationState
                        combinationState[j] = false;
                        imageStates[j].setImageDrawable(getResources().getDrawable(R.drawable.cancel));
                    }
                    return;
                }
            }

            // On vérifie qu'on cherche un nombre
            if (i != combination.length) {
                // On regarde si le nombre de l'itération a déjà été trouvé
                if (!combinationState[i]) {
                    // Si previousZ a été initialisé ET que la position actuelle de Z correspond au nombre à trouver ET le Z précédent correspond au sens de lecture
                    if(previousZ != 200 && z == combination[i] && ((previousZ > z && combination[i] < 0) || (previousZ < z && combination[i] > 0))) {
                        // On valide le nombre
                        combinationState[i] = true;
                        mVibrator.vibrate(200);
                        imageStates[i].setImageDrawable(getResources().getDrawable(R.drawable.checked));

                        // Si denier nombre trouvé
                        if (i == combination.length - 1) {
                            // On fait apparaître le bouton de déverrouillage
                            unlockBtn.setVisibility(View.VISIBLE);
                        }
                    }
                    return;
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
        for (int i = 0 ; i < combinationState.length ; i++) {
            combinationState[i] = false;
        }
    }

    private void getUIElements() {
        zValueText = findViewById(R.id.valuesText);
        gameChrono = findViewById(R.id.gameChrono);
        unlockBtn = findViewById(R.id.unlockBtn);
        endText = findViewById(R.id.endText);
        finishBtn = findViewById(R.id.finishBtn);
        for (int i = 0 ; i < combinationState.length ; i++) {
            int resId = getResources().getIdentifier("imageState" + (i+1), "id", getPackageName());
            imageStates[i] = findViewById(resId);
        }
    }

    private void endGame() {
        gameChrono.stop();
        elapsedSeconds = (int) ((SystemClock.elapsedRealtime() - gameChrono.getBase()) / 1000);
        mSensorManager.unregisterListener(rvSensorListener);
        unlockBtn.setVisibility(View.INVISIBLE);
        endText.setVisibility(View.VISIBLE);
        finishBtn.setVisibility(View.VISIBLE);
    }


}
