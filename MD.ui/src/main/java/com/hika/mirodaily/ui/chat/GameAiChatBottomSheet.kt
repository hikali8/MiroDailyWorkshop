package com.hika.mirodaily.ui.chat

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hika.mirodaily.ui.R
import com.hika.mirodaily.ui.databinding.DialogGameAiChatBinding

/**
 * 底部弹出的 AI 对话框：
 * - 上方是消息区域（LinearLayout 动态添加 TextView）
 * - 下方是输入框 + 发送按钮
 * 这里先用假数据演示，后续你可以在 askGameAi 中接入真正的 AI 接口。
 */
class GameAiChatBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogGameAiChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogGameAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 发送按钮
        binding.btnSend.setOnClickListener {
            val text = binding.etInput.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@setOnClickListener

            // 1. 把用户问题先加入对话
            addMessage(text, isUser = true)
            binding.etInput.setText("")

            // 2. 调用 AI（此处先用假数据，后面可替换为真实网络请求）
            askGameAi(text)
        }
    }

    private fun askGameAi(question: String) {
        // TODO: 在这里接入你的 AI / 后端接口，例如通过 Retrofit/OkHttp 等
        // 网络请求完成后，回调里调用 addMessage(answer, isUser = false) 即可。

        // 目前先给一个假的回复示例，保证功能可跑通：
        val fakeAnswer = "这是针对【$question】的一些游戏建议示例（请在 GameAiChatBottomSheet.askGameAi 中接入真实 AI 接口替换我）。"
        addMessage(fakeAnswer, isUser = false)
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val tv = TextView(requireContext()).apply {
            this.text = text

            // 不再使用任何 R.style.*，直接写死样式，避免 Unresolved reference
            textSize = 14f
            setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    context,
                    android.R.color.black
                )
            )

            val padding = resources.getDimensionPixelSize(R.dimen.chat_message_padding)
            setPadding(padding, padding, padding, padding)

            background = androidx.core.content.ContextCompat.getDrawable(
                context,
                if (isUser) R.drawable.bg_chat_bubble_user else R.drawable.bg_chat_bubble_ai
            )
        }

        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val margin = resources.getDimensionPixelSize(R.dimen.chat_message_margin_vertical)
        lp.topMargin = margin
        lp.bottomMargin = margin
        lp.gravity = if (isUser) Gravity.END else Gravity.START

        binding.messageContainer.addView(tv, lp)

        // 滚动到底部
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }


    companion object {
        const val TAG = "GameAiChatBottomSheet"
    }
}
