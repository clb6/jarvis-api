#! /bin/bash

if [[ $EUID -ne 0 ]]; then
    echo "This script must be run as root" 1>&2
    exit 1
else
    TARGET_DIR="/opt"
    SNAPSHOT_NAME="jarvis_snapshot_$(date -u +%Y%m%d%H%M%S).tar.gz"
    echo "Backing up Jarvis"
    SNAPSHOT_PATH="$TARGET_DIR/jarvis_snapshots/$SNAPSHOT_NAME"
    echo "Backup: $SNAPSHOT_PATH"
fi

cd $TARGET_DIR
tar -czf $SNAPSHOT_PATH jarvis/

echo "Done!"
