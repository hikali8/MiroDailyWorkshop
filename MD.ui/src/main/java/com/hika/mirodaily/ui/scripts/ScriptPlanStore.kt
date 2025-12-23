package com.hika.mirodaily.ui.scripts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ScriptPlanStore {
    private const val SP_NAME = "script_plan_store"
    private const val KEY_JSON = "plan_json"

    fun load(context: Context): ScriptPlan {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_JSON, null) ?: return ScriptPlan()
        return runCatching { fromJson(raw) }.getOrElse { ScriptPlan() }
    }

    fun save(context: Context, plan: ScriptPlan) {
        val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_JSON, toJson(plan)).apply()
    }

    /** 清理：分组里引用但磁盘不存在的脚本名；以及计划里不存在的分组 id */
    fun pruneMissingScripts(plan: ScriptPlan, existedScriptNames: Set<String>): Boolean {
        var changed = false

        for (g in plan.groups) {
            val before = g.scripts.size
            g.scripts = g.scripts.filter { it in existedScriptNames }.toMutableList()
            if (g.scripts.size != before) changed = true
        }

        val groupIds = plan.groups.map { it.id }.toSet()
        val beforePlan = plan.plan.size
        plan.plan = plan.plan.filter { it in groupIds }.toMutableList()
        if (plan.plan.size != beforePlan) changed = true

        return changed
    }

    private fun toJson(plan: ScriptPlan): String {
        val root = JSONObject()

        val groupsArr = JSONArray()
        for (g in plan.groups) {
            val jo = JSONObject()
            jo.put("id", g.id)
            jo.put("name", g.name)
            jo.put("com/hika/mirodaily/ui/scripts", JSONArray(g.scripts))
            groupsArr.put(jo)
        }

        root.put("groups", groupsArr)
        root.put("plan", JSONArray(plan.plan))
        return root.toString()
    }

    private fun fromJson(raw: String): ScriptPlan {
        val root = JSONObject(raw)

        val groups = mutableListOf<ScriptGroup>()
        val groupsArr = root.optJSONArray("groups") ?: JSONArray()
        for (i in 0 until groupsArr.length()) {
            val jo = groupsArr.getJSONObject(i)
            val id = jo.optString("id")
            val name = jo.optString("name")
            val scriptsArr = jo.optJSONArray("com/hika/mirodaily/ui/scripts") ?: JSONArray()
            val scripts = mutableListOf<String>()
            for (j in 0 until scriptsArr.length()) scripts.add(scriptsArr.getString(j))
            if (name.isNotBlank()) groups.add(ScriptGroup(id = id, name = name, scripts = scripts.toMutableList()))
        }

        val planIds = mutableListOf<String>()
        val planArr = root.optJSONArray("plan") ?: JSONArray()
        for (i in 0 until planArr.length()) planIds.add(planArr.getString(i))

        return ScriptPlan(groups = groups.toMutableList(), plan = planIds.toMutableList())
    }
}