package widders.rhetoric.content;

import widders.rhetoric.Active;
import widders.rhetoric.BasicName;
import widders.rhetoric.Container;
import widders.rhetoric.Detail;
import widders.rhetoric.Item;
import widders.rhetoric.Report;
import widders.rhetoric.Selection;

public class DebugCube extends Item {
  private double scale = 0;
  
  public DebugCube(Container initialContainer, String preposition, double scale) {
    super(new BasicName("cube"), array("grey", "cube"),
          initialContainer, preposition);
    this.scale = scale;
  }
  
  public String des(Detail detail) {
    switch (detail) {
      case BASIC:
        return "A featureless grey cube, " + scale + " meters on a side.";
        
      case DETAIL:
        return "There is nothing special about the cube.";
        
      case INSIDE:
        return "The cube is as boring on the inside as it is on the outside.";
        
      default:
        return null;
    }
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Report r) {
    return false;
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Active indirect,
                             Report r) {
    return false;
  }
  
  @Override
  protected boolean used(Active actor, String verb, Selection target, Report r) {
    return false;
  }
  
  @Override
  public double lengthLimit() {
    return scale * .875;
  }
  
  @Override
  public double widthLimit() {
    return scale *.875;
  }
  
  @Override
  public double availableSize() {
    return size() *.875*.875*.875 - contentSize();
  }
  
  @Override
  public double size() {
    return scale * scale * scale;
  }
  
  @Override
  public double baseWeight() {
    return size() * (1000.*(1.-.875*.875*.875));
  }
  
  @Override
  public double length() {
    return scale;
  }
  
  @Override
  public double width() {
    return scale;
  }
}