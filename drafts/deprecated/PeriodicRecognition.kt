package com.hika.accessibility.recognition

import com.hika.accessibility.AccessibilityServicePart4_ScreenWatching
import com.hika.accessibility.AccessibilityServicePart4_ScreenWatching.WatchDelegations
import com.hika.accessibility.recognition.means.ocr.GoogleOCRer.getWordToBoxesMap
import com.hika.core.aidl.accessibility.LocationBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.emptyArray


abstract class PeriodicRecognition<T> (val delegations: WatchDelegations<T>,
                                       val imageHandler: ImageHandler,
                                       val interval: Long = 100L) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun jobStart(): Job
        = scope.launch {
            var thisTime = System.currentTimeMillis()
            var nextTime = thisTime + interval
            while (true){
                if (delegations.toAppear.isEmpty() && delegations.toDisappear.isEmpty())
                    break
                imageHandler.getRecognizable()?.let { recognizable ->
                    watchThroughEachOfDelegations(recognizable)
                }
                purgeDelegationsThatHaveNoCycles()

                thisTime = System.currentTimeMillis()
                val deltaTime = nextTime - thisTime
                if (deltaTime >= 0) {
                    delay(deltaTime)
                    nextTime += interval
                }else{
                    // ensure the step length must be the multiplier of interval, aligning them
                    nextTime += (-deltaTime / interval + 1) * interval  // intentional integrate division
                }
            }
        }


    protected abstract suspend fun watchThroughEachOfDelegations(recognizable: ImageHandler.Recognizable)

    protected fun handleAllFindings(findingBoxes: Map<T, List<LocationBox>>){
        val iterator = delegations.toAppear.iterator()
        while (iterator.hasNext()){
            val delegation = iterator.next()
            if (!delegation.isRunning)
                continue
            val replyParcels = getReplyParcels()

            for (target in delegation.targets){
                val boxes = findingBoxes[target]
                if (boxes != null)
                    employElement(replyParcels, target, boxes)
            }

            if (replyParcels.isEmpty() ||
                (delegation.isAll && replyParcels.size < delegation.targets.size))
                continue

            replyTrue(delegation, replyParcels)
            delegation.isRunning = false
            iterator.remove()
        }
    }


    protected abstract fun getReplyParcels(): MutableList<*>

    protected abstract fun employElement(parcels: MutableList<*>, target: T, boxes: List<LocationBox>)

    protected abstract fun replyTrue(delegation: WatchDelegations<T>.Delegation, parcels: List<*>)

    protected abstract fun replyFalse(delegation: WatchDelegations<T>.Delegation)


    protected fun handleAllToDisappear(findings: Set<T>){
        val iterator = delegations.toDisappear.iterator()
        while (iterator.hasNext()){
            val delegation = iterator.next()
            if (!delegation.isRunning)
                continue

            val ok = if (delegation.isAll){
                findings.containsAll(delegation.targets)
            }else{
                delegation.targets.any { it in findings }
            }

            if (!ok)
                continue

            replyTrue(delegation, emptyList<Any>())
            delegation.isRunning = false
            iterator.remove()
        }
    }

    private fun purgeDelegationsThatHaveNoCycles(){
        // 1. Periodically Decrement (if 0 be set already, may get wrong)
        // 2. Purge it off if cycled to 0.
        // 3. Ensure client notification.

        val filter = { delegation: WatchDelegations<T>.Delegation ->
            if (--delegation.remainingCycles == 0){
                if (delegation.isRunning)
                    replyFalse(delegation)
                delegation.isRunning = false
                true
            }
            else false
        }
        delegations.toAppear.removeIf(filter)
        delegations.toDisappear.removeIf(filter)
    }
}

//class PeriodicRecognitionImage(
//    mapToAppear: MutableList<WatchDelegation<TemplateImageID>>,
//    mapToDisappear: MutableList<WatchDelegation<TemplateImageID>>,
//    imageHandler: ImageHandler,
//    interval: Long = 100L):
//    PeriodicRecognition<TemplateImageID>(mapToAppear, mapToDisappear, imageHandler, interval){
//
//        override suspend fun watchThroughEachOfDelegations(recognizable: ImageHandler.Recognizable)
//            = throw NotImplementedError("object tracking yet to be implemented...")
//    }

class PeriodicRecognitionWord(
    delegations: WatchDelegations<String>,
    imageHandler: ImageHandler,
    interval: Long = 100L):
    PeriodicRecognition<String>(delegations, imageHandler, interval){

        override suspend fun watchThroughEachOfDelegations(recognizable: ImageHandler.Recognizable){
            val wordToBoxesMap = recognizable.findOnGoogleOCRer().getWordToBoxesMap()
            this.handleAllFindings(wordToBoxesMap)
            this.handleAllToDisappear(wordToBoxesMap.keys)
        }


    inline override fun getReplyParcels(): MutableList<WordToBoxesParcel>
        = mutableListOf()

    override fun employElement(parcels: MutableList<*>, target: String,
                               boxes: List<LocationBox>) {
        (parcels as MutableList<WordToBoxesParcel>)
            .add(WordToBoxesParcel(target, boxes))
    }

    override fun replyTrue(delegation: WatchDelegations<String>.Delegation, parcels: List<*>) {
        (delegation as AccessibilityServicePart4_ScreenWatching.WordDelegations.WordDelegation)
            .contactMethod(true, (parcels as List<WordToBoxesParcel>).toTypedArray())
    }

    override fun replyFalse(delegation: WatchDelegations<String>.Delegation){
        (delegation as AccessibilityServicePart4_ScreenWatching.WordDelegations.WordDelegation)
            .contactMethod(false, emptyArray())
    }
}