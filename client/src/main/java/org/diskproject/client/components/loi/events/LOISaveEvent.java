package org.diskproject.client.components.loi.events;

import org.diskproject.shared.classes.loi.LineOfInquiry;

import com.google.gwt.event.shared.GwtEvent;

public class LOISaveEvent extends GwtEvent<LOISaveHandler> {

  public static Type<LOISaveHandler> TYPE = new Type<LOISaveHandler>();

  private final LineOfInquiry loi;

  public LOISaveEvent(LineOfInquiry loi) {
    this.loi = loi;
  }

  @Override
  public Type<LOISaveHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(LOISaveHandler handler) {
    handler.onSave(this);
  }

  public LineOfInquiry getLOI() {
    return this.loi;
  }
}
