package com.hika.core

import kotlinx.coroutines.delay

/**
 *  implement some general wrapper.
 */
object Helpers {
    /**
     * Loop until the condition method gets true or the duration has reached its maximum.
     *
     * @param condition A method to get true or false: Has the condition been satisfied? An method
     *      expression.
     * @param interval Interval of cyclical check. Default is 100. Probably not accurate.
     * @param durationMillis Maximal duration time of loop. Default is 1000 (ms).
     *
     */
    suspend fun loopFor(
        durationMillis: Long = 1000,
        interval: Long = 100,
        condition: suspend () -> Boolean
    ): Boolean {
        var currentTime = System.currentTimeMillis()
        var expectedTime = currentTime + interval
        val terminalTime = currentTime + durationMillis
        while (expectedTime <= terminalTime) {
            if (condition())
                return true

            currentTime = System.currentTimeMillis()
            val delta = expectedTime - currentTime
            if (delta > 0) {
                delay(delta)
                expectedTime += interval
            }else{
                // ensure the step length must be the multiplier of interval
                // if it's already late, immediately run the next loop
                expectedTime = currentTime + interval + delta % interval
            }
        }
        return false
    }
}