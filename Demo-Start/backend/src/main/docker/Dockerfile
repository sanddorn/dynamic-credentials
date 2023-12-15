FROM eclipse-temurin:17-jdk-focal as builder

WORKDIR application
COPY maven/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM gcr.io/distroless/java17
WORKDIR application
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
COPY --from=builder application/dependencies/ application/spring-boot-loader/ application/snapshot-dependencies/ application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
