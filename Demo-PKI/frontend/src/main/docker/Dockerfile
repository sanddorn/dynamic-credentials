FROM eclipse-temurin:17-jdk-focal as builder

WORKDIR application
COPY maven/*.jar application.jar
RUN ls
RUN java -Djarmode=layertools -jar application.jar extract

FROM gcr.io/distroless/java17
WORKDIR application
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
