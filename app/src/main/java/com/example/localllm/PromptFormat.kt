package com.example.localllm

/**
 * How to lay out a conversation for a particular model.
 *
 * Every model family was fine-tuned with its own chat template, and they are
 * not interchangeable. Sending Gemma's <start_of_turn> to a Qwen model means it
 * never sees a structure it recognises, so it does not answer the question.
 * It just carries on writing text, which is where the echoed prompts and the
 * invented follow-up questions were coming from.
 *
 * Getting this right matters far more than any amount of tidying up the reply
 * afterwards.
 */
enum class PromptFormat {
    /** Gemma: <start_of_turn>user … <end_of_turn> */
    GEMMA,

    /** ChatML, used by Qwen, SmolLM and DeepSeek: <|im_start|>user … <|im_end|> */
    CHATML,

    /** Zephyr, used by TinyLlama: <|user|> … </s> <|assistant|> */
    ZEPHYR,

    /** Phi: <|user|> … <|end|><|assistant|> */
    PHI;

    /** The token that ends the model's turn, used to trim the reply. */
    val stopToken: String
        get() = when (this) {
            GEMMA -> "<end_of_turn>"
            CHATML -> "<|im_end|>"
            ZEPHYR -> "</s>"
            PHI -> "<|end|>"
        }

    /**
     * Builds the prompt. [history] is the recent turns, oldest first, and the
     * result ends part-way through the model's turn so it continues from there.
     */
    fun build(history: List<ChatMessage>, systemInstruction: String): String {
        val sb = StringBuilder()

        when (this) {
            GEMMA -> {
                history.forEachIndexed { i, m ->
                    val body = if (i == 0 && m.isUser) "$systemInstruction\n\n${m.text}" else m.text
                    if (m.isUser) sb.append("<start_of_turn>user\n$body<end_of_turn>\n")
                    else if (m.text.isNotEmpty()) sb.append("<start_of_turn>model\n${m.text}<end_of_turn>\n")
                }
                sb.append("<start_of_turn>model\n")
            }

            CHATML -> {
                sb.append("<|im_start|>system\n$systemInstruction<|im_end|>\n")
                history.forEach { m ->
                    val role = if (m.isUser) "user" else "assistant"
                    if (m.text.isNotEmpty()) sb.append("<|im_start|>$role\n${m.text}<|im_end|>\n")
                }
                sb.append("<|im_start|>assistant\n")
            }

            ZEPHYR -> {
                sb.append("<|system|>\n$systemInstruction</s>\n")
                history.forEach { m ->
                    val role = if (m.isUser) "user" else "assistant"
                    if (m.text.isNotEmpty()) sb.append("<|$role|>\n${m.text}</s>\n")
                }
                sb.append("<|assistant|>\n")
            }

            PHI -> {
                sb.append("<|system|>$systemInstruction<|end|>")
                history.forEach { m ->
                    val role = if (m.isUser) "user" else "assistant"
                    if (m.text.isNotEmpty()) sb.append("<|$role|>${m.text}<|end|>")
                }
                sb.append("<|assistant|>")
            }
        }

        return sb.toString()
    }
}
