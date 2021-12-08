package org.diskproject.client.application.terminology;

import org.diskproject.client.application.ApplicationPresenter;
import org.diskproject.client.application.ApplicationSubview;
import org.diskproject.client.place.NameTokens;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;

public class TerminologyPresenter extends
    Presenter<TerminologyPresenter.MyView, TerminologyPresenter.MyProxy> {

  @ProxyCodeSplit
  @NameToken(NameTokens.terminology)
  public interface MyProxy extends ProxyPlace<TerminologyPresenter> {
  }

  public interface MyView extends ApplicationSubview {
  }

  @Inject
  public TerminologyPresenter(EventBus eventBus, MyView view, MyProxy proxy) {
    super(eventBus, view, proxy, ApplicationPresenter.CONTENT_SLOT);
  }
  
}
