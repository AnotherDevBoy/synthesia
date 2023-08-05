style:
	mvn spotless:apply

infra:
	docker-compose -f docker-compose-infra.yaml up