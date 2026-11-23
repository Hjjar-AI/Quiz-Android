package com.mukhtabir.android.ui.demo

import com.mukhtabir.android.shared.R

data class DemoQuestion(val stem: Int, val choices: List<Int>, val correct: Int, val explanation: Int,
    val category: Int = 2, val caseStem: Int? = null, val tags: List<Int> = listOf(R.string.demo_tag_foundations))

val demoQuestions = listOf(
    DemoQuestion(R.string.demo_q1, listOf(R.string.demo_q1_c1, R.string.demo_q1_c2, R.string.demo_q1_c3, R.string.demo_q1_c4), 0, R.string.demo_q1_explanation),
    DemoQuestion(R.string.demo_q2, listOf(R.string.demo_q2_c1, R.string.demo_q2_c2, R.string.demo_q2_c3, R.string.demo_q2_c4), 1, R.string.demo_q2_explanation, category = 1, caseStem = R.string.demo_case,
        tags = listOf(R.string.demo_tag_foundations, R.string.demo_tag_case_context)),
    DemoQuestion(R.string.demo_q3, listOf(R.string.demo_q3_c1, R.string.demo_q3_c2, R.string.demo_q3_c3, R.string.demo_q3_c4), 2, R.string.demo_q3_explanation, category = 1),
    DemoQuestion(R.string.demo_q4, listOf(R.string.demo_q4_c1, R.string.demo_q4_c2, R.string.demo_q4_c3, R.string.demo_q4_c4), 3, R.string.demo_q4_explanation, category = 1, caseStem = R.string.demo_case,
        tags = listOf(R.string.demo_tag_foundations, R.string.demo_tag_case_context)),
    DemoQuestion(R.string.demo_q5, listOf(R.string.demo_q5_c1, R.string.demo_q5_c2, R.string.demo_q5_c3, R.string.demo_q5_c4), 0, R.string.demo_q5_explanation),
    DemoQuestion(R.string.demo_q6, listOf(R.string.demo_q6_c1, R.string.demo_q6_c2, R.string.demo_q6_c3, R.string.demo_q6_c4), 1, R.string.demo_q6_explanation),
    DemoQuestion(R.string.demo_q7, listOf(R.string.demo_q7_c1, R.string.demo_q7_c2, R.string.demo_q7_c3, R.string.demo_q7_c4), 2, R.string.demo_q7_explanation),
    DemoQuestion(R.string.demo_q8, listOf(R.string.demo_q8_c1, R.string.demo_q8_c2, R.string.demo_q8_c3, R.string.demo_q8_c4), 3, R.string.demo_q8_explanation),
)
