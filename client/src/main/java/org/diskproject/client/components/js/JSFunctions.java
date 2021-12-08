package org.diskproject.client.components.js;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.vaadin.polymer.paper.PaperDialogScrollableElement;
import com.vaadin.polymer.paper.PaperListboxElement;

public class JSFunctions {

  public static native double[] calculateTextBounds(Element el) /*-{
    var bbox = el.getBBox();
    return [bbox.x, bbox.y, bbox.width, bbox.height];
  }-*/;
  
  @SuppressWarnings("rawtypes")
  public static native void setMenuItems(PaperListboxElement menu, JsArray names) /*-{
		$wnd.Polymer.dom(menu).innerText = "";
		for (var i = 0; i < names.length; i++) {
			var item = $wnd.document.createElement("paper-item");
			item.innerText = names[i];
			$wnd.Polymer.dom(menu).appendChild(item);
		}
  }-*/;
  
  public static native void clearDialog(PaperDialogScrollableElement el) /*-{
    $wnd.Polymer.dom(el).innerHTML = "";
  }-*/;  
  
  public static native void addToDialog(PaperDialogScrollableElement el, 
      Element item) /*-{
    $wnd.Polymer.dom(el).appendChild(item);        
  }-*/;  
}
