FROM nginx

RUN apt-get update \
    && apt-get install -yq unzip
COPY client/target/*.war /tmp/disk-portal.war
WORKDIR /usr/share/nginx/html
RUN mkdir disk-portal \
    && cd disk-portal \
    && unzip /tmp/disk-portal.war
COPY docker/nginx/config.js /usr/share/nginx/html/disk-portal/customise/config.js