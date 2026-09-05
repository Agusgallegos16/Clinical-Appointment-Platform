# Etapa de compilación
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia únicamente el archivo de configuración y la carpeta de código Java
COPY pom.xml .
COPY src ./src

# Compila exclusivamente el backend
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]