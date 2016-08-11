# jarvis-api

jarvis-api is a RESTful data API that is the core service for jarvis.  It is used to manage the jarvis resources:

* Events - Categorized personal occurrences
* Log entries - Reflections on events
* Tags - Concepts, people, places, things used to label log entries

## Dependencies

Databases:

* [Elasticsearch](https://www.elastic.co/)
* [Redis](http://redis.io/)

## Environment variables

Variable | Description | Optional? | Dockerfile? | Default
--- | --- | --- | --- | ---
`JARVIS_DIR_ROOT` | Top-level directory to data files for Jarvis | N | Y | None
`JARVIS_DATA_VERSION` | Version of the data resources e.g. 20160528 | N | N | None
`JARVIS_ELASTICSEARCH` | Elasticsearch connection full URL | Y | N | http://elasticsearch.jarvis.home:9200
`JARVIS_REDIS_HOST` | Redis hostname  | Y | N | redis.jarvis.home
`JARVIS_REDIS_PORT` | Redis port  | Y | N | 6379

## Installation

Take a look at the jarvis repository for instructions.

## Admin

The jarvis-cli tool has a set of `admin` commands that can be used to help manage jarvis-api.

### Backing up

jarvis-api data can be backed up by taking snapshots of the critical data directory which by default is `/opt/jarvis`.  Use the jarvis-cli tool to take local snapshots.  Run the following for more details:

```
jarvis admin backup --help
```

jarvis-cli also has the ability to restore from an existing snapshot:

```
jarvis admin restore --help
```

## Development

### Run the application locally

`lein ring server`

### Packaging and running as standalone jar

```
lein do clean, ring uberjar
java -jar target/server.jar
```

### Packaging as war

`lein ring uberwar`

### Versioning

[SemVer](http://semver.org/) is used.
