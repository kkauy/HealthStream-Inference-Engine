# -------- build stage --------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -q clean package -DskipTests

# -------- runtime stage --------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    rm -rf /var/lib/apt/lists/*

COPY --from=build /build/hs-orchestrator-java/target/*.jar /app/app.jar
COPY inference-workers /app/inference-workers

RUN pip3 install --no-cache-dir numpy pandas scikit-learn

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]