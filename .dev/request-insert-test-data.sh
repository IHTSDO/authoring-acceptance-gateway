# ----------------------------------------
# This script will insert test data for development purposes.
# ----------------------------------------

# ----------------------------------------
# Variables (Change as required)
# ----------------------------------------
aagUrl=http://localhost:8090/authoring-acceptance-gateway
branchPath=MAIN

# ----------------------------------------
# Criteria Items (Globally required as mandatory)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-final-signoff",
    "label": "Project Iteration Final Sign-off",
    "description": "Final project iteration sign-off by a release lead.",
    "order": 10,
    "authoringLevel": "PROJECT",
    "mandatory": true,
    "manual": true,
    "expiresOnCommit": false,
    "requiredRole": "RELEASE_LEAD",
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "task-release-notes-produced",
    "label": "Release Notes Produced",
    "description": "Release notes for task have been produced.",
    "order": 9,
    "authoringLevel": "TASK",
    "mandatory": true,
    "manual": true,
    "expiresOnCommit": false,
    "requiredRole": "RELEASE_LEAD",
    "complete": false
  }' \
  ${aagUrl}/criteria-items

# ----------------------------------------
# Criteria Items (Optional Task Level)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
     "id": "task-clean-classification",
     "label": "Classified content",
     "description": "All axiom changes and concept inactivations must be classified.",
     "order": 1,
     "authoringLevel": "TASK",
     "mandatory": false,
     "manual": false,
     "expiresOnCommit": true,
     "requiredRole": "AUTHOR",
     "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "task-manual-spellcheck",
    "label": "Manual Spellchecking",
    "description": "Check spellings of new descriptions manually.",
    "order": 2,
    "authoringLevel": "TASK",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": false,
    "requiredRole": "AUTHOR",
    "complete": false
    }' \
  ${aagUrl}/criteria-items

# ----------------------------------------
# Criteria Items (Optional Project Level)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
     "id": "project-clean-classification",
     "label": "Classified content",
     "description": "All axiom changes and concept inactivations must be classified.",
     "order": 1,
     "authoringLevel": "PROJECT",
     "mandatory": false,
     "manual": false,
     "expiresOnCommit": true,
     "requiredRole": "AUTHOR",
     "complete": false
  }' \
  ${aagUrl}/criteria-items

# ----------------------------------------
# Criteria
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data "{
  \"branchPath\": \"$branchPath\",
  \"selectedProjectCriteriaIds\": [
    \"project-clean-classification\"
  ],
  \"selectedTaskCriteriaIds\": [
    \"task-clean-classification\",
    \"task-manual-spellcheck\"
  ]
  }" \
  ${aagUrl}/criteria
