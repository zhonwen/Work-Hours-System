# 前端 Dockerfile
# 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app

ARG VITE_APP_API_BASE_URL=/api
ARG VITE_APP_API_TIMEOUT=15000
ENV VITE_APP_API_BASE_URL=$VITE_APP_API_BASE_URL \
    VITE_APP_API_TIMEOUT=$VITE_APP_API_TIMEOUT

# 复制 package.json 并安装依赖
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install

# 复制源代码并构建（镜像内跳过 vue-tsc，避免与 TypeScript 版本不兼容）
COPY frontend/ ./
RUN npx vite build

# 运行阶段 - 使用 Nginx
FROM nginx:alpine

# 复制构建产物到 Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY deployment/docker/nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
