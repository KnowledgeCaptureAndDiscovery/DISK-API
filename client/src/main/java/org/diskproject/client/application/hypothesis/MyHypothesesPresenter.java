package org.diskproject.client.application.hypothesis;

import org.diskproject.client.application.ApplicationPresenter;
import org.diskproject.client.application.ApplicationSubview;
import org.diskproject.client.place.NameTokens;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;

public class MyHypothesesPresenter extends
    Presenter<MyHypothesesPresenter.MyView, MyHypothesesPresenter.MyProxy> {

  @ProxyCodeSplit
  @NameToken(NameTokens.myHypotheses)
  public interface MyProxy extends ProxyPlace<MyHypothesesPresenter> {
  }

  public interface MyView extends ApplicationSubview {
  }

  @Inject
  public MyHypothesesPresenter(EventBus eventBus, MyView view, MyProxy proxy) {
    super(eventBus, view, proxy, ApplicationPresenter.CONTENT_SLOT);
  }
  
}
