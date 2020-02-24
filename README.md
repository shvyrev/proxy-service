# Proxy Service project

Package project 
```bash
$ ./mvnw clean package
```

Check 
```bash
$ java -jar target/proxy_service-1.0.0-SNAPSHOT-runner.jar

$ http "localhost:8080/ping"
```

Build Docker image
```bash
$ docker build -f src/main/docker/Dockerfile.jvm  -t appmobiles/proxy-service .
```

Run Docker container
```bash
$ docker run -i --rm --name hello_quarkus --env PORT=8081 -p 8081:8081 appmobiles/proxy-service
```

Check 
```bash
$  http "localhost:8081/ping"
```

Heroku 
```bash
$ heroku login

$ heroku create appmobiles-proxy-service

$ heroku container:login

$ docker tag appmobiles/proxy-service registry.heroku.com/appmobiles-proxy-service/web

$ docker push registry.heroku.com/appmobiles-proxy-service/web

$ heroku container:release web -a appmobiles-proxy-service
```

Check 
```bash
$ http "https://appmobiles-proxy-service.herokuapp.com/ping"
```

Statistic
```bash
$ http "https://appmobiles-proxy-service.herokuapp.com/stat"
```

## Packaging and running the application

The application is packageable using `./mvnw package`.
It produces the executable `proxy_service-1.0.0-SNAPSHOT-runner.jar` file in `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/proxy_service-1.0.0-SNAPSHOT-runner.jar`.

## Creating a native executable

You can create a native executable using: `./mvnw package -Pnative`.

Or you can use Docker to build the native executable using: `./mvnw package -Pnative -Dquarkus.native.container-build=true`.

You can then execute your binary: `./target/proxy_service-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/building-native-image-guide .