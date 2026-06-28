# ============================================================
# DataTeaCup 微服务 Dockerfile（多阶段构建）
# 通过 --build-arg SERVICE=dp-system-service 指定要构建的服务
# ============================================================

# ==================== Stage 1: Build Backend ====================
FROM maven:3.9-eclipse-temurin-17 AS backend-build

ARG SERVICE=dp-system-service

WORKDIR /app
# Copy all pom files first for dependency caching
COPY pom.xml ./
COPY dp-common/pom.xml dp-common/
COPY dp-core/pom.xml dp-core/
COPY dp-service-starter/pom.xml dp-service-starter/
COPY dp-gateway/pom.xml dp-gateway/
COPY dp-system-service/pom.xml dp-system-service/
COPY dp-data-service/pom.xml dp-data-service/
COPY dp-analytics-service/pom.xml dp-analytics-service/
COPY dp-collaboration-service/pom.xml dp-collaboration-service/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -q || true

# Copy all source and build the target service
COPY dp-common/ dp-common/
COPY dp-core/ dp-core/
COPY dp-service-starter/ dp-service-starter/
COPY dp-gateway/ dp-gateway/
COPY dp-system-service/ dp-system-service/
COPY dp-data-service/ dp-data-service/
COPY dp-analytics-service/ dp-analytics-service/
COPY dp-collaboration-service/ dp-collaboration-service/
RUN mvn clean package -pl ${SERVICE} -am -Dmaven.test.skip=true -B -q \
    && if ls ${SERVICE}/target/*-exec.jar >/dev/null 2>&1; then \
         cp ${SERVICE}/target/*-exec.jar /app/service.jar; \
       else \
         cp ${SERVICE}/target/${SERVICE}-*.jar /app/service.jar; \
       fi

# ==================== Stage 2: Production Image ====================
FROM eclipse-temurin:17-jre-alpine AS production

ARG SERVICE=dp-system-service

LABEL maintainer="DataTeaCup Team"
LABEL version="2.1.0"
LABEL description="DataTeaCup Microservice - ${SERVICE}"

RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# Create app user
RUN addgroup -S datateacup && adduser -S datateacup -G datateacup

# Setup directories
RUN mkdir -p /app/logs /app/exports /app/temp /app/data /app/license \
    && chown -R datateacup:datateacup /app

# Copy service JAR
COPY --from=backend-build /app/service.jar /app/service.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

EXPOSE ${SERVER_PORT:-8080}

USER datateacup
WORKDIR /app

ENTRYPOINT ["sh", "-c", "java -jar /app/service.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}"]
