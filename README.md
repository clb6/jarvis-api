# jarvis-api

This is the data API for the Jarvis project.  It is responsible for providing the Jarvis resources:

* Log entries
* Tags

## Pre-requisites

1. You must run the `bin/install.sh` script in order to create the necessary filesystem directory structure.
2. Run `docker build -t jarvis-api:<version> .` to create the Docker image.

## Usage

### Run with Docker

Run the Elasticsearch container first:

```
docker run -d -v /opt/jarvis/Elasticsearch/:/usr/share/elasticsearch/data -p 9200:9200 --name jarvis-elasticsearch elasticsearch
```

**Note**: Apparently the last `/` makes a difference.  Docker Elasticsearch when it mounts the volume changes the folder permission to `avahi-autoipd:lpadmin`.  Without the `/` other directories also change permissions.

Now run the jarvis-api linking it to the Elasticsearch container:

```
docker run -d -v /opt/jarvis:/opt/jarvis -p 3000:3000 --link jarvis-elasticsearch:elasticsearch.jarvis.home --name jarvis-api-container jarvis-api:<version>
```

### Development

#### Run the application locally

`lein ring server`

#### Packaging and running as standalone jar

```
lein do clean, ring uberjar
java -jar target/server.jar
```

#### Packaging as war

`lein ring uberwar`

