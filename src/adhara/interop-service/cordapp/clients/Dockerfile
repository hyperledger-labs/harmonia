FROM alpine:3.17.3

RUN apk add openjdk8-jre

WORKDIR /clients

COPY /build/libs/ /clients

ENTRYPOINT [ "java", "-jar", "clients-0.1.jar" ]