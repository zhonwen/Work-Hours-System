# 后端 Dockerfile
FROM maven:3.8.6-openjdk-8 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY backend/pom.xml .
RUN mvn dependency:go-offline

# 复制源代码并构建
COPY backend/src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:8-jre-alpine

WORKDIR /app

# curl: 健康检查；fontconfig + 字体: Alpine 下 AWT 生成验证码图需要
RUN apk add --no-cache curl fontconfig ttf-dejavu \
    && fc-cache -f

# 创建日志目录
RUN mkdir -p /tmp/logs && chmod 777 /tmp/logs

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 无头模式，避免容器内 AWT 依赖显示器
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
