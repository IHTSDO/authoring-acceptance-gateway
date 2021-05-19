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
# Revert the signing off of a Criteria Item.
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request DELETE \
  ${aagUrl}/acceptance/${branchPath}/item/${criteriaItemId}/accept
