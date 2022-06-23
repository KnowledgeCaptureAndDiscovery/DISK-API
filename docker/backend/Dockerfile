FROM tomcat:8
LABEL author="Maximiliano Osorio <mosorio@isi.edu>" \
      maintainer="Maximiliano Osorio <mosorio@isi.edu>"
COPY build/disk-project*.war /usr/local/tomcat/webapps/disk-server.war
COPY context.xml /usr/local/tomcat/conf/
