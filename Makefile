# these will speed up builds, for docker-compose >= 1.25
export COMPOSE_DOCKER_CLI_BUILD=1
export DOCKER_BUILDKIT=1

all: down build up test

build:
	docker-compose build

deps:
	docker-compose up -d  rabbitmq mongo

up:
	docker-compose up -d

down:
	docker-compose down --remove-orphans

test: build up
	docker-compose run --rm --no-deps --entrypoint=lein api midje

unit-tests:
	lein  midje architecture-patterns-with-clojure.unit.*

e2e-tests: build up
	docker-compose run --rm --no-deps --entrypoint=lein api midje architecture-patterns-with-clojure.e2e.*

auto-tests: deps
	lein  midje :autotest


logs:
	docker-compose logs --tail=25 api redis_pubsub
#
# black:
# 	black -l 86 $$(find * -name '*.clj')
