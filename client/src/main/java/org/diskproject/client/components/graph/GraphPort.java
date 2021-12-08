package org.diskproject.client.components.graph;

import com.github.gwtd3.api.Coords;
import com.github.gwtd3.api.D3;
import com.github.gwtd3.api.behaviour.Drag;
import com.github.gwtd3.api.behaviour.Drag.DragEventType;
import com.github.gwtd3.api.core.Selection;
import com.github.gwtd3.api.core.Value;
import com.github.gwtd3.api.functions.DatumFunction;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;

public class GraphPort {
  String id;
  String argid;
  Selection item;
  GraphItemConfig config;
  boolean input;
  boolean eventEnabled = true;
  int duration = 120;
  
  Coords coords;
  double radius;

  public GraphPort(String id, String argid, boolean isInput) {
    // Disambiguation of portids with node ids in older templates
    if(!id.startsWith("port_")) 
      id = "port_" + id;
    this.id = id;
    this.argid = argid;
    this.input = isInput;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Selection getItem() {
    return item;
  }

  public void setItem(Selection item) {
    this.item = item;
  }
  
  public String getArgid() {
    return argid;
  }

  public void setArgid(String argid) {
    this.argid = argid;
  }

  public boolean isInput() {
    return input;
  }

  public void setInput(boolean input) {
    this.input = input;
  }

  public boolean isEventEnabled() {
    return eventEnabled;
  }

  public GraphItemConfig getConfig() {
    return config;
  }

  public void setConfig(GraphItemConfig config) {
    this.config = config;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public Coords getCoords() {
    return coords;
  }

  public void setCoords(Coords coords) {
    this.coords = coords;
  }

  public double getRadius() {
    return radius;
  }

  public void setRadius(double radius) {
    this.radius = radius;
  }

  public void setEventEnabled(boolean eventEnabled) {
    this.eventEnabled = eventEnabled;
  }

  public void draw(final GraphItem gitem,
      final double x, final double y, final double r, String color) {
    this.coords = Coords.create(x, y);
    this.radius = r;
    
    this.item = gitem.getItem().append("circle")
        .attr("id", id)
        .attr("cx",  x)
        .attr("cy", y)
        .attr("r", r)
        .attr("stroke", config.getStrokecolor())
        .attr("stroke-width", 0.5)
        .attr("fill", color);   
    
    if(this.isEventEnabled()) {
      this.item.style("cursor", "pointer");
      this.item.on(BrowserEvents.MOUSEOVER, new DatumFunction<Void>() {
        @Override
        public Void apply(Element context, Value d, int index) {
          item.transition().duration(duration).attr("r", r*2);
          return null;
        }
      });
      this.item.on(BrowserEvents.MOUSEOUT, new DatumFunction<Void>() {
        @Override
        public Void apply(Element context, Value d, int index) {
          item.transition().duration(duration).attr("r", r);
          return null;
        }
      });
      
      Drag drag = D3.behavior().drag();
      drag.on(DragEventType.DRAG,
          new DatumFunction<Void>() {
            Selection tmplink = null;
            
            @Override
            public Void apply(Element context, Value d, int index) {
              Coords pos = D3.eventAsCoords();
              if(tmplink != null)
                tmplink.remove();
              
              Coords c = gitem.getCoords();
              tmplink = gitem.getParentitem().insert("line", "g")
                .attr("x1", x).attr("y1", y)
                .attr("x2", pos.x()).attr("y2", pos.y())               
                .attr("stroke", "rgba(0,0,0,0.9)")
                .attr("stroke-width", 0.6)
                .attr("transform", "translate("+c.x()+","+c.y()+")")
                .attr("marker-end","url(#arrow)");
              return null;
            }
      });
      item.on(BrowserEvents.MOUSEUP, new DatumFunction<Void>() {
            @Override
            public Void apply(Element context, Value d, int index) {
              GWT.log("stopped over " + argid);
              return null;
            }
      });
      item.call(drag);      
    }

  }
}
