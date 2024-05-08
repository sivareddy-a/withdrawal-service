#!/bin/bash

# Build the Spring Boot application
mvn clean package

# Check if the build was successful
if [ $? -eq 0 ]; then
    echo "Build successful"
    # Run the Spring Boot application
    java -jar target/withdraw-service-0.0.1-SNAPSHOT.jar
    echo "API documentation is available at http://localhost:8080/swagger-ui/index.html"
else
    echo "Build failed"
fi