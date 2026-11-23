package com.mukhtabir.android.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View

/** User-initiated feedback only; callers own the app preference and foreground gate. */
@Suppress("DEPRECATION")
fun performAppHaptic(view: View, confirmed: Boolean = false) {
    try {
        if (!view.isHapticFeedbackEnabled || Settings.System.getInt(view.context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return
        val feedback = if (confirmed) {
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        } else HapticFeedbackConstants.VIRTUAL_KEY
        if (view.performHapticFeedback(feedback)) return
        // Some devices reject virtual-key feedback while allowing ordinary vibration.
        val vibrator = view.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        val duration = if (confirmed) 28L else 16L
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(duration,VibrationEffect.DEFAULT_AMPLITUDE))
        else vibrator.vibrate(duration)
    } catch (_: SecurityException) {
        // A device policy can reject system-setting reads or vibration access.
    }
}
