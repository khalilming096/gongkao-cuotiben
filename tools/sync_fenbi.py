# -*- coding: utf-8 -*-
"""把粉笔采集数据（answered_questions.jsonl）同步成错题本可导入的备份 JSON。

数据来源：C:\\Users\\Administrator\\Documents\\fenbi\\outputs\\collection-*/
每条作答记录里：
    module            顶部模块（言语理解与表达 / 判断推理 / 资料分析 / 常识判断 / 数量关系 / 政治理论）
    knowledge_points  粉笔打的知识点标签，二级 + 三级混在一起，如 ['实词填空','对应关系-前后呼应']
    is_correct        是否答对，只导错题
    history_display_date  作答日期
    question_alias    题目 id，写进错题本的「题号」字段
    ordinal_in_paper  本卷题号

映射思路：
    1. 模块名 → 错题本模块 key
    2. 标签 → (题型, 子类)：先精确匹配，再按前缀最长匹配
    3. 一道题可能同时带「题型层」和「技巧层」标签
       （如 混搭填空 + 对应关系-前后呼应），
       → 归到题型层子类，技巧标签作为知识点名称，
         这样「错在什么题型下、错的是哪个点」都能看出来
    4. 材料类型标签（文字资料/统计图…）不是考点，忽略
    5. 实在对不上题库的标签 → 归「未归类」，保留原标签名，App 里可再归类
"""
import io
import json
import os
import re
import shutil
import sys
from datetime import datetime

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CATALOG_PATH = os.path.join(ROOT, 'catalog.json')

DEFAULT_SRC = os.path.expanduser(
    r'~/Documents/fenbi/outputs/collection-20261001-20261112')

# ---------------------------------------------------------------- 模块映射
MODULE_MAP = {
    '言语理解与表达': 'yy',
    '判断推理': 'pd',
    '资料分析': 'zl',
    '常识判断': 'cs',
    '数量关系': 'sl',
    '政治理论': 'zzll',
}

# 没有题库的三个模块：只记模块级知识点，标签直接当知识点名称
NO_CATALOG = {'cs', 'sl', 'zzll'}

# 材料类型标签（资料分析的资料载体，不是考点）
MATERIAL_TAGS = {
    '综合资料', '文字资料', '统计图', '统计表', '图形资料',
    '表格资料', '文字+表格', '文字+图形', '表格+图形',
}

