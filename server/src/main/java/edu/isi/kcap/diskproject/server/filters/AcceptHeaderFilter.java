package edu.isi.kcap.diskproject.server.filters;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;

@PreMatching
public class AcceptHeaderFilter implements ContainerRequestFilter {

  public void filter(ContainerRequestContext requestContext)
      throws IOException {

    MultivaluedMap<String, String> headers = requestContext.getHeaders();
    List<String> accept_headers = headers.get("Accept");
    if (!accept_headers.contains("application/rdf+xml")
        && !accept_headers.contains("*/*")
        && !accept_headers.contains("application/xml"))
      accept_headers.add(0, "application/json");
  }

}
