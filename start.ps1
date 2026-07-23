#requires -Version 5.1
<#
.SYNOPSIS
    个人 AI 助手项目 - 开机后基础设施一键检查与启动
.DESCRIPTION
    检查并按需启动: MySQL80 / Docker Desktop / Redis 容器 / Qdrant 容器 / Ollama
    自启动链: Ollama(自启) + Docker Desktop(自启)->redis/qdrant 容器(unless-stopped 自动恢复)
    唯一不稳定项是 MySQL80，本脚本对其兜底检查与提权启动。
.NOTES
    用法: 开机后在此目录打开 PowerShell，运行 .\start.ps1
    MySQL 启动需要管理员权限；脚本会自动弹 UAC 提权，确认即可。
#>

$ErrorActionPreference = "Continue"
$composeFile = Join-Path $PSScriptRoot "docker-compose.yml"

function Write-Ok($t) { Write-Host "  [OK] $t" -ForegroundColor Green }
function Write-Warn($t) { Write-Host "  [!]  $t" -ForegroundColor Yellow }
function Write-Err($t) { Write-Host "  [X]  $t" -ForegroundColor Red }

# ---------- 1. MySQL80 ----------
Write-Host "`n[1/5] MySQL" -ForegroundColor Cyan
$mysql = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if (-not $mysql) {
    Write-Err "未找到 MySQL80 服务，请确认 MySQL 已安装"
} elseif ($mysql.Status -eq 'Running') {
    Write-Ok "MySQL80 运行中 (localhost:3306)"
} else {
    Write-Warn "MySQL80 未运行 (Status=$($mysql.Status))，尝试启动..."
    try {
        Start-Service -Name "MySQL80" -ErrorAction Stop
        Write-Ok "MySQL80 已启动"
    } catch {
        Write-Warn "需要管理员权限，弹出 UAC 提权启动（请点“是”）..."
        Start-Process powershell -Verb RunAs -Wait -ArgumentList "-NoProfile","-Command","Start-Service MySQL80"
        Start-Sleep -Seconds 2
        $mysql = Get-Service MySQL80
        if ($mysql.Status -eq 'Running') { Write-Ok "MySQL80 已启动（提权）" }
        else { Write-Err "MySQL80 启动失败，请手动以管理员运行: Start-Service MySQL80" }
    }
}

# ---------- 2. Docker Desktop ----------
Write-Host "`n[2/5] Docker Desktop" -ForegroundColor Cyan
$dd = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
if (-not $dd) {
    Write-Warn "Docker Desktop 未运行，启动中..."
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
}
Write-Host "  等待 Docker 引擎就绪（最多 120s）..." -NoNewline
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 2
    Write-Host "." -NoNewline
}
Write-Host ""
if ($ready) { Write-Ok "Docker 引擎就绪" }
else { Write-Err "Docker 引擎 120s 内未就绪，请手动打开 Docker Desktop 等其完全启动后重跑此脚本" }

# ---------- 3. Redis + Qdrant 容器 ----------
Write-Host "`n[3/5] Redis + Qdrant 容器" -ForegroundColor Cyan
if ($ready -and (Test-Path $composeFile)) {
    # 兜底：即便 unless-stopped 没自动恢复，也强制起一次（同时起 redis + qdrant）
    docker compose -f $composeFile up -d 2>&1 | Out-Null
    Start-Sleep -Seconds 2
}
$r = docker ps --filter name="^/redis$" --format "{{.Status}}"
if ($r) { Write-Ok "Redis: $r" } else { Write-Err "Redis 容器未运行" }

# ---------- 4. Qdrant 容器 ----------
Write-Host "`n[4/5] Qdrant 容器" -ForegroundColor Cyan
$q = docker ps --filter name="^/qdrant$" --format "{{.Status}}"
if ($q) { Write-Ok "Qdrant: $q" } else { Write-Err "Qdrant 容器未运行（向量数据可能受影响）" }

# ---------- 5. Ollama ----------
Write-Host "`n[5/5] Ollama" -ForegroundColor Cyan
$ollama = Get-NetTCPConnection -LocalPort 11434 -State Listen -ErrorAction SilentlyContinue
if ($ollama) { Write-Ok "Ollama 监听 localhost:11434" }
else { Write-Err "Ollama 未监听 11434，请手动启动 Ollama" }

# ---------- 总结 ----------
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "基础设施检查完成。接下来启动应用：" -ForegroundColor Cyan
Write-Host "  后端: 在 IDE 里 Run/Debug  (监听 localhost:8080)"
Write-Host "  前端: cd frontend; npm run dev  (Vite 默认 localhost:5173)"
Write-Host "========================================`n" -ForegroundColor Cyan