# ------------------------------------------------------- 标签 → (题型, 子类, 层级)
# 层级 0 = 题型层（实词填空 / 混搭填空 / 中心理解…）
# 层级 1 = 技巧层（语境分析 / 词的辨析），只在没有题型层标签时兜底
RULES = {
    'yy': [
        # 逻辑填空
        ('ltk', '实词填空', 0, ['实词填空']),
        ('ltk', '混搭填空', 0, ['混搭填空', '综合填空']),
        ('ltk', '成语填空', 0, ['成语填空']),
        ('ltk', '语境分析', 1, ['对应关系', '关联关系', '方式目的', '语境分析']),
        ('ltk', '词的辨析', 1, ['词的辨析']),
        # 片段阅读
        ('pdyy', '中心理解', 0, ['中心理解', '主旨概括', '意图判断', '特殊问法',
                                 '主题词', '关联词', '分述句特征', '行文脉络', '对策']),
        ('pdyy', '细节判断', 0, ['细节判断', '细节理解']),
        ('pdyy', '标题填入', 0, ['标题填入']),
        ('pdyy', '词句理解', 0, ['词句理解', '词语理解', '句子理解']),
        ('pdyy', '其他', 0, ['态度理解']),
        # 语句表达
        ('yybd', '语句排序', 0, ['语句排序', '确定顺序', '确定捆绑', '确定首句', '确定尾句',
                                 '语句排序题']),
        ('yybd', '语句填空', 0, ['语句填空', '横线在']),
        ('yybd', '语句衔接', 0, ['语句衔接']),
        ('yybd', '下文推断', 0, ['下文推断', '接语选择']),
        ('yybd', '病句辨析', 0, ['病句辨析']),
        ('yybd', '成语运用', 0, ['成语运用']),
        ('yybd', '其他', 0, ['语句']),
    ],
    'pd': [
        ('ljpd', '前提假设', 0, ['大前提', '假设', '前提']),
        ('ljpd', '分析推理', 0, ['组合排列', '从最大信息开始推', '代入排除', '最大信息']),
        ('ljpd', '翻译推理', 0, ['常规翻译', '翻译推理', '集合推理', '翻译']),
        ('ljpd', '真假推理', 0, ['真假推理', '矛盾关系']),
        ('ljpd', '评价型', 0, ['论证结构', '评价']),
        ('ljpd', '原因解释', 0, ['原因解释']),
        ('ljpd', '结论推出', 0, ['结论推出', '结论型']),
        ('ljpd', '加强削弱', 0, ['削弱', '加强', '搭桥', '补充论据', '他因',
                                 '方式目的', '原因结果', '必要条件', '论点', '论据']),
        ('txxx', '数量规律', 0, ['数量规律', '笔画']),
        ('txxx', '位置规律', 0, ['位置规律']),
        ('txxx', '样式规律', 0, ['样式规律', '黑白块']),
        ('txxx', '属性规律', 0, ['属性规律']),
        ('txxx', '立体图形', 0, ['空间类', '立体图形', '六面体', '截面', '三视图']),
        ('txxx', '其他', 0, ['特殊规律']),
        ('lbtl', '对应关系', 0, ['逻辑关系-对应关系', '对应关系']),
        ('lbtl', '逻辑关系', 0, ['逻辑关系']),
        ('lbtl', '语义关系', 0, ['语义关系']),
        ('lbtl', '语法关系', 0, ['语法关系']),
        ('lbtl', '范围关系', 0, ['范围关系']),
        ('dypd', '单定义', 0, ['单定义', '主客体', '拆词']),
        ('dypd', '多定义', 0, ['多定义', '常规问法', '特殊问法']),
        # 说明：粉笔给定义判断打的「常规问法/特殊问法」只描述问法（属于/不属于），
        # 不区分单/多定义。实测这一类题都给出两个以上定义（2026-11-13 核过 7 道题干），
        # 归多定义；题库里单定义占多数，若将来出现单定义题会被记为多定义，报告中会提示。
    ],
    'zl': [
        ('zz', '一般增长率计算', 0, ['一般增长率']),
        ('zz', '混合增长率', 0, ['混合增长率']),
        ('zz', '间隔增长', 0, ['间隔增长', '间隔基期', '间隔倍数']),
        ('zz', '年均增长量', 0, ['年均增长量']),
        ('zz', '年均增长率', 0, ['年均增长率', '年均增长']),
        ('zz', '增长量计算', 0, ['增长量计算']),
        ('zz', '增长量比较', 0, ['增长量比较']),
        ('zz', '基期量计算', 0, ['基期量']),
        ('zz', '现期量计算', 0, ['现期量']),
        ('bz', '现期比重', 0, ['现期比重']),
        ('bz', '基期比重', 0, ['基期比重']),
        ('bz', '两期比重差', 0, ['两期比重']),
        ('pjs', '现期平均数', 0, ['现期平均数']),
        ('pjs', '基期平均数', 0, ['基期平均数']),
        ('pjs', '平均数增长率', 0, ['平均数增长率']),
        ('bs', '现期倍数', 0, ['现期倍数']),
        ('bs', '基期倍数', 0, ['基期倍数']),
        ('jdjs', '简单加减计算', 0, ['简单加减计算']),
        ('jdjs', '直接找数', 0, ['直接找数']),
        ('jdjs', '排序比较', 0, ['排序比较']),
        # 综合分析分选是型/选非型，标签看不出，用题干问法判定（见 pick_zhfx）
        ('zhfx', '__TEXT__', 0, ['综合分析']),
    ],
}

# 资料分析「综合分析」的选是/选非，靠题干问法判定
ZH_NO = ('不正确', '不准确', '错误的是', '不符合', '不能推出', '无法推出', '不正确的是')

# 技巧层标签的取用顺序：同一题带多个技巧标签时，按这个优先级定知识点名称
CN_DETAIL_ORDER = ['对应关系', '关联关系', '方式目的', '词的辨析', 'zz']

# 卷名压缩：去掉套话，保留「年份 + 地区/考试 + 卷别」
PAPER_NOISE = [
    (r'公务员录用考试|公务员考试', ''),
    (r'录用考试', ''),
    (r'《[^》]*》', ''),
    (r'[（(]([^）)]*?)网友回忆版[）)]', r'（\1）'),
    (r'[（(]网友回忆版[）)]', ''),
    (r'试题', ''),
    (r'题(?=[（(]|$)', ''),
    (r'真题|题本|试卷', ''),
    (r'[（(][）)]', ''),
]


