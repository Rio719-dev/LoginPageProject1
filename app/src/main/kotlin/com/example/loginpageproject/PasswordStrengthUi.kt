package com.example.loginpageproject

import android.animation.ObjectAnimator
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.widget.ProgressBar
import com.example.loginpageproject.auth.PasswordRequirements

/**
 * Animates a horizontal ProgressBar to reflect password strength and recolors only the
 * filled portion (not the background track) red/yellow/green based on how many of the
 * five requirements are satisfied.
 */
fun ProgressBar.updatePasswordStrengthBar(rules: PasswordRequirements) {
    val targetProgress = (rules.score * 100) / MAX_PASSWORD_SCORE
    ObjectAnimator.ofInt(this, "progress", progress, targetProgress).apply {
        duration = 200
        start()
    }
    val color = when {
        rules.isStrong -> context.getColor(R.color.success_light)
        rules.score >= 3 -> context.getColor(R.color.warning_light)
        else -> context.getColor(R.color.danger_light)
    }
    val layers = progressDrawable as? LayerDrawable
    val progressLayer = layers?.findDrawableByLayerId(android.R.id.progress)
    val fill = (progressLayer as? ClipDrawable)?.drawable as? GradientDrawable
    fill?.setColor(color)
}

fun passwordStrengthLabel(rules: PasswordRequirements): String = when {
    rules.isStrong -> "🟢 Strong"
    rules.score >= 3 -> "🟡 Average"
    else -> "🔴 Weak"
}

private const val MAX_PASSWORD_SCORE = 5
