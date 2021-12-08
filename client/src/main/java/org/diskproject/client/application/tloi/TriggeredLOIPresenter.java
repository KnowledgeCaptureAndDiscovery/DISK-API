package org.diskproject.client.application.tloi;

import org.diskproject.client.application.ApplicationPresenter;
import org.diskproject.client.application.ApplicationSubview;
import org.diskproject.client.place.NameTokens;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;

public class TriggeredLOIPresenter extends
    Presenter<TriggeredLOIPresenter.MyView, TriggeredLOIPresenter.MyProxy> {

  @ProxyCodeSplit
  @NameToken(NameTokens.tlois)
  public interface MyProxy extends ProxyPlace<TriggeredLOIPresenter> {
  }

  public interface MyView extends ApplicationSubview {
  }

  @Inject
  public TriggeredLOIPresenter(EventBus eventBus, MyView view, MyProxy proxy) {
    super(eventBus, view, proxy, ApplicationPresenter.CONTENT_SLOT);
  }
  
}
