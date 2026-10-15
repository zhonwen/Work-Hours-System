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

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 创建日志目录
RUN mkdir -p /tmp/logs && chmod 777 /tmp/logs

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
