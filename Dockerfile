FROM eclipse-temurin:17-jdk-jammy
COPY build/libs/farmon-1.0.0.jar /app.jar
ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app.jar"]