def load_texts(src):
    """单题题干（papers/*.json），只用来判「综合分析」的选是/选非，取不到就返回空"""
    texts = {}
    paper_dir = os.path.join(src, 'papers')
    if not os.path.isdir(paper_dir):
        return texts
    for fn in os.listdir(paper_dir):
        if not fn.endswith('.json'):
            continue
        try:
            with io.open(os.path.join(paper_dir, fn), encoding='utf-8') as f:
                d = json.load(f)
        except Exception:
            continue
        for q in d.get('questions', []) or []:
            a = q.get('question_alias')
            if a:
                texts[a] = (q.get('question') or {}).get('text') or ''
    return texts


def pick_zhfx(text):
    return '选非型' if any(k in (text or '') for k in ZH_NO) else '选是型'


def load_catalog():
    with io.open(CATALOG_PATH, encoding='utf-8') as f:
        cat = json.load(f)
    valid = set()
    labels = {}
    for m in cat['modules']:
        for t in m['topics']:
            for s in t.get('subs', []):
                valid.add((m['key'], t['key'], s['key']))
                labels[(m['key'], t['key'], s['key'])] = (t['label'], s['label'])
    return valid, labels


def build_index(mod):
    """返回 (精确表, 前缀规则列表) —— 前缀按长度倒序，保证最长匹配优先"""
    exact, prefix = {}, []
    for g, s, lvl, tags in RULES.get(mod, []):
        for tag in tags:
            if '-' in tag or tag.endswith(('题', '填空', '关系')):
                exact.setdefault((mod, tag), (g, s, lvl))
            exact.setdefault((mod, tag), (g, s, lvl))
            prefix.append((tag, g, s, lvl))
    prefix.sort(key=lambda x: -len(x[0]))
    return exact, prefix


INDEX = {m: build_index(m) for m in RULES}


def resolve(mod, tag):
    """标签 → (g, s, level)，对不上返回 None"""
    exact, prefix = INDEX.get(mod, ({}, []))
    hit = exact.get((mod, tag))
    if hit:
        return hit
    for pat, g, s, lvl in prefix:
        if tag.startswith(pat + '-') or tag.startswith(pat):
            return (g, s, lvl)
    return None


def short_paper(name):
    out = name
    for pat, rep in PAPER_NOISE:
        out = re.sub(pat, rep, out)
    out = re.sub(r'\s+', '', out).strip('·-—_ ')
    return out or name


def day_ms(date_str, seq):
    """作答日期 → 毫秒时间戳；seq 用来保证同一知识点内的记录时间不重复"""
    try:
        d = datetime.strptime(date_str, '%Y-%m-%d')
    except Exception:
        d = datetime.now()
    base = int(d.replace(hour=12, minute=0, second=0).timestamp() * 1000)
    return base + (seq % 3600) * 1000


