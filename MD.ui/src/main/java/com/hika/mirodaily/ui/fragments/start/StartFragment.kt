package com.hika.mirodaily.ui.ui.fragments.start


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.hika.mirodaily.ui.MainActivity
import com.hika.mirodaily.core.game_labors.hoyolab.DailyCheckIn
import com.hika.mirodaily.ui.databinding.FragmentStartBinding
import androidx.lifecycle.lifecycleScope
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.ui.fragments.start.FloatingWindow
import kotlinx.coroutines.Job

class StartFragment : Fragment() {
    private lateinit var binding: FragmentStartBinding
    private lateinit var floatingWindow: FloatingWindow


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)
        floatingWindow = FloatingWindow(requireContext(), inflater, overlayRequestLauncher)

        val root = binding.root

        // bind functions for buttons
        binding.btnAccessibility.setOnClickListener{
            (activity as MainActivity).enableAccessibilitySetting()
        }
        binding.btnProjection.setOnClickListener{
            (activity as MainActivity).requestProjection()
        }
        binding.btnStart.setOnClickListener(::onStartClick)

        return root
    }

    override fun onDestroyView() {
        floatingWindow.close()
        super.onDestroyView()
    }

    // Open Floating window and start automation.
    private fun onStartClick(view: View){
        floatingWindow.open()

        if (iAccessibilityService?.isProjectionStarted() != true){
            Log.d("#0x-SF", "Projection is not yet started")
            return
        }
        job?.cancel()
        job = DailyCheckIn(requireContext(), lifecycleScope, floatingWindow.logger).start()
    }

    private var job: Job? = null

    private val overlayRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { floatingWindow.onLaunchResult() }
}