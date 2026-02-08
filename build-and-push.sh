#!/bin/bash

# 构建并推送镜像到阿里云容器镜像服务的脚本

set -e  # 遇到错误时退出

echo "开始构建镜像..."

# 构建指定的镜像
docker-compose build frontend backend

echo "构建完成，开始推送镜像..."

# 登录阿里云容器镜像服务 (请先运行: docker login --username=your-username registry.cn-hangzhou.aliyuncs.com)
echo "推送前端镜像..."
docker push registry.cn-hangzhou.aliyuncs.com/harryzhang/monitor-agent-frontend:latest

echo "推送后端镜像..."
docker push registry.cn-hangzhou.aliyuncs.com/harryzhang/monitor-agent-backend:latest

echo "所有镜像推送完成!"