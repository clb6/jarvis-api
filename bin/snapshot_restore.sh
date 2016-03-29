#! /bin/bash

# Purpose of this script is to restore the jarvis-api data files with a provided
# snapshot file - compressed tarball.

function showSummary {
    if [ "$1" != "dev" ]; then
        curl localhost:3000/datasummary/logentries | jq .
        curl localhost:3000/datasummary/tags | jq .
    fi

}

if [ "$2" == "dev" ]; then
    echo "Restoring Jarvis (local development)"
    TARGET_DIR="/tmp"
elif [[ $EUID -ne 0 ]]; then
    echo "This script must be run as root" 1>&2
    exit 1
else
    echo "Restoring Jarvis"
    TARGET_DIR="/opt"
fi

# TODO: More snapshot file validation

if [ -e "$1" ]; then
    echo "Snapshot file: $1"
else
    echo "Invalid snapshot file: $1"
    echo "Please provide a valid snapshot file"
    exit 1
fi

TARGET_JARVIS_DIR="$TARGET_DIR/jarvis"
TARGET_JARVIS_PREV_DIR="$TARGET_DIR/jarvis_prev"

echo "Before"
showSummary $2
echo ""

rm -Rf $TARGET_JARVIS_PREV_DIR
mv $TARGET_JARVIS_DIR $TARGET_JARVIS_PREV_DIR
tar -xf $1 -C $TARGET_DIR

echo "After"
showSummary $2
echo ""

echo "Done!"
