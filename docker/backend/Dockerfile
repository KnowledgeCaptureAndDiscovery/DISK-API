FROM tomcat:8.5.32
LABEL author="Maximiliano Osorio <mosorio@isi.edu>" \
      maintainer="Maximiliano Osorio <mosorio@isi.edu>"
COPY build/disk-project*.war /usr/local/tomcat/webapps/disk-server.war
RUN mkdir /root/.disk \
      && mdkir /root/.disk/storage \
      && /root/.disk/storage/TDB \
      && /root/disk/.disk/storage/DB
