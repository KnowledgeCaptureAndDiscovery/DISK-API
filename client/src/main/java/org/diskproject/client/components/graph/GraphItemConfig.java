package org.diskproject.client.components.graph;

public class GraphItemConfig {
  // Configuration
  // TODO: Get from config file
  double xpad = 6;
  double ypad = 3;
  double portsize = 10;
  double portpad = 10;
  double stackspacing = 3;
  int fontsize = 13;
  String font = "Tahoma";
  String fontweight = "bold";
  String textcolor = "rgba(72,42,3,1)";
  String strokecolor = "rgba(0,0,0,0.6)";
  String bgcolor = "rgba(200,200,200,1)";

  public double getXpad() {
    return xpad;
  }

  public void setXpad(double xpad) {
    this.xpad = xpad;
  }

  public double getYpad() {
    return ypad;
  }

  public void setYpad(double ypad) {
    this.ypad = ypad;
  }

  public double getPortsize() {
    return portsize;
  }

  public void setPortsize(double portsize) {
    this.portsize = portsize;
  }

  public double getPortpad() {
    return portpad;
  }

  public void setPortpad(double portpad) {
    this.portpad = portpad;
  }

  public double getStackspacing() {
    return stackspacing;
  }

  public void setStackspacing(double stackspacing) {
    this.stackspacing = stackspacing;
  }

  public int getFontsize() {
    return fontsize;
  }

  public void setFontsize(int fontsize) {
    this.fontsize = fontsize;
  }

  public String getFontweight() {
    return fontweight;
  }

  public void setFontweight(String fontweight) {
    this.fontweight = fontweight;
  }

  public String getFont() {
    return font;
  }

  public void setFont(String font) {
    this.font = font;
  }

  public String getTextcolor() {
    return textcolor;
  }

  public void setTextcolor(String textcolor) {
    this.textcolor = textcolor;
  }

  public String getStrokecolor() {
    return strokecolor;
  }

  public void setStrokecolor(String strokecolor) {
    this.strokecolor = strokecolor;
  }

  public String getBgcolor() {
    return bgcolor;
  }

  public void setBgcolor(String bgcolor) {
    this.bgcolor = bgcolor;
  }
}
