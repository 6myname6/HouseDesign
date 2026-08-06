#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
security-audit: 扫描代码库中的常见安全隐患。

用法:
    python security_audit.py [扫描路径] [选项]

选项:
    --path PATH          要扫描的目录或文件(默认: 当前目录)
    --format {text,json} 输出格式(默认: text)
    --output FILE        将报告写入文件(可选)
    --strict             若存在 HIGH 级别问题则以非零状态码退出
    --max-size BYTES     单文件最大扫描字节数(默认: 1MB)
    --no-skip-vcs        不排除 .git 等目录(默认排除)

说明:
    本脚本为启发式(基于规则/正则)扫描器, 结果仅作为"可疑线索",
    需要人工复核确认是否为真实漏洞, 可能存在误报与漏报。
"""

import argparse
import json
import os
import re
import sys

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------

SEVERITY_ORDER = {"HIGH": 3, "MEDIUM": 2, "LOW": 1, "INFO": 0}

# 默认跳过的目录(版本控制/依赖/构建产物/工具目录)
SKIP_DIRS = {
    ".git", ".svn", ".hg",
    "node_modules", "bower_components",
    "target", "build", "dist", "out", "bin", "obj",
    ".codebuddy", ".idea", ".vscode", ".settings",
    "venv", ".venv", "env", "__pycache__",
    ".mypy_cache", ".pytest_cache", "coverage",
}

# 扫描的文本类扩展名(代码 + 配置)
SCAN_EXTS = {
    ".java", ".kt", ".scala", ".groovy",
    ".js", ".jsx", ".ts", ".tsx", ".vue", ".mjs", ".cjs",
    ".py", ".rb", ".php", ".go", ".cs", ".swift", ".rs",
    ".properties", ".yml", ".yaml", ".env", ".ini", ".cfg", ".conf", ".config",
    ".json", ".xml", ".toml", ".sql", ".sh", ".bat", ".ps1", ".cmd",
    ".html", ".htm", ".erb", ".jsp",
}

# 仅用于配置类文件的扩展名
CONFIG_EXTS = {
    ".properties", ".yml", ".yaml", ".env", ".ini", ".cfg", ".conf", ".config", ".toml",
}

# 常见占位符值(命中这些值时不视为泄露)
PLACEHOLDER_RE = re.compile(
    r"(?i)^\s*("
    r"\$\{[^}]*\}|"          # ${...}
    r"\{\{[^}]*\}\}|"        # {{...}}
    r"<[^>]+>|"              # <...>
    r"env\(|getenv\(|os\.|config\.|"  # 引用环境变量/配置
    r"null|none|nil|undefined|"       # 空值
    r"changeme|change_it|placeholder|example|sample|your[-_]|xxxx|todo|fixme|"
    r"\*+|password|passwd|secret|token|key|admin|test|demo"
    r")\s*$"
)

# ---------------------------------------------------------------------------
# 规则定义
#   每条规则: id, category, severity, message, recommendation, pattern,
#             config_only(默认False)
# ---------------------------------------------------------------------------

SQL_KEYWORDS = (
    r"(?:SELECT\s+.+\s+FROM|INSERT\s+INTO|UPDATE\s+.+\s+SET|"
    r"DELETE\s+FROM|DROP\s+TABLE|ALTER\s+TABLE|CREATE\s+TABLE|TRUNCATE)"
)

RULES = [
    # ===== 1. 硬编码凭证 / 敏感信息泄露 =====
    {
        "id": "HARDCODED_CREDENTIAL",
        "category": "硬编码凭证",
        "severity": "HIGH",
        "message": "检测到可能为硬编码的密码/密钥/令牌赋值",
        "recommendation": "将凭证移出代码, 使用环境变量、密钥管理服务或配置中心, 并通过引用方式注入。",
        "pattern": re.compile(
            r"(?i)\b(password|passwd|pwd|secret|api[_-]?key|apikey|access[_-]?key|"
            r"private[_-]?key|auth[_-]?token|token|credential[s]?|client[_-]?secret)\b"
            r"\s*[:=]\s*[\"']([^\"']{3,})[\"']"
        ),
        "value_group": 2,
    },
    {
        "id": "HARDCODED_GENERIC_KEY",
        "category": "硬编码密钥",
        "severity": "HIGH",
        "message": "检测到可能为硬编码的 API Key / Client Secret",
        "recommendation": "通过环境变量或密钥库注入, 禁止写入源码与配置文件。",
        "pattern": re.compile(
            r"(?i)\b(api[_-]?key|apikey|client[_-]?secret|secret[_-]?key)\b"
            r"\s*[:=]\s*[\"']([A-Za-z0-9_\-]{8,})[\"']"
        ),
        "value_group": 2,
    },
    {
        "id": "AWS_ACCESS_KEY_ID",
        "category": "云凭证泄露",
        "severity": "HIGH",
        "message": "检测到 AWS Access Key ID (AKIA...) 字面量",
        "recommendation": "立即轮换该密钥, 使用 IAM 角色或密钥管理服务, 禁止硬编码。",
        "pattern": re.compile(r"AKIA[0-9A-Z]{16}"),
    },
    {
        "id": "GOOGLE_API_KEY",
        "category": "云凭证泄露",
        "severity": "HIGH",
        "message": "检测到 Google API Key (AIza...) 字面量",
        "recommendation": "轮换密钥并移出代码, 使用安全的配置注入方式。",
        "pattern": re.compile(r"AIza[0-9A-Za-z_\-]{35}"),
    },
    {
        "id": "SLACK_TOKEN",
        "category": "凭证泄露",
        "severity": "HIGH",
        "message": "检测到 Slack Token (xox...) 字面量",
        "recommendation": "轮换 Token 并移出代码。",
        "pattern": re.compile(r"xox[baprs]-[0-9A-Za-z\-]{10,}"),
    },
    {
        "id": "PRIVATE_KEY_BLOCK",
        "category": "私钥泄露",
        "severity": "HIGH",
        "message": "检测到 PEM 私钥块 (-----BEGIN ... PRIVATE KEY-----)",
        "recommendation": "私钥严禁进入仓库, 立即吊销并改用密钥管理服务/挂载卷。",
        "pattern": re.compile(r"-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP ||)PRIVATE KEY-----"),
    },
    {
        "id": "JWT_TOKEN",
        "category": "令牌泄露",
        "severity": "MEDIUM",
        "message": "检测到 JWT Token 字面量",
        "recommendation": "确认是否为测试用的过期 Token; 生产 Token 不应硬编码。",
        "pattern": re.compile(r"eyJ[A-Za-z0-9_\-]+\.eyJ[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+"),
    },
    {
        "id": "CREDENTIAL_CONNECTION_STRING",
        "category": "连接串凭证泄露",
        "severity": "HIGH",
        "message": "检测到包含用户名/密码的连接字符串 (scheme://user:pass@host)",
        "recommendation": "将连接串中的凭证抽取到环境变量或密钥管理, 禁止明文写库。",
        "pattern": re.compile(
            r"(?i)\b(jdbc|mongodb(\+srv)?|postgres|postgresql|mysql|redis|amqp|"
            r"ftp|https?|mongodb)://[^\s:@/]+:[^\s:@/]+@[^\s/@]+"
        ),
    },

    # ===== 2. SQL 注入风险 =====
    {
        "id": "SQL_INJECTION_CONCAT",
        "category": "SQL注入",
        "severity": "HIGH",
        "message": "SQL 语句与变量拼接, 存在 SQL 注入风险",
        "recommendation": "使用参数化查询/预编译语句(PreparedStatement、? 占位符、ORM 参数绑定), 禁止字符串拼接 SQL。",
        "pattern": re.compile(
            r"(?i)" + SQL_KEYWORDS + r".{0,300}?(?:\+\s*[\"']|[\"']\s*\+|\.format\s*\(|\%[sd]|\{\s*[a-zA-Z_]|f[\"'])",
            re.DOTALL,
        ),
    },
    {
        "id": "SQL_STATEMENT_RAW",
        "category": "SQL注入",
        "severity": "MEDIUM",
        "message": "使用 Statement(非 PreparedStatement) 执行 SQL, 易引入注入",
        "recommendation": "优先使用 PreparedStatement 并绑定参数。",
        "pattern": re.compile(r"(?i)\bStatement\b\s+\w+\s*=\s*\w+\.createStatement\s*\("),
    },
    {
        "id": "SQL_STRING_FORMAT",
        "category": "SQL注入",
        "severity": "HIGH",
        "message": "使用字符串格式化(% / format / f-string / ${}) 构造 SQL",
        "recommendation": "改为参数化查询, 不要通过格式化把外部输入拼入 SQL。",
        "pattern": re.compile(
            r"(?i)(?:%[sd]|\.format\s*\(|\bf[\"']|[\"\'][^\"\' ]*\$\{)"
            r".{0,200}?" + SQL_KEYWORDS,
            re.DOTALL,
        ),
    },

    # ===== 3. 命令注入 / 危险函数 =====
    {
        "id": "COMMAND_INJECTION",
        "category": "命令注入",
        "severity": "HIGH",
        "message": "执行系统命令且可能拼接外部输入",
        "recommendation": "避免将用户输入传入 shell; 使用参数数组而非字符串拼接, 并做白名单校验。",
        "pattern": re.compile(
            r"(?i)(?:Runtime\.getRuntime\(\)\.exec|\.exec\(|ProcessBuilder|"
            r"os\.system|os\.popen|subprocess\.(?:call|run|Popen|check_output|check_call))\b"
            r".{0,120}?(?:\+|\%[sd]|\{|\$\{|f[\"']|,?\s*[\"'][^\"']*\+)",
            re.DOTALL,
        ),
    },
    {
        "id": "SUBPROCESS_SHELL_TRUE",
        "category": "命令注入",
        "severity": "HIGH",
        "message": "subprocess 使用 shell=True, 存在命令注入风险",
        "recommendation": "移除 shell=True, 使用参数列表方式调用, 并对参数做校验。",
        "pattern": re.compile(r"(?i)subprocess\.\w+\s*\([^)]*shell\s*=\s*True"),
    },
    {
        "id": "DANGEROUS_EVAL",
        "category": "代码注入",
        "severity": "HIGH",
        "message": "使用了 eval/exec 执行动态内容, 可能执行任意代码",
        "recommendation": "避免对不可信输入使用 eval/exec; 必要时用安全的解析器(如 ast.literal_eval)并做沙箱。",
        "pattern": re.compile(r"(?i)(?<![.\w])eval\s*\(|(?<![.\w])exec\s*\("),
    },

    # ===== 4. 不安全反序列化 =====
    {
        "id": "INSECURE_DESERIALIZE_JAVA",
        "category": "不安全反序列化",
        "severity": "HIGH",
        "message": "使用 ObjectInputStream 反序列化, 可能执行恶意 payload",
        "recommendation": "对反序列化来源做白名单校验, 或使用安全的序列化协议。",
        "pattern": re.compile(r"(?i)\bnew\s+ObjectInputStream\b"),
    },
    {
        "id": "INSECURE_DESERIALIZE_PY",
        "category": "不安全反序列化",
        "severity": "MEDIUM",
        "message": "使用 pickle/yaml.load 反序列化, 来源不可信时危险",
        "recommendation": "优先使用 json; 若必须 pickle/yaml, 仅处理可信数据, yaml 使用 safe_load。",
        "pattern": re.compile(r"(?i)\b(pickle\.load(?:s)?|yaml\.load)\s*\("),
    },

    # ===== 5. 弱密码学 =====
    {
        "id": "WEAK_HASH",
        "category": "弱密码学",
        "severity": "MEDIUM",
        "message": "使用了弱哈希算法 (MD5/SHA-1)",
        "recommendation": "密码存储使用 bcrypt/Argon2/PBKDF2; 完整性校验优先 SHA-256 及以上。",
        "pattern": re.compile(r"(?i)(?:MessageDigest|DigestUtils|hashlib)\b.{0,40}?\b(?:MD5|SHA-?1|SHA1)\b"),
    },
    {
        "id": "WEAK_CIPHER",
        "category": "弱密码学",
        "severity": "MEDIUM",
        "message": "使用了弱加密算法/模式 (DES/RC4/ECB)",
        "recommendation": "改用 AES-GCM 等强算法, 避免 ECB 模式。",
        "pattern": re.compile(r"(?i)\b(?:DES|RC4|ECB)\b"),
    },

    # ===== 6. 不安全的 TLS / 校验绕过 =====
    {
        "id": "TLS_VERIFY_DISABLED",
        "category": "传输安全",
        "severity": "HIGH",
        "message": "禁用了 TLS 证书校验 (verify=False / SSL_VERIFYPEER=false)",
        "recommendation": "保持证书校验开启, 使用正确的 CA 证书而非关闭校验。",
        "pattern": re.compile(
            r"(?i)(verify\s*=\s*False|SSL_VERIFYPEER\s*=\s*false|"
            r"setSSLPeerVerification\s*\(\s*false|InsecureRequestWarning|"
            r"verify\s*:\s*false|rejectUnauthorized\s*:\s*false)"
        ),
    },

    # ===== 7. CORS 配置过宽 =====
    {
        "id": "CORS_WILDCARD",
        "category": "配置安全",
        "severity": "MEDIUM",
        "message": "CORS 允许来源设置为通配符 *",
        "recommendation": "明确指定受信任的域名, 避免 * 与凭据(Credentials)同时使用。",
        "pattern": re.compile(r"(?i)(Access-?Control-?Allow-?Origin|allowedOrigins|@CrossOrigin)[^;\n]{0,60}?[\"']?\*[\"']?"),
    },

    # ===== 8. 配置明文敏感信息(仅配置类文件) =====
    {
        "id": "CONFIG_PLAINTEXT_SECRET",
        "category": "配置明文敏感信息",
        "severity": "HIGH",
        "message": "配置文件中存在明文敏感字段(密码/密钥/令牌)",
        "recommendation": "配置中仅保留占位符或引用环境变量/密钥服务, 明文凭证移出仓库。",
        "pattern": re.compile(
            r"(?im)^\s*(?:(?:spring\.)?datasource\.)?"
            r"(password|passwd|secret|api[_-]?key|access[_-]?key|private[_-]?key|"
            r"token|auth[_-]?token|client[_-]?secret|credential[s]?)\b"
            r"[\s]*[:=]\s*[\"']?([^\s\"']{2,})[\"']?"
        ),
        "value_group": 2,
        "config_only": True,
    },
    {
        "id": "CONFIG_DEBUG_ENABLED",
        "category": "配置安全",
        "severity": "LOW",
        "message": "配置中开启了 debug / 详细错误暴露",
        "recommendation": "生产环境关闭 debug, 避免泄露堆栈与内部信息。",
        "pattern": re.compile(r"(?im)^\s*(?:debug|spring\.profiles\.active|server\.error\.include[^\n]*)\b[^\n]*\b(true|dev|development|local)\b"),
        "config_only": True,
    },
]


# ---------------------------------------------------------------------------
# 扫描逻辑
# ---------------------------------------------------------------------------

def is_text_file(path):
    ext = os.path.splitext(path)[1].lower()
    return ext in SCAN_EXTS


def should_skip_dir(dirname):
    return dirname in SKIP_DIRS


def line_of(content, pos):
    return content.count("\n", 0, pos) + 1


def scan_file(path, max_size, findings):
    try:
        size = os.path.getsize(path)
    except OSError:
        return
    if size > max_size:
        return
    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
    except (OSError, IOError):
        return

    ext = os.path.splitext(path)[1].lower()
    is_config = ext in CONFIG_EXTS

    for rule in RULES:
        if rule.get("config_only") and not is_config:
            continue
        for m in rule["pattern"].finditer(content):
            # 占位符/空值豁免
            vg = rule.get("value_group")
            if vg:
                val = m.group(vg)
                if not val or PLACEHOLDER_RE.match(val or ""):
                    continue
            findings.append({
                "rule_id": rule["id"],
                "category": rule["category"],
                "severity": rule["severity"],
                "file": path,
                "line": line_of(content, m.start()),
                "message": rule["message"],
                "recommendation": rule["recommendation"],
                "snippet": m.group(0)[:160],
            })


def walk_and_scan(root, max_size, findings):
    if os.path.isfile(root):
        if is_text_file(root):
            scan_file(root, max_size, findings)
        return
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if not should_skip_dir(d)]
        for fn in filenames:
            full = os.path.join(dirpath, fn)
            if is_text_file(full):
                scan_file(full, max_size, findings)


# ---------------------------------------------------------------------------
# 报告输出
# ---------------------------------------------------------------------------

def make_summary(findings):
    summary = {"HIGH": 0, "MEDIUM": 0, "LOW": 0, "INFO": 0, "total": len(findings)}
    for f in findings:
        summary[f["severity"]] = summary.get(f["severity"], 0) + 1
    return summary


def render_text(findings):
    if not findings:
        return "✅ 未检测到明显的安全隐患线索。\n(注: 启发式扫描可能存在漏报, 建议结合人工复核。)"
    order = {"HIGH": 0, "MEDIUM": 1, "LOW": 2, "INFO": 3}
    findings.sort(key=lambda f: (order.get(f["severity"], 9), f["file"], f["line"]))
    lines = []
    summary = make_summary(findings)
    lines.append("=" * 70)
    lines.append("安全审计报告 (security-audit)")
    lines.append("=" * 70)
    lines.append(
        "总计: {}  高危(HIGH): {}  中危(MEDIUM): {}  低危(LOW): {}".format(
            summary["total"], summary["HIGH"], summary["MEDIUM"], summary["LOW"]
        )
    )
    lines.append("")
    for f in findings:
        lines.append("[{}] {}  ({}:{})".format(f["severity"], f["category"], f["file"], f["line"]))
        lines.append("  规则: {}".format(f["rule_id"]))
        lines.append("  描述: {}".format(f["message"]))
        lines.append("  命中: {}".format(f["snippet"].replace("\n", " ").strip()))
        lines.append("  建议: {}".format(f["recommendation"]))
        lines.append("")
    return "\n".join(lines)


def render_markdown(findings):
    if not findings:
        return "# 安全审计报告\n\n✅ 未检测到明显的安全隐患线索。\n"
    order = {"HIGH": 0, "MEDIUM": 1, "LOW": 2, "INFO": 3}
    findings.sort(key=lambda f: (order.get(f["severity"], 9), f["file"], f["line"]))
    summary = make_summary(findings)
    out = ["# 安全审计报告", "",
           "## 概览", "",
           "- 总计: **{}**".format(summary["total"]),
           "- 高危 HIGH: **{}**".format(summary["HIGH"]),
           "- 中危 MEDIUM: **{}**".format(summary["MEDIUM"]),
           "- 低危 LOW: **{}**".format(summary["LOW"]),
           "", "## 明细", ""]
    for i, f in enumerate(findings, 1):
        out.append("### {}. [{}] {}".format(i, f["severity"], f["category"]))
        out.append("- 文件: `{}:{}`".format(f["file"], f["line"]))
        out.append("- 规则: `{}`".format(f["rule_id"]))
        out.append("- 描述: {}".format(f["message"]))
        out.append("- 命中片段: `{}`".format(f["snippet"].replace("`", "'").replace("\n", " ").strip()))
        out.append("- 修复建议: {}".format(f["recommendation"]))
        out.append("")
    return "\n".join(out)


def main():
    parser = argparse.ArgumentParser(description="安全审计扫描器")
    parser.add_argument("path", nargs="?", default=".", help="扫描路径(目录或文件)")
    parser.add_argument("--format", choices=["text", "json", "markdown"], default="text")
    parser.add_argument("--output", help="报告输出文件")
    parser.add_argument("--strict", action="store_true", help="存在 HIGH 时以非零码退出")
    parser.add_argument("--max-size", type=int, default=1024 * 1024, help="单文件最大字节数")
    parser.add_argument("--no-skip-vcs", action="store_true", help="不排除 .git 等目录")
    args = parser.parse_args()

    if args.no_skip_vcs:
        SKIP_DIRS.discard(".git")
        SKIP_DIRS.discard(".svn")
        SKIP_DIRS.discard(".hg")

    findings = []
    walk_and_scan(args.path, args.max_size, findings)

    if args.format == "json":
        report = json.dumps({
            "summary": make_summary(findings),
            "findings": findings,
        }, ensure_ascii=False, indent=2)
    elif args.format == "markdown":
        report = render_markdown(findings)
    else:
        report = render_text(findings)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(report)
        print("报告已写入: {}".format(args.output))
    else:
        print(report)

    if args.strict and any(f["severity"] == "HIGH" for f in findings):
        sys.exit(1)


if __name__ == "__main__":
    main()
