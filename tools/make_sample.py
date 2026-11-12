# -*- coding: utf-8 -*-
"""生成一份测试用备份文件，覆盖 3 个有题库的模块 + 2 个无题库模块"""
import io
import json
import os
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT_DIR = os.path.join(ROOT, ".testdata")
os.makedirs(OUT_DIR, exist_ok=True)

now = int(time.time() * 1000)
HOUR = 3600 * 1000

points = [
    (1, "yy", "pdyy", "中心理解", "关联词-对策", "看到「需要」就选对策项，没分清是建议还是主旨", 4),
    (2, "yy", "pdyy", "细节判断", "偷换概念", "把「部分」换成「全部」没看出来", 2),
    (3, "yy", "ltk", "成语填空", "贬义褒用", "把贬义词当褒义词用", 3),
    (4, "zl", "zz", "一般增长率计算", "增长率公式混淆", "把增长率当成增长量算", 5),
    (5, "zl", "bz", "现期比重", "分母取错", "分母取了基期量", 2),
    (6, "pd", "txxx", "数量规律", "面数量漏数", "被分割的小面没数进去", 6),
    (7, "pd", "ljpd", "加强削弱", "加强削弱弄反", "把加强项当成削弱项", 2),
    (8, "cs", "", "", "二十四节气顺序", "小暑大暑记混", 2),
    (9, "zzll", "", "", "新发展理念内涵", "记不全五个方面", 1),
]

notes = {
    1: ("2023 联考 A 卷第 12 题，漏看「不」字", "10204478"),
    4: ("2022 国考副省级，基期量算成现期量", "11226890"),
}

records = []
rid = 0
for pid, _, _, _, _, _, count in points:
    for k in range(count):
        rid += 1
        note, qid = ("", "")
        if k == 0 and pid in notes:
            note, qid = notes[pid]
        records.append({
            "id": rid,
            "pid": pid,
            "ts": now - (count - k) * 6 * HOUR,
            "n": note,
            "q": qid,
        })

data = {
    "app": "gongkao-cuotiben",
    "version": 3,
    "exportedAt": time.strftime("%Y-%m-%dT%H:%M:%S"),
    "points": [
        {
            "id": pid, "mod": mod, "g": g, "s": s,
            "name": name, "reason": reason, "count": count,
            "created": now - (10 - pid) * 24 * HOUR,
        }
        for pid, mod, g, s, name, reason, count in points
    ],
    "records": records,
}

path = os.path.join(OUT_DIR, "sample-backup.json")
with io.open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=1)

print("已生成", path)
print("知识点 %d 个，错误记录 %d 条，累计 %d 次"
      % (len(data["points"]), len(records), sum(p[6] for p in points)))
