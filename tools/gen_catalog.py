# -*- coding: utf-8 -*-
"""
从「错题复盘」题库的 source 路径生成分类表（catalog.json）。

题库目录：模块 / 项目 / 10-真题 / [模块重复段] / 题型 / 二级分类[-三级] / [三级] / 题号.md

层级取「模块 → 题型 → 二级分类」，三级只作为新增知识点时的推荐名称，
避免把 300 多个细分全铺进筛选条。

用法： python tools/gen_catalog.py
"""
import collections
import glob
import io
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
BANK_DIR = os.path.join(os.path.expanduser("~"), "Documents", "错题复盘", "app", "dist", "data")
OUT = os.path.join(ROOT, "catalog.json")

MODULE_KEYS = {
    "言语理解": ("yy", "言"),
    "判断推理": ("pd", "判"),
    "资料分析": ("zl", "资"),
}

TOPIC_KEYS = {
    "片段阅读": "pdyy",
    "逻辑填空": "ltk",
    "语句表达": "yybd",
    "图形推理": "txxx",
    "定义判断": "dypd",
    "类比推理": "lbtl",
    "逻辑判断": "ljpd",
    "科学推理": "kxlt",
    "增长": "zz",
    "比重": "bz",
    "平均数": "pjs",
    "倍数": "bs",
    "简单计算": "jdjs",
    "综合分析": "zhfx",
}

# (模块, 题型, 二级) -> 计数;  (模块, 题型, 二级) -> Counter(三级)
sub_counts = collections.Counter()
detail_counts = collections.defaultdict(collections.Counter)
total = 0
skipped = 0

# 题型专属错因：每个题型的典型失分动作，前端会再拼上通用错因
TOPIC_ERRORS = {
    "pdyy": ["没抓中心句", "以偏概全", "偷换概念", "答非所问", "无中生有"],
    "ltk":  ["呼应点找错", "词义侧重混", "搭配对象错", "褒贬误用", "望文生义"],
    "yybd": ["首尾句判错", "话题不统一", "关联词错位", "语序判错"],
    "txxx": ["规律没看出", "数量数错", "元素组成判错", "空间想不出"],
    "dypd": ["要素漏抓", "主体范围错", "设问看反"],
    "lbtl": ["一级关系判错", "二级辨析漏", "词性不一致"],
    "ljpd": ["论点论据找错", "削弱加强弄反", "翻译规则记错", "力度比较错"],
    "kxlt": ["公式记错", "单位换算错", "概念混淆"],
    "zz":   ["公式混淆", "时间定位错", "截位位数错", "增长量与率混"],
    "bz":   ["部分整体找错", "基期现期搞混", "两期方向判错"],
    "pjs":  ["分子分母找错", "增长率与量混"],
    "bs":   ["倍数与多几倍混", "基期现期找错"],
    "jdjs": ["加减错位", "排序看漏", "单位不统一"],
    "zhfx": ["选项顺序乱", "漏看「不正确」", "中途放弃"],
}

# 输入框提示语，按题型给例子
TOPIC_HINTS = {
    "pdyy": "只看了首句，没发现转折后的句子才是重点",
    "ltk":  "只按词义选，没看搭配对象",
    "yybd": "排序没先定首句，凭语感排",
    "txxx": "只数了直线，漏了曲线",
    "dypd": "漏看「主体必须是企业」这一条",
    "lbtl": "只看到近义，没比词性",
    "ljpd": "把加强项当成了削弱项",
    "kxlt": "公式记混，用了动能公式",
    "zz":   "把增长率当成增长量算",
    "bz":   "分母取了基期量",
    "pjs":  "把平均数增长率当成普通增长率",
    "bs":   "「是几倍」和「多几倍」搞混",
    "jdjs": "位数没对齐算错",
    "zhfx": "没注意题目问的是「不正确的是」",
}

