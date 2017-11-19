package com.sebastiengaya.safebreak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView bestScoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bestScoreText = findViewById(R.id.bestScoreText);

        SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences("user_prefs", MODE_PRIVATE);
        int bestScore = sharedPreferences.getInt("bestScore", 999999);

        Intent intent = getIntent();
        int score = intent.getIntExtra(GameActivity.EXTRA_SCORE, 0);

        if (score != 0) {
            // Coming from GameActivity
            if (bestScore > score) {
               sharedPreferences.edit().putInt("bestScore", score).apply();
               bestScore = score;
            }
        }

        if (bestScore == 999999) {
            // Pas de record
            bestScoreText.setText(getResources().getString(R.string.noBestScore));
        } else {
            Resources res = getResources();
            String bestScoreString = res.getString(R.string.bestScoreText, String.format(Locale.getDefault(), "%02d", ((int) bestScore / 60)), String.format(Locale.getDefault(), "%02d", (bestScore % 60)));
            bestScoreText.setText(bestScoreString);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void onClickPlayBtn(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void onClickResetBtn(View view) {
        SharedPreferences sharedPreferences = getBaseContext().getSharedPreferences("user_prefs", MODE_PRIVATE);
        sharedPreferences.edit().putInt("bestScore", 999999).apply();
        bestScoreText.setText(getResources().getString(R.string.noBestScore));
    }
}