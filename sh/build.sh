#!/usr/bin/env bash

./mvnw clean package;
DOCKER_APP_NAME=appmobiles/proxy-service
HEROKU_APP_NAME=appmobiles-proxy-service
docker build -f src/main/docker/Dockerfile.jvm  -t ${DOCKER_APP_NAME} .;
heroku login;
heroku create ${HEROKU_APP_NAME};
heroku container:login;
docker tag ${DOCKER_APP_NAME} registry.heroku.com/${HEROKU_APP_NAME}/web;
docker push registry.heroku.com/${HEROKU_APP_NAME}/web;
heroku container:release web -a ${HEROKU_APP_NAME};
docker rmi ${DOCKER_APP_NAME} registry.heroku.com/${HEROKU_APP_NAME}/web;