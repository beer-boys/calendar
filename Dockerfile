FROM eclipse-temurin:21-jdk-noble AS builder
WORKDIR /app

COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradlew detekt.yml ./
COPY buildSrc/ buildSrc/

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src ./src

# todo also exclude detekt
RUN ./gradlew clean build -x test --no-daemon

RUN java -Djarmode=layertools -jar build/libs/*.jar extract

FROM eclipse-temurin:21-jdk-noble
WORKDIR /app

RUN apt-get update && apt-get install -y \
    wget \
    locales \
    && rm -rf /var/lib/apt/lists/*

RUN locale-gen ru_RU.UTF-8

ENV TZ=Europe/Moscow
ENV LANG=ru_RU.UTF-8
ENV LANGUAGE=ru_RU:ru
ENV LC_ALL=ru_RU.UTF-8

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/readyz | grep UP || exit 1

EXPOSE 8080
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
