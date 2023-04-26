package org.diskproject.server;

import javax.annotation.PreDestroy;

import org.diskproject.server.api.impl.DiskResource;
import org.diskproject.server.api.impl.StaticResource;
import org.diskproject.server.filters.AcceptHeaderFilter;
import org.diskproject.server.filters.CORSResponseFilter;
import org.diskproject.server.filters.KeycloakAuthenticationFilter;
import org.diskproject.server.repository.DiskRepository;
import org.diskproject.server.util.Config;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

class DiskServer extends ResourceConfig {

  public DiskServer() {
    Config.load();
    DiskRepository.get(); // To create the server first

    register(AcceptHeaderFilter.class);
    register(CORSResponseFilter.class);
    register(RolesAllowedDynamicFeature.class);
    register(KeycloakAuthenticationFilter.class);
    register(DiskResource.class);
    register(StaticResource.class);
  }

  @PreDestroy
  public void onDestroy() {
    // Cleanup tasks
    DiskRepository.get().shutdownExecutors();
  }
}