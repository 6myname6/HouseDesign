#!/usr/bin/env python3
"""Scaffold unit-test infrastructure for the HouseDesign project if missing.

- Backend : ensure the JaCoCo Maven plugin is present in backend/pom.xml (coverage reports).
- Frontend: ensure Vitest + coverage provider + test scripts + vitest.config.js exist.

Idempotent: safe to run repeatedly. Backend changes are offline; the frontend step
installs npm dev-dependencies (requires network access on first run).
"""
import json
import os
import subprocess

SKILL_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(SKILL_DIR, "..", "..", ".."))
BACKEND = os.path.join(ROOT, "backend")
FRONTEND = os.path.join(ROOT, "frontend")

JACOCO_PLUGIN = """
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.12</version>
            <executions>
                <execution>
                    <id>prepare-agent</id>
                    <goals><goal>prepare-agent</goal></goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals><goal>report</goal></goals>
                </execution>
            </executions>
        </plugin>"""

VITEST_CONFIG = """import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx,vue}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'json-summary'],
      reportsDirectory: 'coverage',
      include: ['src/**/*.{js,ts,vue}'],
      exclude: ['src/**/*.{test,spec}.{js,ts,vue}', 'src/main.js', 'src/router/**']
    }
  }
})
"""


def setup_backend():
    pom = os.path.join(BACKEND, "pom.xml")
    if not os.path.exists(pom):
        print("[backend] pom.xml 不存在，跳过")
        return
    s = open(pom, encoding="utf-8").read()
    if "jacoco-maven-plugin" in s:
        print("[backend] JaCoCo 已配置，跳过")
        return
    marker = "</plugins>"
    if marker not in s:
        print("[backend] 未找到 </plugins>，跳过")
        return
    s = s.replace(marker, JACOCO_PLUGIN + "\n    " + marker, 1)
    open(pom, "w", encoding="utf-8").write(s)
    print("[backend] 已注入 JaCoCo 插件 -> 测试后生成 target/site/jacoco 覆盖率报告")


def setup_frontend():
    pkg = os.path.join(FRONTEND, "package.json")
    if not os.path.exists(pkg):
        print("[frontend] package.json 不存在，跳过")
        return
    data = json.load(open(pkg, encoding="utf-8"))
    deps = data.get("devDependencies", {})
    if "vitest" not in deps:
        print("[frontend] 安装 Vitest 相关依赖 (需联网) ...")
        try:
            subprocess.run(
                ["npm", "--prefix", FRONTEND, "install", "-D",
                 "vitest", "@vitest/coverage-v8", "@vue/test-utils", "jsdom"],
                check=True,
            )
        except subprocess.CalledProcessError as e:
            print(f"[frontend] 依赖安装失败: {e}")
            return
    scripts = data.setdefault("scripts", {})
    if "test" not in scripts:
        scripts["test"] = "vitest run"
    if "test:coverage" not in scripts:
        scripts["test:coverage"] = "vitest run --coverage"
    json.dump(data, open(pkg, "w", encoding="utf-8"), indent=2, ensure_ascii=False)
    vcfg = os.path.join(FRONTEND, "vitest.config.js")
    if not os.path.exists(vcfg):
        open(vcfg, "w", encoding="utf-8").write(VITEST_CONFIG)
        print("[frontend] 已生成 vitest.config.js")
    print("[frontend] 测试脚本就绪: npm run test / npm run test:coverage")


if __name__ == "__main__":
    setup_backend()
    setup_frontend()
    print("\n初始化完成。运行 scripts/run_tests.py 执行测试并生成报告。")
