package com.mukhtabir.android.ui.components

import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.MaterialIcons

data class CategoryIconOption(val value: String, val label: Int, val nativeIcon: String)
val categoryIconOptions = listOf(
    CategoryIconOption("bi-folder", R.string.category_icon_folder, MaterialIcons.CATEGORIES),
    CategoryIconOption("bi-book", R.string.category_icon_book, MaterialIcons.QUESTIONS),
    CategoryIconOption("bi-heart-pulse", R.string.category_icon_psychiatry, MaterialIcons.VERIFIED),
    CategoryIconOption("bi-brain", R.string.category_icon_brain, MaterialIcons.MAP),
    CategoryIconOption("bi-emoji-frown", R.string.category_icon_depression, MaterialIcons.FRAGILE),
    CategoryIconOption("bi-emoji-angry", R.string.category_icon_bipolar, MaterialIcons.FRAGILE),
    CategoryIconOption("bi-chat-dots", R.string.category_icon_anxiety, MaterialIcons.INFO),
    CategoryIconOption("bi-cup-straw", R.string.category_icon_addiction, MaterialIcons.QUESTIONS),
    CategoryIconOption("bi-alarm", R.string.category_icon_sleep, MaterialIcons.PLANNER),
    CategoryIconOption("bi-hospital", R.string.category_icon_geriatric, MaterialIcons.HOME),
    CategoryIconOption("bi-people", R.string.category_icon_pediatric, MaterialIcons.GROUPS),
    CategoryIconOption("bi-capsule", R.string.category_icon_medication, MaterialIcons.QUESTIONS),
    CategoryIconOption("bi-gavel", R.string.category_icon_forensic, MaterialIcons.VERIFIED),
    CategoryIconOption("bi-exclamation-triangle", R.string.category_icon_emergency, MaterialIcons.FRAGILE),
)
