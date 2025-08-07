# ---------- builder ----------
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Копируем весь проект целиком
COPY . .

# Запускаем сборку конкретного модуля из корня проекта
RUN ./gradlew :bot-gateway:shadowJar --no-daemon

# ---------- runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Копируем JAR из правильной папки сборки
COPY --from=builder /app/bot-gateway/build/libs/*.jar app.jar

HEALTHCHECK --interval=15s CMD curl -f http://localhost:8080/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]