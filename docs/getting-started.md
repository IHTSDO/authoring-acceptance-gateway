# Getting Started
This guide will help you in getting started with the Authoring Acceptance Gateway (AAG).

## Prerequisites
Before you can use AAG, please ensure you have installed & configured the services listed below.

- [Elasticsearch](https://www.elastic.co/elasticsearch/)
- [Snowstorm](https://github.com/IHTSDO/snowstorm)
- [Docker](https://www.docker.com/) (required if building from source)

## Installation
There are two approaches to installing AAG; both of which require running an executable JAR. 

##### Source code
In order to use AAG from the source code, it will first have to be built. To do this, follow the steps listed below.

- Clone the repository.
- Run the command ```mvn clean package```. 

Please note that part of the packaging phase requires access to Docker. Docker can be omitted by running the command ```mvn clean package -DskipTests```.

##### Latest package
Alternatively, you can download and run the latest JAR from the [releases page](https://github.com/IHTSDO/authoring-acceptance-gateway/releases).  

## Usage
The functionality of AAG is accessible by sending requests using HTTP methods. For ease of use, you can use the embedded [Swagger UI](http://localhost:8090/authoring-acceptance-gateway/swagger-ui.html) page.    

## Notes
- Java 11
- Maven 
