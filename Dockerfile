# 第一阶段：使用 JDK 镜像编译 Spring Boot 项目
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 先复制 Maven Wrapper 和 pom.xml，方便 Docker 利用依赖缓存
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

# 跳过测试打包，测试建议在 CI 或本地单独执行
RUN chmod +x ./mvnw && ./mvnw -q -DskipTests package

# 第二阶段：使用更小的 JRE 镜像运行 jar
FROM eclipse-temurin:21-jre
WORKDIR /app

# 容器默认配置，可在 docker-compose.yml 中继续覆盖
ENV SERVER_PORT=8123 \
    SPRING_PROFILES_ACTIVE=docker \
    KNOWLEDGE_BASE_PATH=/app/data/documents

# 知识库上传目录，compose 会挂载为持久化卷
RUN mkdir -p /app/data/documents

# 只复制最终 jar，减少运行镜像体积
COPY --from=build /workspace/target/enterprise-ai-agent-*.jar /app/app.jar

EXPOSE 8123

# 启动 Spring Boot 应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
