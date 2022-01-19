# syntax=docker/dockerfile:1
FROM maven:3.8.4-jdk-8 AS build
COPY . /home/app/
RUN mvn -f /home/app/pom.xml clean package


FROM tomcat:8.5.32

LABEL author="Maximiliano Osorio <mosorio@isi.edu>" \
      maintainer="Maximiliano Osorio <mosorio@isi.edu>"

COPY server/target/*.war /usr/local/tomcat/webapps/disk-server.war