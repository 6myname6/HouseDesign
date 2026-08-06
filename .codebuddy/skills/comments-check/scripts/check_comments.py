#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
comments-check 分析器
=====================
对代码进行三类注释检查（对应技能要求 1/2/3）：

  要求1 - 完整性 & 占比：每个函数/重要字段应有注释；注释行占总代码量(注释+代码) >= 30%，代码 <= 70%
  要求2 - 一致性：注释内容应与代码匹配（占位注释、引用了代码中不存在的符号等启发式检测 + 由 AI 做语义复核）
  要求3 - 可读性：注释应易于阅读（过长行、过短/无意义、全大写刷屏、乱码等启发式检测）

支持语言：Java(.java)、JavaScript/TypeScript(.js/.ts/.jsx/.tsx)、Vue(.vue)

输出：
  - 控制台摘要 + 主要问题清单
  - JSON 报告（--json PATH）
  - HTML 报告（--html PATH），含分级着色与占比条

注意：结构性检测（缺注释、字段/函数识别）为基于正则的启发式，可能存在少量误报/漏报，
最终判定应由 AI 结合语义复核。脚本负责把所有可疑点量化并列出，供 AI 高效复核。
"""
import argparse
import json
import os
import re
import sys
from pathlib import Path

SUPPORTED_EXT = {".java", ".js", ".ts", ".jsx", ".tsx", ".vue"}
DEFAULT_RATIO = 0.30          # 注释占比目标下限
RATIO_TOLERANCE = 0.10        # 超过 30%+容差 视为注释过多（可选提醒，不计入失败）
MAX_COMMENT_LEN = 120         # 单行注释可读长度上限
MIN_MEANINGFUL_LEN = 4        # 注释至少应有意义的字符数（排除 "// x" 这类）

# ---------- 正则 ----------
# Java 方法/构造函数声明（忽略行内注解，靠调用方跳过以 @ 开头的行）
JAVA_METHOD_RE = re.compile(
    r"^\s*(?:public|private|protected|static|final|native|synchronized|abstract|default|transient|volatile|@\w+[\s\w]*)*"
    r"\s*[\w$.<>\[\],\s?]+\s+([A-Za-z_]\w*)\s*\(([^)]*)\)\s*(?:throws\s+[\w$,\s<>]+)?\s*\{?\s*$"
)
# Java 类级字段
JAVA_FIELD_RE = re.compile(
    r"^\s*(?:public|private|protected|static|final|transient|volatile)*\s+[\w$.<>\[\],\s?]+\s+([A-Za-z_]\w*)\s*(=\s*[^;]+)?;\s*$"
)
# JS/TS 函数/方法/箭头函数/字段
JS_FUNC_RE = re.compile(
    r"^\s*(?:export\s+)?(?:async\s+)?function\s+([A-Za-z_]\w*)\s*\("
    r"|^\s*([A-Za-z_]\w*)\s*=\s*(?:async\s+)?\([^)]*\)\s*=>"
    r"|^\s*([A-Za-z_]\w*)\s*\([^)]*\)\s*\{"
    r"|^\s*([A-Za-z_]\w*)\s*\([^)]*\)\s*\{"
)
JS_FIELD_RE = re.compile(r"^\s*(?:export\s+)?(?:const|let|var)\s+([A-Za-z_]\w*)\s*[:=]")
JS_CLASS_PROP_RE = re.compile(r"^\s*([A-Za-z_]\w*)\s*[=:)]")

CONTROL_KW = ("if", "for", "while", "switch", "catch", "try", "do", "else", "synchronized", "return", "throw")

PLACEHOLDER_RE = re.compile(r"\b(TODO|FIXME|XXX|HACK|待补充|待完善|待实现|待定|占位|临时|temporarily|临时方案)\b", re.I)
ALL_CAPS_RE = re.compile(r"\b[A-Z]{3,}\b")
WORD_RE = re.compile(r"[\u4e00-\u9fff]|[A-Za-z]+")
IDENT_RE = re.compile(r"[A-Za-z_]\w*")
CAMEL_RE = re.compile(r"\b([A-Za-z][a-z0-9]*[A-Z][\w]*)\b")


def strip_strings_and_comments_for_structure(line, in_block):
    """粗略去除字符串与块注释，用于结构识别（不追求完美）。"""
    out = []
    i = 0
    n = len(line)
    block = in_block
    while i < n:
        if block:
            j = line.find("*/", i)
            if j == -1:
                return "", True
            i = j + 2
            block = False
            continue
        if line[i:i + 2] == "/*":
            j = line.find("*/", i + 2)
            if j == -1:
                return "".join(out), True
            i = j + 2
            continue
        if line[i:i + 2] == "//":
            break
        if line[i] in ("'", '"', "`"):
            q = line[i]
            j = i + 1
            while j < n:
                if line[j] == "\\":
                    j += 2
                    continue
                if line[j] == q:
                    j += 1
                    break
                j += 1
            out.append(" " * (j - i))
            i = j
            continue
        out.append(line[i])
        i += 1
    return "".join(out), block


def classify_line(line, in_block):
    """返回 (kind, still_in_block)
    kind ∈ {'blank','comment','code','code_comment'}"""
    s = line.strip()
    if not s:
        return "blank", in_block
    if in_block:
        if "*/" in line:
            # 块注释结束；同行结束符之后可能还有代码
            after = line.split("*/", 1)[1]
            if after.strip() and not after.strip().startswith("//"):
                return "code_comment", False
            return "comment", False
        return "comment", True
    # 行注释（整行）
    stripped = line.lstrip()
    if stripped.startswith("//"):
        return "comment", False
    if stripped.startswith("/*"):
        if "*/" in line:
            after = line.split("*/", 1)[1]
            if after.strip() and not after.strip().startswith("//"):
                return "code_comment", False
            return "comment", False
        return "comment", True
    # 行尾注释
    _, blk = strip_strings_and_comments_for_structure(line, False)
    # 重新判断：去掉字符串后看是否只剩注释
    core = line
    # 简化：若 // 出现在去字符串之后则视为 code_comment
    core_no_str, _ = strip_strings_and_comments_for_structure(line, False)
    if "//" in core_no_str:
        return "code_comment", False
    return "code", False


def is_comment_line(kind):
    return kind in ("comment", "code_comment")


def analyze_file(path: Path, ratio_target: float):
    """分析单个文件，返回结果字典。"""
    text = path.read_text(encoding="utf-8", errors="ignore")
    raw_lines = text.split("\n")

    comment_lines = 0
    code_lines = 0
    total_lines = len(raw_lines)
    blank_lines = 0

    in_block = False
    # 收集注释文本片段（用于一致性/可读性启发式）
    comment_spans = []  # (start_line, end_line, list_of_comment_text_lines)
    cur_span = None

    # 结构状态
    brace_depth = 0
    in_method_stack = [False]  # True 表示当前处于某个方法体内
    last_nonblank_kind = None
    last_comment_text_lines = []   # 最近一个注释块的文本行
    findings = []

    file_idents = set(IDENT_RE.findall(text))  # 文件中出现的标识符（用于“引用不存在符号”检测）

    # 先做一次注释行统计与注释块收集
    processed = []  # (kind, line_str)
    for idx, line in enumerate(raw_lines, start=1):
        kind, in_block = classify_line(line, in_block)
        processed.append((kind, line, idx))
        if kind == "blank":
            blank_lines += 1
            continue
        if is_comment_line(kind):
            comment_lines += 1
        else:
            code_lines += 1

        # 收集注释块
        if kind == "comment":
            if cur_span is None:
                cur_span = [idx, idx, []]
            cur_span[1] = idx
            cur_span[2].append(line.strip())
        else:
            if cur_span is not None:
                comment_spans.append(tuple(cur_span))
                cur_span = None
    if cur_span is not None:
        comment_spans.append(tuple(cur_span))
        cur_span = None

    denom = comment_lines + code_lines
    ratio = (comment_lines / denom) if denom else 0.0

    # ---- 结构检测：函数/字段缺注释 + 一致性 + 可读性 ----
    in_block2 = False
    brace_depth = 0
    in_method = False
    pending_comment_text = []   # 当前待关联方法/字段的前置注释文本
    pending_comment_lines = []
    last_meaningful_was_comment = False

    def collect_consistency_readability(span, line_no):
        """对单个注释块做占位/可读性等启发式，返回 findings。"""
        cfindings = []
        full = " ".join(span[2])
        text_joined = " ".join(s.strip().lstrip("/*").lstrip("*").lstrip("/").strip() for s in span[2])
        # 占位注释
        if PLACEHOLDER_RE.search(full):
            cfindings.append(dict(category="placeholder", severity="warning",
                                  line=span[0], message="发现占位/未完成注释（TODO/FIXME/待补充等）", snippet=text_joined[:80]))
        # 过长行
        for ln in span[2]:
            clen = len(ln)
            if clen > MAX_COMMENT_LEN:
                cfindings.append(dict(category="too_long", severity="info",
                                      line=span[0], message=f"注释单行过长({clen}字符> {MAX_COMMENT_LEN})，影响阅读", snippet=ln[:80]))
        # 过短/无意义
        stripped = text_joined.strip()
        meaningful = len(re.sub(r"[\s*/*]+", "", stripped))
        if 0 < meaningful < MIN_MEANINGFUL_LEN:
            cfindings.append(dict(category="too_short", severity="warning",
                                  line=span[0], message=f"注释过短/无意义（'{stripped}'）", snippet=stripped))
        # 全大写刷屏
        caps = ALL_CAPS_RE.findall(stripped)
        if len(caps) >= 3:
            cfindings.append(dict(category="all_caps", severity="info",
                                  line=span[0], message=f"注释含过多全大写词({', '.join(caps[:5])})，可读性差", snippet=stripped[:80]))
        # 引用代码中不存在的标识符（驼峰符号）
        for sym in CAMEL_RE.findall(stripped):
            if sym not in file_idents and len(sym) > 3:
                cfindings.append(dict(category="undefined_ref", severity="warning",
                                      line=span[0], message=f"注释提到符号 '{sym}' 但在代码中未找到定义，可能内容不匹配", snippet=stripped[:80]))
                break
        return cfindings

    for kind, line, idx in processed:
        if kind == "blank":
            continue
        if kind == "comment":
            # 一致性/可读性对注释块在块结束时处理；这里仅累积
            if not is_comment_line(kind):
                pass
            continue

        # kind == code / code_comment
        struct, in_block2 = strip_strings_and_comments_for_structure(line, in_block2)
        s = struct.strip()

        # 大括号深度（用于字段/方法作用域近似）
        brace_depth += struct.count("{") - struct.count("}")

        # 跳过注解行
        if s.startswith("@"):
            continue

        # ---- 方法/函数缺注释检测 ----
        java_m = JAVA_METHOD_RE.match(line)
        js_m = JS_FUNC_RE.search(line)
        method_name = None
        is_method = False
        if java_m and not s.split()[0] in CONTROL_KW and ";" not in s:
            method_name = java_m.group(1)
            is_method = True
        elif js_m and not any(s.startswith(k) for k in CONTROL_KW):
            method_name = next((g for g in js_m.groups() if g), None)
            is_method = True

        if is_method:
            # 判定是否有前置注释：看该声明行之前最近的非空行是否为注释块
            has_doc = last_meaningful_was_comment
            if not has_doc:
                findings.append(dict(category="missing_comment_function", severity="error",
                                     line=idx, message=f"函数/方法 '{method_name}' 缺少前置注释", snippet=line.strip()[:80]))
            # 进入方法体
            in_method = True
            last_meaningful_was_comment = False
            continue

        # ---- 字段缺注释检测（类级/顶层）----
        java_f = JAVA_FIELD_RE.match(line)
        js_f = JS_FIELD_RE.match(line)
        field_name = None
        is_field = False
        if java_f and not in_method and brace_depth <= 1 and ";" in s and "new " not in s.split(";")[0] + ";" or (java_f and not in_method and ";" in s):
            # 简化：Java 类级字段（不在方法内）
            if not in_method and brace_depth <= 1:
                field_name = java_f.group(1)
                is_field = True
        elif js_f and not in_method:
            field_name = js_f.group(1)
            is_field = True

        if is_field:
            has_doc = last_meaningful_was_comment
            if not has_doc:
                findings.append(dict(category="missing_comment_field", severity="warning",
                                     line=idx, message=f"重要字段 '{field_name}' 缺少前置注释", snippet=line.strip()[:80]))
            last_meaningful_was_comment = False
            continue

        # 普通代码行
        last_meaningful_was_comment = False

    # 注释块级启发式
    for span in comment_spans:
        findings.extend(collect_consistency_readability(span, span[0]))

    # 占比检查（要求1）
    if denom > 0 and ratio < ratio_target:
        findings.append(dict(category="low_ratio", severity="error",
                             line=0,
                             message=f"注释占比 {ratio*100:.1f}% < 目标 {ratio_target*100:.0f}%（代码应不多于70%）",
                             snippet=f"注释行 {comment_lines} / 代码行 {code_lines}"))

    return dict(
        file=str(path),
        total_lines=total_lines,
        blank_lines=blank_lines,
        comment_lines=comment_lines,
        code_lines=code_lines,
        comment_ratio=round(ratio, 4),
        ratio_target=ratio_target,
        functions_missing=sum(1 for f in findings if f["category"] == "missing_comment_function"),
        fields_missing=sum(1 for f in findings if f["category"] == "missing_comment_field"),
        findings=findings,
    )


def scan_paths(paths, ratio_target):
    files = []
    for p in paths:
        pp = Path(p)
        if pp.is_file():
            if pp.suffix in SUPPORTED_EXT:
                files.append(pp)
        else:
            for root, dirs, fnames in os.walk(pp):
                dirs[:] = [d for d in dirs if d not in ("node_modules", "target", "dist", "build", ".git", "coverage")]
                for fn in fnames:
                    if Path(fn).suffix in SUPPORTED_EXT:
                        files.append(Path(root) / fn)
    results = [analyze_file(f, ratio_target) for f in files]
    return results


def build_summary(results, ratio_target):
    total_files = len(results)
    total_comment = sum(r["comment_lines"] for r in results)
    total_code = sum(r["code_lines"] for r in results)
    denom = total_comment + total_code
    overall_ratio = (total_comment / denom) if denom else 0.0
    counts = {}
    for r in results:
        for f in r["findings"]:
            counts[f["category"]] = counts.get(f["category"], 0) + 1
    return dict(
        total_files=total_files,
        total_comment_lines=total_comment,
        total_code_lines=total_code,
        overall_comment_ratio=round(overall_ratio, 4),
        ratio_target=ratio_target,
        ratio_pass=overall_ratio >= ratio_target,
        finding_counts=counts,
    )


def severity_rank(sev):
    return {"error": 0, "warning": 1, "info": 2}.get(sev, 3)


def render_cli(results, summary):
    print("=" * 70)
    print(" comments-check 检查报告")
    print("=" * 70)
    print(f" 扫描文件数      : {summary['total_files']}")
    print(f" 注释总行数      : {summary['total_comment_lines']}")
    print(f" 代码总行数      : {summary['total_code_lines']}")
    print(f" 整体注释占比    : {summary['overall_comment_ratio']*100:.1f}%  (目标 >= {summary['ratio_target']*100:.0f}%)  "
          f"[{'PASS' if summary['ratio_pass'] else 'FAIL'}]")
    print("-" * 70)
    print(" 问题统计:")
    for cat, n in sorted(summary["finding_counts"].items(), key=lambda x: -x[1]):
        print(f"   - {cat:28s}: {n}")
    print("-" * 70)
    print(" 各文件占比:")
    for r in sorted(results, key=lambda x: x["comment_ratio"]):
        flag = "OK " if r["comment_ratio"] >= r["ratio_target"] else "LOW"
        print(f"   [{flag}] {r['comment_ratio']*100:5.1f}%  {r['file']}")
    print("-" * 70)
    print(" 重点问题 (error/warning, 按文件):")
    any_issue = False
    for r in results:
        for f in sorted(r["findings"], key=lambda x: severity_rank(x["severity"])):
            if f["severity"] in ("error", "warning"):
                any_issue = True
                loc = f"{r['file']}:{f['line']}" if f["line"] else r["file"]
                print(f"   [{f['severity']:7s}] {f['category']:24s} {loc}")
                print(f"             {f['message']}")
    if not any_issue:
        print("   无 error/warning 级问题。")
    print("=" * 70)


HTML_TMPL = """<!doctype html><html lang="zh"><head><meta charset="utf-8">
<title>comments-check 报告</title>
<style>
 body{{font-family:-apple-system,Segoe UI,Roboto,'Microsoft YaHei',sans-serif;margin:24px;color:#222}}
 h1{{font-size:20px}} .card{{border:1px solid #e3e3e3;border-radius:8px;padding:16px;margin:12px 0}}
 .pass{{color:#1a7f37;font-weight:700}} .fail{{color:#cf222e;font-weight:700}}
 .bar{{height:14px;border-radius:7px;background:#eee;overflow:hidden}} .bar>i{{display:block;height:100%}}
 table{{border-collapse:collapse;width:100%;font-size:13px}} th,td{{border:1px solid #ddd;padding:6px 8px;text-align:left}}
 th{{background:#f6f8fa}} .err{{color:#cf222e;background:#fff0f0}} .warn{{color:#9a6700;background:#fff8e6}} .info{{color:#0969da;background:#eef6ff}}
 .pill{{display:inline-block;padding:1px 8px;border-radius:10px;font-size:12px}}
</style></head><body>
<h1>comments-check 注释检查报告</h1>
<div class="card">
  <p>扫描文件数：<b>{total_files}</b> ｜ 注释行：<b>{total_comment}</b> ｜ 代码行：<b>{total_code}</b></p>
  <p>整体注释占比：<b class="{pp}">{ratio}%</b>（目标 ≥ {target}%） — <span class="{pp}">{status}</span></p>
  <div class="bar"><i style="width:{ratio}%;background:{color}"></i></div>
</div>
<div class="card"><h3>问题分类统计</h3><table>
<tr><th>类别</th><th>数量</th></tr>
{counts_rows}
</table></div>
<div class="card"><h3>各文件占比</h3><table>
<tr><th>文件</th><th>注释占比</th><th>注释行</th><th>代码行</th><th>缺函数注释</th><th>缺字段注释</th></tr>
{files_rows}
</table></div>
<div class="card"><h3>重点问题明细</h3><table>
<tr><th>级别</th><th>类别</th><th>位置</th><th>说明</th></tr>
{findings_rows}
</table></div>
</body></html>"""


def render_html(results, summary):
    color = "#1a7f37" if summary["ratio_pass"] else "#cf222e"
    counts_rows = "".join(
        f"<tr><td>{c}</td><td>{n}</td></tr>" for c, n in sorted(summary["finding_counts"].items(), key=lambda x: -x[1])
    ) or "<tr><td colspan=2>无</td></tr>"
    files_rows = ""
    for r in sorted(results, key=lambda x: x["comment_ratio"]):
        cls = "pass" if r["comment_ratio"] >= r["ratio_target"] else "fail"
        files_rows += (f"<tr><td>{r['file']}</td>"
                       f"<td class='{cls}'>{r['comment_ratio']*100:.1f}%</td>"
                       f"<td>{r['comment_lines']}</td><td>{r['code_lines']}</td>"
                       f"<td>{r['functions_missing']}</td><td>{r['fields_missing']}</td></tr>")
    findings_rows = ""
    for r in results:
        for f in sorted(r["findings"], key=lambda x: severity_rank(x["severity"])):
            if f["severity"] == "info":
                continue
            cls = "err" if f["severity"] == "error" else "warn"
            loc = f"{r['file']}:{f['line']}" if f["line"] else r["file"]
            findings_rows += (f"<tr class='{cls}'><td>{f['severity']}</td><td>{f['category']}</td>"
                              f"<td>{loc}</td><td>{f['message']}</td></tr>")
    if not findings_rows:
        findings_rows = "<tr><td colspan=4>无 error/warning 级问题</td></tr>"
    return HTML_TMPL.format(
        total_files=summary["total_files"],
        total_comment=summary["total_comment_lines"],
        total_code=summary["total_code_lines"],
        ratio=f"{summary['overall_comment_ratio']*100:.1f}",
        target=f"{summary['ratio_target']*100:.0f}",
        status="PASS" if summary["ratio_pass"] else "FAIL",
        pp="pass" if summary["ratio_pass"] else "fail",
        color=color,
        counts_rows=counts_rows,
        files_rows=files_rows,
        findings_rows=findings_rows,
    )


def main():
    ap = argparse.ArgumentParser(description="comments-check 注释检查器")
    ap.add_argument("paths", nargs="*", help="待检查的文件或目录（默认扫描当前目录）")
    ap.add_argument("--ratio", type=float, default=DEFAULT_RATIO, help="注释占比目标下限 (默认 0.30)")
    ap.add_argument("--json", help="输出 JSON 报告路径")
    ap.add_argument("--html", help="输出 HTML 报告路径")
    args = ap.parse_args()

    paths = args.paths or ["."]
    results = scan_paths(paths, args.ratio)
    summary = build_summary(results, args.ratio)

    render_cli(results, summary)

    if args.json:
        Path(args.json).write_text(json.dumps({"summary": summary, "files": results}, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"\nJSON 报告已写入: {args.json}")
    if args.html:
        Path(args.html).write_text(render_html(results, summary), encoding="utf-8")
        print(f"HTML 报告已写入: {args.html}")


if __name__ == "__main__":
    main()
