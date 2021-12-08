package org.diskproject.client.components.searchpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.diskproject.client.Utils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.polymer.paper.widget.PaperIconButton;

public class SearchPanel extends Composite {
	interface Binder extends UiBinder<Widget, SearchPanel> {};
	private static Binder uiBinder = GWT.create(Binder.class);
	
	private static String [] orderTypes = {"Date", "Date (asc)"};
  
	@UiField PaperIconButton clearSearchButton;
	@UiField DivElement itemContainer;
	@UiField TextBox inputSearch;
	@UiField ListBox orderMenu;
	
	private String activeOrder;
	private Map<String, SearchableItem> items;
	private Map<String, Boolean> visibleItems;

	public SearchPanel () {
		initWidget(uiBinder.createAndBindUi(this)); 
		items = new HashMap<String, SearchableItem>();
		visibleItems = new HashMap<String, Boolean>();
		for (int i = 0; i < orderTypes.length; i++) {
			orderMenu.addItem(orderTypes[i]);
		}
	}
	
	@UiHandler("clearSearchButton")
	void onClearSearchButtonClicked(ClickEvent event) {
	    inputSearch.setValue("");
	    onSearchChange(null);
	}

	@UiHandler("inputSearch")
	void onSearchChange(KeyUpEvent event) {
		String searchStr = inputSearch.getValue().toLowerCase();
		for (String key: items.keySet()) {
			if (searchStr.equals("")) {
				visibleItems.put(key, true);
			} else {
				String fullText = items.get(key).getTextRepresentation();
				if (fullText.toLowerCase().contains(searchStr)) {
					visibleItems.put(key, true);
				} else {
					visibleItems.put(key, false);
				}
			}
		}
		updateList();
	}

	@UiHandler("orderMenu")
	void onOrderClicked(ChangeEvent  event) {
		String order = orderMenu.getSelectedItemText();
		if (activeOrder == null || !activeOrder.equals(order)) {
			activeOrder = order;
			updateList();
		}
	}
	
	public List<SearchableItem> getOrderedItems () {
	    List<SearchableItem> all = new ArrayList<SearchableItem>( items.values() );
	    if (activeOrder != null) {
			if (activeOrder.equals("Date (asc)")) {
			    Collections.sort(all, Utils.orderAscDate);
			} else if (activeOrder.equals("Date")) {
			    Collections.sort(all, Utils.orderDesDate);
			}
	    }
	    return all;
	}
	
	public void addItem (String id, SearchableItem item) {
	    item.addStyleName("searchable-result");
		items.put(id, item);
		visibleItems.put(id, true);
		updateList();
	}

	public SearchableItem getItem (String id) {
		return items.containsKey(id) ? items.get(id) : null;
	}
	
	private void updateList () {
		itemContainer.removeAllChildren();
		//TODO Apply order
		List<SearchableItem> ordered = this.getOrderedItems();
		for (SearchableItem item : ordered) {
		    if (visibleItems.get(item.getId()))
		        itemContainer.appendChild( item.getElement() );
		}
		/* TODO: show something if:
		 * - catalog empty
		 * - loading
		 * - search params returns nothing
		 */
	}
}