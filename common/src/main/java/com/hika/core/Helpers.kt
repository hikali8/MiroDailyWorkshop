package com.hika.core

import kotlinx.coroutines.delay

/**
 * Loop until the condition method gets true or the duration has reached its maximum.
 *
 * @param condition A method to get true or false.
 * @param interval Interval of cyclical check. Default is 100. Probably not accurate.
 * @param durationMillis Maximal duration time of loop. Default is 1000 (ms).
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