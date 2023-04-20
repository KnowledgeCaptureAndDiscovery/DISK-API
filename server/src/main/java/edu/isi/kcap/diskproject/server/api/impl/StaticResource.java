package edu.isi.kcap.diskproject.server.api.impl;

import java.io.InputStream;
import java.util.Objects;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import edu.isi.kcap.diskproject.shared.api.StaticService;

@Path("public")
public class StaticResource implements StaticService {
	@Inject
	ServletContext context;

	@GET
	@Path("{path:.*}")
	@Override
	public Response staticResources(@PathParam("path") final String path) {
		InputStream resource = context.getResourceAsStream(String.format("/public/%s", path));

		return Objects.isNull(resource)
				? Response.status(404).build()
				: Response.ok(resource, MediaType.APPLICATION_JSON).build();
		// : Response.ok().entity(resource).build();
	}
}