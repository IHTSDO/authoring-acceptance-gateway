# Permissions and Roles
This document will detail the permissions and roles required for accessing a subset of endpoints within the Authoring Acceptance Gateway (AAG) and interacting with user-created
 `CriteriaItem`s.

## Endpoints
Please see the table below for a subset of endpoints within AAG which have been restricted to users that have certain roles associated with their account. 

The endpoints described below allow for general management and maintenance of data store records, i.e. basic CRUD operations. 

Endpoint | Method | Required role(s) | Java Class | Java Method | Comments 
--- | --- | --- | --- |--- |--- 
/criteria-items | POST | ADMIN | CriteriaItemLibraryController | createCriteriaItem | Creating an individual criterion.
/criteria-items/{id} | PUT | ADMIN | CriteriaItemLibraryController | updateCriteriaItem | Updating an individual criterion
/criteria-items/{id} | DELETE | ADMIN | CriteriaItemLibraryController | deleteCriteriaItem | Deleting an individual criterion
 |  |  |  |  |
 |  |  |  |  |
/criteria | POST | PROJECT_MANAGER | AcceptanceCriteriaController | createProjectCriteria | Creating a group which references many individual criterion.
/criteria/{branch} | PUT | PROJECT_MANAGER | AcceptanceCriteriaController | updateProjectCriteria | Updating a group which references many individual criterion.
/criteria/{branch} | DELETE | ADMIN | AcceptanceCriteriaController | deleteProjectCriteria | Deleting a group which references many individual criterion.
 |  |  |  |  |
 |  |  |  |  |  
/admin/criteria/{branchPath}/accept | POST | ADMIN | AdminController | signOffAllCriteriaItems | Force all criteria associated with a project / task to be accepted, regardless of any `requiredRole`. 
/admin/criteria/{branchPath}/accept | DELETE | ADMIN | AdminController | rejectAllCriteriaItems | Force all criteria associated with a project / task to be rejected, regardless of any `requiredRole`. 
 |  |  |  |  |
 |  |  |  |  |

## Criteria Items
In order to accept or reject a `CriteriaItem` for a given project, the user must first have permission on the individual `CriteriaItem`.

When creating a `CriteriaItem`, the property `requiredRole` is required. This property determines which role the user should have associated with a `Branch` (see Snowstorm). For
 example, if a `CriteriaItem` was created with the `requiredRole` property set to "TEST_AUTHOR", then the user must have "TEST_AUTHOR" included in their `Branch` permissions. 

As the data is user-defined, an exhaustive list cannot be created. However, for reference, test data includes the following: 
 - RELEASE_LEAD
 - AUTHOR

The endpoints involved in accepting or rejecting a `CriteriaItem` are not secured in the same way as the endpoints previously listed in the above table. Instead, the endpoints
 make use of verifying the `requiredRole` property against a `Branch`.

## Notes
- The subet of endpoints which have been secured is done through Spring Security's `PreAuthorize` annotation. As well as referencing the table above for understanding
 restricted endpoints, [please view the search results on GitHub](https://github.com/search?q=PreAuthorize+repo%3AIHTSDO%2Fauthoring-acceptance-gateway&type=code).

###### Last updated: 21/07/2021 for version 1.1.0-SNAPSHOT.