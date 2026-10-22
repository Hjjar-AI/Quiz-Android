package com.mukhtabir.android.ui.components

/** Reveal-safe presentation only. Callers decide whether correctness may be supplied. */
enum class ChoiceVisualState(val selected: Boolean, val correctness: Boolean? = null) {
    PLAIN(false), SELECTED(true), NEUTRAL_SELECTED(true), REVIEWED_SELECTED(true),
    CORRECT(false, true), SELECTED_CORRECT(true, true),
    INCORRECT(false, false), SELECTED_INCORRECT(true, false);

    companion object {
        fun from(selected: Boolean, correctness: Boolean?, neutral: Boolean, review: Boolean): ChoiceVisualState = when {
            correctness == true -> if (selected) SELECTED_CORRECT else CORRECT
            correctness == false -> if (selected) SELECTED_INCORRECT else INCORRECT
            !selected -> PLAIN
            review -> REVIEWED_SELECTED
            neutral -> NEUTRAL_SELECTED
            else -> SELECTED
        }
    }
}
