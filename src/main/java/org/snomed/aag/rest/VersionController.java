package org.snomed.aag.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.snomed.aag.rest.pojo.BuildVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Version", description = "Build Version")
public class VersionController {

	@Autowired(required = false)
	private BuildProperties buildProperties;

	@ApiOperation("Software build version and timestamp.")
	@GetMapping(value = "/version", produces = "application/json")
	public BuildVersion getBuildInformation() {
	    if (buildProperties == null) {
	        throw new IllegalStateException("Build properties are not present.");
        }
		return new BuildVersion(buildProperties.getVersion(), buildProperties.getTime().toString());
	}

}
