step1:
we start docker database.
 step1.1:
 connect to database and create database (only if running it for the very first time)
 step1.2:
 set environement variable.(globally)

step2:
backend and its services. 
 step2.1:
 run backend services one by one in different terminals. for eg-
 
 ```
 # Run collab-editor (port 8080)
mvn spring-boot:run -pl collab-editor

# In separate terminal - cloud-storage (port 8081)
mvn spring-boot:run -pl cloud-storage

```
 step2.2:
 build JAR and run those JAR. for eg-

 ```
 # Build all modules
mvn clean package -DskipTests

# Run collab-editor
java -jar collab-editor/target/collab-editor-0.0.1-SNAPSHOT.jar

# In separate terminal - cloud-storage
java -jar cloud-storage/target/cloud-storage-0.0.1-SNAPSHOT.jar

 ```
 step2.3:
 run storage node. (for cloud storage) for eg-

 ```
cd d:\newblah\storage-node

# Node 1
$env:NODE_ID="node-1"; $env:NODE_PORT="9001"; $env:DATA_DIR="./data/node1"; mvn spring-boot:run

# Node 2 (separate terminal)
$env:NODE_ID="node-2"; $env:NODE_PORT="9002"; $env:DATA_DIR="./data/node2"; mvn spring-boot:run

# Node 3 (separate terminal)
$env:NODE_ID="node-3"; $env:NODE_PORT="9003"; $env:DATA_DIR="./data/node3"; mvn spring-boot:run

 ```

step3:
start frontend.