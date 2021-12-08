package org.diskproject.client.application.assertion;

import org.diskproject.client.application.ApplicationSubviewImpl;
import org.diskproject.client.rest.DiskREST;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AssertionView extends ApplicationSubviewImpl implements
		AssertionPresenter.MyView {

	String userid;
	String domain;

	interface Binder extends UiBinder<Widget, AssertionView> {
	}

	@Inject
	public AssertionView(Binder binder) {
		initWidget(binder.createAndBindUi(this));
	}

	@Override
	public void initializeParameters(String userid, String domain,
			String[] params, boolean edit, SimplePanel sidebar,
			SimplePanel toolbar) {

		clear();

		 if(this.userid != userid || this.domain != domain) {
		      this.userid = userid;
		      this.domain = domain;
		      DiskREST.setDomain(domain);
		      DiskREST.setUsername(userid);
		    }
		    
		    this.setHeader(toolbar);    
		    this.setSidebar(sidebar);
		  }

	private void clear() {
	}

	private void setHeader(SimplePanel toolbar) {
		// Set Toolbar header
		toolbar.clear();
		String title = "<h3>Data</h3>";
		String icon = "icons:list";

		HTML div = new HTML("<nav><div class='layout horizontal center'>"
				+ "<iron-icon class='blue' icon='" + icon + "'/></div></nav>");
		div.getElement().getChild(0).getChild(0)
				.appendChild(new HTML(title).getElement());
		toolbar.add(div);
	}

	private void setSidebar(SimplePanel sidebar) {
		// TODO: Modify sidebar
	}

}
