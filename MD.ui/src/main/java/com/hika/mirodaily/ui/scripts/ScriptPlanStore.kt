package com.hika.mirodaily.ui.scripts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ScriptPlanStore {
    private const val SP_NAME = "script_plan_store"
    private const val KEY_JSON = "plan_json"

    private const val KEY_GROUPS = "groups"
    private const val KEY_PLAN = "plan"
    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_SCRIPTS = "scripts"

    private const val KEY_SCRIPTS_WRONG = "com/hika/mirodaily/ui/scripts"

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
            // 需要 ScriptGroup.scripts 是 var
            g.scripts = g.scripts.filter { it in existedScriptNames }.toMutableList()
            if (g.scripts.size != before) changed = true
        }

        val groupIds = plan.groups.map { it.id }.toSet()
        val beforePlan = plan.plan.size
        // 需要 ScriptPlan.plan 是 var
        plan.plan = plan.plan.filter { it in groupIds }.toMutableList()
        if (plan.plan.size != beforePlan) changed = true

        return changed
    }

    private fun toJson(plan: ScriptPlan): String {
        val root = JSONObject()

        val groupsArr = JSONArray()
        for (g in plan.groups) {
            val jo = JSONObject()
            jo.put(KEY_ID, g.id)
            jo.put(KEY_NAME, g.name)
            jo.put(KEY_SCRIPTS, JSONArray(g.scripts)) // ✅ 写回用正确键名
            groupsArr.put(jo)
        }

        root.put(KEY_GROUPS, groupsArr)
        root.put(KEY_PLAN, JSONArray(plan.plan))
        return root.toString()
    }

    private fun fromJson(raw: String): ScriptPlan {
        val root = JSONObject(raw)

        val groups = mutableListOf<ScriptGroup>()
        val groupsArr = root.optJSONArray(KEY_GROUPS) ?: JSONArray()
        for (i in 0 until groupsArr.length()) {
            val jo = groupsArr.getJSONObject(i)
            val id = jo.optString(KEY_ID)
            val name = jo.optString(KEY_NAME)

            val scriptsArr = jo.optJSONArray(KEY_SCRIPTS)
                ?: jo.optJSONArray(KEY_SCRIPTS_WRONG)
                ?: JSONArray()

            val scripts = mutableListOf<String>()
            for (j in 0 until scriptsArr.length()) scripts.add(scriptsArr.getString(j))

            if (name.isNotBlank()) {
                groups.add(
                    ScriptGroup(
                        id = id,
                        name = name,
                        scripts = scripts.toMutableList()
                    )
                )
            }
        }

        val planIds = mutableListOf<String>()
        val planArr = root.optJSONArray(KEY_PLAN) ?: JSONArray()
        for (i in 0 until planArr.length()) planIds.add(planArr.getString(i))

        return ScriptPlan(
            groups = groups.toMutableList(),
            plan = planIds.toMutableList()
        )
    }
}