package org.diskproject.client.application;

import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;

import org.diskproject.client.authentication.KeycloakUser;
import org.diskproject.client.place.NameTokens;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;
import com.vaadin.polymer.Polymer;
import com.vaadin.polymer.elemental.Function;
import com.vaadin.polymer.paper.PaperDrawerPanelElement;
import com.vaadin.polymer.paper.PaperToastElement;

public class ApplicationView extends ViewImpl implements
    ApplicationPresenter.MyView {
  interface Binder extends UiBinder<Widget, ApplicationView> { }

  @UiField public static PaperDrawerPanelElement drawer;
  @UiField public static SimplePanel contentContainer;
  @UiField public static PaperToastElement toast;
  
  @UiField public static DivElement 
    hypothesesMenu, loisMenu, terminologyMenu, assertionsMenu, myHypothesesMenu, myTerminologyMenu;
  
  @UiField SimplePanel sidebar;
  @UiField SimplePanel toolbar;
  @UiField DivElement userDiv;

  @Inject
  public ApplicationView(Binder binder) {
    initWidget(binder.createAndBindUi(this));
  }

  @Override
  public void setInSlot(Object slot, IsWidget content) {
    if (slot == ApplicationPresenter.CONTENT_SLOT)
      contentContainer.setWidget(content);
    else
      super.setInSlot(slot, content);
  }
  
  public void initializeParameters(String userid, String domain, 
      String[] params, boolean edit, 
      SimplePanel sidebar, SimplePanel toolbar) {
    this.initializeParameters(userid, domain, null, params, 
        edit, sidebar, toolbar);
  }

  public void initializeParameters(String userid, String domain, 
      final String nametoken, final String[] params, boolean edit, 
      SimplePanel sidebar, SimplePanel toolbar) {
    toolbar.clear();
    toolbar.add(new HTML("<h3>NeuroDISK: An AI Discovery System Driven by Your Questions</h3>"));
     
    Polymer.ready(drawer, new Function<Object, Object>() {
      @Override
      public Object call(Object arg) {
        drawer.closeDrawer();
        hypothesesMenu.removeClassName("activeMenu");
        loisMenu.removeClassName("activeMenu");
        //tloisMenu.removeClassName("activeMenu");
        assertionsMenu.removeClassName("activeMenu");
        terminologyMenu.removeClassName("activeMenu");
        myHypothesesMenu.removeClassName("activeMenu");
        myTerminologyMenu.removeClassName("activeMenu");
        
        DivElement menu = null;
        if(nametoken.equals(NameTokens.hypotheses))
          menu = hypothesesMenu;
        else if(nametoken.equals(NameTokens.lois))
          menu = loisMenu;
        else if(nametoken.equals(NameTokens.tlois))
          //menu = tloisMenu;
          menu = hypothesesMenu;
        else if(nametoken.equals(NameTokens.assertions))
          menu = assertionsMenu;
        else if(nametoken.equals(NameTokens.terminology))
          menu = terminologyMenu;
        else if(nametoken.equals(NameTokens.myTerminology))
          menu = myTerminologyMenu;
        else if(nametoken.equals(NameTokens.myHypotheses))
          menu = myHypothesesMenu;
        
        clearMenuClasses(hypothesesMenu);
        clearMenuClasses(loisMenu);
        //clearMenuClasses(tloisMenu);
        clearMenuClasses(assertionsMenu);
        clearMenuClasses(terminologyMenu);
        clearMenuClasses(myHypothesesMenu);
        clearMenuClasses(myTerminologyMenu);
        
        if(menu != null) {
          menu.addClassName("activeMenu");
          if(params.length > 0) {
            addClassToMenus("hiddenMenu");
            menu.addClassName("activeItemMenu");
            menu.removeClassName("hiddenMenu");
          }
        }
        userDiv.setInnerText("Logged in as " + KeycloakUser.getUsername());
        return null;
      }
    });
  }
  
  @UiHandler("logoutButton")
  void onLogoutButtonClicked(ClickEvent event) {
	  KeycloakUser.kc.logout();
  }

  private void clearMenuClasses(DivElement menu) {
    menu.removeClassName("activeMenu");
    menu.removeClassName("hiddenMenu");
    menu.removeClassName("activeItemMenu");    
  }

  private void addClassToMenus(String cls) {
    hypothesesMenu.addClassName(cls);
    loisMenu.addClassName(cls);
    //tloisMenu.addClassName(cls);
    //assertionsMenu.addClassName(cls);
    terminologyMenu.addClassName(cls);
  }

}