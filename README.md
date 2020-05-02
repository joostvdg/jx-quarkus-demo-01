# Jenkins X Quarkus Showcase

## Pre-requisites

* Kubernetes Cluster
* Jenkins X installed with jx boot + (HashiCorp) Vault

## Proposed Stack

* Jenkins X
* Kubernetes
* Google Cloud SQL/MySQL
* HashiCorp Vault
* Quarkus
* GraalVM
* Spring (Data JPA/DI/Web)
    * Quarkus flavors

## What Will We Do

* Create Google Cloud SQL (MySql flavor) as datasource
* Create Quarkus application with:
** Spring Data JPA
** Spring Web
* Import the application into Jenkins X
* Build the application as a Native Image
* Retrieve application secrets (such as Database username/password) from HashiCorp Vault
* With Java 11!

## Google Cloud SQL Server

Coming Soon...

## Quarkus Application

* start from the Quarkus Quickstarts for Spring Data JPA
    * https://github.com/quarkusio/quarkus-quickstarts/tree/master/spring-data-jpa-quickstart
    * https://quarkus.io/guides/spring-data-jpa

### Add Additional Dependencies

First, remove the `quarkus-jdbc-postgresql` dependency.

* **quarkus-spring-web**: we're going to use Spring's RestControllers
* **quarkus-spring-di**: for injection of our CRUD Repository
* **quarkus-jdbc-mysql**: because our database is MySQL and not Postgresql
* **quarkus-logging-json**: for log parsing, it is best to use a structured format, such as json
* **quarkus-smallrye-health**: built-in healthcheck, will include other resources such as the database, very useful in Kubernetes 

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-spring-web</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-spring-di</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-jdbc-mysql</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-logging-json</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
```

### Transform Resource to Controller

* Change the class annotations
* Change the method annotations
* Rename the file to FruitController

#### Class Annotation

Replace this annotation:

```java
@Path("/fruits")
```

With this.

```java
@RestController
@RequestMapping(value = "/fruits", produces="application/json", consumes = "application/json")
```

Replace all `@PathParams` with Spring's `@PathVariable`'s.
Mind you, these require the name of the variable as a parameter.

For example:

```java
@POST
@Path("/name/{name}/color/{color}")
@Produces("application/json")
public Fruit create(@PathParam String name, @PathParam String color) {}
```

Becomes:

```java
@PostMapping("/name/{name}/color/{color}")
public Fruit create(@PathVariable(value = "name") String name, @PathVariable(value = "color") String color) {
```

Then, replace the following annotations for the methods:

```java
@GET
``` 

with 

```java
@GetMapping
```

```java
@DELETE
@PATH("{id}")
```

With 

```java
@DeleteMapping("{id}")
```


```java
@POST
@Path("/name/{name}/color/{color}")
@Produces("application/json")
```
With 

```java
@PostMapping("/name/{name}/color/{color}"
```

```java
@PUT
@Path("/id/{id}/color/{color}")
@Produces("application/json")
```

With

```java
@PutMapping("/id/{id}/color/{color}")
```

And

```java
@GET
@Path("/color/{color}")
@Produces("application/json")
```

With

```java
@GetMapping("/color/{color}")
```

### Update Application Properties

This is an example configuraiot

```properties
quarkus.datasource.db-kind=mysql
quarkus.datasource.jdbc.url=jdbc:mysql://127.0.0.1:3306/fruits
quarkus.datasource.jdbc.max-size=8
quarkus.datasource.jdbc.min-size=2

quarkus.datasource.username=${GOOGLE_SQL_USER}
quarkus.datasource.password=${GOOGLE_SQL_PASS}

quarkus.hibernate-orm.database.generation=drop-and-create
quarkus.hibernate-orm.sql-load-script=import.sql
```

### Testing

Inspired by [@hantsy](https://medium.com/@hantsy/kickstart-your-first-quarkus-application-cde54f469973) on Medium.

* add h2 test dependencies
* add h2 datasource config to `src/test/resoureces/application.properties`

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>io.quarkus:quarkus-test-h2</artifactId>
  <scope>test</scope>
</dependency>
```

```properties
quarkus.datasource.url=jdbc:h2:tcp://localhost/mem:test
quarkus.datasource.driver=org.h2.Driver
quarkus.hibernate-orm.database.generation = drop-and-create
quarkus.hibernate-orm.log.sql=true
```

### Replace json with jackson

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-resteasy-jackson</artifactId>
</dependency>
```

## Jenkins X

* import into Jenkins X
* update pipeline configuration
* update Dockerfile (optional)
* create secrets in HashiCorp Vault
* update Helm Chart files

### Import

```sh
jx import --pack maven-quarkus
```

### Update Jenkins X Pipeline

* Use up-to-date Quarkus Image
    * https://hub.docker.com/repository/docker/caladreas/jx-builder-graalvm-maven-jdk11
    * https://github.com/joostvdg/jenkins-x-builders
* Change our build command to do a Native Image Build
* Use more memory in the Native Image build process
    * it requires ~6-8GB

```yaml
buildPack:  maven-java11
pipelineConfig:
  agent:
    image: caladreas/jx-builder-graalvm-maven-jdk11:v0.7.0
  pipelines:
    overrides:
      - pipeline: release
        stage: build
        name: mvn-deploy
        containerOptions:
          env:
            - name: _JAVA_OPTIONS
              value: >-
                -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler
                -Xms8g -Xmx8g -XX:+PrintCommandLineFlags -XX:+UseSerialGC
          resources:
            requests:
              cpu: "2"
              memory: 10Gi
            limits:
              cpu: "2"
              memory: 10Gi
        steps:
          - name: mvn-deploy
            command: mvn -Pnative clean package -e --show-version -DskipDocs
            env:
              - name: _JAVA_OPTIONS
                value: >-
                  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler
                  -Xms8g -Xmx8g -XX:+PrintCommandLineFlags -XX:+UseSerialGC
```

### Optional - Update Dockerfile

To help you understand the native image runtime better, I've included the GC logging.
Feel free to remove that if you don't want to see that.

I've also set the maximum memory to 64m, via `"-Xmx32m"`, which should be more than enough.

```Dockerfile
FROM registry.access.redhat.com/ubi8/ubi-minimal:8.1
WORKDIR /work/
COPY target/*-runner /work/application

# set up permissions for user `1001`
RUN chmod 775 /work /work/application \
  && chown -R 1001 /work \
  && chmod -R "g+rwX" /work \
  && chown -R 1001:root /work

EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0","-Xmx32m", "-XX:+PrintGC", "-XX:+PrintGCTimeStamps", "-XX:+VerboseGC", "+XX:+PrintHeapShape"]
```

### Vault For Secrets

* enable vault for environment
* create secret in vault with all required key/value pairs
* for a json value to be added to a Kubernetes secret, insert it as base64 encoded
* `GOOGLE_APPLICATION_CREDENTIALS` -> assumes the location, so better mount the secret into this via a volume + volume mount

* https://jenkins-x.io/docs/using-jx/faq/#how-do-i-inject-vault-secrets-into-stagingproductionpreview-environments
* https://jenkins-x.io/docs/using-jx/faq/#how-do-i-inject-a-vault-secret-via-a-kubernetes-secret

#### Enable Vault In Jenkins X Environment

For each Jenkins X environment that your application is going to land in, such as `jx-staging`, we have to enable Vault support.

We do this by making a change in the Environment's `jx-requirements.yml` file in the root of the repository.

All we need to add is `secretStorage: vault`. The file will look like this:

```yaml
secretStorage: vault
```

Note: Vault is automatically enabled for Preview Environments.

#### Create Secrets In Vault

I owe you the way to do this via the CLI. For now, I'd recommend you go the Vault UI.
Don't know where it is? Well, lets solve that via a _magic_ `jx` command.

```sh
jx get vault-config
```

It should return something like this:

```sh
export VAULT_ADDR=https://vault-jx.mydomain.com
export VAULT_TOKEN=s.BRH5xPYxxxxxxxxxxxxxxxxxxx
```

Open the page and login with the token.
Go to the `secret/` page, and create a new secret (top right `Create secret >`).

Here you can specify a name, this is the secret _container_, it can contain one ore more Key Value pairs.
For each configuration element we have, we create a secret.

* **GOOGLE_SQL_PASS** database password
* **GOOGLE_SQL_USER** database user
* **INSTANCE_CONNECTION_NAME** the instance connection string, look at your Google Cloud UI
* **SA**: the service account that is allowed to connect to the proxy 
    * make a base64 encoding of the json string prior to adding the secret!

TODO: show Vault CLI commands

### Update Chart Configurations

* add google sql proxy
* change health check
* change resources
* add secret resource
* add mounts for secrets
* add values mapping in environment
* add values placeholder values
* add values env mapping

#### Values

```yaml
secrets:
  sql_password: ""
  sql_connection: ""
  sqlsa: ""
env:
  GOOGLE_SQL_USER: vault:quarkus-petclinic:GOOGLE_SQL_USER
```

#### Environment Values

```yaml
jx-quarkus-demo-01:
  secrets:
    sql_password: vault:quarkus-petclinic:GOOGLE_SQL_PASS
    sql_connection: vault:quarkus-petclinic:INSTANCE_CONNECTION_NAME
    sql_sa: vault:quarkus-petclinic:SA
```

#### Resource And Probe

```yaml
resources:
  limits:
    cpu: 250m
    memory: 64Mi
  requests:
    cpu: 250m
    memory: 64Mi
probePath: /health
```

The path, `/health`, is given to us by the dependency `quarkus-smallrye-health` and will serve as the health check endpoint for Kubernetes.

## TODO

* https://quarkus.io/guides/spring-cloud-config-client
* https://quarkus.io/guides/logging-sentry
* https://quarkus.io/guides/vault-datasource
* 

## Other Reading Material

* https://quarkus.io/guides/writing-native-applications-tips
* https://quarkus.io/guides/building-native-image
* https://cloud.google.com/community/tutorials/run-spring-petclinic-on-app-engine-cloudsql
* https://github.com/GoogleCloudPlatform/community/tree/master/tutorials/run-spring-petclinic-on-app-engine-cloudsql/spring-petclinic/src/main/resources
* https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/blob/master/google-cloud-spanner-hibernate-samples/quarkus-jpa-sample
* https://medium.com/@hantsy/kickstart-your-first-quarkus-application-cde54f469973
* https://developers.redhat.com/blog/2020/04/10/migrating-a-spring-boot-microservices-application-to-quarkus/
* https://www.baeldung.com/rest-assured-header-cookie-parameter
