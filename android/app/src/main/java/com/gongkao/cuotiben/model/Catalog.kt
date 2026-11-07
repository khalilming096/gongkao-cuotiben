package com.gongkao.cuotiben.model

/**
 * 子类：子模块内部的最小分类单元。
 *
 * @param key 唯一标识，存进知识点表的 subKey 字段
 * @param label 展示名，2–4 字名词短语，与模块名风格一致
 * @param basis 该子类的判定依据，用于新增时提示「这条错题该归到哪」
 */
data class SubCategory(
    val key: String,
    val label: String,
    val basis: String
)

/**
 * 子模块：模块下的第二层，只在言语理解、判断推理两个模块存在。
 *
 * @param dimension 细分维度，即这一层按什么把题目切开
 */
data class SubGroup(
    val key: String,
    val label: String,
    val dimension: String,
    val categories: List<SubCategory>
)

/**
 * 分类目录。
 *
 * 设计原则：
 * 1. 子模块按「题型」切分，与行测真题的题号顺序一致，考场上想到哪个题型就能直接找到；
 * 2. 子类按「解法差异」切分，同一子模块下的子类必须对应不同的解题动作，
 *    避免出现「知道归到哪但解法一样」的无效分类；
 * 3. 政治理论、常识、资料分析三个模块考点线性、题量集中，不设子模块，直接挂知识点。
 */
object Catalog {

    // ---------------- 言语理解 ----------------

    val verbal: List<SubGroup> = listOf(
        SubGroup(
            key = "ltk",
            label = "逻辑填空",
            dimension = "按「先看语境、再辨词语」的解题顺序细分",
            categories = listOf(
                SubCategory("yjdy", "语境对应", "解释、反对、并列、递进四类逻辑关系的呼应点没找准"),
                SubCategory("scby", "实词辨析", "词义侧重、搭配范围、语体色彩三项辨析不清"),
                SubCategory("cyby", "成语辨析", "望文生义、褒贬误用、对象误用、语义轻重"),
                SubCategory("glc", "关联词与虚词", "转折、因果、条件、递进等关联词搭配错位"),
                SubCategory("dkzh", "多空综合", "多空题未按「先易后难、逐空排除」处理")
            )
        ),
        SubGroup(
            key = "pdyy",
            label = "片段阅读",
            dimension = "按「设问方式」细分，设问决定找什么、找多细",
            categories = listOf(
                SubCategory("zxll", "中心理解", "主旨概括、意图判断，未抓中心句与主题词"),
                SubCategory("xpjd", "细节判断", "偷换概念、无中生有、以偏概全、时态错误"),
                SubCategory("btlr", "标题填入", "新闻、说明、议论三类文体对标题的凝练要求不同"),
                SubCategory("yybd", "语句表达", "语句排序、语句填空、下文推断，抓首尾句与话题一致"),
                SubCategory("pwyda", "篇章阅读", "多段材料综合，未先看题再回文定位")
            )
        )
    )

    // ---------------- 判断推理 ----------------

    val judgement: List<SubGroup> = listOf(
        SubGroup(
            key = "txxx",
            label = "图形推理",
            dimension = "按「规律类型」细分，同时就是考场上的观察顺序",
            categories = listOf(
                SubCategory("wzgl", "位置规律", "平移、旋转、翻转，元素组成完全相同时优先看"),
                SubCategory("ysgl", "样式规律", "遍历、加减同异、黑白叠加，元素组成相似时看"),
                SubCategory("slgl", "数量规律", "点、线、面、角、素，元素组成凌乱时数"),
                SubCategory("sxgl", "属性规律", "对称性、曲直性、开闭性，图形规整时看"),
                SubCategory("ltfg", "立体与分组", "空间重构、截面图、三视图、分组分类")
            )
        ),
        SubGroup(
            key = "dypd",
            label = "定义判断",
            dimension = "按「定义数量 + 设问方向」细分",
            categories = listOf(
                SubCategory("ddyd", "单定义判断", "只给一个定义，抓主体、客体、条件、方式、结果五要素"),
                SubCategory("ddby", "多定义辨析", "多个定义并列，先对号入座再区分相近定义"),
                SubCategory("fwsw", "反向设问", "「不属于」「不符合」类，须逐项验证后再排除")
            )
        ),
        SubGroup(
            key = "lbtl",
            label = "类比推理",
            dimension = "按「关系层级」细分：先语义、再逻辑、最后语法",
            categories = listOf(
                SubCategory("yygx", "语义关系", "近义、反义、比喻象征，先看词义是否相关"),
                SubCategory("ljgx", "逻辑关系", "全同、包含、交叉、并列、对应（因果、条件、目的）"),
                SubCategory("yfgx", "语法关系", "主谓、动宾、偏正、并列，词性结构是否一致"),
                SubCategory("ejby", "二级辨析", "一级关系排除不掉时，用词性、感情色彩、范围大小二次筛选")
            )
        ),
        SubGroup(
            key = "ljtl",
            label = "逻辑推理",
            dimension = "按「题型与论证结构」细分",
            categories = listOf(
                SubCategory("fytl", "翻译推理", "假言命题、递推、逆否等价，先翻译成箭头再推"),
                SubCategory("zjtl", "真假推理", "矛盾关系、反对关系，找不到矛盾就用代入排除"),
                SubCategory("jqxr", "加强削弱", "先定论点论据，再看是搭桥拆桥还是补否论据"),
                SubCategory("rqjl", "日常结论", "归纳推理与原因解释，选项不能过度推断"),
                SubCategory("jspj", "解释评价", "解释矛盾现象、评价论证方法与逻辑漏洞")
            )
        )
    )

    /** 某模块下的子模块列表；没有子模块的模块返回空列表 */
    fun groupsOf(module: StudyModule): List<SubGroup> = when (module) {
        StudyModule.VERBAL -> verbal
        StudyModule.JUDGEMENT -> judgement
        else -> emptyList()
    }

    fun groupOf(module: StudyModule, groupKey: String): SubGroup? =
        groupsOf(module).firstOrNull { it.key == groupKey }

    fun categoryOf(group: SubGroup?, subKey: String): SubCategory? =
        group?.categories?.firstOrNull { it.key == subKey }

    /** 层级路径文案，例如「言语理解 · 逻辑填空 · 成语辨析」 */
    fun breadcrumb(module: StudyModule, groupKey: String, subKey: String): String {
        val group = groupOf(module, groupKey) ?: return module.label
        val category = categoryOf(group, subKey) ?: return "${module.label} · ${group.label}"
        return "${module.label} · ${group.label} · ${category.label}"
    }
}
