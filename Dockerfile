FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul

ENTRYPOINT ["java", "-jar", "app.jar"]