FROM tomcat:8
LABEL author="Maximiliano Osorio <mosorio@isi.edu>" \
      maintainer="Maximiliano Osorio <mosorio@isi.edu>"
COPY build/disk-project*.war /usr/local/tomcat/webapps/disk-server.war
COPY tomcat-users.xml /usr/local/tomcat/conf/
COPY context.xml /usr/local/tomcat/webapps/manager/META-INF/
