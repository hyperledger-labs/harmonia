FROM node:18-alpine3.15 as builder

WORKDIR /crosschainInterop

RUN apk add --update git python3 krb5 krb5-libs gcc make g++ krb5-dev

COPY ./package.json .
COPY ./package-lock.json .

RUN npm i

WORKDIR /crosschainInterop

COPY . .

RUN npx hardhat compile

FROM node:18-alpine3.15

RUN apk update && apk upgrade

WORKDIR /crosschainInterop/api

ENV PATH=${PATH}:/crosschainInterop/ethgwctl/

RUN apk --no-cache add curl

COPY --from=builder /crosschainInterop /crosschainInterop

ENTRYPOINT ["node" , "app/index.js"]

EXPOSE 3030
EXPOSE 3031
