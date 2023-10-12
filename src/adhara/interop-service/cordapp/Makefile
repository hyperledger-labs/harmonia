PROJECT ?= corda-node
TAG ?= local
DOCKER_REPO ?= adhara-docker.jfrog.io/adharaprojects
CERTIFICATES_PATH ?= /build/nodes/Notary/certificates
NETWORK_PARAMETERS ?= /build/nodes/Notary/network-parameters
P2P_PORT ?= 10002
RPC_PORT ?= 10003
RPC_ADMIN_PORT ?= 10043

build:
	 docker build -t ${DOCKER_REPO}/${PROJECT}:${TAG} \
	  --build-arg ARTIFACTORY_USER=${ARTIFACTORY_USER} \
	  --build-arg ARTIFACTORY_PASSWORD=${ARTIFACTORY_PASSWORD} \
	  --build-arg CERTIFICATES=${CERTIFICATES_PATH} \
	  --build-arg NETWORK_PARAMETERS=${NETWORK_PARAMETERS} \
	  --build-arg P2P_PORT=${P2P_PORT} \
	  --build-arg RPC_PORT=${RPC_PORT} \
	  --build-arg RPC_ADMIN_PORT=${RPC_ADMIN_PORT} \
	  -f ./corda-experiments/cordapp/Dockerfile \
	  ./corda-experiments/cordapp

publish: build
	docker push ${DOCKER_REPO}/${PROJECT}:${TAG}