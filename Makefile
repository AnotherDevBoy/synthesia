# Grab the version out of the pom file
VERSION := $(shell sed -n 's/<version>\(.*\)<\/version>/\1/p' pom.xml | head -1 | tr -d '[:space:]')

IMAGE := synthesia-app:$(VERSION)

style:
	mvn spotless:apply

infra:
	docker-compose -f docker-compose-infra.yaml up

build:
	mvn clean install

docker: build
	docker build -t $(IMAGE) -f Dockerfile .

run:
	APP_VERSION=$(VERSION) API_KEY=$(API_KEY) docker-compose -f docker-compose.yaml up

stop:
	APP_VERSION=$(VERSION) docker-compose -f docker-compose.yaml down
	docker-compose -f docker-compose-infra.yaml down