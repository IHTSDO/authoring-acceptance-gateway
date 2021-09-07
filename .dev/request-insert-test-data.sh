# ----------------------------------------
# This script will insert test data for development purposes.
# ----------------------------------------

# ----------------------------------------
# Variables (Change as required)
# ----------------------------------------
aagUrl=http://localhost:8090/authoring-acceptance-gateway
branchPath=MAIN

# ----------------------------------------
# Criteria Items (Project Level, mandatory)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-classification-clean",
    "label": "Classification Report Clean",
    "description": "Project classification has been run, saved and come back with no results.",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": true,
    "manual": false,
    "expiresOnCommit": true,
    "requiredRole": "RELEASE_LEAD",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-validation-clean",
    "label": "RVF Report Clean",
    "description": "Project Validation has been run and come back with no results.",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": true,
    "manual": false,
    "expiresOnCommit": true,
    "requiredRole": "RELEASE_LEAD",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

# ----------------------------------------
# Criteria Items (Project Level, complex)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-inactivations-associations",
    "label": "Report: Validate Inactivations with Associations Clean",
    "description": "Validate Inactivations with Associations Report Clean",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [
      "complex"
    ],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-template-validation",
    "label": "Report: Template Compliance Clean",
    "description": "Project Template Validation Run and Clean",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [
      "complex"
    ],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-patterns-report",
    "label": "Report: KPI Patterns Clean",
    "description": "Reporting platform patterns report run and checked.",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [
      "complex"
    ],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-new-descriptions",
    "label": "Report: New Descriptions Clean",
    "description": "New Descriptions Report Clean",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [
      "complex"
    ],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-duplicate-terms",
    "label": "Report: Duplicate Terms Clean",
    "description": "Duplicate Terms Report Clean",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [
      "complex"
    ],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-case-significance",
    "label": "Report: Case Significance Clean",
    "description": "Case Significance Report Clean",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [
      "complex"
    ],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-release-issues",
    "label": "Report: Release Issues Clean",
    "description": "Release Issues Report Clean",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [
      "complex"
    ],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

# ----------------------------------------
# Criteria Items (Project Level, optional)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-release-validation-report-clean",
    "label": "Release Lead: All Release Validation Reports Checked",
    "description": "Release Validation Reports Checked by Release Lead(s)",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-documentation-completed",
    "label": "Project Documentation (TIG, Editorial Guide) Complete",
    "description": "Project Documentation (TIG, Editorial Guide) Complete.",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-mrcm",
    "label": "Relevant MRCM Changes Implemented",
    "description": "Any new MRCM changes implemented and sequestered into the project until all content that is impacted by the change is updated and the MRCM project validation is clean.",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-whitelist-review",
    "label": "Release Lead: Exceptions Checked and Signed off",
    "description": "Exceptions to assertions checked and signed off by Release Lead(s).",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-final-signoff",
    "label": "Release Lead: Final Sign-Off",
    "description": "Final project iteration sign-off by a release lead.",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": false,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "project-lead-signoff",
    "label": "Project Lead: Signed Off Project as Ready to Promote",
    "description": "Project Lead(s) Signed Off Project as Ready to Promote.",
    "order": 1,
    "authoringLevel": "PROJECT",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

# ----------------------------------------
# Criteria Items (Task Level, optional)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "task-manual-spellcheck",
    "label": "Manual spellcheck.",
    "description": "Confirm there are no spelling mistakes.",
    "order": 1,
    "authoringLevel": "TASK",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

curl --header "Content-Type: application/json" \
  --request POST \
  --data '{
    "id": "task-validation-clean",
    "label": "RVF Report Clean",
    "description": "Task Validation has been run and come back with no results.",
    "order": 1,
    "authoringLevel": "TASK",
    "mandatory": false,
    "manual": true,
    "expiresOnCommit": true,
    "requiredRole": "AUTHOR",
    "enabledByFlag": [],
    "complete": false
  }' \
  ${aagUrl}/criteria-items

# ----------------------------------------
# Project Acceptance Criteria (only subset explicitly configured)
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data "{
  \"branchPath\": \"$branchPath\",
  \"projectIteration\": 1,
  \"selectedProjectCriteriaIds\": [
    \"project-release-validation-report-clean\",
    \"project-documentation-completed\",
    \"project-mrcm\",
    \"project-whitelist-review\",
    \"project-final-signoff\",
    \"project-lead-signoff\"
  ],
  \"selectedTaskCriteriaIds\": [
    \"task-manual-spellcheck\",
    \"task-validation-clean\"
  ]
  }" \
  ${aagUrl}/criteria

# ----------------------------------------
# Whitelist
# ----------------------------------------
curl --header "Content-Type: application/json" \
  --request POST \
  --data "{
  \"validationRuleId\": \"fbd4bbb5-3e62-4ccb-824a-e82d9771c0ee\",
  \"componentId\": \"9327948011\",
  \"conceptId\": \"3641484006\",
  \"branch\": \"$branchPath\",
  \"additionalFields\": \"1,900000000000207008,3641484006,en,900000000000003001,\$processOutput\$ by microscopy technique using microscope camera (observable entity),900000000000448009\"
  }" \
  ${aagUrl}/whitelist-items
