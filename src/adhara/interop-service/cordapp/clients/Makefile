PROJECT ?= corda-api
TAG ?= local
DOCKER_REPO ?= adhara-docker.jfrog.io/adharaprojects

build:
	 docker build -t ${DOCKER_REPO}/${PROJECT}:${TAG} \
	  --build-arg ARTIFACTORY_USER=${ARTIFACTORY_USER} \
	  --build-arg ARTIFACTORY_PASSWORD=${ARTIFACTORY_PASSWORD} \
	  -f ./corda-experiments/cordapp/clients/Dockerfile \
	  ./corda-experiments/cordapp/clients

publish: build
	docker push ${DOCKER_REPO}/${PROJECT}:${TAG}

	
