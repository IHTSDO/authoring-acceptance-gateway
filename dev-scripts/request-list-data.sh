#!/usr/bin/env bash

doCount() {
    index=$1
    echo -n "$(printf '%30s' "$index") : "
    curl -s "http://localhost:9200/${index}/_count" | jq '.count'
}

doCount "aag-criteria-item"
doCount "aag-criteria-item-sign-off"
doCount "aag-project-criteria"
doCount "aag-whitelist-item"
