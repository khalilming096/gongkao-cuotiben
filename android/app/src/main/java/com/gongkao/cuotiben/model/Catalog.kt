package com.gongkao.cuotiben.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 子类：对应题库里的一层分类 */
data class SubCategory(
    val key: String,
    val label: String,
    val total: Int,
    val details: List<Detail>,
    val merged: Int
) {
    data class Detail(val name: String, val count: Int)
}

/** 题型：题库里的中间层，带题型专属错因 */
data class Topic(
    val key: String,
    val label: String,
    val total: Int,
    val errors: List<String>,
    val hint: String,
    val subs: List<SubCategory>
)

data class CatalogModule(
    val key: String,
    val label: String,
    val short: String,
    val total: Int,
    val topics: List<Topic>
)

/**
 * 分类目录：从 assets/catalog.json 读取，与网页版共用同一份数据。
 * 由 tools/gen_catalog.py 扫描题库 source 路径生成，不在代码里手写。
 */
object Catalog {

    private var loaded: Map<String, CatalogModule> = emptyMap()

    /** 在 Application / ViewModel 初始化时调用一次 */
    fun load(context: Context) {
        if (loaded.isNotEmpty()) return
        val text = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        loaded = parse(JSONObject(text))
    }

    fun module(key: String): CatalogModule? = loaded[key]

    fun topicsOf(moduleKey: String): List<Topic> = loaded[moduleKey]?.topics ?: emptyList()

    fun topicOf(moduleKey: String, topicKey: String): Topic? =
        topicsOf(moduleKey).firstOrNull { it.key == topicKey }

    fun subOf(moduleKey: String, topicKey: String, subKey: String): SubCategory? =
        topicOf(moduleKey, topicKey)?.subs?.firstOrNull { it.key == subKey }

    fun hasTopics(moduleKey: String): Boolean = topicsOf(moduleKey).isNotEmpty()

    /** v2.1 手写的英文子类 key → 题库分类（题型 key + 子类名），用于旧数据迁移 */
    fun legacyMapping(moduleKey: String, groupKey: String, subKey: String): Pair<String, String>? =
        LEGACY_SUB_MAP["$moduleKey|$groupKey|$subKey"]

    private val LEGACY_SUB_MAP = mapOf(
        "yy|ltk|yjdy" to ("ltk" to "语境分析"),
        "yy|ltk|scby" to ("ltk" to "实词填空"),
        "yy|ltk|cyby" to ("ltk" to "成语填空"),
        "yy|ltk|glc" to ("ltk" to "语境分析"),
        "yy|ltk|dkzh" to ("ltk" to "混搭填空"),
        "yy|pdyy|zxll" to ("pdyy" to "中心理解"),
        "yy|pdyy|xpjd" to ("pdyy" to "细节判断"),
        "yy|pdyy|btlr" to ("pdyy" to "标题填入"),
        "yy|pdyy|yybd" to ("yybd" to "其他"),
        "yy|pdyy|pwyda" to ("pdyy" to "其他"),
        "yy|pdyy|pwyd" to ("pdyy" to "其他"),
        "pd|txxx|wzgl" to ("txxx" to "位置规律"),
        "pd|txxx|ysgl" to ("txxx" to "样式规律"),
        "pd|txxx|slgl" to ("txxx" to "数量规律"),
        "pd|txxx|sxgl" to ("txxx" to "属性规律"),
        "pd|txxx|ltfg" to ("txxx" to "立体图形"),
        "pd|dypd|ddyd" to ("dypd" to "单定义"),
        "pd|dypd|ddby" to ("dypd" to "多定义"),
        "pd|dypd|fwsw" to ("dypd" to "其他"),
        "pd|lbtl|yygx" to ("lbtl" to "语义关系"),
        "pd|lbtl|ljgx" to ("lbtl" to "逻辑关系"),
        "pd|lbtl|yfgx" to ("lbtl" to "语法关系"),
        "pd|lbtl|ejby" to ("lbtl" to "其他"),
        "pd|ljtl|fytl" to ("ljpd" to "翻译推理"),
        "pd|ljtl|zjtl" to ("ljpd" to "真假推理"),
        "pd|ljtl|jqxr" to ("ljpd" to "加强削弱"),
        "pd|ljtl|rqjl" to ("ljpd" to "结论推出"),
        "pd|ljtl|jspj" to ("ljpd" to "评价型")
    )

