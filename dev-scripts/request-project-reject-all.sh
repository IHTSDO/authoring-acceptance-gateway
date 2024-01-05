#!/usr/bin/env bash

# ----------------------------------------
# Variables (Change as required)
# ----------------------------------------
aagUrl=http://localhost/authoring-acceptance-gateway
branchPath=MAIN

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
# Reject all Criteria Item(s) associated with Branch, including non-manual.
# ----------------------------------------
doCurl ${aagUrl}/admin/criteria/${branchPath}/accept
