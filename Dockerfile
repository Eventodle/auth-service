FROM clojure

WORKDIR /build
COPY ./project.clj /build
RUN lein with-profile uberjar deps

COPY . /build
RUN lein uberjar

FROM java:8-alpine

COPY --from=0 /build/target/uberjar/auth-service.jar /auth-service/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/auth-service/app.jar"]
