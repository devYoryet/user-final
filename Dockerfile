FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copiar archivos de configuración Maven
COPY pom.xml .

# Copiar código fuente
COPY src ./src

# Instalar Maven y construir aplicación
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Exponer puerto
EXPOSE 8081

# Variables de entorno para Oracle
ENV ORACLE_NET_WALLET_LOCATION=/app/wallet

# Crear directorio para wallet
RUN mkdir -p /app/wallet

# Comando para ejecutar
CMD ["java", "-jar", "target/user-service-0.0.1-SNAPSHOT.jar"]