# -*- coding: utf-8 -*-
"""把 catalog.json 注入 index.html 的 <script id="catalog-data"> 区块"""
import io
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
HTML = os.path.join(ROOT, "index.html")
CATALOG = os.path.join(ROOT, "catalog.json")

cat = json.load(io.open(CATALOG, encoding="utf-8"))
payload = json.dumps(cat, ensure_ascii=False, separators=(",", ":"))

html = io.open(HTML, encoding="utf-8").read()
pattern = re.compile(r'(<script id="catalog-data" type="application/json">).*?(</script>)', re.S)
if not pattern.search(html):
    raise SystemExit("没有找到 catalog-data 区块")

html = pattern.sub(lambda m: m.group(1) + payload + m.group(2), html, count=1)
io.open(HTML, "w", encoding="utf-8", newline="\n").write(html)

print("注入完成：%d 个模块，%d 题，catalog 片段 %d 字符"
      % (len(cat["modules"]), cat["total"], len(payload)))
