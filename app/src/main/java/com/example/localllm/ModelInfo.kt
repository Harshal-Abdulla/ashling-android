package com.example.localllm

data class ModelInfo(
    val displayName: String,
    val description: String,
    val fileName: String,
    val sizeLabel: String,       // storage needed: "~1.3 GB"
    val ramLabel: String,        // minimum RAM: "3 GB RAM"
    val deviceTag: String,       // "Works on most phones"
    val deviceTagColor: String,  // "green", "orange", "red"
    val license: String,
    val licenseUrl: String,
    val kaggleUrl: String,       // browser link to Kaggle page
    val kaggleApiPath: String,   // used for direct API download
                                 // format: "owner/model/framework/variation/version"

    // If this is set the model downloads straight from the URL with no login.
    // Only the Gemma ones need a Kaggle account, because that is where Google
    // puts them.
    val directUrl: String? = null,

    // Which chat template this model was trained with. Getting this wrong is
    // what made replies ramble instead of answering.
    val promptFormat: PromptFormat = PromptFormat.GEMMA
) {
    val needsKaggle: Boolean get() = directUrl == null
}

object ModelLibrary {
    val models = listOf(

        ModelInfo(
            displayName = "SmolLM 135M, smallest",
            description = "Tiny model from Hugging Face. Downloads in a minute and runs on anything, but at 135M parameters the answers are rough. Good for checking the app works; get Qwen for real use.",
            fileName = "smollm-135m-q8.task",
            sizeLabel = "~160 MB",
            ramLabel = "1 GB RAM",
            deviceTag = "Works on any phone",
            deviceTagColor = "green",
            license = "Apache 2.0",
            licenseUrl = "https://huggingface.co/HuggingFaceTB/SmolLM-135M-Instruct",
            kaggleUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct",
            kaggleApiPath = "",
            directUrl = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task"
        ,
            promptFormat = PromptFormat.CHATML),

        ModelInfo(
            displayName = "TinyLlama 1.1B",
            description = "Small chat model, no account needed. A decent step up from SmolLM without asking much of the phone.",
            fileName = "tinyllama-1.1b-q8.task",
            sizeLabel = "~1.1 GB",
            ramLabel = "3 GB RAM",
            deviceTag = "Works on most phones",
            deviceTagColor = "green",
            license = "Apache 2.0",
            licenseUrl = "https://huggingface.co/TinyLlama/TinyLlama-1.1B-Chat-v1.0",
            kaggleUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0",
            kaggleApiPath = "",
            directUrl = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task"
        ,
            promptFormat = PromptFormat.ZEPHYR),

        ModelInfo(
            displayName = "Qwen 2.5 1.5B, recommended",
            description = "Alibaba's small model, and the best balance here. Answers real questions properly rather than the rambling the tiny ones produce. Start with this if your phone can take it.",
            fileName = "qwen2.5-1.5b-q8.task",
            sizeLabel = "~1.5 GB",
            ramLabel = "4 GB RAM",
            deviceTag = "Works on most phones",
            deviceTagColor = "green",
            license = "Apache 2.0",
            licenseUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct",
            kaggleUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct",
            kaggleApiPath = "",
            directUrl = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"
        ,
            promptFormat = PromptFormat.CHATML),

        ModelInfo(
            displayName = "DeepSeek R1 1.5B",
            description = "Reasoning model. It works through the problem before answering, so replies are slower but better on maths. No account needed.",
            fileName = "deepseek-r1-1.5b-q8.task",
            sizeLabel = "~1.8 GB",
            ramLabel = "4 GB RAM",
            deviceTag = "Newer phones",
            deviceTagColor = "orange",
            license = "MIT",
            licenseUrl = "https://huggingface.co/deepseek-ai/DeepSeek-R1-Distill-Qwen-1.5B",
            kaggleUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
            kaggleApiPath = "",
            directUrl = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/deepseek_q8_ekv1280.task"
        ,
            promptFormat = PromptFormat.CHATML),

        ModelInfo(
            displayName = "Phi-4 Mini, best quality",
            description = "Microsoft's 3.8B model and the best one here by a distance. Only worth it on a recent flagship. Anything less and it will either refuse to load or crawl.",
            fileName = "phi-4-mini-q8.task",
            sizeLabel = "~3.8 GB",
            ramLabel = "8 GB RAM",
            deviceTag = "Flagship phones only",
            deviceTagColor = "red",
            license = "MIT",
            licenseUrl = "https://huggingface.co/microsoft/Phi-4-mini-instruct",
            kaggleUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct",
            kaggleApiPath = "",
            directUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task"
        ,
            promptFormat = PromptFormat.PHI),

        ModelInfo(
            displayName = "Gemma 2 2B, recommended",
            description = "Google's latest 2B model. Much smarter than Gemma 1.1. Best balance of speed and quality.",
            fileName = "gemma-2-2b-it-cpu-int4.bin",
            sizeLabel = "~1.3 GB",
            ramLabel = "3 GB RAM",
            deviceTag = "Works on most phones",
            deviceTagColor = "green",
            license = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            kaggleUrl = "https://www.kaggle.com/models/google/gemma2/frameworks/tfLite/variations/gemma-2-2b-it-cpu-int4",
            kaggleApiPath = "google/gemma2/tflite/gemma-2-2b-it-cpu-int4/1"
        ),

        ModelInfo(
            displayName = "Gemma 2 2B, high quality",
            description = "Higher precision version of Gemma 2. Smarter answers, needs more storage and RAM.",
            fileName = "gemma-2-2b-it-cpu-int8.bin",
            sizeLabel = "~2.6 GB",
            ramLabel = "5 GB RAM",
            deviceTag = "High-end phones only",
            deviceTagColor = "orange",
            license = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            kaggleUrl = "https://www.kaggle.com/models/google/gemma2/frameworks/tfLite/variations/gemma-2-2b-it-cpu-int8",
            kaggleApiPath = "google/gemma2/tflite/gemma-2-2b-it-cpu-int8/1"
        ),

        ModelInfo(
            displayName = "Gemma 1.1 2B",
            description = "Older Gemma model. Good fallback if Gemma 2 is too slow on your device.",
            fileName = "gemma-1.1-2b-it-cpu-int4.bin",
            sizeLabel = "~1.3 GB",
            ramLabel = "3 GB RAM",
            deviceTag = "Works on most phones",
            deviceTagColor = "green",
            license = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            kaggleUrl = "https://www.kaggle.com/models/google/gemma/frameworks/tfLite/variations/gemma-1.1-2b-it-cpu-int4",
            kaggleApiPath = "google/gemma/tflite/gemma-1.1-2b-it-cpu-int4/1"
        ),

        ModelInfo(
            displayName = "Gemma 2B (Original)",
            description = "The first Gemma 2B release. Gemma 2 2B is better in every way, so only use this if you already have it.",
            fileName = "gemma-2b-it-cpu-int4.bin",
            sizeLabel = "~1.4 GB",
            ramLabel = "3 GB RAM",
            deviceTag = "Works on most phones",
            deviceTagColor = "green",
            license = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            kaggleUrl = "https://www.kaggle.com/models/google/gemma/frameworks/tfLite/variations/gemma-2b-it-cpu-int4",
            kaggleApiPath = "google/gemma/tflite/gemma-2b-it-cpu-int4/1"
        )
    )
}
