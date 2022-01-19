# syntax=docker/dockerfile:1
FROM maven:3.8.4-jdk-8 AS build
WORKDIR /home/app/
COPY . /home/app/
RUN export MAVEN_OPTS='-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn' \ 
    && mvn -f /home/app/pom.xml package

FROM nginx

RUN apt-get update \
    && apt-get install -yq unzip
COPY client/target/*.war /tmp/disk-portal.war
WORKDIR /usr/share/nginx/html
RUN mkdir disk-portal \
    && cd disk-portal \
    && unzip /tmp/disk-portal.war
COPY docker/frontend/default.conf /etc/nginx/conf.d/default.conf