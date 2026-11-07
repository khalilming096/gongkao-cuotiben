package com.gongkao.cuotiben.model

import androidx.compose.ui.graphics.Color

/**
 * 五大考试模块，顺序与主界面展示顺序一致，属于固定入口。
 */
enum class StudyModule(
    val key: String,
    val label: String,
    val short: String,
    val subtitle: String,
    val mainColor: Color,
    val softColor: Color
) {
    POLITICS(
        key = "zzll",
        label = "政治理论",
        short = "政",
        subtitle = "时政热点 · 理论 · 党建",
        mainColor = Color(0xFFC0392B),
        softColor = Color(0xFFFBEDEC)
    ),
    COMMON(
        key = "cs",
        label = "常识",
        short = "常",
        subtitle = "法律 · 历史 · 科技 · 地理",
        mainColor = Color(0xFFB4791C),
        softColor = Color(0xFFFBF3E2)
    ),
    VERBAL(
        key = "yy",
        label = "言语理解",
        short = "言",
        subtitle = "逻辑填空 · 片段阅读 · 排序",
        mainColor = Color(0xFF1F7A6B),
        softColor = Color(0xFFE6F4F0)
    ),
    JUDGEMENT(
        key = "pd",
        label = "判断推理",
        short = "判",
        subtitle = "图形 · 定义 · 类比 · 逻辑",
        mainColor = Color(0xFF4C4FBF),
        softColor = Color(0xFFECECFB)
    ),
    DATA(
        key = "zl",
        label = "资料分析",
        short = "资",
        subtitle = "ABRX · 速算 · 比重 · 平均数",
        mainColor = Color(0xFF2B6CB0),
        softColor = Color(0xFFE8F0FA)
    );

    companion object {
        fun of(key: String): StudyModule = values().firstOrNull { it.key == key } ?: DATA

        /** 按 key 反查模块，找不到返回 null（用于路由判断） */
        fun byKey(key: String): StudyModule? = values().firstOrNull { it.key == key }
    }
}

/** 新增知识点时的常用错因，点一下即可填入 */
val QUICK_REASONS = listOf(
    "公式混淆", "计算失误", "审题不清", "概念混淆",
    "时间不够", "选项陷阱", "知识点遗忘", "方法用错"
)
