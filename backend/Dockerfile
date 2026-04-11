# ── 构建阶段 ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 先只复制 pom.xml，利用 Docker 层缓存下载依赖
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q 2>/dev/null || true

# 复制源码并打包（跳过测试，测试在 CI 阶段单独运行）
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -q

# ── 运行阶段 ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# 创建非 root 用户运行
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# JVM 参数：容器友好的内存设置
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
