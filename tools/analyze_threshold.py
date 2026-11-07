# -*- coding: utf-8 -*-
"""分析不同合并阈值对分类体系的影响，用于确定 MIN_SUB"""
import collections
import glob
import io
import json
import os

BANK_DIR = os.path.join(os.path.expanduser("~"), "Documents", "错题复盘", "app", "dist", "data")

sub_counts = collections.Counter()      # (module, topic, sub) -> n
topic_total = collections.Counter()     # (module, topic) -> n

for path in glob.glob(os.path.join(BANK_DIR, "bank-*.json")):
    with io.open(path, encoding="utf-8") as f:
        data = json.load(f)
    for q in data.values():
        parts = (q.get("source") or "").split("/")
        if "10-真题" not in parts:
            continue
        i = parts.index("10-真题")
        module = parts[0]
        rest = parts[i + 1:]
        if rest and (rest[0] == module or rest[0].startswith(module)):
            rest = rest[1:]
        if len(rest) < 2:
            continue
        topic, level2 = rest[0], rest[1]
        if "-" in level2:
            a, b = level2.split("-", 1)
            parent = b if (a == topic or a.startswith(topic)) else a
        else:
            parent = level2
        sub_counts[(module, topic, parent)] += 1
        topic_total[(module, topic)] += 1

grand = sum(sub_counts.values())
print("题目总数 %d，未合并时的子类数 %d\n" % (grand, len(sub_counts)))

rows = []
for th in [1, 3, 5, 8, 10, 15, 20, 30, 50, 80]:
    kept = [k for k, v in sub_counts.items() if v >= th]
    merged_items = [(k, v) for k, v in sub_counts.items() if v < th]
    merged_q = sum(v for _, v in merged_items)
    # 每个题型内「其他」占该题型题量的比例，取最大值（最坏情况）
    worst = 0.0
    worst_topic = ""
    for (m, t), total in topic_total.items():
        other = sum(v for (mm, tt, _), v in ((k, v) for k, v in sub_counts.items()) if mm == m and tt == t)
        o = 0
        for (mm, tt, s), v in sub_counts.items():
            if mm == m and tt == t and v < th:
                o += v
        if total >= 200 and o / total > worst:
            worst = o / total
            worst_topic = t
    rows.append((th, len(kept), len(merged_items), merged_q, merged_q / grand * 100, worst * 100, worst_topic))

print("阈值  保留子类  被合并小类  被合并题数  占全部题  最坏题型「其他」占比")
for th, k, mi, mq, pct, worst, wt in rows:
    print("%4d  %8d  %10d  %10d  %7.2f%%  %6.1f%%  (%s)" % (th, k, mi, mq, pct, worst, wt))

print("\n--- 阈值 10 下被合并的具体是哪些（按题量倒序前 15）---")
for (m, t, s), v in sorted([(k, v) for k, v in sub_counts.items() if v < 10], key=lambda x: -x[1])[:15]:
    print("  %s / %s / %s : %d 题" % (m, t, s, v))

print("\n--- 阈值 10 下保留的最小 8 个子类（边界情况）---")
for (m, t, s), v in sorted([(k, v) for k, v in sub_counts.items() if v >= 10], key=lambda x: x[1])[:8]:
    print("  %s / %s / %s : %d 题" % (m, t, s, v))
