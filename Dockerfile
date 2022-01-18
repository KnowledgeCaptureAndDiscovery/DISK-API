FROM tomcat:8.5.32

LABEL author="Maximiliano Osorio <mosorio@isi.edu>" \
      maintainer="Maximiliano Osorio <mosorio@isi.edu>"

COPY client/target/*.war /usr/local/tomcat/webapps/disk-portal.war
COPY server/target/*.war /usr/local/tomcat/webapps/disk-server.war
