# ----------------------------------------
# This script will insert test data for development purposes.
# ----------------------------------------

# ----------------------------------------
# Variables (Change as required).
# ----------------------------------------
aagUrl=http://localhost:8090/authoring-acceptance-gateway
branchPath=MAIN
criteriaItemId=task-manual-spellcheck

# ----------------------------------------
# Manually sign off a Criteria Item.
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  ${aagUrl}/acceptance/${branchPath}/item/${criteriaItemId}/accept
