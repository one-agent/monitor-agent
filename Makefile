# 简化构建和部署的 Makefile

.PHONY: build push build-and-push login

# 构建镜像
build:
	docker-compose build

# 推送镜像到阿里云
push:
	docker push registry.cn-hangzhou.aliyuncs.com/harryzhang/monitor-agent-frontend:latest
	docker push registry.cn-hangzhou.aliyuncs.com/harryzhang/monitor-agent-backend:latest

# 构建并推送
build-and-push: build push

# 登录阿里云
login:
	@echo "请运行: docker login --username=<your-username> registry.cn-hangzhou.aliyuncs.com"