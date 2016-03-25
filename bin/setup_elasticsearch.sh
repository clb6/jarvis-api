#! /bin/bash

# TODO: Fix this what the message below says
echo "This script might have to be run at the top-level of jarvis-api"

URL_ELASTICSEARCH="$1"
echo "Elasticsearch URL: $URL_ELASTICSEARCH"

DIR_MAPPINGS="setup/mappings"

curl -X PUT $URL_ELASTICSEARCH/jarvis_v3
curl -X PUT $URL_ELASTICSEARCH/jarvis_v3/_mapping/tags -d \
    @$DIR_MAPPINGS/mapping_tags.json
curl -X PUT $URL_ELASTICSEARCH/jarvis_v3/_mapping/logentries -d \
    @$DIR_MAPPINGS/mapping_log_entries.json
curl -X PUT $URL_ELASTICSEARCH/jarvis_v3/_alias/jarvis
