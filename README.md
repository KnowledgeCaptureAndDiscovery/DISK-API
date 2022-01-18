# Disk


Installation
=============
Requirements
------------
1. Java JDK 1.7+ (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
2. Maven 2/3 (http://maven.apache.org/)
3. Tomcat 7+ (http://tomcat.apache.org/)

Installation
-------------
1. $ mvn clean && mvn install
	- This will create a disk-server-[version].war file in server/target
	- It will also create a disk-client-[version].war file in client/target

2. Move the war files to a Servlet container (Tomcat)
	- $ mv /path/to/disk-server-<version>.war /path/to/tomcat/webapps/disk-server.war
	- $ mv /path/to/disk-client-<version>.war /path/to/tomcat/webapps/disk-client.war

3. Start tomcat
	- $ /path/to/tomcat/bin/startup.sh

4. Open http://[your-server-name]:8080/disk-server/vocabulary to check that the local repository server is working fine. It might take a little while to open it for the first time as it downloads vocabularies from the internet.

5. Check $HOME/.disk/server.properties file to see that server name is correctly identified and edit the file as necessary

6. Open http://[your-server-name]:8080/disk-client/index.html to access the Disk UI that connects with the local repository

7. Customize the client by changing /path/to/tomcat/webapps/disk-client/customize/config.js

8. The default userid is "admin" with password "changeme!". Remember to change it :)


## Change the version

```bash
version="1.1.1"
mvn --settings pom.xml org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=${version}
```
