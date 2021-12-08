package org.diskproject.client.components.hypothesis.events;

import org.diskproject.shared.classes.hypothesis.Hypothesis;

import com.google.gwt.event.shared.GwtEvent;

public class HypothesisSaveEvent extends GwtEvent<HypothesisSaveHandler> {

  public static Type<HypothesisSaveHandler> TYPE = new Type<HypothesisSaveHandler>();

  private final Hypothesis hypothesis;

  public HypothesisSaveEvent(Hypothesis hypothesis) {
    this.hypothesis = hypothesis;
  }

  @Override
  public Type<HypothesisSaveHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(HypothesisSaveHandler handler) {
    handler.onSave(this);
  }

  public Hypothesis getHypothesis() {
    return this.hypothesis;
  }
}
