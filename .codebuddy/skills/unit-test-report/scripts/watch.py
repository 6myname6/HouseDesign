#!/usr/bin/env python3
"""Watch HouseDesign source directories and auto-run the unit-test suite on change.

Polls modification times of backend/src and frontend/src every 2 seconds; when a
change is detected it debounces for a few seconds, then invokes run_tests.py so the
combined report is regenerated. Use Ctrl+C to stop.
"""
import os
import subprocess
import sys
import time

SKILL_DIR = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(SKILL_DIR, "..", "..", ".."))
WATCH = [
    os.path.join(ROOT, "backend", "src"),
    os.path.join(ROOT, "frontend", "src"),
]
DEBOUNCE = 3


def snapshot():
    mx = 0
    for base in WATCH:
        if not os.path.isdir(base):
            continue
        for dp, _, fs in os.walk(base):
            for f in fs:
                try:
                    mx = max(mx, os.path.getmtime(os.path.join(dp, f)))
                except OSError:
                    pass
    return mx


if __name__ == "__main__":
    print("监听源码改动，自动运行测试 (Ctrl+C 退出) ...")
    print("监听目录: " + ", ".join(WATCH))
    last = snapshot()
    while True:
        time.sleep(2)
        cur = snapshot()
        if cur and cur != last:
            last = cur
            print(f"\n[{time.strftime('%H:%M:%S')}] 检测到代码改动，{DEBOUNCE} 秒后运行测试...")
            time.sleep(DEBOUNCE)
            subprocess.run([sys.executable, os.path.join(SKILL_DIR, "run_tests.py")], cwd=SKILL_DIR)
