#! /bin/bash

# TODO: Fix this what the message below says
echo "This script might have to be run at the top-level of jarvis-api"

URL_ELASTICSEARCH="$1"
echo "Elasticsearch URL: $URL_ELASTICSEARCH"

DIR_MAPPINGS="setup/mappings"

curl -X PUT $URL_ELASTICSEARCH/jarvis-$JARVIS_DATA_VERSION
curl -X PUT $URL_ELASTICSEARCH/jarvis-$JARVIS_DATA_VERSION/_mapping/tags -d \
    @$DIR_MAPPINGS/mapping_tags.json
curl -X PUT $URL_ELASTICSEARCH/jarvis-$JARVIS_DATA_VERSION/_mapping/logentries -d \
    @$DIR_MAPPINGS/mapping_log_entries.json
curl -X PUT $URL_ELASTICSEARCH/jarvis-$JARVIS_DATA_VERSION/_mapping/events -d \
    @$DIR_MAPPINGS/mapping_events.json
# Moving away from alias but keep here for now
#curl -X PUT $URL_ELASTICSEARCH/jarvis-$JARVIS_DATA_VERSION/_alias/jarvis
