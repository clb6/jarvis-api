#! /bin/bash

# TODO: Stdin the directory

if [ "$1" == "dev" ]; then
    echo "Installing Jarvis (local development)"
    JARVIS_DIR_ROOT="/tmp/jarvis"
elif [[ $EUID -ne 0 ]]; then
    echo "This script must be run as root" 1>&2
    exit 1
else
    echo "Installing Jarvis"
    JARVIS_DIR_ROOT="/opt/jarvis"
fi

VERSION=3

LOG_ENTRIES_DIR="$JARVIS_DIR_ROOT/LogEntries_v$VERSION"
echo "Log entries: $LOG_ENTRIES_DIR"
mkdir -p $LOG_ENTRIES_DIR
ln -s $LOG_ENTRIES_DIR $JARVIS_DIR_ROOT/LogEntries

TAGS_DIR="$JARVIS_DIR_ROOT/Tags_v$VERSION"
echo "Tags: $TAGS_DIR"
mkdir -p $TAGS_DIR
ln -s $TAGS_DIR $JARVIS_DIR_ROOT/Tags

IMAGES_DIR="$JARVIS_DIR_ROOT/Images"
echo "Images: $IMAGES_DIR"
mkdir -p $IMAGES_DIR

ELASTIC_DIR="$JARVIS_DIR_ROOT/Elasticsearch"
echo "Elasticsearch: $ELASTIC_DIR"
mkdir -p $ELASTIC_DIR
