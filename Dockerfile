FROM gradle:8.6.0-jdk21-alpine AS builder

WORKDIR /workspace
COPY build.gradle settings.gradle ./
RUN gradle build -x test --parallel --continue > /dev/null 2>&1 || true

COPY ./src /workspace/src
RUN gradle build -x test --parallel

FROM eclipse-temurin:21-alpine

COPY --from=builder /workspace/build/libs/NetflixChecker-0.0.1-SNAPSHOT.jar ./app.jar

ENV TZ Asia/Seoul

VOLUME ["/workspace/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]