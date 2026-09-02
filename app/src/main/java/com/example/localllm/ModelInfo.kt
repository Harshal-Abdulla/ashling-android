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
    val kaggleApiPath: String    // used for direct API download
                                 // format: "owner/model/framework/variation/version"
)

object ModelLibrary {
    val models = listOf(

        ModelInfo(
            displayName = "Gemma 2 2B — Recommended",
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
            displayName = "Gemma 2 2B — High Quality",
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
            description = "The first Gemma 2B release. Gemma 2 2B is better in every way — only use this if you already have it.",
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
