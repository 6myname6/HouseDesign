#!/usr/bin/env python3
"""Run unit tests for the HouseDesign project and generate a combined report.

Covers both stacks:
  - Backend : Spring Boot (Maven + JUnit5 + JaCoCo)
  - Frontend: Vue (Vitest + v8 coverage)

Produces:
  - A CLI summary printed to stdout.
  - A combined HTML report at <skill>/reports/report-<ts>.html (and latest.html).
  - Machine-readable JSON at <skill>/reports/latest.json.
  - Links to the framework-native HTML reports (JaCoCo + Vitest coverage).
"""
import argparse
import datetime
import glob
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET

SKILL_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(SKILL_DIR, "..", "..", ".."))
BACKEND = os.path.join(ROOT, "backend")
FRONTEND = os.path.join(ROOT, "frontend")
REPORT_DIR = os.path.join(SKILL_DIR, "reports")


def run(cmd, cwd, label):
    print(f"\n=== {label} ===\n> " + " ".join(cmd))
    try:
        p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)
    except FileNotFoundError as e:
        print(f"命令未找到: {e}")
        return 1, ""
    out = p.stdout + "\n" + p.stderr
    tail = out[-2500:] if len(out) > 2500 else out
    print(tail)
    return p.returncode, out


def parse_surefire():
    files = glob.glob(os.path.join(BACKEND, "target", "surefire-reports", "*.xml"))
    t = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "suites": 0}
    for f in files:
        try:
            r = ET.parse(f).getroot()
            if r.tag != "testsuite":
                continue
            t["tests"] += int(r.get("tests", 0))
            t["failures"] += int(r.get("failures", 0))
            t["errors"] += int(r.get("errors", 0))
            t["skipped"] += int(r.get("skipped", 0))
            t["suites"] += 1
        except Exception:
            pass
    return t


def parse_jacoco():
    csv = os.path.join(BACKEND, "target", "site", "jacoco", "jacoco.csv")
    if not os.path.exists(csv):
        return None
    lines = open(csv, encoding="utf-8").read().splitlines()
    hdr = lines[0].split(",")
    im, ic = hdr.index("INSTRUCTION_MISSED"), hdr.index("INSTRUCTION_COVERED")
    missed = cov = 0
    for ln in lines[1:]:
        parts = ln.split(",")
        missed += int(parts[im])
        cov += int(parts[ic])
    tot = missed + cov
    return {"pct": round(cov / tot * 100, 1) if tot else 0.0, "covered": cov, "missed": missed}


def parse_vitest(stdout):
    res = {"test_files": None, "tests": None, "coverage": None}
    m = re.search(r"Test Files\s+(\d+)", stdout)
    if m:
        res["test_files"] = int(m.group(1))
    m = re.search(r"\nTests\s+(\d+)", stdout)
    if m:
        res["tests"] = int(m.group(1))
    summary = os.path.join(FRONTEND, "coverage", "coverage-summary.json")
    if os.path.exists(summary):
        d = json.load(open(summary, encoding="utf-8")).get("total", {})

        def g(k):
            v = d.get(k, {})
            return {"pct": v.get("pct", 0), "covered": v.get("covered", 0), "total": v.get("total", 0)}

        res["coverage"] = {k: g(k) for k in ["lines", "statements", "functions", "branches"]}
    return res


def _badge(p):
    if p is None:
        return '<span class="badge na">N/A</span>'
    color = "#2e7d32" if p >= 80 else ("#f9a825" if p >= 60 else "#c62828")
    return f'<span class="badge" style="background:{color}">{p}%</span>'


def make_html(be, be_cov, fe, fe_cov, ts):
    be_html = (
        os.path.relpath(os.path.join(BACKEND, "target", "site", "jacoco", "index.html"), REPORT_DIR)
        if be_cov
        else "#"
    )
    fe_html = (
        os.path.relpath(os.path.join(FRONTEND, "coverage", "index.html"), REPORT_DIR)
        if fe_cov
        else "#"
    )

    be_lines = []
    if be:
        be_status = "PASS" if (be["failures"] == 0 and be["errors"] == 0) else "FAIL"
        be_lines.append(f"<tr><td>用例总数</td><td>{be['tests']}</td></tr>")
        be_lines.append(f"<tr><td>失败 / 错误</td><td>{be['failures']} / {be['errors']}</td></tr>")
        be_lines.append(f"<tr><td>跳过</td><td>{be['skipped']}</td></tr>")
        be_lines.append(f"<tr><td>指令覆盖率</td><td>{_badge(be_cov['pct'] if be_cov else None)}</td></tr>")
        be_lines.append(f"<tr><td>详细报告</td><td><a href='{be_html}'>JaCoCo HTML</a></td></tr>")
    else:
        be_status = "N/A"
        be_lines.append("<tr><td colspan='2'>未运行</td></tr>")

    fe_lines = []
    if fe:
        fe_cov_pct = fe_cov["lines"]["pct"] if fe_cov else None
        fe_status = "PASS" if fe.get("test_files") else "N/A"
        fe_lines.append(f"<tr><td>测试文件</td><td>{fe.get('test_files')}</td></tr>")
        fe_lines.append(f"<tr><td>用例总数</td><td>{fe.get('tests')}</td></tr>")
        fe_lines.append(f"<tr><td>行覆盖率</td><td>{_badge(fe_cov_pct)}</td></tr>")
        fe_lines.append(f"<tr><td>详细报告</td><td><a href='{fe_html}'>Vitest HTML</a></td></tr>")
    else:
        fe_status = "N/A"
        fe_lines.append("<tr><td colspan='2'>未运行</td></tr>")

    return f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<title>HouseDesign 单元测试报告 {ts}</title>
