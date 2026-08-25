FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /app
RUN microdnf install -y findutils && microdnf clean all
COPY . .
RUN chmod +x gradlew
RUN ./gradlew nativeCompile --no-daemon -x test -x integrationTest

FROM ubuntu:22.04
WORKDIR /app
COPY --from=build /app/build/native/nativeCompile/app /app/app
COPY src/main/resources/db/migration /app/db/migration
EXPOSE 8080
ENTRYPOINT ["/app/app"]
