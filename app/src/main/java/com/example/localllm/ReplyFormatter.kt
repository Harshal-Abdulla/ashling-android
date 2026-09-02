package com.example.localllm

/**
 * Tidies up what the model writes so it can just be read.
 *
 * Small models are trained on markdown and LaTeX, so replies come back full of
 * **stars**, ### hashes and \( maths \) wrappers. There is no markdown renderer
 * here, so all of that shows up literally and makes the answer hard to read.
 * Reasoning models like DeepSeek also print their working inside <think> tags,
 * which is not something you want in a chat bubble.
 *
 * This strips the markup rather than rendering it. Plain text in a TextView is
 * good enough for a chat reply, and it is a lot less work than pulling in a
 * markdown library.
 */
object ReplyFormatter {

    /**
     * Tokens that mean "this turn is over".
     *
     * The prompt is written in Gemma's format, so a model trained on a
     * different chat template does not recognise the stop token and just keeps
     * going — it writes its own answer, then invents the next question and
     * answers that too. Cutting at the first of these keeps the reply to the
     * bit that was actually meant for you.
     */
    private val STOP_MARKERS = listOf(
        "<end_of_turn>",
        "<start_of_turn>",
        "<|im_end|>",
        "<|im_start|>",
        "<|endoftext|>",
        "<|eot_id|>",
        "<|start_header_id|>",
        "</s>"
    )

    /**
     * Any special token of the form <|...|>, including the malformed closing
     * ones like </|user|> that these models sometimes emit.
     *
     * Listing them individually was not enough. Every model family uses its own
     * names — im_start, user, assistant, system — and a new model brings new
     * ones. Matching the shape catches them all.
     */
    private val SPECIAL_TOKEN = Regex("""</?\|[a-zA-Z0-9_]+\|>""")

    /**
     * The instruction that gets prepended to the first user message. Small
     * models often echo it straight back instead of acting on it, so it has to
     * be recognised here to be removed.
     */
    const val SYSTEM_INSTRUCTION =
        "You are a helpful AI assistant. Answer questions directly and concisely."

    fun clean(raw: String): String {
        var text = raw

        // Drop an echoed prompt. A model that continues the text rather than
        // answering it will repeat the system line, and sometimes the question,
        // before it gets to the reply.
        text = text.trimStart()
        if (text.startsWith(SYSTEM_INSTRUCTION)) {
            text = text.removePrefix(SYSTEM_INSTRUCTION).trimStart()
        }

        // Stop at whichever end-of-turn marker shows up first.
        //
        // If that would throw the whole reply away — which happens when the
        // model opens with a marker instead of ending with one — keep the text
        // and just delete the markers. An awkward answer is still better than
        // an empty bubble.
        val markerCuts = STOP_MARKERS.mapNotNull { marker ->
            text.indexOf(marker).takeIf { it >= 0 }
        }
        val specialCut = SPECIAL_TOKEN.find(text)?.range?.first
        val cut = (markerCuts + listOfNotNull(specialCut)).minOrNull()
        text = if (cut != null && text.substring(0, cut).isNotBlank()) {
            text.substring(0, cut)
        } else {
            SPECIAL_TOKEN.replace(
                STOP_MARKERS.fold(text) { acc, marker -> acc.replace(marker, "") },
                ""
            )
        }

        // Reasoning models think out loud first. Drop it — including the case
        // where the reply is cut off mid-thought and the tag never closes.
        text = text.replace(Regex("(?s)<think>.*?</think>"), "")
        text = text.replace(Regex("(?s)<think>.*"), "")

        // LaTeX wrappers. \(x\) and \[x\] carry no meaning without a renderer.
        text = text.replace(Regex("""\\[\(\)\[\]]"""), "")
        text = text.replace(Regex("""\\(times|div|cdot)\b"""), "×")
        text = text.replace(Regex("""\\text\{([^}]*)\}"""), "$1")
        text = text.replace(Regex("""\\boxed\{([^}]*)\}"""), "$1")

        // Headings: keep the words, lose the hashes.
        text = text.replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")

        // Bold and italic markers.
        text = text.replace(Regex("""\*\*\*(.+?)\*\*\*"""), "$1")
        text = text.replace(Regex("""\*\*(.+?)\*\*"""), "$1")
        text = text.replace(Regex("""(?<![*\w])\*(?!\s)(.+?)(?<!\s)\*(?![*\w])"""), "$1")
        text = text.replace(Regex("""(?<![_\w])__(.+?)__(?![_\w])"""), "$1")

        // Inline code and fences.
        text = text.replace(Regex("(?m)^\\s*```.*$"), "")
        text = text.replace(Regex("`([^`]+)`"), "$1")

        // Bullets: a real bullet reads better than a stray asterisk or dash.
        text = text.replace(Regex("(?m)^\\s*[*+-]\\s+"), "• ")

        // Horizontal rules.
        text = text.replace(Regex("(?m)^\\s*([-*_]\\s*){3,}$"), "")

        // Tidy the whitespace the substitutions leave behind.
        text = text.replace(Regex("[ \\t]+\n"), "\n")
        text = text.replace(Regex("\n{3,}"), "\n\n")

        return text.trim()
    }
}
