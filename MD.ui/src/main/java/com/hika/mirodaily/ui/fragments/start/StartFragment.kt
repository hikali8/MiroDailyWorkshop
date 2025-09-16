package com.hika.mirodaily.ui.ui.fragments.start


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hika.mirodaily.core.ASReceiver
import com.hika.mirodaily.ui.MainActivity
import com.hika.mirodaily.core.game_labors.hoyolab.DailyCheckIn
import com.hika.mirodaily.ui.databinding.FragmentStartBinding

class StartFragment : Fragment() {
    private var _binding: FragmentStartBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // like DOM
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        val root: View = binding.root

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
        super.onDestroyView()
        _binding = null
    }

    private fun onStartClick(view: View){
        //open an app
        if (ASReceiver.executeCode { scope ->
            DailyCheckIn(requireContext(), scope).openApp()
        } ?.isActive != true)
            Toast.makeText(context, "Failed to start code",
                Toast.LENGTH_SHORT).show()

        // click the demonstrative button
//        iAccessibilityService?.click(PointF(623f, 2120f), 0, 100)
    }
}