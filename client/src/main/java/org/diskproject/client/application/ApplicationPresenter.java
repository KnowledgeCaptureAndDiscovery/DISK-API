package org.diskproject.client.application;

import java.util.Arrays;

import org.diskproject.client.application.assertion.AssertionView;
import org.diskproject.client.application.hypothesis.HypothesisView;
import org.diskproject.client.application.hypothesis.MyHypothesesView;
import org.diskproject.client.application.loi.LOIView;
import org.diskproject.client.application.terminology.MyTerminologyView;
import org.diskproject.client.application.terminology.TerminologyView;
import org.diskproject.client.application.tloi.TriggeredLOIView;
import org.diskproject.client.application.users.UserView;
import org.diskproject.client.place.NameTokens;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.presenter.slots.NestedSlot;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;

public class ApplicationPresenter extends
    Presenter<ApplicationPresenter.MyView, ApplicationPresenter.MyProxy> {

  @ProxyCodeSplit
  public interface MyProxy extends Proxy<ApplicationPresenter> {
  }

  public interface MyView extends ApplicationSubview {
  }

  public static final NestedSlot CONTENT_SLOT = new NestedSlot();

  @Inject
  public ApplicationPresenter(EventBus eventBus, final MyView view, MyProxy proxy,
      final PlaceManager placemanager, final ApplicationView appview,
      final UserView userview, 
      final HypothesisView hypview,
      final LOIView loiview,
      final AssertionView assview,
      final TerminologyView termview,
      final MyTerminologyView mytermview,
      final MyHypothesesView myhypview,
      final TriggeredLOIView tloiview
     ) {
    super(eventBus, view, proxy, RevealType.Root);
    
    final PlaceRequest.Builder builder = new PlaceRequest.Builder();
    
    // Add history change handler
    History.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(final ValueChangeEvent<String> event) {
        boolean edit=false;
        String userid=null, domain=null;
        String[] params = new String[] {};
        
        String token = event.getValue();
        if(token.endsWith("?edit")) {
          token = token.replaceAll("\\?edit$", "");
          edit = true;
        }
        String[] tokens = token.split("/");
        String nametoken = tokens[0];
        if (tokens.length >= 3) {
          userid = tokens[1];
          domain = tokens[2];
          params = Arrays.copyOfRange(tokens, 3, tokens.length);
        }
        else if(tokens.length >= 2) {
          userid = tokens[1];
          params = Arrays.copyOfRange(tokens, 2, tokens.length);          
        }
      
        ApplicationSubview sectionview = null;
        if(nametoken.equals(NameTokens.users))
          sectionview = userview;
        else if(nametoken.equals(NameTokens.hypotheses))
          sectionview = hypview;
        else if(nametoken.equals(NameTokens.assertions))
          sectionview = assview;
        else if(nametoken.equals(NameTokens.lois))
          sectionview = loiview;
        else if(nametoken.equals(NameTokens.tlois))
          sectionview = tloiview;              
        else if(nametoken.equals(NameTokens.terminology))
          sectionview = termview;
        else if(nametoken.equals(NameTokens.myTerminology))
          sectionview = mytermview;
        else if(nametoken.equals(NameTokens.myHypotheses))
          sectionview = myhypview;

        // Tell application view about the view being loaded in case it wants to change something
        ((ApplicationView)view).initializeParameters(userid, domain, nametoken, params, 
            edit, appview.sidebar, appview.toolbar);
        
        // Login if needed
        
        // Reveal called view with parameters & sidebar/toolbar to populate
        if(sectionview != null) {
          placemanager.revealPlace(builder.nameToken(nametoken).build(), false);
          sectionview.initializeParameters(userid, domain, params, edit, appview.sidebar, appview.toolbar);
        }
      }
    });
    
    if(History.getToken() !=  null)
      History.fireCurrentHistoryState();
  }
}
