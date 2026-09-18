FROM eclipse-temurin:21.0.12_8-jdk-alpine AS builder
WORKDIR /usr/app/freshhire
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests


FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24 AS extractor
WORKDIR /usr/app/freshhire
COPY --from=builder /usr/app/freshhire/target/*.jar freshhire.jar
RUN java -Djarmode=tools -jar freshhire.jar extract \
    --layers \
    --destination extracted


FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24 AS runner
WORKDIR /usr/app/freshhire
RUN addgroup -S freshhire && \
    adduser -S freshhire -G freshhire
COPY --from=extractor --chown=freshhire:freshhire /usr/app/freshhire/extracted/dependencies/ ./
COPY --from=extractor --chown=freshhire:freshhire /usr/app/freshhire/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=freshhire:freshhire /usr/app/freshhire/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=freshhire:freshhire /usr/app/freshhire/extracted/application/ ./
USER freshhire
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "freshhire.jar"]
