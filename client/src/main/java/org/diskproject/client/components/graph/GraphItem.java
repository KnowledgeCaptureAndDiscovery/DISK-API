package org.diskproject.client.components.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

import org.diskproject.client.components.js.JSFunctions;

import com.github.gwtd3.api.Coords;
import com.github.gwtd3.api.D3;
import com.github.gwtd3.api.behaviour.Drag;
import com.github.gwtd3.api.behaviour.Drag.DragEventType;
import com.github.gwtd3.api.core.Selection;
import com.github.gwtd3.api.core.Value;
import com.github.gwtd3.api.functions.DatumFunction;
import com.google.gwt.dom.client.Element;

public abstract class GraphItem {
  String id;
  String text;
  
  Selection parentitem;
  Selection item;
  Selection textitem;
  List<Selection> bgitems;
  
  int dimensionality;
  boolean eventEnabled = true;
  
  Coords coords;
  double width, height; // Calculated from Text size & config (padding, etc)
  
  Map<String, GraphPort> inputPorts;
  Map<String, GraphPort> outputPorts;
  GraphItemConfig config;
  
  public GraphItem(String id, Selection parentitem) {
    this.id = id;
    this.text = id;
    this.parentitem = parentitem;
    
    this.dimensionality = 0;
    this.config = new GraphItemConfig();
    this.coords = Coords.create(0, 0);
    this.inputPorts = new TreeMap<String, GraphPort>();
    this.outputPorts = new TreeMap<String, GraphPort>();
    this.initializeItem();
  }

  private void initializeItem() {
    if(this.item != null)
      this.item.remove();
    this.item = this.parentitem.append("g").attr("id", this.id);
  }
  
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Selection getItem() {
    return item;
  }

  public void setItem(Selection item) {
    this.item = item;
  }

  public int getDimensionality() {
    return this.dimensionality;
  }

  public void setDimensionality(int dimensionality) {
    this.dimensionality = dimensionality;
  }

  public boolean isEventEnabled() {
    return eventEnabled;
  }

  public void setEventEnabled(boolean eventEnabled) {
    this.eventEnabled = eventEnabled;
  }

  public Selection getParentitem() {
    return parentitem;
  }

  public void setParentitem(Selection parentitem) {
    this.parentitem = parentitem;
  }

  public double getX() {
    return this.coords.x();
  }

  public void setX(double x) {
    this.setCoords(Coords.create(x, this.getY()));
  }

  public double getY() {
    return this.coords.y();
  }

  public void setY(double y) {
    this.setCoords(Coords.create(this.getX(), y));
  }
  
  public Coords getCoords() {
    return this.coords;
  }
  
  public void setCoords(Coords coords) {
    if(coords.x() < width/2)
      coords.x(width/2);
    if(coords.y() < (height/2 + config.portsize*2))
      coords.y(height/2 + config.portsize*2);
    if(item != null)
      item.attr("transform", "translate("+ coords.x() +","+ coords.y() +")");
    this.coords = coords;
  }

  public Map<String, GraphPort> getInputPorts() {
    return inputPorts;
  }

  public void setInputPorts(Map<String, GraphPort> inputPorts) {
    this.inputPorts.clear();
    this.inputPorts.putAll(inputPorts);
  }

  public void addInputPort(GraphPort port) {
    this.inputPorts.put(port.getId(), port);
  }
  
  public  Map<String, GraphPort> getOutputPorts() {
    return outputPorts;
  }

  public void setOutputPorts(Map<String, GraphPort> outputPorts) {
    this.outputPorts.clear();
    this.outputPorts.putAll(outputPorts);
  }
  
  public void addOutputPort(GraphPort port) {
    this.outputPorts.put(port.getId(), port);
  }

  public double getWidth() {
    return this.width;
  }

  public double getHeight() {
    return this.height;
  }

  public GraphItemConfig getConfig() {
    return config;
  }

  public void setConfig(GraphItemConfig config) {
    this.config = config;
  }
  
  // Needs to be overridden by subclasses
  protected abstract Selection drawBackground(double x, double y, double w, double h);
  
