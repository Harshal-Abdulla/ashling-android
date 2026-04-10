package com.example.localllm

// ModelInfo = a blueprint for one AI model entry in the library
// Each model the app supports is represented as one ModelInfo object
data class ModelInfo(
    val displayName: String,    // shown to user: "Gemma 1.1 2B"
    val description: String,    // short description of what it's good at
    val fileName: String,       // exact filename on disk: "gemma-1.1-2b-it-cpu-int4.bin"
    val sizeLabel: String,      // shown to user: "~1.3 GB"
    val license: String,        // license name: "Gemma Terms of Use"
    val licenseUrl: String,     // full license URL for users to read
    val kaggleVariation: String // Kaggle variation name for download instructions
)

// All models the app supports — add more here as we support them
object ModelLibrary {
    val models = listOf(
        ModelInfo(
            displayName = "Gemma 1.1 2B",
            description = "Great for general chat, Q&A, and everyday tasks. Fast on most phones.",
            fileName = "gemma-1.1-2b-it-cpu-int4.bin",
            sizeLabel = "~1.3 GB",
            license = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            kaggleVariation = "gemma-1.1-2b-it-cpu-int4"
        ),
        ModelInfo(
            displayName = "Gemma 2B",
            description = "Original Gemma 2B model. Good general performance.",
            fileName = "gemma-2b-it-cpu-int4.bin",
            sizeLabel = "~1.4 GB",
            license = "Gemma Terms of Use",
            licenseUrl = "https://ai.google.dev/gemma/terms",
            kaggleVariation = "gemma-2b-it-cpu-int4"
        )
    )
}
