# 1) Берём готовый JDK 17 (multi-arch, без alpine)
FROM eclipse-temurin:17-jdk

# 2) Устанавливаем рабочую директорию
WORKDIR /app

# 3) Копируем собранный fat JAR (shadowJar) в контейнер
COPY build/libs/bot-gateway-all.jar app.jar

# 4) Точка входа
ENTRYPOINT ["java", "-jar", "app.jar"]