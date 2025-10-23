# Loading Database Configuration from System Environment Variables

This logic initializes a `DatabaseConfigs` instance using values retrieved from system environment variables. It is used when the configuration source is explicitly set to `SYSTEM_ENV_VARS`.

When `dbId` equals `DatabaseConfigs.SYSTEM_ENV_VARS`, the application reads the ArangoDB connection parameters from environment variables instead of configuration files or other sources. This is especially useful in containerized or cloud-based deployments where credentials and connection details are injected via environment variables.

## Behavior

1. **Retrieve Environment Variables**
   The application reads the following environment variables:

   * `ARANGODB_USERNAME`
   * `ARANGODB_PASSWORD`
   * `ARANGODB_DATABASE`
   * `ARANGODB_HOST`
   * `ARANGODB_PORT`

   If any variable is not present in the system environment, a default value is applied.

2. **Parse Port Value**
   The `ARANGODB_PORT` value is parsed from `String` to `int` using `StringUtil.safeParseInt`.
   If parsing fails, the default port value is used.

3. **Create Database Configuration Object**
   A new `DatabaseConfigs` instance is created using the resolved values.

## Code Reference

```java
if (DatabaseConfigs.SYSTEM_ENV_VARS.equalsIgnoreCase(dbId)) {
    String username = System.getenv().getOrDefault(ArangoDbUtil.ENV_ARANGO_USERNAME, DEFAULT_ARANGO_USERNAME);
    String password = System.getenv().getOrDefault(ENV_ARANGO_PASSWORD, DEFAULT_ARANGO_PASSWORD);
    String database = System.getenv().getOrDefault(ENV_ARANGO_DATABASE, DEFAULT_ARANGO_DATABASE);
    String host     = System.getenv().getOrDefault(ENV_ARANGO_HOST, DEFAULT_ARANGO_HOST);
    int port = StringUtil.safeParseInt(System.getenv().getOrDefault(ENV_ARANGO_PORT, DEFAULT_ARANGO_PORT));

    configs = new DatabaseConfigs(username, password, database, host, port);
}
```

## Use Cases

This configuration mode is intended for:

* **Docker and Kubernetes deployments**
* **CI/CD pipelines**
* **Cloud-native environments**
* Situations requiring **decoupled credential injection** without modifying application source code

## Advantages

* Ensures sensitive credentials are not hard-coded.
* Supports secure and flexible runtime configuration.
* Compatible with containerized deployment best practices.
