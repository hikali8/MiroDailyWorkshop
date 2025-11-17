package com.hika.accessibility

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.hika.core.toastLine

// request projection permission and start accessibility-service's projection.
class ProjectionRequesterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("#0x-PA", "onCreate Projection activity.")
        launchProjection()
    }

    private fun launchProjection(){
        val captureIntent = getSystemService(MediaProjectionManager::class.java)
            .createScreenCaptureIntent()
        activityLauncher.launch(captureIntent)
    }

    private val accessibilityCoreService by lazy{ AccessibilityCoreService.instance.get()!! }

    private val activityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        if (it.resultCode != RESULT_OK || it.data == null){
            toastLine("User denied screen sharing permission",this)
        }else{
            Log.d("#0x-PA", "Projection Permitted.")
            accessibilityCoreService.startProjection(it.resultCode, it.data!!)
        }
        this.finish()
    }
}
