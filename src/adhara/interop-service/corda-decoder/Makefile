PROJECT ?= corda-decoder
TAG ?= local
DOCKER_REPO ?= adhara-docker.jfrog.io/adharaprojects

build:
	 docker build -t ${DOCKER_REPO}/${PROJECT}:${TAG} \
	  --build-arg ARTIFACTORY_USER=${ARTIFACTORY_USER} \
	  --build-arg ARTIFACTORY_PASSWORD=${ARTIFACTORY_PASSWORD} \
	  -f ./corda-experiments/corda-decoder/Dockerfile \
	  ./corda-experiments/corda-decoder

publish: build
	docker push ${DOCKER_REPO}/${PROJECT}:${TAG}

scan:
	@echo "Scanning docker image"
	trivy image --exit-code 0 --severity "UNKNOWN,LOW" --no-progress ${DOCKER_REPO}/${PROJECT}:${TAG}
	trivy image --exit-code 0 --severity "MEDIUM,HIGH,CRITICAL" --no-progress ${DOCKER_REPO}/${PROJECT}:${TAG}