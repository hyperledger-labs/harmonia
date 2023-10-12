FROM corda/community:4.9.6-zulu-openjdk8

ARG CERTIFICATES
ARG NETWORK_PARAMETERS
ARG P2P_PORT
ARG RPC_PORT
ARG RPC_ADMIN_PORT

COPY --chown=corda:corda entrypoint.sh /opt/corda
COPY ${CERTIFICATES} /opt/corda/certificates
COPY /build/nodes/Notary/cordapps /opt/corda/cordapps
COPY ${NETWORK_PARAMETERS} /opt/corda/network-parameters

RUN chmod +x /opt/corda/entrypoint.sh

ENTRYPOINT [ "./entrypoint.sh" ]

EXPOSE ${P2P_PORT}
EXPOSE ${RPC_PORT}
EXPOSE ${RPC_ADMIN_PORT}
EXPOSE 22022
