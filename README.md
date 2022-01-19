
# DISK

The DISK system automates the execution of scientific workflows triggered 
on data changes. To do this DISK collects data from different data repositories
and defines methods on different workflows systems. User defined goals are 
periodically check for new data/methods available. When a method detects new data,
a new workflow execution will be send. Each experiment execution is stored with its
metadata and outputs for posterior analysis.

## Installation

We recommend to use `docker` to install DISK.

### Docker

Install DISK with docker

```bash
docker-compose up -d
```

### Build project

To build from source, you need the following installed and available in your $PATH:


- Java JDK 1.7+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
- Maven 2/3 (http://maven.apache.org/)
- Tomcat 7+ (http://tomcat.apache.org/)

1. Install using maven
```
mvn clean && mvn install
```
- This will create a disk-server-[version].war file in server/target
- It will also create a disk-client-[version].war file in client/target

2. Move the war files to a Servlet container (Tomcat)

```bash
mv /path/to/disk-server-<version>.war /path/to/tomcat/webapps/disk-server.war
mv /path/to/disk-client-<version>.war /path/to/tomcat/webapps/disk-client.war
```

3. Start tomcat

```bash
/path/to/tomcat/bin/startup.sh
```

4. Open http://[your-server-name]:8080/disk-server/vocabulary to check that the local repository server is working fine. It might take a little while to open it for the first time as it downloads vocabularies from the internet.

5. Check $HOME/.disk/server.properties file to see that server name is correctly identified and edit the file as necessary

6. Open http://[your-server-name]:8080/disk-client/index.html to access the Disk UI that connects with the local repository

7. Customize the client by changing /path/to/tomcat/webapps/disk-client/customize/config.js

8. The default userid is "admin" with password "changeme!". Remember to change it :)

## Configuration

To run this project, you will need to add the following environment variables to your .env file

`API_KEY`

`ANOTHER_API_KEY`

