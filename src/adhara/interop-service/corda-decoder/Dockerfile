FROM alpine:3.16.2 as builder

WORKDIR /cordaDecoder

RUN apk add openjdk11 maven

COPY . /cordaDecoder

RUN mvn package

FROM alpine:3.16.2

WORKDIR /cordaDecoder

RUN apk update && apk upgrade

RUN apk add openjdk11

COPY --from=builder /cordaDecoder/target .

ENTRYPOINT ["java" , "-jar" , "corda-decode-0.0.1-SNAPSHOT.jar"]

EXPOSE 8080