def main():
    src = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SRC
    src_jsonl = os.path.join(src, 'answered_questions.jsonl')
    if not os.path.exists(src_jsonl):
        print('找不到 %s' % src_jsonl)
        return 1

    valid, labels = load_catalog()
    texts = load_texts(src)
    tag_name = os.path.basename(src.rstrip('/\\'))
    out_dir = os.path.join(ROOT, 'data', tag_name)
    raw_dir = os.path.join(out_dir, 'raw')
    os.makedirs(raw_dir, exist_ok=True)

    # 知识点索引： (mod,g,s,name) → point
    points, pidx = [], {}
    records = []
    stat_mod = {}
    stat_rule = {}      # 标签 → 命中/未命中
    unmapped = {}       # 有题库模块里没对上的标签 → 次数
    mod_tags = {}       # 无题库模块的标签 → 次数（本来就只做模块级）
    skipped_material = {}
    papers = {}

    def point_of(mod, g, s, name):
        key = (mod, g, s, name)
        p = pidx.get(key)
        if p is None:
            p = {
                'id': len(points) + 1, 'mod': mod, 'g': g, 's': s,
                'name': name, 'reason': '', 'count': 0, 'created': None,
            }
            pidx[key] = p
            points.append(p)
        return p

    n_wrong = 0
    with io.open(src_jsonl, encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            o = json.loads(line)
            if o.get('is_correct'):
                continue
            n_wrong += 1
            mod_name = o.get('module') or ''
            mod = MODULE_MAP.get(mod_name)
            if not mod:
                unmapped[mod_name or '(无模块)'] = unmapped.get(mod_name or '(无模块)', 0) + 1
                continue
            stat_mod[mod] = stat_mod.get(mod, 0) + 1
            paper = o.get('paper_name') or ''
            papers[paper] = papers.get(paper, 0) + 1
            tags = [t for t in (o.get('knowledge_points') or []) if t]

            # ---- 选主标签 ----
            cand = []          # [(level, tag, g, s)]
            for t in tags:
                if t in MATERIAL_TAGS:
                    skipped_material[t] = skipped_material.get(t, 0) + 1
                    continue
                if mod in NO_CATALOG:
                    continue
                hit = resolve(mod, t)
                if hit and hit[1] == '__TEXT__':
                    sub = pick_zhfx(texts.get(o.get('question_alias') or '', ''))
                    cand.append((hit[2], t, hit[0], sub))
                elif hit and (mod, hit[0], hit[1]) in valid:
                    cand.append((hit[2], t, hit[0], hit[1]))
                else:
                    unmapped[t] = unmapped.get(t, 0) + 1

            if mod in NO_CATALOG:
                g = s = ''
                name = tags[0] if tags else '未标注考点'
                for t in tags:
                    mod_tags[t] = mod_tags.get(t, 0) + 1
            elif cand:
                # 归属（题型/子类）优先用题型层：错题本按题型浏览，
                # 名称再取更细的技巧标签，形成「混搭填空 / 对应关系-前后呼应」
                # 这样既有题型视角，又能看出具体错在哪个点
                lvl, tag, g, s = min(cand, key=lambda c: (c[0], -len(c[1])))
                fine = [c for c in cand if c[0] == 1]
                if fine:
                    fine.sort(key=lambda c: (CN_DETAIL_ORDER.index(
                        next((p for p in CN_DETAIL_ORDER if c[1].startswith(p)), 'zz')), -len(c[1])))
                    name = fine[0][1]
                else:
                    name = tag
            else:
                g = s = ''
                name = tags[0] if tags else '未标注考点'

            p = point_of(mod, g, s, name)
            seq = len(records) + 1
            ts = day_ms(o.get('history_display_date') or '', seq)
            qid = o.get('question_alias') or ''
            parts = [short_paper(paper)]
            if o.get('ordinal_in_paper'):
                parts.append('第%s题' % o['ordinal_in_paper'])
            if o.get('duration_seconds'):
                parts.append('%ss' % o['duration_seconds'])
            if len(tags) > 1:
                parts.append('考点：' + '/'.join(tags))
            records.append({
                'id': seq, 'pid': p['id'], 'ts': ts,
                'n': ' · '.join(parts), 'q': qid,
            })
            p['count'] += 1
            p['created'] = ts if p['created'] is None else min(p['created'], ts)

    # created 为空的兜底
    for p in points:
        if p['created'] is None:
            p['created'] = int(datetime.now().timestamp() * 1000)

    backup = {
        'app': 'gongkao-cuotiben',
        'version': 3,
        'exportedAt': datetime.now().strftime('%Y-%m-%dT%H:%M:%S'),
        'source': tag_name,
        'points': points,
        'records': records,
    }
    out_json = os.path.join(out_dir, 'backup.json')
    with io.open(out_json, 'w', encoding='utf-8') as f:
        json.dump(backup, f, ensure_ascii=False, indent=1)

    # 原始数据留档
    for fn in ('answered_questions.jsonl', 'collection_report.md', 'summary.json',
               'collection_rules.md'):
        p = os.path.join(src, fn)
        if os.path.exists(p):
            shutil.copy2(p, os.path.join(raw_dir, fn))

    # ---------------- 报告 ----------------
    mod_label = {'yy': '言语理解', 'pd': '判断推理', 'zl': '资料分析',
                 'cs': '常识', 'sl': '数量关系', 'zzll': '政治理论'}
    lines = []
    lines.append('# 粉笔采集数据 → 错题本 同步报告')
    lines.append('')
    lines.append('数据源：`%s`' % src_jsonl)
    lines.append('生成时间：%s' % backup['exportedAt'])
    lines.append('')
    lines.append('## 一、总量')
    lines.append('')
    lines.append('| 项目 | 数量 |')
    lines.append('| --- | --- |')
    lines.append('| 涉及试卷 | %d 套 |' % len(papers))
    lines.append('| 错题 | %d 道 |' % n_wrong)
    lines.append('| 生成知识点 | %d 个 |' % len(points))
    lines.append('| 生成错误记录 | %d 条 |' % len(records))
    lines.append('')
    lines.append('## 二、模块分布')
    lines.append('')
    lines.append('| 模块 | 错题数 | 知识点 | 说明 |')
    lines.append('| --- | --- | --- | --- |')
    for k, v in sorted(stat_mod.items(), key=lambda x: -x[1]):
        pn = len([p for p in points if p['mod'] == k])
        note = '按题库题型/子类归类' if k in RULES else '无题库，标签直接当知识点'
        lines.append('| %s | %d | %d | %s |' % (mod_label.get(k, k), v, pn, note))
    lines.append('')
    lines.append('## 三、知识点明细（按错误次数）')
    lines.append('')
    lines.append('| 模块 | 归类 | 知识点 | 次数 |')
    lines.append('| --- | --- | --- | --- |')
    for p in sorted(points, key=lambda x: -x['count'])[:40]:
        if p['g']:
            t_label, s_label = labels.get((p['mod'], p['g'], p['s']), (p['g'], p['s']))
            cat = '%s / %s' % (t_label, s_label)
        else:
            cat = '未归类' if p['mod'] in RULES else '模块级'
        lines.append('| %s | %s | %s | %d |' % (
            mod_label.get(p['mod'], p['mod']), cat, p['name'], p['count']))
    if len(points) > 40:
        lines.append('')
        lines.append('（共 %d 个知识点，此处只列前 40）' % len(points))
    lines.append('')
    lines.append('## 四、未能对上的标签')
    lines.append('')
    if unmapped:
        lines.append('这些标签出现在**有题库的模块**里，但题库没有对应子类，'
                     '已归入「未归类」，App 里点开详情可手动重新归类：')
        lines.append('')
        lines.append('| 标签 | 次数 | 原因 |')
        lines.append('| --- | --- | --- |')
        why = {
            '综合分析': '资料分析的「综合分析」题分选是型/选非型，标签看不出是哪种',
            '特殊问法': '判断推理里的问法标签，无法确定属于加强削弱还是定义判断',
            '常规问法': '同上，问法标签不足以定位题型',
        }
        for k, v in sorted(unmapped.items(), key=lambda x: -x[1]):
            lines.append('| %s | %d | %s |' % (k, v, why.get(k, '题库里没有这个分类')))
    else:
        lines.append('无。')
    lines.append('')
    if mod_tags:
        lines.append('## 五、无题库模块的标签')
        lines.append('')
        lines.append('常识 / 数量关系 / 政治理论在 App 里没有题库分类，'
                     '标签直接作为知识点名称（模块级），共 %d 个：' % len(mod_tags))
        lines.append('')
        lines.append('| 标签 | 次数 |')
        lines.append('| --- | --- |')
        for k, v in sorted(mod_tags.items(), key=lambda x: -x[1]):
            lines.append('| %s | %d |' % (k, v))
        lines.append('')
    if skipped_material:
        lines.append('## 六、被忽略的材料类型标签')
        lines.append('')
        lines.append('资料分析题同时带「材料载体」标签（文字资料/统计图…）和「考点」标签，'
                     '前者不是考点，只保留考点：')
        lines.append('')
        for k, v in sorted(skipped_material.items(), key=lambda x: -x[1]):
            lines.append('- %s：%d 次' % (k, v))
        lines.append('')
    lines.append('## 七、导入方法')
    lines.append('')
    lines.append('1. 打开 `index.html`（网页版）');
    lines.append('2. 右上角「？」→ 说明弹窗底部点 **导入备份**；')
    lines.append('3. 选 `data/%s/backup.json`。' % tag_name)
    lines.append('')
    lines.append('导入是合并式的：同模块+同题型+同子类+同知识点名视为同一个，'
                 '次数取大、记录按时间戳去重，所以重复导入不会翻倍。')
    lines.append('')
    out_md = os.path.join(out_dir, '同步报告.md')
    with io.open(out_md, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print('已生成 %s' % out_json)
    print('已生成 %s' % out_md)
    print('错题 %d 道 → 知识点 %d 个 / 记录 %d 条' % (n_wrong, len(points), len(records)))
    for k, v in sorted(stat_mod.items(), key=lambda x: -x[1]):
        print('  %-6s %3d 道' % (mod_label.get(k, k), v))
    if unmapped:
        print('未映射标签 %d 个（共 %d 次）' % (len(unmapped), sum(unmapped.values())))
    return 0


if __name__ == '__main__':
    sys.exit(main())
