FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S minipay && adduser -S minipay -G minipay
COPY --from=build /app/target/*.jar app.jar
USER minipay

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
