# jarvis-api

`jarvis-api` is a REST API that is responsible for managing personal metadata in the form of the resources:

* Events - Categorized personal occurrences that are tied to personal information and artifacts
* Log entries - Reflections upon events
* Tags - Concepts, people, places, things used to label log entries

This metadata is used to build a richer context for your personal data to help you with retaining and digesting the information and to help store the information for better retrieval.  Personal data like notes, papers, drafts, reports, emails, photos, videos are each enhanced by tying in additional subjective tidbits relevant to you and your relationship with your piece of data.

The metadata answers subjective questions like:

* What were you doing?
* Where were you?
* What time of day?
* Who was involved?
* How did you feel?
* How did it impact you?
* What are your opinions?
* What are the next steps?

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

Take a look at the [`jarvis` repository](https://github.com/clb6/jarvis) for instructions.

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
