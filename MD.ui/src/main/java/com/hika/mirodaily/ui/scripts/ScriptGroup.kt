package com.hika.mirodaily.ui.scripts

import java.util.UUID

data class ScriptGroup(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var scripts: MutableList<String> = mutableListOf()
)

