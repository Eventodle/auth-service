FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/auth-service.jar /auth-service/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/auth-service/app.jar"]
