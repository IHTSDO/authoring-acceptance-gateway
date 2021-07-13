# ----------------------------------------
# This script will insert test data for development purposes.
# ----------------------------------------

# ----------------------------------------
# Variables (Change as required).
# ----------------------------------------
aagUrl=http://localhost:8090/authoring-acceptance-gateway
branchPath=MAIN

# ----------------------------------------
# Accept all Criteria Item(s) associated with Branch, including non-manual.
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  ${aagUrl}/admin/criteria/${branchPath}/accept
