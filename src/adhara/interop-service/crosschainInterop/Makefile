PROJECT ?= crosscha-interop
TAG ?= local
DOCKER_REPO ?= adhara-docker.jfrog.io/adharaprojects

build:
	 docker build -t ${DOCKER_REPO}/${PROJECT}:${TAG} \
	  --build-arg ARTIFACTORY_USER=${ARTIFACTORY_USER} \
	  --build-arg ARTIFACTORY_PASSWORD=${ARTIFACTORY_PASSWORD} \
	  -f ./crosschainInterop/Dockerfile \
	  ./crosschainInterop

publish: build
	docker push ${DOCKER_REPO}/${PROJECT}:${TAG}

scan:
	@echo "Scanning docker image"
	trivy image --exit-code 0 --severity "UNKNOWN,LOW" --no-progress ${DOCKER_REPO}/${PROJECT}:${TAG}
	trivy image --exit-code 0 --severity "MEDIUM,HIGH,CRITICAL" --no-progress ${DOCKER_REPO}/${PROJECT}:${TAG}


.PHONY: setup
setup:
	@npm ci

.PHONY: clean
clean:
	@rm -rf build

.PHONY: compile
compile:
	@npx hardhat compile

.PHONY: test
test:
	@npx hardhat specific-test --sources contracts/libraries --tests test/ --show-stack-traces
