# 多阶段构建：每个微服务及其依赖打包成独立容器镜像（任务书：容器化）
FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# 依赖先拉取，利用 Docker 层缓存
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# 运行阶段：仅 JRE + 产物
FROM docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN adduser -D -g "" appuser
COPY --from=builder /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
