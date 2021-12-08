package org.diskproject.client.gin;

import org.diskproject.client.application.ApplicationModule;
import org.diskproject.client.place.NameTokens;

import com.gwtplatform.mvp.client.annotations.DefaultPlace;
import com.gwtplatform.mvp.client.annotations.ErrorPlace;
import com.gwtplatform.mvp.client.annotations.UnauthorizedPlace;
import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;
import com.gwtplatform.mvp.client.gin.DefaultModule;

public class MainModule extends AbstractPresenterModule {
  @Override
  protected void configure() {
    // Singletons
    install(new DefaultModule());
    install(new ApplicationModule());

    // Constants
    bindConstant().annotatedWith(DefaultPlace.class).to(NameTokens.home);
    bindConstant().annotatedWith(ErrorPlace.class).to(NameTokens.loader);
    bindConstant().annotatedWith(UnauthorizedPlace.class).to(NameTokens.loader);
  }
}
