# Changelog
All notable changes to this project will be documented in this file.

- This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
- The format of this changelog is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## 1.1.2 (September 2021)

### Added
- Changelog
- Permissions & roles document
- Admin endpoint for marking all `CriteriaItem` for a ProjectAcceptanceCriteria complete
- Admin endpoint for marking all `CriteriaItem` for a ProjectAcceptanceCriteria incomplete

### Breaking
- N/A

### Changed
- ServiceIntegrationController to confirm whether `ProjectAcceptanceCriteria` has all relevant `CriteriaItem` marked as complete when actioning a promotion.
- ServiceIntegrationController to potentially mark relevant `CriteriaItem` as incomplete when actioning a promotion.
- AcceptanceCriteriaController to use latest `projectIteration` if not otherwise specified when updating `ProjectAcceptanceCriteria`. 
- Version of ihtsdo-spring-sso dependency from 2.1.0 to 2.2.0.

### Deprecated
- N/A

### Fixed
- Bug where `creationDate` field of `ProjectAcceptanceCriteria` was being changed when updating the `ProjectAcceptanceCriteria`.
- Bug where  incorrect `CriteriaItem` were being marked as incomplete when actioning a rebase.

### Removed
- N/A

## 1.0.0 (June 28th 2021)
This release is the first release of Authoring Acceptance Gateway. Within this release, the foundations of the service have been laid. This includes various web endpoints as
 well as build tools for taking the service through to deployment/distribution (i.e. Debian packaging).
 
### Added
- Functionality to perform general CRUD operations for `ProjectAcceptanceCriteria`.
- Functionality to perform general CRUD operations for `CriteriaItem`.
- Functionality to perform general CRUD operations for `WhitelistItem`.
- Functionality to handle uncaught exceptions via `ControllerAdvice`.
- Functionality to accept or reject a `CriteriaItem` for a specific branch.
- Redirect to Swagger UI when requesting root endpoint.
- Endpoint for receiving requests from [Snowstorm](https://github.com/IHTSDO/snowstorm).
- Endpoint for retrieving current version of running application.

### Breaking
- N/A

### Changed
- N/A

### Deprecated
- N/A

### Fixed
- N/A

### Removed
- N/A
