# Authoring Acceptance Gateway 
Microservice to ensure service acceptance criteria are met before content promotion within the SNOMED CT Authoring Platform.

## Project Acceptance Criteria
Acceptance criteria are defined at the project level. Projects typically exist within a branch that is a direct child of a code system branch (e.g. `MAIN` or 
`MAIN/SNOMEDCT-BE`). Each project acceptance criteria contains two sets of criteria items which must be signed off at project and task level respectively.

### Criteria Items
Criteria items represent any criteria which must be met for the authoring to be accepted. These may include classification, peer review, spellchecking, mapping etc.

#### Manual Sign-Off
All Criteria items with the `manual` flag set must be accepted manually.

#### Automatic Sign-Off
Some Criteria Items, those without the `manual` flag, are signed off automatically by the system by listening to various events.
These include  `project-clean-classification` and `task-clean-classification`.

#### Automatic Expiration
Any Criteria Item which has the `expiresOnCommit` flag set to try will have any existing Sign-Off deleted when a new commit is made on that branch.

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.4.3/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.4.3/maven-plugin/reference/html/#build-image)
