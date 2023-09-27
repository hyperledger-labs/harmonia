FROM node:18-alpine3.15 as builder

WORKDIR /crosschainInterop

RUN apk add --update git python3 krb5 krb5-libs gcc make g++ krb5-dev

COPY ./package.json .
COPY ./package-lock.json .

RUN npm i

WORKDIR /crosschainInterop/src/Infrastructure/HqlaxClient

COPY /src/Infrastructure/HqlaxClient/package.json .
COPY /src/Infrastructure/HqlaxClient/package-lock.json .

RUN npm i

WORKDIR /crosschainInterop

COPY . .

RUN npx truffle compile

FROM node:18-alpine3.15

RUN apk update && apk upgrade

WORKDIR /crosschainInterop/api

ENV PATH=${PATH}:/crosschainInterop/ethgwctl/

RUN apk --no-cache add curl

COPY --from=builder /crosschainInterop /crosschainInterop
COPY --from=builder /crosschainInterop/src/Infrastructure/HqlaxClient /crosschainInterop/src/Infrastructure/HqlaxClient

ENTRYPOINT ["node" , "app.js"]

EXPOSE 3030
EXPOSE 3031
