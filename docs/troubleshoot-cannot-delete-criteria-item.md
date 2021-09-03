# Troubleshoot - Cannot delete CriteriaItem
This technical document will help with troubleshooting why a `CriteriaItem` cannot be deleted from the store.

## Problem
When running `DELETE /criteria-items/$criteriaItemId`, a `409 Conflict` status may be returned with a message indicating the `CriteriaItem` cannot be deleted as it is referenced by
 a number of `ProjectAcceptanceCriteria`. See below for an example response.
 
 ```
 {
     "error": 409,
     "message": "Criteria can not be deleted, it is used in 8 project criteria."
 }
 ```

## Solution
There are a number of steps involved in order to resolve this problem. See below for the steps involved in deleting the `CriteriaItem`.
1) Identify the `ProjectAcceptanceCriteria` which reference the `CriteriaItem`. To do this, it is best to use Kibana to run an Elasticsearch query.
2) Inspect the response from Elasticsearch and collect the `branchPath` and `projectIteration` values from all documents.
3) For each `branchPath` and `projectIteration` combination, use the `GET /criteria/$branchPath?projectIteration=$projectIteration` and `PUT /criteria/$branchPath
?projectIteration=$projectIteration` endpoints accordingly.
4) The `DELETE /criteria-items/$criteriaItemId` endpoint will now work as expected.

See below for each step discussed with more detail.

#### 1. Identify `ProjectAcceptanceCriteria` 
There are two different Elasticsearch queries to run depending on the `CriteriaItem` itself.

If the `CriteriaItem` to be deleted has an `AuthoringLevel` of `PROJECT`, use the below query.

```
GET aag-project-criteria/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "selectedProjectCriteriaIds": "$criteriaItemId"
          }
        }
      ]
    }
  }
}
```

Likewise, if the `CriteriaItem` to be deleted has an `AuthoringLevel` of `TASK`, use the below query.

```
GET aag-project-criteria/_search
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "selectedTaskCriteriaIds": "$criteriaItemId"
          }
        }
      ]
    }
  }
}
```

Please note the variable `$criteriaItemId`. This should be substituted for the identifier of the `CriteriaItem` to be deleted.

#### 2. Collect `branchPath` and `projectIteration`
From the Elasticsearch query response, collect all `branchPath` and `projectIteration` values from all documents. For example, from the below response, all `branchPath` and
 `projectIteration` combinations are as follows:
  
  branchPath: MAIN/PROJECTA, projectIteration: 0
  
  branchPath: MAIN/PROJECTB, projectIteration: 2 
  
```
{
  "took" : 1,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 2,
      "relation" : "eq"
    },
    "max_score" : 1.632871,
    "hits" : [
      {
        "_index" : "aag-project-criteria",
        "_type" : "_doc",
        "_id" : "MAIN/PROJECTA_0",
        "_score" : 1.632871,
        "_source" : {
          "_class" : "org.snomed.aag.data.domain.ProjectAcceptanceCriteria",
          "key" : "MAIN/PROJECTA_0",
          "branchPath" : "MAIN/PROJECTA",
          "projectIteration" : 0,
          "selectedProjectCriteriaIds" : [
            "project-validation-clean"
          ],
          "selectedTaskCriteriaIds" : [
            "task-review-changes",
            "task-validation-clean"
          ]
        }
      },
      {
        "_index" : "aag-project-criteria",
        "_type" : "_doc",
        "_id" : "MAIN/PROJECTB_2",
        "_score" : 1.632871,
        "_source" : {
          "_class" : "org.snomed.aag.data.domain.ProjectAcceptanceCriteria",
          "key" : "MAIN/PROJECTB_2",
          "branchPath" : "MAIN/PROJECTB",
          "projectIteration" : 2,
          "creationDate" : 1630317903809,
          "selectedProjectCriteriaIds" : [
            "project-validation-clean"
          ],
          "selectedTaskCriteriaIds" : [
            "task-review-changes",
            "task-validation-clean"
          ]
        }
      }
    ]
  }
}

```

#### 3. Update accordingly
To update accordingly, it is best to use the API. It is possible to use an additional Elasticsearch query but that is out of scope of this document.

First, run `GET /criteria/$branchPath?projectIteration=$projectIteration`. See below for an example response.

```

{
    "branchPath": "MAIN/PROJECTA",
    "projectIteration": 0,
    "creationDate": "2021-09-01T12:05:02.287+00:00",
    "selectedProjectCriteriaIds": [
        "project-validation-clean",
        "project-documentation-completed",
        "project-template-validation"
    ],
    "selectedTaskCriteriaIds": [
        "task-manual-spellcheck",
        "task-early-visibility"
    ],
    "creationDateLong": 1630497902287
},

```

Please note the variables `$branchPath` and `$projectIteration` in the request URL. These should be substituted for each combination previously collected. 

Second, run `PUT /criteria/$branchPath?projectIteration=$projectIteration` using the request payload as the response from the previous request. Within the `PUT` request, remove
 the reference to the `CriteriaItem` no longer required. See below for an example. 

```

{
    "branchPath": "MAIN/PROJECTA",
    "projectIteration": 0,
    "creationDate": "2021-09-01T12:05:02.287+00:00",
    "selectedProjectCriteriaIds": [
        "project-validation-clean",
        "project-documentation-completed",
        "project-template-validation"
    ],
    "selectedTaskCriteriaIds": [
        "task-manual-spellcheck"
    ],
    "creationDateLong": 1630497902287
},

```
 
Please note the `CriteriaItem` named `task-early-visibility` has been removed. Again, please note the variables `$branchPath` and `$projectIteration` in the request URL. These
 should be substituted for each combination previously collected.
 
#### 4. Re-run original failed request
Re-running the original failed request `DELETE /criteria-items/$criteriaItemId` will now successfully run as expected.

## Notes 
- If the request continues to fail, please review this document again in case a step has been omitted. If, after review, the `CriteriaItem` still cannot be deleted, please log an
 issue for investigation.

###### Last updated: 03/10/2021 for version 1.2.0-SNAPSHOT. 