#!/usr/bin/env bash

# ----------------------------------------
# Variables (Change as required)
# ----------------------------------------
aagUrl=http://localhost/authoring-acceptance-gateway
branchPath=MAIN
criteriaItemId=task-manual-spellcheck

# ----------------------------------------
# This script will insert test data for development purposes.
# ----------------------------------------

doCurl() {
    url=$1

    echo -n "$url - "

    curl -s\
        -w "%{http_code}\n" \
        --header "Content-Type: application/json" \
        --request DELETE \
        "${url}" | jq
}

# ----------------------------------------
# Revert the signing off of a Criteria Item.
# ----------------------------------------
doCurl ${aagUrl}/acceptance/${branchPath}/item/${criteriaItemId}/accept
