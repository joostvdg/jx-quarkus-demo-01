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

## Sonar Integration

### Self-Hosted

* create secrets
* add secrets to build
* add build step to do sonar analysis

#### Create Secrets

* create API Token in Sonar
* create Kubernetes Secrets for the SonarURL URL and API Token
    * `SONAR_API_TOKEN` -> will be automatically used by the Sonar Maven plugin
    * `SONAR_HOST_URL` 

```sh
kubectl create secret generic jx-quarkus-demo-01-sonar -n jx --from-literal=SONAR_API_TOKEN='mytoken' --from-literal=SONAR_HOST_URL='myurl'
```

#### Secrets Injection Alternatives

* https://www.hashicorp.com/blog/injecting-vault-secrets-into-kubernetes-pods-via-a-sidecar/

### Override Pipeline

To run the SonarQube analysis, we add another step to the pipeline, in the `jenkins-x.yml` file.
There are various ways to do it, in this case I'm using the `overrides` mechanic to add a new step _after_ `mvn-deploy`.

Remember the Kubernetes secret we created earlier, due to the key names, we can now add them as environment variables directly from the secret.

We do this via the `envFrom` construction:

```yaml
envFrom:
  - secretRef:
      name: jx-quarkus-demo-01-sonar
```

We can add any Kubernetes container configuration to our stage's container, via Jenkins X's `containerOptions` key.

```yaml
pipelineConfig:
  pipelines:
    overrides:
      - name: mvn-deploy
        pipeline: release
        stage: build
        containerOptions:
          envFrom:
            - secretRef:
                name: jx-quarkus-demo-01-sonar
        step:
          name: sonar
          command: mvn
          args:
            - compile
            - org.sonarsource.scanner.maven:sonar-maven-plugin:3.6.0.1398:sonar
            - -Dsonar.host.url=$SONAR_HOST_URL
            - -e
            - --show-version
            - -DskipTests=true
        type: after
```

