package com.hika.mirodaily.ui.ui.fragments.more_dots

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hika.mirodaily.core.AccessibilityPackageName
import com.hika.mirodaily.core.ProjectionRequesterClassName
import com.hika.mirodaily.ui.databinding.FragmentMoreDotsBinding

val DebugViewClassName = "com.hika.accessibility.debug.TestFloatingViewActivity"

class MoreDotsFragment : Fragment() {

    private var _binding: FragmentMoreDotsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentMoreDotsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.btnDebugView.setOnClickListener {
            val intent = Intent().apply {
                setClassName(AccessibilityPackageName, DebugViewClassName)
            }
            startActivity(intent)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}