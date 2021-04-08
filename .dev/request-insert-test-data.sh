# ----------------------------------------
# This script will insert test data for development purposes.
# ----------------------------------------

# ----------------------------------------
# Criteria Items (Globally required)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "authoringLevel": "PROJECT",
    "description": "Ensure project successfully validates.",
    "expiresOnCommit": true,
    "id": "project-run-validation",
    "label": "Run project validation",
    "mandatory": true,
    "manual": true,
    "order": 0,
    "requiredRole": "AUTHOR"
  }' \
  http://localhost:8090/authoring-acceptance-gateway/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "authoringLevel": "PROJECT",
    "description": "Ensure project successfully classifies.",
    "expiresOnCommit": true,
    "id": "project-run-classification",
    "label": "Run project classification",
    "mandatory": true,
    "manual": true,
    "order": 1,
    "requiredRole": "AUTHOR"
  }' \
  http://localhost:8090/authoring-acceptance-gateway/criteria-items

# ----------------------------------------
# Criteria Items (Optional)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "authoringLevel": "PROJECT",
    "description": "Update documentation.",
    "expiresOnCommit": true,
    "id": "project-update-documentation",
    "label": "Update project documentation",
    "mandatory": false,
    "manual": true,
    "order": 1,
    "requiredRole": "AUTHOR"
  }' \
  http://localhost:8090/authoring-acceptance-gateway/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "authoringLevel": "TASK",
    "description": "Review changes with another author.",
    "expiresOnCommit": true,
    "id": "task-review-changes",
    "label": "Review changes.",
    "mandatory": true,
    "manual": true,
    "order": 1,
    "requiredRole": "AUTHOR"
  }' \
  http://localhost:8090/authoring-acceptance-gateway/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "authoringLevel": "TASK",
    "description": "Send email to project lead with latest changes.",
    "expiresOnCommit": true,
    "id": "task-email-project-lead",
    "label": "Inform project lead.",
    "mandatory": true,
    "manual": false,
    "order": 2,
    "requiredRole": "AUTHOR"
  }' \
  http://localhost:8090/authoring-acceptance-gateway/criteria-items

# ----------------------------------------
# Criteria
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
  "branchPath": "MAIN",
  "selectedProjectCriteriaIds": [
    "project-update-documentation"
  ],
  "selectedTaskCriteriaIds": [
    "task-review-changes",
    "task-email-project-lead"
  ]
  }' \
  http://localhost:8090/authoring-acceptance-gateway/criteria
