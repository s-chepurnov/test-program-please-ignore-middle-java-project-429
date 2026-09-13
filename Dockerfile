# 1) Собранный фронтенд Хекслета — забираем из npm-пакета
FROM node:22-alpine AS frontend
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# 2) Сборка приложения. Сначала описание сборки и прогрев зависимостей, потом исходники: правка
# кода не заставляет качать дерево зависимостей заново.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
ENV GRADLE_USER_HOME=/opt/gradle-home
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN ./gradlew --no-daemon --console=plain -q dependencies > /dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew --no-daemon --console=plain -q shadowJar

# 3) Рантайм: только JRE, без JDK и без Gradle
FROM eclipse-temurin:25-jre

# make нужен, потому что образ запускается через `make start` — той же командой, что локально.
RUN apt-get update \
    && apt-get install -y --no-install-recommends make \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY Makefile ./
COPY --from=build /build/build/libs/app.jar ./build/libs/app.jar
COPY --from=frontend /build/node_modules/@hexlet/java-flight-booking-frontend/dist/. ./public/

CMD ["make", "start"]
