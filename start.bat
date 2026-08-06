@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

cd /d "%~dp0"

echo ============================================================
echo   筑梦家 · 房屋装修设计网站  一键启动
echo ============================================================
echo.

REM ---------- 环境检查 ----------
where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Java，请先安装 JDK 17+ 并配置到 PATH。
    goto :fail
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Maven，请先安装 Maven 3.8+ 并配置到 PATH。
    goto :fail
)

where npm >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Node.js/npm，请先安装 Node.js 18+ 并配置到 PATH。
    goto :fail
)

echo [1/3] 环境检查通过（Java / Maven / Node）。
echo.

REM ---------- 启动后端 ----------
echo [2/3] 正在新窗口启动后端（Spring Boot，端口 8080）...
start "HouseDesign-Backend" cmd /k "cd /d "%~dp0backend" && mvn spring-boot:run"
echo.

REM ---------- 启动前端 ----------
echo [3/3] 正在新窗口启动前端（Vite，端口 5173）...
if not exist "%~dp0frontend\node_modules" (
    echo      首次运行，需要安装前端依赖，请稍候...
    start "HouseDesign-Frontend" cmd /k "cd /d "%~dp0frontend" && npm install && npm run dev"
) else (
    start "HouseDesign-Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev"
)

echo.
echo ============================================================
echo   已在两个新窗口分别启动后端与前端。
echo   - 后端接口: http://localhost:8080
echo   - 前端页面: http://localhost:5173
echo.
echo   提示：后端首次启动会下载 Maven 依赖并自动建库建表，
echo         前端首次启动会执行 npm install，请耐心等待编译完成。
echo   编译完成后浏览器访问: http://localhost:5173
echo ============================================================
echo.
echo 等待前端就绪后自动打开浏览器（约 20 秒）...
timeout /t 20 /nobreak >nul
start "" http://localhost:5173
goto :eof

:fail
echo.
echo 启动已中止，请解决上述问题后重试。
pause
