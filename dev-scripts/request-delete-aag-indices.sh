#!/usr/bin/env bash

curl -s -X DELETE http://localhost:9200/aag-criteria-item | jq
curl -s -X DELETE http://localhost:9200/aag-criteria-item-sign-off | jq
curl -s -X DELETE http://localhost:9200/aag-project-criteria | jq
curl -s -X DELETE http://localhost:9200/aag-whitelist-item | jq
