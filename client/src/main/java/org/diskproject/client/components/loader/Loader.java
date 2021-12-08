package org.diskproject.client.components.loader;

import com.vaadin.polymer.iron.widget.IronFlexLayout;

public class Loader extends IronFlexLayout {

  public Loader(String html) {
    super("<div style=\"height: 40px; display: flex; align-items: center; justify-content: center;\"><h4>Loading... </h4></div>");
  }
}
