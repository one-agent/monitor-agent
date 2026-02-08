@echo off
setlocal enabledelayedexpansion

echo 开始构建镜像...

REM 构建指定的镜像
docker-compose build frontend backend

if %errorlevel% neq 0 (
    echo 构建失败
    exit /b %errorlevel%
)

echo 构建完成，开始推送镜像...

REM 推送前端镜像
echo 推送前端镜像...
docker push registry.cn-hangzhou.aliyuncs.com/harryzhang/monitor-agent-frontend:latest

if %errorlevel% neq 0 (
    echo 推送前端镜像失败
    exit /b %errorlevel%
)

REM 推送后端镜像
echo 推送后端镜像...
docker push registry.cn-hangzhou.aliyuncs.com/harryzhang/monitor-agent-backend:latest

if %errorlevel% neq 0 (
    echo 推送后端镜像失败
    exit /b %errorlevel%
)

echo 所有镜像推送完成!