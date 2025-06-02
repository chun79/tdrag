# 使用OpenJDK基础镜像
FROM openjdk:21-jdk-slim

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 创建应用目录
WORKDIR /app

# 创建非root用户
RUN groupadd -r raguser && useradd -r -g raguser raguser

# 复制本地构建的JAR文件
COPY target/rag-service.jar app.jar

# 创建必要的目录
RUN mkdir -p logs uploads && chown -R raguser:raguser /app

# 切换到非root用户
USER raguser

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# 启动应用 - 使用环境变量中的JAVA_OPTS
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"] 