<style>
 body{{font-family:-apple-system,Segoe UI,Roboto,'PingFang SC',sans-serif;margin:32px;color:#222;}}
 h1{{font-size:22px;}} .meta{{color:#777;font-size:13px;margin-bottom:20px;}}
 .grid{{display:flex;gap:24px;flex-wrap:wrap;}}
 .card{{border:1px solid #e3e3e3;border-radius:10px;padding:18px 22px;min-width:320px;flex:1;}}
 .card h2{{margin:0 0 12px;font-size:17px;}}
 table{{width:100%;border-collapse:collapse;}} td{{padding:6px 4px;border-bottom:1px solid #f0f0f0;font-size:14px;}}
 .status{{display:inline-block;padding:2px 10px;border-radius:10px;color:#fff;font-size:13px;font-weight:600;}}
 .pass{{background:#2e7d32;}} .fail{{background:#c62828;}} .na{{background:#9e9e9e;}}
 .badge{{display:inline-block;padding:2px 10px;border-radius:10px;color:#fff;font-weight:600;font-size:13px;}}
 .badge.na{{background:#9e9e9e;}}
 a{{color:#1565c0;}}
</style></head><body>
<h1>HouseDesign 单元测试报告</h1>
<div class="meta">生成时间: {ts} &nbsp;|&nbsp; 后端: Spring Boot + JUnit5 + JaCoCo &nbsp;|&nbsp; 前端: Vue + Vitest</div>
<div class="grid">
  <div class="card">
    <h2>后端 (Backend) <span class="status {'pass' if be_status=='PASS' else ('fail' if be_status=='FAIL' else 'na')}">{be_status}</span></h2>
    <table>{''.join(f'<tr><td>{k}</td><td>{v}</td></tr>' for k,v in [])}{''.join(be_lines)}</table>
  </div>
  <div class="card">
    <h2>前端 (Frontend) <span class="status {'pass' if fe_status=='PASS' else 'na'}">{fe_status}</span></h2>
    <table>{''.join(fe_lines)}</table>
  </div>
</div>
<p style="margin-top:24px;color:#777;font-size:12px;">本报告由 unit-test-report 技能生成。详细覆盖率请点击上方链接查看框架原生 HTML 报告。</p>
</body></html>"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--backend-only", action="store_true")
    ap.add_argument("--frontend-only", action="store_true")
    ap.add_argument("--no-report", action="store_true")
    args = ap.parse_args()
    os.makedirs(REPORT_DIR, exist_ok=True)
    ts = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")

    be = be_cov = fe = fe_cov = None
    if not args.frontend_only:
        rc, out = run(["mvn", "-q", "test"], BACKEND, "后端测试 (Maven + JUnit5 + JaCoCo)")
        be = parse_surefire()
        be_cov = parse_jacoco()
        if rc != 0:
            print("[warn] 后端测试返回非零退出码，请检查失败用例")
    if not args.backend_only:
        rc, out = run(["npm", "--prefix", FRONTEND, "run", "test:coverage"], ROOT,
                      "前端测试 (Vitest + coverage)")
        fe = parse_vitest(out)
        fe_cov = fe["coverage"] if fe else None

    print("\n================ 测试报告摘要 ================")
    if be:
        print(f"[后端] 用例={be['tests']} 失败={be['failures']} 错误={be['errors']} "
              f"跳过={be['skipped']} 覆盖率={be_cov['pct'] if be_cov else 'N/A'}%")
    if fe:
        cov = fe_cov["lines"]["pct"] if fe_cov else "N/A"
        print(f"[前端] 测试文件={fe['test_files']} 用例={fe['tests']} 行覆盖率={cov}%")
    if not args.no_report:
        html = make_html(be, be_cov, fe, fe_cov, ts)
        path = os.path.join(REPORT_DIR, f"report-{ts}.html")
        open(path, "w", encoding="utf-8").write(html)
        open(os.path.join(REPORT_DIR, "latest.html"), "w", encoding="utf-8").write(html)
        json.dump(
            {"ts": ts, "backend": be, "backend_coverage": be_cov, "frontend": fe,
             "frontend_coverage": fe_cov},
            open(os.path.join(REPORT_DIR, "latest.json"), "w", encoding="utf-8"),
            ensure_ascii=False, indent=2,
        )
        print(f"\nHTML 报告: {path}")
        print(f"最新报告: {os.path.join(REPORT_DIR, 'latest.html')}")


if __name__ == "__main__":
    main()
