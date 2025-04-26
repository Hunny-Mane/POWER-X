package com.example.power_x;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class EqualizerActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    Equalizer equalizer;
    BassBoost bassBoost;
    short bassBoostStrength = 1000;
    String[][] presets = {
            {"Bass Boost", "Enhances bass frequencies for deeper sound."},
            {"Treble Boost", "Sharpens high frequencies for more clarity."},
            {"Vocal Mode", "Optimized for podcasts and voice calls."},
            {"Balanced", "Flat profile for general use."},
            {"Gaming Mode", "Directional sound for better in-game awareness."}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor("#1F1F1F"));
        }

        LinearLayout container = findViewById(R.id.equalizer_container);
        LayoutInflater inflater = LayoutInflater.from(this);
        Toolbar toolbar = findViewById(R.id.toolbar_equalizer);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            TextView titleTextView = new TextView(this);
            titleTextView.setText("Equalizer");
            titleTextView.setTextColor(Color.WHITE);
            titleTextView.setTextSize(20);
            titleTextView.setPadding((int) (25 * getResources().getDisplayMetrics().density), 0, 0, 0);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setCustomView(titleTextView);
        }

        for (String[] preset : presets) {
            View item = inflater.inflate(R.layout.equalizer_item, container, false);
            TextView title = item.findViewById(R.id.equalizer_title);
            TextView tv1 = item.findViewById(R.id.tx1);
            LinearLayout desc = item.findViewById(R.id.equalizer_description);
            Switch s1 = item.findViewById(R.id.switch1);

            title.setText(preset[0]);
            tv1.setText(preset[1]);
            desc.setVisibility(View.GONE);

            item.setOnClickListener(v -> {
                if (desc.getVisibility() == View.GONE) {
                    desc.setVisibility(View.VISIBLE);
                    int initialHeight = 0;
                    desc.measure(0, 0);
                    int targetHeight = desc.getMeasuredHeight();
                    desc.getLayoutParams().height = initialHeight;
                    desc.requestLayout();

                    ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
                    animator.setDuration(300);
                    animator.addUpdateListener(valueAnimator -> {
                        int animatedValue = (int) valueAnimator.getAnimatedValue();
                        desc.getLayoutParams().height = animatedValue;
                        desc.requestLayout();
                    });
                    animator.start();
                } else {
                    int initialHeight = desc.getHeight();
                    int targetHeight = 0;

                    ValueAnimator animator = ValueAnimator.ofInt(initialHeight, targetHeight);
                    animator.setDuration(300);
                    animator.addUpdateListener(valueAnimator -> {
                        int animatedValue = (int) valueAnimator.getAnimatedValue();
                        desc.getLayoutParams().height = animatedValue;
                        desc.requestLayout();
                    });
                    animator.start();

                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            desc.setVisibility(View.GONE);
                        }
                    });
                }
            });

            s1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && preset[0].equals("Bass Boost")) {
                    mediaPlayer = MediaPlayer.create(this, R.raw.bass_boost_sample);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();

                    int audioSessionId = mediaPlayer.getAudioSessionId();
                    equalizer = new Equalizer(0, audioSessionId);
                    equalizer.setEnabled(true);

                    bassBoost = new BassBoost(0, audioSessionId);
                    bassBoost.setStrength(bassBoostStrength);
                    bassBoost.setEnabled(true);

                    Toast.makeText(this, "Bass Boost Sample Playing", Toast.LENGTH_SHORT).show();
                } else {
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;

                        if (equalizer != null) {
                            equalizer.release();
                            equalizer = null;
                        }

                        if (bassBoost != null) {
                            bassBoost.release();
                            bassBoost = null;
                        }
                    }

                    Toast.makeText(this, "Bass Boost Sample Stopped", Toast.LENGTH_SHORT).show();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int thumbColor = isChecked ? Color.parseColor("#4CAF50") : Color.parseColor("#BDBDBD");
                    int trackColor = isChecked ? Color.parseColor("#4CAF50") : Color.parseColor("#BDBDBD");

                    s1.getThumbDrawable().setColorFilter(thumbColor, PorterDuff.Mode.SRC_IN);
                    s1.getTrackDrawable().setColorFilter(trackColor, PorterDuff.Mode.SRC_IN);
                } else {
                    if (isChecked) {
                        s1.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.switch_thumb_on), PorterDuff.Mode.SRC_IN);
                    } else {
                        s1.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.switch_thumb_off), PorterDuff.Mode.SRC_IN);
                    }
                }
            });

            container.addView(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }
}
