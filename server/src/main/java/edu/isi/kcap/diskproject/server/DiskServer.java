package edu.isi.kcap.diskproject.server;

import javax.annotation.PreDestroy;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import edu.isi.kcap.diskproject.server.api.impl.DiskResource;
import edu.isi.kcap.diskproject.server.api.impl.StaticResource;
import edu.isi.kcap.diskproject.server.filters.AcceptHeaderFilter;
import edu.isi.kcap.diskproject.server.filters.CORSResponseFilter;
import edu.isi.kcap.diskproject.server.filters.KeycloakAuthenticationFilter;
import edu.isi.kcap.diskproject.server.repository.DiskRepository;
import edu.isi.kcap.diskproject.server.util.Config;

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