    /** 层级面包屑：言语理解 · 片段阅读 · 中心理解 */
    fun breadcrumb(module: StudyModule, groupKey: String, subKey: String): String {
        val topic = topicOf(module.key, groupKey) ?: return module.label
        val sub = topic.subs.firstOrNull { it.key == subKey } ?: return "${module.label} · ${topic.label}"
        return "${module.label} · ${topic.label} · ${sub.label}"
    }

    /** 错因：题型专属 + 通用兜底 */
    fun reasonsFor(module: StudyModule, topicKey: String): Pair<List<String>, List<String>> {
        val own = topicOf(module.key, topicKey)?.errors?.takeIf { it.isNotEmpty() }
            ?: MODULE_ERRORS[module.key].orEmpty()
        return own to COMMON_REASONS.filter { it !in own }
    }

    /** 错因输入框的示例文案 */
    fun hintFor(module: StudyModule, topicKey: String): String {
        return topicOf(module.key, topicKey)?.hint?.takeIf { it.isNotEmpty() }
            ?: MODULE_HINTS[module.key]
            ?: "说说当时是怎么错的"
    }

    /** 名称输入框的示例：优先用该子类最常见的细分名 */
    fun namePlaceholder(module: StudyModule, sub: SubCategory?): String {
        val first = sub?.details?.firstOrNull()?.name
        if (!first.isNullOrBlank()) return "如：$first"
        return MODULE_NAME_HINTS[module.key] ?: "如：把某个细节记混了"
    }

    private fun parse(root: JSONObject): Map<String, CatalogModule> {
        val modules = root.optJSONArray("modules") ?: JSONArray()
        val map = LinkedHashMap<String, CatalogModule>()
        for (i in 0 until modules.length()) {
            val m = modules.getJSONObject(i)
            val topicsJson = m.optJSONArray("topics") ?: JSONArray()
            val topics = ArrayList<Topic>(topicsJson.length())
            for (j in 0 until topicsJson.length()) {
                val t = topicsJson.getJSONObject(j)
                val subsJson = t.optJSONArray("subs") ?: JSONArray()
                val subs = ArrayList<SubCategory>(subsJson.length())
                for (k in 0 until subsJson.length()) {
                    val s = subsJson.getJSONObject(k)
                    val detailsJson = s.optJSONArray("details") ?: JSONArray()
                    val details = ArrayList<SubCategory.Detail>(detailsJson.length())
                    for (d in 0 until detailsJson.length()) {
                        val one = detailsJson.getJSONObject(d)
                        details.add(
                            SubCategory.Detail(
                                name = one.optString("name"),
                                count = one.optInt("count")
                            )
                        )
                    }
                    subs.add(
                        SubCategory(
                            key = s.optString("key"),
                            label = s.optString("label"),
                            total = s.optInt("total"),
                            details = details,
                            merged = s.optInt("merged")
                        )
                    )
                }
                val errorsJson = t.optJSONArray("errors") ?: JSONArray()
                val errors = ArrayList<String>(errorsJson.length())
                for (e in 0 until errorsJson.length()) errors.add(errorsJson.optString(e))

                topics.add(
                    Topic(
                        key = t.optString("key"),
                        label = t.optString("label"),
                        total = t.optInt("total"),
                        errors = errors,
                        hint = t.optString("hint"),
                        subs = subs
                    )
                )
            }
            map[m.optString("key")] = CatalogModule(
                key = m.optString("key"),
                label = m.optString("label"),
                short = m.optString("short"),
                total = m.optInt("total"),
                topics = topics
            )
        }
        return map
    }

    private val COMMON_REASONS = listOf("审题不清", "时间不够", "蒙错", "知识点遗忘")

    private val MODULE_ERRORS = mapOf(
        "zzll" to listOf("时政记混", "理论表述记错", "会议精神混淆"),
        "cs" to listOf("法律条文混", "历史年代错", "地理常识混", "科技成就记错")
    )

    private val MODULE_HINTS = mapOf(
        "zzll" to "把「两个确立」和「两个维护」记混了",
        "cs" to "把行政复议和行政诉讼搞混了"
    )

    private val MODULE_NAME_HINTS = mapOf(
        "zzll" to "如：新发展理念的内涵",
        "cs" to "如：二十四节气顺序"
    )
}
