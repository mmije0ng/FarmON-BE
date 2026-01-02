# OpenJDK 17 이미지
FROM eclipse-temurin:17-jdk-jammy

# JAR 복사
COPY build/libs/farmon-1.0.0.jar farmon-backend-dev.jar

# Timezone 설정
ENV TZ=Asia/Seoul

# Port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=dev", "-jar", "/farmon-backend-dev.jar"]