package com.hika.mirodaily.ui.ui.fragments.config

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConfigViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is config Fragment. (Not Implemented)"
    }
    val text: LiveData<String> = _text
}