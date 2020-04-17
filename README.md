# quarkus project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

## Packaging and running the application

The application can be packaged using `./mvnw package`.
It produces the `quarkus-1.0.0-SNAPSHOT-runner.jar` file in the `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/quarkus-1.0.0-SNAPSHOT-runner.jar`.

## Creating a native executable

You can create a native executable using: `./mvnw package -Pnative`.

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: `./mvnw package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your native executable with: `./target/quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image.

## Spanner

* https://github.com/GoogleCloudPlatform/google-cloud-spanner-hibernate/blob/master/google-cloud-spanner-hibernate-samples/quarkus-jpa-sample

## Jenkins X

* enable vault for environment
* create secret in vault with all required key/value pairs
* for a json value to be added to a Kubernetes secret, insert it as base64 encoded
* `GOOGLE_APPLICATION_CREDENTIALS` -> assumes the location, so better mount the secret into this via a volume + volume mount

### Vault For Secrets

* https://jenkins-x.io/docs/using-jx/faq/#how-do-i-inject-vault-secrets-into-stagingproductionpreview-environments
* https://jenkins-x.io/docs/using-jx/faq/#how-do-i-inject-a-vault-secret-via-a-kubernetes-secret
