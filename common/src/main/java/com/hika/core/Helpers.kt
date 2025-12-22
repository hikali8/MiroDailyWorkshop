package com.hika.core

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.delay

// Helpers: Methods to help

/**
 * Loop until the condition method gets true or the duration has reached its maximum.
 *
 * @param condition A method to get true or false.
 * @param interval Interval of cyclical check. Default is 100. Probably not accurate.
 * @param durationMillis Maximal duration time of loop. Default is 1000 (ms).
 *
 * @return A boolean whether the condition has been met
 *
 */
suspend fun loopUntil(
    durationMillis: Long = 1000,
    interval: Long = 200,
    condition: suspend () -> Boolean
): Boolean {
    var currentTime = System.currentTimeMillis()
    var expectingTime = currentTime + interval
    val terminalTime = currentTime + durationMillis
    while (expectingTime <= terminalTime) {
        if (condition())
            return true

        currentTime = System.currentTimeMillis()
        val delta = expectingTime - currentTime
        if (delta > 0) {
            delay(delta)
            expectingTime += interval
        }else{
            // since it's already late, immediately run the next loop
            // but ensure the step length is the multiplier of interval
            expectingTime = currentTime + interval + delta % interval
        }
    }
    return false
}

// too many code, that i have to simplify Toast.makeText
fun toastLine(text: CharSequence, context: Context?, isLong: Boolean = false){
    Toast.makeText(context, text,
        if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()
}

fun ComponentActivity.toastLine(text: CharSequence, isLong: Boolean = false){
    Toast.makeText(this, text,
        if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()
}

fun Fragment.toastLine(text: CharSequence, isLong: Boolean = false){
    Toast.makeText(context, text,
        if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()
}