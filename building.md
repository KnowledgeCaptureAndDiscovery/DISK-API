
# Build project

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

4. Customize the client by changing /path/to/tomcat/webapps/disk-client/customize/config.js

5. Check $HOME/.disk/server.properties file to see that server name is correctly identified and edit the file as necessary
