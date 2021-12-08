package org.diskproject.client.application;

import org.diskproject.client.application.assertion.AssertionPresenter;
import org.diskproject.client.application.assertion.AssertionView;
import org.diskproject.client.application.terminology.TerminologyPresenter;
import org.diskproject.client.application.terminology.TerminologyView;
import org.diskproject.client.application.home.HomePresenter;
import org.diskproject.client.application.home.HomeView;
import org.diskproject.client.application.hypothesis.HypothesisPresenter;
import org.diskproject.client.application.hypothesis.HypothesisView;
import org.diskproject.client.application.loader.LoaderPresenter;
import org.diskproject.client.application.loader.LoaderView;
import org.diskproject.client.application.loi.LOIPresenter;
import org.diskproject.client.application.loi.LOIView;
import org.diskproject.client.application.tloi.TriggeredLOIPresenter;
import org.diskproject.client.application.tloi.TriggeredLOIView;
import org.diskproject.client.application.users.UserPresenter;
import org.diskproject.client.application.users.UserView;

import org.diskproject.client.application.terminology.MyTerminologyPresenter;
import org.diskproject.client.application.terminology.MyTerminologyView;
import org.diskproject.client.application.hypothesis.MyHypothesesPresenter;
import org.diskproject.client.application.hypothesis.MyHypothesesView;

import com.google.gwt.core.client.GWT;
import com.gwtplatform.mvp.client.gin.AbstractPresenterModule;

public class ApplicationModule extends AbstractPresenterModule {
  
  @Override
  protected void configure() {
    GWT.log("configuring Application Module");
    
    // Main Application
    bindPresenter(ApplicationPresenter.class, ApplicationPresenter.MyView.class, 
        ApplicationView.class, ApplicationPresenter.MyProxy.class);
    
    // Modules
    bindPresenter(HomePresenter.class, HomePresenter.MyView.class,
        HomeView.class, HomePresenter.MyProxy.class);
    bindPresenter(LoaderPresenter.class, LoaderPresenter.MyView.class,
        LoaderView.class, LoaderPresenter.MyProxy.class);
    bindPresenter(UserPresenter.class, UserPresenter.MyView.class,
        UserView.class, UserPresenter.MyProxy.class);
    bindPresenter(HypothesisPresenter.class, HypothesisPresenter.MyView.class,
        HypothesisView.class, HypothesisPresenter.MyProxy.class);
    bindPresenter(LOIPresenter.class, LOIPresenter.MyView.class,
        LOIView.class, LOIPresenter.MyProxy.class);
    bindPresenter(TriggeredLOIPresenter.class, TriggeredLOIPresenter.MyView.class,
        TriggeredLOIView.class, TriggeredLOIPresenter.MyProxy.class);
    bindPresenter(AssertionPresenter.class, AssertionPresenter.MyView.class,
        AssertionView.class, AssertionPresenter.MyProxy.class);
    bindPresenter(TerminologyPresenter.class, TerminologyPresenter.MyView.class,
        TerminologyView.class, TerminologyPresenter.MyProxy.class);
    bindPresenter(MyTerminologyPresenter.class, MyTerminologyPresenter.MyView.class,
        MyTerminologyView.class, MyTerminologyPresenter.MyProxy.class);
    bindPresenter(MyHypothesesPresenter.class, MyHypothesesPresenter.MyView.class,
        MyHypothesesView.class, MyHypothesesPresenter.MyProxy.class);
  }

}