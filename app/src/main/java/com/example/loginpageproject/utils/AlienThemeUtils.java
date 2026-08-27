package com.example.loginpageproject.utils;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class AlienThemeUtils {
    public static void applyGlowEffect(View view) {
        Animation anim = new AlphaAnimation(0.4f, 1.0f);
        anim.setDuration(1500);
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        view.startAnimation(anim);
    }
}