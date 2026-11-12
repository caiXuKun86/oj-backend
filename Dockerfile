# ==========================================
# 阶段 1: 构建编译阶段 (Build Stage)
# ==========================================
FROM maven:3.8.8-eclipse-temurin-8-alpine AS builder

WORKDIR /build

# 复制 pom.xml
COPY pom.xml .

# 配置阿里云 Maven 镜像源以加速国内下载
RUN mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
    <mirrors> \
        <mirror> \
            <id>aliyunmaven</id> \
            <mirrorOf>central</mirrorOf> \
            <name>阿里云公共仓库</name> \
            <url>https://maven.aliyun.com/repository/public</url> \
        </mirror> \
    </mirrors> \
</settings>' > /root/.m2/settings.xml

# 复制源码并执行构建打包（增加网络超时重试参数）
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.wagon.http.retryHandler.count=3 -Dhttp.keepAlive=false

# ==========================================
# 阶段 2: 运行时精简镜像 (Production Stage)
# ==========================================
FROM eclipse-temurin:8-jre-alpine

LABEL maintainer="Kun OJ Team"
LABEL description="Kun Online Judge Backend Service"

# 设置容器时区为东八区（Asia/Shanghai）
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone && \
    apk del tzdata

# 创建判题机所需的工作目录与测试用例缓存目录
RUN mkdir -p /app \
             /tmp/oj/upload_testcase \
             /tmp/oj/code \
             /var/lib/oj/testcase \
             /tmp/oj/result

WORKDIR /app

# 从构建阶段复制生成的 jar 包
COPY --from=builder /build/target/oj-backend-*.jar /app/app.jar

# 暴露后端应用端口
EXPOSE 8080

# 默认 JVM 参数与 Spring Profile
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8"
ENV SPRING_PROFILES_ACTIVE="prod"

# 启动 Spring Boot 服务
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=$SPRING_PROFILES_ACTIVE"]