  // X and Y refer to mid-point of the graph item
  public void draw() {
    // Draw Text (Center aligned)
    this.textitem = this.drawText(0, 0);
    
    // Calculate item's top-left corner from text size (to pass to drawBackground)
    double[] bounds = JSFunctions.calculateTextBounds(this.textitem.node());
    double tlx = bounds[0] - config.getXpad();
    double tly = bounds[1] - config.getYpad();
    
    // Calculate item's width and height
    this.width = bounds[2] + config.getXpad()*2;
    this.height = bounds[3] + config.getYpad()*2;
    
    // Check if ports can fit inside
    // - else increase width and fix top-left corner
    double pwidth = this.getPortsWidth();
    if(pwidth > this.width) {
      tlx = tlx - (pwidth - this.width)/2;
      this.width = pwidth;
    }

    // Draw a stack
    this.bgitems = new ArrayList<Selection>();
    int dim = this.dimensionality > 0 ? this.dimensionality + 1 : 0;
    for(int i=0; i<=dim; i++) {
      double spacing = (dim-i)*config.getStackspacing();
      double opacity = 1-(dim-i)*0.2;
      Selection bgitem = 
          this.drawBackground(tlx+spacing, tly+spacing, this.width, this.height);
      bgitem.style("opacity", opacity);
      if(i != dim)
        bgitem.style("fill", "white");
      bgitems.add(bgitem);
    }
    Collections.reverse(this.bgitems);
    
    // Draw ports
    this.drawPorts(tlx, tly, this.width, this.height);
    
    // Position the item
    this.setCoords(Coords.create(coords.x()+this.width/2, coords.y()+this.height/2));
    
    // Set events
    if(this.isEventEnabled()) {
      // Dragger
      this.bgitems.get(0).style("cursor", "pointer");
      final Drag drag = D3.behavior().drag().origin(new DatumFunction<Coords>() {
        @Override
        public Coords apply(Element context, Value d, int index) {
          return Coords.create(0, 0);
        }
      });
      drag.on(DragEventType.DRAG,
          new DatumFunction<Void>() {
            @Override
            public Void apply(Element context, Value d, int index) {
              // TODO: Fire item drag event
              Coords pos = D3.eventAsCoords();
              double newx = coords.x() + pos.x();
              double newy = coords.y() + pos.y();
              setCoords(Coords.create(newx, newy));
              return null;
            }
      });
      /*drag.on(DragEventType.DRAGEND, new DatumFunction<Void>() {
            @Override
            public Void apply(Element context, Value d, int index) {
              for(GraphLink glink : wgraph.links.values()) 
                glink.draw();
              return null;
            }
      });*/
      this.bgitems.get(0).call(drag);
    }
  }
  
  private Selection drawText(double x, double y) {
    return item.append("text")
        .attr("x", x).attr("y", y)
        .attr("dy", ".35em") 
        .attr("pointer-events", "none")
        .style("text-anchor", "middle")
        .style("font-size", config.getFontsize()+"px")
        .style("font-family", config.getFont())
        .style("font-weight", config.getFontweight())          
        .style("fill", config.getTextcolor())
        .text(this.text);
  }

  private double getPortsWidth() {
    double portsize = config.getPortsize();
    double portpad = config.getPortpad();
    double ipsize = this.inputPorts.size()*(portsize+portpad) + portpad;
    double opsize = this.outputPorts.size()*(portsize+portpad) + portpad;
    double psize = ipsize > opsize ? ipsize :opsize;
    return psize;
  }
  
  private void drawPorts(double x, double y, double w, double h) {
    double portsize = config.getPortsize();
    double portpad = config.getPortpad();
    double ipsize = this.inputPorts.size()*(portsize+portpad) + portpad;
    double opsize = this.outputPorts.size()*(portsize+portpad) + portpad;
    
    String color = config.getBgcolor();
    
    // Input ports
    int i=0;
    double ipstart = portpad + (w - ipsize)/2;
    for(GraphPort gport : this.getInputPorts().values()) {
      gport.setConfig(config);
      double px = x + ipstart + i*(portsize + portpad) + portsize/2;
      double py = y - portsize/2;
      gport.draw(this, px, py, portsize/2, color);
      i++;
    }
    
    // Output ports
    i=0;
    double opstart = portpad + (w - opsize)/2;
    for(GraphPort gport : this.getOutputPorts().values()) {
      gport.setConfig(config);
      double px = x + opstart + i*(portsize + portpad) + portsize/2;
      double py = y+h + portsize/2;
      gport.draw(this, px, py, portsize/2, color);
      i++;
    }
  }

}
