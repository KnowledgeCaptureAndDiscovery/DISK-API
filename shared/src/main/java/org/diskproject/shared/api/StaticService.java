package org.diskproject.shared.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.fusesource.restygwt.client.DirectRestService;

@Path("public")
public interface StaticService extends DirectRestService {
	@GET
	@Path("{path:.*}")
	public Response staticResources(@PathParam("path") final String path);
}
