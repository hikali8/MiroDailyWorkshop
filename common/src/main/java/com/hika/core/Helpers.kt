package com.hika.core

import kotlinx.coroutines.delay

/**
 *  implement some general wrapper.
 */
object Helpers {
    /**
     * Loop until the condition method gets true or the duration has reached its maximum.
     *
     * @param condition A method to get true or false.
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
                // since it's already late, immediately run the next loop
                // but ensure the step length is really the multiplier of interval
                expectedTime = currentTime + interval + delta % interval
            }
        }
        return false
    }
}