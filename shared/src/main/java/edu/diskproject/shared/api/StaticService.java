package edu.diskproject.shared.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("public")
public interface StaticService {
	@GET
	@Path("{path:.*}")
	public Response staticResources(@PathParam("path") final String path);
}
