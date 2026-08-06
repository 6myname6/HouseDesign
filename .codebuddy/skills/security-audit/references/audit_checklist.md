# 安全审计检查清单 (security-audit)

本清单供 `security-audit` 技能在执行审计时参考。扫描脚本(`scripts/security_audit.py`)
提供自动化线索, 但无法覆盖所有场景。请结合本清单进行人工复核, 重点关注
**业务逻辑层面的安全隐患** 与 **脚本漏报项**。

> 等级说明: 🔴 HIGH(高危) / 🟠 MEDIUM(中危) / 🟡 LOW(低危)

---

## 1. 敏感信息泄露 (硬编码凭证)
**检查目标**: 源码、提交历史、配置文件、测试文件中是否写入明文凭证。

- 🔴 数据库密码、`password`/`secret`/`token`/`apiKey` 等直接赋值字符串
- 🔴 云厂商密钥: AWS `AKIA...`、Google `AIza...`、Azure、Slack `xox...`、Stripe `sk_live_...`
- 🔴 私钥文件内容 (`-----BEGIN ... PRIVATE KEY-----`) 出现在源码或 `.pem` 误提交
- 🔴 连接字符串 `jdbc:...://user:pass@host`、`mongodb://user:pass@host`
- 🔴 JWT / Session Secret / 加密密钥 硬编码
- 🟠 将凭证打印到日志 (`log.debug(password)`)
- 🟠 `.env`、`.properties`、`.yml` 被提交到仓库(应加入 `.gitignore`)

**修复**: 使用环境变量、密钥管理服务(如 Vault / KMS / 云密钥库)、配置中心;
源码仅保留占位符 `${DB_PASSWORD}`。已泄露的密钥必须**立即轮换**。

---

## 2. SQL 注入
**检查目标**: 所有与数据库交互的位置。

- 🔴 字符串拼接构造 SQL:
  - Java: `"SELECT * FROM user WHERE id=" + id`、使用 `Statement` 而非 `PreparedStatement`
  - MyBatis: `${}` 拼接(应使用 `#{}`)
  - Python: `cursor.execute("SELECT ... " + user_input)`、`f"SELECT ... {x}"`、`%` 格式化
  - Node: 模板字符串拼 SQL
- 🔴 ORM 中仍用字符串拼接原生 SQL(`entityManager.createNativeQuery(... + ...)`)
- 🟠 LIKE / ORDER BY 等子句参数化不全(ORDER BY 字段名无法直接参数化, 需白名单)

**修复**: 一律使用参数化查询 / 预编译语句 / ORM 参数绑定; 表名/列名用白名单校验。

---

## 3. 配置明文敏感信息
**检查目标**: `application.properties/.yml`、`.env`、`*.conf`、Docker/K8s 配置。

- 🔴 `spring.datasource.password=...`、`jwt.secret=...` 等明文值
- 🔴 配置中内联完整连接串(含账号密码)
- 🟠 生产配置开启 `debug=true`、`server.error.include-stacktrace=always`
- 🟠 `CORS` 允许来源为 `*` 且与 `allowCredentials=true` 共存

**修复**: 配置仅放占位符或引用环境变量; 使用配置中心/Secret 管理; 生产关闭调试。

---

## 4. 命令注入 / 危险函数
- 🔴 `Runtime.exec` / `ProcessBuilder` / `os.system` / `subprocess` 拼接外部输入
- 🔴 `subprocess(..., shell=True)` 接收不可信参数
- 🔴 `eval()` / `exec()` 处理用户输入或反序列化数据
- 🟠 模板引擎(`Freemarker`/`Velocity`/`Jinja2`)执行用户输入的表达式

**修复**: 用参数数组调用命令并做白名单; 禁止对用户数据 `eval`; 模板沙箱。

---

## 5. 不安全反序列化
- 🔴 Java `ObjectInputStream` 反序列化不可信数据(可 RCE)
- 🔴 Python `pickle.loads` 来自网络/文件的内容
- 🔴 YAML 使用 `yaml.load` 而非 `yaml.safe_load`
- 🟠 PHP `unserialize` 用户可控数据

**修复**: 仅反序列化可信来源; 使用 JSON 等安全格式; 加类型白名单。

---

## 6. 路径遍历 / 文件操作
- 🔴 用户提供的文件名/路径直接拼接后 `File`/`open`/`fs.readFile`
  (如 `new File(uploadDir + "/" + userInput)`)
- 🔴 未校验 `../` 导致任意文件读取/写入
- 🟠 上传文件未限制类型/大小/存储路径

**修复**: 规范化路径并限制在白名单根目录内; 文件名做校验/重命名; 限制扩展名。

---

## 7. 跨站脚本 (XSS)
- 🔴 将用户输入直接拼接进 HTML 响应 / 模板且未转义
- 🟠 富文本未做白名单过滤(应使用 DOMPurify 等)

**修复**: 输出编码/上下文转义; 启用 CSP; 富文本走白名单净化。

---

## 8. 认证与授权
- 🔴 关键接口缺失鉴权(如直接通过 ID 访问他人资源 -> IDOR)
- 🔴 权限校验仅在前端实现, 后端未校验
- 🟠 使用弱口令/默认口令; 无登录失败锁定/验证码
- 🟠 Session/JWT 未设置 HttpOnly/Secure、过期时间过长

**修复**: 后端强制鉴权与鉴权对象归属校验; 密码强度策略; 安全 Cookie 属性。

---

## 9. 弱密码学 / 传输安全
- 🔴 密码以明文/仅 MD5/SHA1(无盐)存储
- 🔴 使用 DES/RC4/ECB
- 🔴 禁用 TLS 证书校验 (`verify=False`、`SSL_VERIFYPEER=false`)
- 🟠 敏感数据明文传输(非 HTTPS)

**修复**: 密码用 bcrypt/Argon2/PBKDF2+salt; AES-GCM; 开启证书校验; 全站 HTTPS。

---

## 10. 依赖与供应链
- 🟠 存在已知漏洞的依赖版本(建议结合 `npm audit` / `mvn dependency-check` / `pip-audit`)
- 🟠 锁文件缺失导致构建不可重现

**修复**: 定期更新依赖, 引入 SCA 工具并阻断高危漏洞。

---

## 审计输出建议
最终应向用户输出结构化报告, 至少包含:
1. **问题清单**: 文件:行号、类别、严重级别、描述、命中片段
2. **修复建议**: 针对每条可操作
3. **优先级**: 先修 HIGH(凭证泄露、注入、RCE 类)
4. **误报说明**: 标注哪些为启发式线索需人工确认
