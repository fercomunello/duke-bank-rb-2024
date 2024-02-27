# Rinha de Backend - 2024/Q1
```
$$$$$$$\            $$\                       $$$$$$$\                      $$\       
$$  __$$\           $$ |                      $$  __$$\                     $$ |      
$$ |  $$ |$$\   $$\ $$ |  $$\  $$$$$$\        $$ |  $$ | $$$$$$\  $$$$$$$\  $$ |  $$\
$$ |  $$ |$$ |  $$ |$$ | $$  |$$  __$$\       $$$$$$$\ | \____$$\ $$  __$$\ $$ | $$  |
$$ |  $$ |$$ |  $$ |$$$$$$  / $$$$$$$$ |      $$  __$$\  $$$$$$$ |$$ |  $$ |$$$$$$  /
$$ |  $$ |$$ |  $$ |$$  _$$<  $$   ____|      $$ |  $$ |$$  __$$ |$$ |  $$ |$$  _$$<  
$$$$$$$  |\$$$$$$  |$$ | \$$\ \$$$$$$$\       $$$$$$$  |\$$$$$$$ |$$ |  $$ |$$ | \$$\
\_______/  \______/ \__|  \__| \_______|      \_______/  \_______|\__|  \__|\__|  \__|
```
## Quarkus
This project uses [Quarkus](https://quarkus.io), the Supersonic Subatomic Java Framework.

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Starting PostgreSQL

**Recommended:** To create the bank schema, run this interactive bash script to be able to reset and 
restart the container at runtime.
```shell script
# Give executable permission and run the shell script
chmod +x postgres-local.sh && ./postgres-local.sh
# Start Quarkus dev-mode for live coding 
./mvnw clean compile quarkus:dev
```

**Alternative**: You could also run docker-compose directly.
```shell script
# Start the container
docker-compose -f postgres-local.yaml up --detach
# Set the app to regenerate the database schema on every reload
./mvnw clean compile quarkus:dev -Dquarkus.args=regenerate-bank-schema
```

Or you could let Dev Services starts the database container automatically via Testcontainers.
```shell script
mvn clean compile quarkus:dev -Dquarkus.args=regenerate-bank-schema -Dquarkus.datasource.devservices.enabled=true
```

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/duke-backend-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- Eclipse Vert.x ([guide](https://quarkus.io/guides/vertx)): Write reactive applications with the Vert.x API
- Reactive PostgreSQL client ([guide](https://quarkus.io/guides/reactive-sql-clients)): Connect to the PostgreSQL database using the reactive pattern
- Reactive Routes ([guide](https://quarkus.io/guides/reactive-routes)): REST framework offering the route model to define non blocking endpoints
