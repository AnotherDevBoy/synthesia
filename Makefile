# Grab the version out of the pom file
VERSION := $(shell sed -n 's/<version>\(.*\)<\/version>/\1/p' pom.xml | head -1 | tr -d '[:space:]')

# Grab the image from the local machine's registry
# (... and don't be fooled by the registry rcluster.io part - it's just namespacing within the bigger string)
IMAGE := synthesia-app:$(VERSION)

style:
	mvn spotless:apply

infra:
	docker-compose -f docker-compose-infra.yaml up

build:
	mvn clean install

docker:
	docker build --build-arg HERMES_APP_VERSION=$(VERSION) -t $(IMAGE) -f Dockerfile .

run:
	APP_VERSION=$(VERSION) API_KEY=$(API_KEY) docker-compose -f docker-compose.yaml up