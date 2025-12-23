package com.hika.mirodaily.ui.fragments.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.hika.core.toastLine
import com.hika.mirodaily.core.iAccessibilityService
import com.hika.mirodaily.ui.databinding.FragmentConfigBinding
import com.hika.mirodaily.ui.scripts.ScriptGroup
import com.hika.mirodaily.ui.scripts.ScriptPlan
import com.hika.mirodaily.ui.scripts.ScriptPlanStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class ConfigFragment : Fragment() {

    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!
    private var selectedScriptFile: File? = null
    private var planJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScriptManagerUiInConfig()
        setPlanUiInConfig()
    }

    override fun onDestroyView() {
        planJob?.cancel()
        planJob = null
        _binding = null
        super.onDestroyView()
    }

    // ⬇️脚本管理（粘贴保存/选择/预览/删除）

    private fun setScriptManagerUiInConfig() {
        binding.btnRefreshScripts.setOnClickListener { refreshScriptSpinner() }
        binding.btnPasteSaveScript.setOnClickListener { dialogPasteAndSaveScript() }
        binding.btnDeleteScript.setOnClickListener { confirmDeleteSelectedScript() }
        binding.btnPreviewScript.setOnClickListener { previewSelectedScript() }

        refreshScriptSpinner()
    }

    private fun scriptsDir(): File? = requireActivity().getExternalFilesDir(null)

    private fun listScriptFiles(): List<File> {
        val dir = scriptsDir() ?: return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.canRead() }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun refreshScriptSpinner() {
        val files = listScriptFiles()
        val names = if (files.isEmpty()) listOf("（暂无脚本）") else files.map { it.name }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spScripts.adapter = adapter

        // 默认选第一个
        selectedScriptFile = if (files.isEmpty()) null else files[0]
        binding.tvSelectedScript.text = selectedScriptFile?.absolutePath ?: "未选择脚本"

        binding.spScripts.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: View?,
                pos: Int,
                id: Long
            ) {
                selectedScriptFile = if (files.isEmpty()) null else files[pos]
                binding.tvSelectedScript.text = selectedScriptFile?.absolutePath ?: "未选择脚本"
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        // 同步清理：分组里已不存在的脚本
        val plan = ScriptPlanStore.load(requireContext())
        val existed = files.map { it.name }.toSet()
        if (ScriptPlanStore.pruneMissingScripts(plan, existed)) {
            ScriptPlanStore.save(requireContext(), plan)
        }
        refreshPlanSummary()
    }

    private fun dialogPasteAndSaveScript() {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }

        val nameEt = EditText(requireContext()).apply {
            hint = "文件名（可不填，自动生成）"
        }
        val contentEt = EditText(requireContext()).apply {
            hint = "把脚本内容粘贴到这里..."
            minLines = 8
        }

        root.addView(nameEt)
        root.addView(contentEt)

        AlertDialog.Builder(requireContext())
            .setTitle("粘贴并保存脚本")
            .setView(root)
            .setPositiveButton("保存") { _, _ ->
                val text = contentEt.text.toString()
                if (text.isBlank()) {
                    toastLine("脚本内容为空", requireContext(), true)
                    return@setPositiveButton
                }
                val dir = scriptsDir()
                if (dir == null) {
                    toastLine("无法获取脚本目录", requireContext(), true)
                    return@setPositiveButton
                }

                val fileName = buildFileName(nameEt.text.toString().trim())
                File(dir, fileName).writeText(text)

                toastLine("已保存：$fileName", requireContext(), true)
                refreshScriptSpinner()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun buildFileName(input: String): String {
        val base = input.ifBlank {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            "script_${sdf.format(Date())}"
        }.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()

        return if (base.endsWith(".csv", true) || base.endsWith(".txt", true)) base else "$base.csv"
    }

    private fun previewSelectedScript() {
        val f = selectedScriptFile
        if (f == null || !f.exists()) {
            toastLine("未选择脚本", requireContext(), true)
            return
        }
        val text = runCatching { f.readText() }.getOrNull().orEmpty()

        AlertDialog.Builder(requireContext())
            .setTitle("预览：${f.name}")
            .setMessage(if (text.isBlank()) "（空文件）" else text.take(4000))
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun confirmDeleteSelectedScript() {
        val f = selectedScriptFile
        if (f == null || !f.exists()) {
            toastLine("未选择脚本", requireContext(), true)
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("删除脚本")
            .setMessage("确定删除：${f.name} ?")
            .setPositiveButton("删除") { _, _ ->
                val ok = f.delete()
                toastLine(if (ok) "已删除：${f.name}" else "删除失败：${f.name}", requireContext(), true)
                refreshScriptSpinner()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ⬇️分组 / 执行计划（管理 + 执行）

    private fun setPlanUiInConfig() {
        refreshPlanSummary()
        binding.btnManageGroups.setOnClickListener { openGroupAndPlanManager() }
        binding.btnRunPlan.setOnClickListener { runPlanInOrder() }
    }

    private fun refreshPlanSummary() {
        val plan = ScriptPlanStore.load(requireContext())
        val idToName = plan.groups.associate { it.id to it.name }
        binding.tvPlanSummary.text = if (plan.plan.isEmpty()) {
            "当前计划：未设置"
        } else {
            "当前计划：" + plan.plan.mapNotNull { idToName[it] }.joinToString(" → ")
        }
    }

    private fun openGroupAndPlanManager() {
        val items = arrayOf("新建分组", "给分组添加脚本", "设置执行顺序（计划）", "重命名/删除分组")
        AlertDialog.Builder(requireContext())
            .setTitle("分组 / 执行计划")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> dialogCreateGroup()
                    1 -> dialogAssignScriptsToGroup()
                    2 -> dialogEditPlanOrder()
                    3 -> dialogRenameOrDeleteGroup()
                }
            }
            .show()
    }

    private fun dialogCreateGroup() {
        val input = EditText(requireContext()).apply { hint = "例如：日常/周常/采集/战斗" }
        AlertDialog.Builder(requireContext())
            .setTitle("新建分组")
            .setView(input)
            .setPositiveButton("创建") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) return@setPositiveButton
                val plan = ScriptPlanStore.load(requireContext())
                plan.groups.add(ScriptGroup(name = name))
                ScriptPlanStore.save(requireContext(), plan)
                refreshPlanSummary()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dialogAssignScriptsToGroup() {
        val plan = ScriptPlanStore.load(requireContext())
        if (plan.groups.isEmpty()) {
            toastLine("还没有分组，请先新建分组", requireContext(), true)
            return
        }
        val groupNames = plan.groups.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("选择要编辑的分组")
            .setItems(groupNames) { _, idx ->
                dialogPickScriptsForGroup(plan.groups[idx])
            }
            .show()
    }

    private fun dialogPickScriptsForGroup(group: ScriptGroup) {
        val scripts = listScriptFiles().map { it.name }
        if (scripts.isEmpty()) {
            toastLine("当前没有脚本文件，请先粘贴脚本保存", requireContext(), true)
            return
        }

        val checked = BooleanArray(scripts.size) { i -> group.scripts.contains(scripts[i]) }
        val selected = group.scripts.toMutableSet()

        AlertDialog.Builder(requireContext())
            .setTitle("为「${group.name}」选择脚本（多选）")
            .setMultiChoiceItems(scripts.toTypedArray(), checked) { _, which, isChecked ->
                val name = scripts[which]
                if (isChecked) selected.add(name) else selected.remove(name)
            }
            .setPositiveButton("保存") { _, _ ->
                group.scripts = scripts.filter { it in selected }.toMutableList()

                val plan = ScriptPlanStore.load(requireContext())
                val target = plan.groups.firstOrNull { it.id == group.id } ?: return@setPositiveButton
                target.scripts = group.scripts
                ScriptPlanStore.save(requireContext(), plan)
                refreshPlanSummary()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun dialogEditPlanOrder() {
        val plan = ScriptPlanStore.load(requireContext())
        if (plan.groups.isEmpty()) {
            toastLine("还没有分组，请先新建分组", requireContext(), true)
            return
        }

        val planIds = plan.plan.toMutableList()
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }
        val listContainer = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val addBtn = com.google.android.material.button.MaterialButton(requireContext()).apply {
            text = "添加分组到计划末尾"
            isAllCaps = false
            setOnClickListener {
                val candidates = plan.groups.filter { it.id !in planIds }
                if (candidates.isEmpty()) {
                    toastLine("没有可添加的分组（都已经在计划里）", requireContext(), true)
                    return@setOnClickListener
                }
                AlertDialog.Builder(requireContext())
                    .setTitle("选择要添加的分组")
                    .setItems(candidates.map { it.name }.toTypedArray()) { _, idx ->
                        planIds.add(candidates[idx].id)
                        renderPlanRows(listContainer, plan, planIds)
                    }
                    .show()
            }
        }

        root.addView(listContainer)
        root.addView(addBtn)
        renderPlanRows(listContainer, plan, planIds)

        AlertDialog.Builder(requireContext())
            .setTitle("设置执行顺序（计划）")
            .setView(root)
            .setPositiveButton("保存") { _, _ ->
                plan.plan = planIds
                ScriptPlanStore.save(requireContext(), plan)
                refreshPlanSummary()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun renderPlanRows(container: LinearLayout, plan: ScriptPlan, planIds: MutableList<String>) {
        container.removeAllViews()
        val idToGroup = plan.groups.associateBy { it.id }

        planIds.forEachIndexed { index, gid ->
            val g = idToGroup[gid] ?: return@forEachIndexed

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(6), 0, dp(6))
            }
            val tv = TextView(requireContext()).apply {
                text = "${index + 1}. ${g.name}"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            fun smallBtn(label: String, onClick: () -> Unit) =
                com.google.android.material.button.MaterialButton(requireContext()).apply {
                    text = label
                    isAllCaps = false
                    minHeight = dp(32)
                    setPadding(dp(8), 0, dp(8), 0)
                    setOnClickListener { onClick() }
                }

            val up = smallBtn("↑") {
                if (index <= 0) return@smallBtn
                Collections.swap(planIds, index, index - 1)
                renderPlanRows(container, plan, planIds)
            }
            val down = smallBtn("↓") {
                if (index >= planIds.lastIndex) return@smallBtn
                Collections.swap(planIds, index, index + 1)
                renderPlanRows(container, plan, planIds)
            }
            val remove = smallBtn("×") {
                planIds.removeAt(index)
                renderPlanRows(container, plan, planIds)
            }

            row.addView(tv)
            row.addView(up)
            row.addView(down)
            row.addView(remove)
            container.addView(row)
        }
    }

    private fun dialogRenameOrDeleteGroup() {
        val plan = ScriptPlanStore.load(requireContext())
        if (plan.groups.isEmpty()) {
            toastLine("还没有分组", requireContext(), true)
            return
        }

        val groupNames = plan.groups.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("选择分组")
            .setItems(groupNames) { _, idx ->
                val g = plan.groups[idx]
                val ops = arrayOf("重命名", "删除")
                AlertDialog.Builder(requireContext())
                    .setTitle("分组：${g.name}")
                    .setItems(ops) { _, which ->
                        when (which) {
                            0 -> {
                                val input = EditText(requireContext()).apply { setText(g.name) }
                                AlertDialog.Builder(requireContext())
                                    .setTitle("重命名分组")
                                    .setView(input)
                                    .setPositiveButton("保存") { _, _ ->
                                        val newName = input.text.toString().trim()
                                        if (newName.isBlank()) return@setPositiveButton
                                        g.name = newName
                                        ScriptPlanStore.save(requireContext(), plan)
                                        refreshPlanSummary()
                                    }
                                    .setNegativeButton("取消", null)
                                    .show()
                            }
                            1 -> {
                                plan.groups.removeAll { it.id == g.id }
                                plan.plan.removeAll { it == g.id }
                                ScriptPlanStore.save(requireContext(), plan)
                                refreshPlanSummary()
                            }
                        }
                    }
                    .show()
            }
            .show()
    }

    private fun runPlanInOrder() {
        if (iAccessibilityService == null) {
            toastLine("无障碍服务未连接", requireContext(), true)
            return
        }

        val plan = ScriptPlanStore.load(requireContext())
        if (plan.plan.isEmpty()) {
            toastLine("计划为空：请先设置执行顺序（计划）", requireContext(), true)
            return
        }

        val files = listScriptFiles()
        val fileMap = files.associateBy { it.name }

        if (ScriptPlanStore.pruneMissingScripts(plan, fileMap.keys)) {
            ScriptPlanStore.save(requireContext(), plan)
            refreshPlanSummary()
        }

        planJob?.cancel()
        planJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val idToGroup = plan.groups.associateBy { it.id }

            for (gid in plan.plan) {
                if (!isActive) return@launch
                val g = idToGroup[gid] ?: continue

                for (scriptName in g.scripts) {
                    if (!isActive) return@launch
                    val f = fileMap[scriptName] ?: continue
                    val scriptText = f.readText()

                    iAccessibilityService?.replayScript(scriptText)

                    delay(estimateScriptDurationMs(scriptText) + 800)
                }
            }
        }
    }

    private fun estimateScriptDurationMs(script: String): Long {
        var total = 0L
        for (line in script.lineSequence()) {
            val cols = line.split(',')
            if (cols.isEmpty()) continue
            when (cols[0]) {
                "wait", "Move", "NEXT" -> total += cols.getOrNull(1)?.toLongOrNull() ?: 0L
            }
        }
        return total.coerceAtLeast(0L)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}