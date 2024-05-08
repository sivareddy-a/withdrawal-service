@echo off

REM Build the Spring Boot application
mvn clean package

REM Check if the build was successful
if %errorlevel% equ 0 (
    echo Build successful
    REM Run the Spring Boot application
    java -jar target\your-application.jar
    echo API documentation is available at http://localhost:8080/swagger-ui/index.html
) else (
    echo Build failed
)
