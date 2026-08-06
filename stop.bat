@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================================================
echo   筑梦家 · 房屋装修设计网站  一键停止
echo ============================================================
echo.

REM ---------- 按窗口标题关闭启动窗口 ----------
echo [1/2] 关闭启动窗口（Backend / Frontend）...
taskkill /FI "WINDOWTITLE eq HouseDesign-Backend*" /T /F >nul 2>nul
taskkill /FI "WINDOWTITLE eq HouseDesign-Frontend*" /T /F >nul 2>nul

REM ---------- 按端口关闭残留进程 ----------
echo [2/2] 释放端口 8080（后端）与 5173（前端）...

for %%P in (8080 5173) do (
    set "found="
    for /f "tokens=5" %%A in ('netstat -ano ^| findstr /R /C:":%%P .*LISTENING"') do (
        set "found=1"
        echo      端口 %%P 被进程 %%A 占用，正在结束...
        taskkill /PID %%A /T /F >nul 2>nul
    )
    if not defined found echo      端口 %%P 未被占用。
)

echo.
echo ============================================================
echo   已停止后端与前端服务。
echo ============================================================
echo.
pause