For more syntax details, see the [Jenkins X Pipeline page](https://jenkins-x.io/docs/reference/pipeline-syntax-reference/#containerOptions).

## Dependency Vulnerability Check

* Snyk: https://github.com/snyk/snyk-maven-plugin
* Sonatype: https://sonatype.github.io/ossindex-maven/maven-plugin/

### Add Pipeline Step

```yaml
  - name: sonar
    stage: build
    containerOptions:
      envFrom:
        - secretRef:
            name: sonatype-oss-index
    step:
      name: sonatype-ossindex
      command: mvn 
      args: 
        - org.sonatype.ossindex.maven:ossindex-maven-plugin:audit 
        - -f
        - pom.xml
        - -Dossindex.scope=compile
        - -Dossindex.reportFile=ossindex.json
        - -Dossindex.cvssScoreThreshold=4.0
        - -Dossindex.fail=false
    type: after
```

There's currently a vulnerability related to `org.apache.thrift:libthrift`, which is part of `quarkus-smallrey-opentracing`.
Replacing `libthrift` with a version that is not vulnerable causes errors.

So, we can:

* not use open tracing
* implement open tracing with another library (might not be Native Image compatible)
* ignore this particular vulnerability
* not fail the build

## Monitoring Metrics

As we're running Jenkins X, we run in Kubernetes. 
The most commonly used monitoring solution with Kubernetes is Prometheus and Grafana.

Quarkus has out-of-the-box support for exposing prometheus metrics, via the `smallrye-metrics` library.

To create a Grafana dashboard for our application, we need to take the following steps:

* add dependency on Quarkus' `smallrye-metrics` library
* add (Kubernetes) annotations to our Helm Chart's Deployment definition
* add (Java) annotations to our code, specifying the metrics

For more information on adding metrics to your Quarkus application, [read the Quarkus Metrics guide](https://quarkus.io/guides/microprofile-metrics).

### Add quarkus-smallrye-metrics Dependency

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-metrics</artifactId>
</dependency>
```

### Update Deployment Definition

```yaml
      annotations:
        prometheus.io/port: "8080"
        prometheus.io/scrape: "true"
{{- if .Values.podAnnotations }}
{{ toYaml .Values.podAnnotations | indent 8 }} #Only for pods
{{- end }}
```
 
### Add Metrics Annotations to our Code

Look at [FruitResource.java](src/main/java/com/github/joostvdg/jx/quarkus/fruits/FruitResource.java) for all the metrics.

For example, on the `findAll()` method, for the `/fruits` endpoint, we can add a counter - how many times is this endpoint called - and a timer - various percentile buckets on the duration of the call:

```java
@Counted(name = "fruit_get_all_count", description = "How many times all Fruits have been retrieved.")
@Timed(name = "fruit_get_all_timer", description = "A measure of how long it takes to retrieve all Fruits.", unit = MetricUnits.MILLISECONDS)
```

### Grafana Dashboard

See [grafana-dashboard.json](grafana-dashboard.json) for the dashboard to import into Grafana.

## Tracing

* jaeger / opentracing

## Stack Traces

* sentry

## Docker Scan with Anchore

* https://github.com/anchore/anchore-engine

## Integration Testing In Preview Charts

### Database Schema Management

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-flyway</artifactId>
</dependency>
```

```java
@GeneratedValue(strategy = GenerationType.IDENTITY)
```

```java
@Entity
public class Fruit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
```

```properties
quarkus.datasource.url=jdbc:h2:tcp://localhost/mem:fruits;MODE=MYSQL;DB_CLOSE_DELAY=-1
quarkus.datasource.driver=org.h2.Driver
#quarkus.hibernate-orm.database.generation = drop-and-create

quarkus.hibernate-orm.log.sql=true
#quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.database.default-schema=FRUITS
quarkus.flyway.migrate-at-start=true
quarkus.flyway.schemas=FRUITS
#quarkus.flyway.baseline-on-migrate=true

quarkus.log.console.level=DEBUG
quarkus.log.category."org.flywaydb.core".level=DEBUG
quarkus.log.category."io.quarkus.flyway".level=DEBUG
```

### Use Actual MySQL Database In Local Build

TODO

* see: https://phauer.com/2017/dont-use-in-memory-databases-tests-h2/

## TODO

* https://quarkus.io/guides/spring-cloud-config-client
* https://quarkus.io/guides/logging-sentry
* https://quarkus.io/guides/vault-datasource
* SonarQube Integration
    * Self-hosted
    * SonarCloud
* Image Scanning?
    * Whitesource?
    * ??
* Security Scanning
* Integration Test in Preview
    * spin up database with test datasource
    * run tests
    * post test results somewhere
* some analysis' can be do in parallel -> how to configure with prow?
    * maybe Lighthouse is easier?
* Explore Secrets injection from Vault / External Secrets Controller (forgot the name)
    * this is for the pipeline secrets, which Jenkins X currently does _not_ inject


## Other Reading Material

* https://quarkus.io/guides/writing-native-applications-tips
* https://quarkus.io/guides/building-native-image
* https://cloud.google.com/community/tutorials/run-spring-petclinic-on-app-engine-cloudsql
* https://github.com/GoogleCloudPlatform/community/tree/master/tutorials/run-spring-petclinic-on-app-engine-cloudsql/spring-petclinic/src/main/resources
* https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/blob/master/google-cloud-spanner-hibernate-samples/quarkus-jpa-sample
* https://medium.com/@hantsy/kickstart-your-first-quarkus-application-cde54f469973
* https://developers.redhat.com/blog/2020/04/10/migrating-a-spring-boot-microservices-application-to-quarkus/
* https://www.baeldung.com/rest-assured-header-cookie-parameter
* https://jenkins-x.io/docs/reference/pipeline-syntax-reference/#containerOptions
* https://openliberty.io/blog/2020/04/09/microprofile-3-3-open-liberty-20004.html#gra
* https://openliberty.io/docs/ref/general/#metrics-catalog.html
* https://grafana.com/grafana/dashboards/4701
* https://phauer.com/2017/dont-use-in-memory-databases-tests-h2/
