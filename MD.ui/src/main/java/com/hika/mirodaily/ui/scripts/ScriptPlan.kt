package com.hika.mirodaily.ui.scripts

data class ScriptPlan(
    var groups: MutableList<ScriptGroup> = mutableListOf(),
    var plan: MutableList<String> = mutableListOf()
)

