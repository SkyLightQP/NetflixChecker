# syntax=docker/dockerfile:1

FROM gradle:8.6.0-jdk21-alpine AS builder

ENV GRADLE_USER_HOME=/gradle-cache
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY src ./src

RUN --mount=type=cache,target=/gradle-cache \
    gradle bootJar --no-daemon

RUN java -Djarmode=tools -jar build/libs/NetflixChecker-0.0.1-SNAPSHOT.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine

ENV TZ=Asia/Seoul
WORKDIR /workspace

COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./

EXPOSE 8080
VOLUME ["/workspace/data"]

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseCompactObjectHeaders", "org.springframework.boot.loader.launch.JarLauncher"]
