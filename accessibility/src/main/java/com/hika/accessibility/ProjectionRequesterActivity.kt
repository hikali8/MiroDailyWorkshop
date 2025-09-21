package com.hika.accessibility

import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

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
        projectionRequestLauncher.launch(captureIntent)
    }

    private val accessibilityCoreService by lazy{ AccessibilityCoreService.instance.get()!! }

    private val projectionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode != RESULT_OK || it.data == null){
            Toast.makeText(this, "User denied screen sharing permission",
                Toast.LENGTH_SHORT).show()
        }else{
            Log.d("#0x-PA", "Projection Permitted.")
            accessibilityCoreService.startProjection(it.resultCode, it.data!!)
        }
        this.finish()
    }
}
