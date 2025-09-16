package com.hika.mirodaily.ui.ui.fragments.more_dots

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MoreDotsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is more Fragment. (Not Implemented)"
    }
    val text: LiveData<String> = _text
}