for path in glob.glob(os.path.join(BANK_DIR, "bank-*.json")):
    with io.open(path, encoding="utf-8") as f:
        data = json.load(f)
    for q in data.values():
        parts = (q.get("source") or "").split("/")
        if "10-真题" not in parts:
            skipped += 1
            continue
        i = parts.index("10-真题")
        module = parts[0]
        rest = parts[i + 1:]
        if not rest:
            skipped += 1
            continue
        # 言语理解的路径里多一层「言语理解与表达」，跳过它
        if rest[0] == module or rest[0].startswith(module):
            rest = rest[1:]
        if not rest:
            skipped += 1
            continue
        topic = rest[0]
        level2 = rest[1] if len(rest) > 1 else ""
        level3 = rest[2] if len(rest) > 2 and not rest[2].endswith(".md") else ""
        if module not in MODULE_KEYS or not topic or not level2:
            skipped += 1
            continue
        # 拆「A-B」：A 等于题型名时说明前缀是题型（如「片段阅读-细节判断」），
        # 真正的二级是 B；否则 A 是二级、B 是三级（如「加强削弱-削弱型」）
        if "-" in level2:
            a, b = level2.split("-", 1)
            if a == topic or a.startswith(topic):
                parent, child = b, level3
            else:
                parent, child = a, b
        else:
            parent, child = level2, level3
        sub_counts[(module, topic, parent)] += 1
        if child:
            detail_counts[(module, topic, parent)][child] += 1
        total += 1

modules = collections.OrderedDict()
for (module, topic, sub), n in sub_counts.items():
    if module not in modules:
        key, short = MODULE_KEYS[module]
        modules[module] = {"key": key, "short": short, "label": module, "total": 0,
                           "topics": collections.OrderedDict()}
    m = modules[module]
    m["total"] += n
    if topic not in m["topics"]:
        m["topics"][topic] = {"key": TOPIC_KEYS.get(topic, topic), "label": topic, "total": 0,
                              "subs": collections.OrderedDict()}
    t = m["topics"][topic]
    t["total"] += n
    if sub not in t["subs"]:
        t["subs"][sub] = {"key": sub, "label": sub, "total": 0, "details": []}
    s = t["subs"][sub]
    s["total"] += n

for (module, topic, sub), counter in detail_counts.items():
    s = modules[module]["topics"][topic]["subs"][sub]
    s["details"] = [{"name": k, "count": v} for k, v in counter.most_common(8)]

out = {"version": 1, "source": "错题复盘/app/dist/data", "total": total, "modules": []}
MIN_SUB = 10  # 题量低于此值的子类合并为「其他」，避免筛选条塞满 1 题小类
for module, m in modules.items():
    topics = []
    for topic, t in sorted(m["topics"].items(), key=lambda kv: -kv[1]["total"]):
        subs = sorted(t["subs"].values(), key=lambda x: -x["total"])
        kept = [s for s in subs if s["total"] >= MIN_SUB]
        rest = [s for s in subs if s["total"] < MIN_SUB]
        if rest:
            kept.append({
                "key": "其他",
                "label": "其他",
                "total": sum(s["total"] for s in rest),
                "merged": len(rest),
                "details": [{"name": s["label"], "count": s["total"]} for s in sorted(rest, key=lambda x: -x["total"])],
            })
        topics.append({
            "key": t["key"],
            "label": t["label"],
            "total": t["total"],
            "errors": TOPIC_ERRORS.get(t["key"], []),
            "hint": TOPIC_HINTS.get(t["key"], ""),
            "subs": kept,
        })
    out["modules"].append({"key": m["key"], "short": m["short"], "label": m["label"],
                           "total": m["total"], "topics": topics})

with io.open(OUT, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=1)

print("题目总数", total, "跳过", skipped)
for m in out["modules"]:
    subs_all = sum(len(t["subs"]) for t in m["topics"])
    print(f'{m["label"]}: {m["total"]} 题 · {len(m["topics"])} 题型 · {subs_all} 子类')
    for t in m["topics"]:
        print(f'   {t["label"]}({t["total"]}) ' + "、".join(f'{s["label"]}{s["total"]}' for s in t["subs"]))
print("输出", OUT, os.path.getsize(OUT), "bytes")
