package org.diskproject.client.application.tloi;

import java.util.List;

import org.diskproject.client.application.ApplicationSubviewImpl;
import org.diskproject.client.components.list.ListNode;
import org.diskproject.client.components.list.ListWidget;
import org.diskproject.client.components.list.events.ListItemActionEvent;
import org.diskproject.client.components.list.events.ListItemSelectionEvent;
import org.diskproject.client.components.loader.Loader;
import org.diskproject.client.components.tloi.TriggeredLOIViewer;
import org.diskproject.client.place.NameTokens;
import org.diskproject.client.rest.AppNotification;
import org.diskproject.client.rest.DiskREST;
import org.diskproject.shared.classes.loi.TriggeredLOI;
import org.diskproject.shared.classes.loi.TriggeredLOI.Status;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.vaadin.polymer.paper.widget.PaperButton;

public class TriggeredLOIView extends ApplicationSubviewImpl 
  implements TriggeredLOIPresenter.MyView {

  String userid;
  String domain;

  @UiField Loader loader;
  @UiField ListWidget tloilist; 
  @UiField TriggeredLOIViewer viewer;
  @UiField PaperButton reloadbutton;

  Timer timer;
  
  interface Binder extends UiBinder<Widget, TriggeredLOIView> {
  }

  @Inject
  public TriggeredLOIView(Binder binder) {
    initWidget(binder.createAndBindUi(this));
    tloilist.addDeleteAction();
  }
  
  @Override
  public void initializeParameters(String userid, String domain, 
      String[] params, boolean edit, 
      SimplePanel sidebar, SimplePanel toolbar) {
    
    clear();
    
    if(this.userid != userid || this.domain != domain) {
      this.userid = userid;
      this.domain = domain;
      DiskREST.setDomain(domain);
      DiskREST.setUsername(userid);
      viewer.initialize(userid, domain);
    }
    
    this.setHeader(toolbar);    
    this.setSidebar(sidebar);
    
    if(params.length == 0) {
      this.showTLOIList();
    }
    else {
      this.showTLOI(params[0]);
    }
  }
  
  private void clear() {
    loader.setVisible(false);
    viewer.setVisible(false);
    tloilist.setVisible(false);
    reloadbutton.setVisible(false);
    if(timer != null) {
      timer.cancel();
      timer = null;
    }
  }
  
  private void showTLOIList() {
    loader.setVisible(true);
    DiskREST.listTriggeredLOIs(new Callback<List<TriggeredLOI>, Throwable>() {
      @Override
      public void onSuccess(List<TriggeredLOI> result) {
        tloilist.clear();
        for(TriggeredLOI item : result) {
          String description = item.getDescription();
          if(item.getStatus() != null) {
            description += "<div class='"+item.getStatus()+"'>";
            description += "STATUS: "+item.getStatus()+"</div>";
          }
          ListNode node = new ListNode(
              item.getId(),
              item.getName(), 
              description,
              item.getDateCreated(),
              item.getAuthor());
          node.setIcon("icons:explore");
          node.setIconStyle("green");
          tloilist.addNode(node);
        }
        loader.setVisible(false);  
        tloilist.setVisible(true);
      }
      @Override
      public void onFailure(Throwable reason) {
        loader.setVisible(false);   
        AppNotification.notifyFailure(reason.getMessage());
        GWT.log("Failed", reason);
      }
    });    
  }
  
  private void showTLOI(final String tloiId) {
    //loader.setVisible(true);
    DiskREST.getTriggeredLOI(tloiId, new Callback<TriggeredLOI, Throwable>() {
      @Override
      public void onSuccess(TriggeredLOI tloi) {
        loader.setVisible(false);
        if(tloi.getStatus() != null && 
            (tloi.getStatus() == Status.QUEUED ||
            tloi.getStatus() == Status.RUNNING)) {
          reloadbutton.setVisible(true);
          if(timer != null) {
            timer.cancel();
            timer = null;
          }
          timer = new Timer() {
            public void run() {
                showTLOI(tloiId);
            }
          };
          timer.schedule(5000);
        }
        else {
          reloadbutton.setVisible(false);
        }
        
        viewer.setVisible(true);
        viewer.load(tloi);
      }
      @Override
      public void onFailure(Throwable reason) {
        loader.setVisible(false);
        AppNotification.notifyFailure(reason.getMessage());
      }
    });
  }
  
  @UiHandler("tloilist")
  void onListItemSelected(ListItemSelectionEvent event) {
    ListNode node = event.getItem();
    String token = NameTokens.getTLOIs()+"/"+this.userid+"/"+this.domain;
    token += "/" + node.getId();
    History.newItem(token);
  }
  
  @UiHandler("tloilist")
  void onListItemDeleted(ListItemActionEvent event) {
    final ListNode node = event.getItem();
    if(tloilist.getNode(node.getId()) != null) {
      if(Window.confirm("Are you sure you want to delete " + node.getName())) {
        DiskREST.deleteTriggeredLOI(node.getId(), new Callback<Void, Throwable>() {
          @Override
          public void onFailure(Throwable reason) {
            AppNotification.notifyFailure(reason.getMessage());
          }
          @Override
          public void onSuccess(Void result) {
            tloilist.removeNode(node);
          }
        });
      }
    }
  }
  
  @UiHandler("reloadbutton")
  void onReloadClick(ClickEvent event) {
    this.showTLOI(viewer.getTLOI().getId());
  }
  
  private void setHeader(SimplePanel toolbar) {
    // Set Toolbar header
    toolbar.clear();
    String title = "<h3>Triggered Lines of Inquiry</h3>";
    String icon = "icons:explore";

    HTML div = new HTML("<nav><div class='layout horizontal center'>"
        + "<iron-icon style=\"color: #229E6A;\" icon='" + icon + "'/></div></nav>");
    div.getElement().getChild(0).getChild(0).appendChild(new HTML(title).getElement());
    toolbar.add(div);    
  }
  
  private void setSidebar(SimplePanel sidebar) {
    // TODO: Modify sidebar
  }
}
