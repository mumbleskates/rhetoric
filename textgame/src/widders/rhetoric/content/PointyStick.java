package widders.rhetoric.content;

import static widders.rhetoric.Units.*;
import widders.rhetoric.Active;
import widders.rhetoric.BasicName;
import widders.rhetoric.Container;
import widders.rhetoric.Detail;
import widders.rhetoric.Item;
import widders.rhetoric.Report;
import widders.rhetoric.Selection;

public class PointyStick extends Item {
  public PointyStick(Container container, String preposition) {
    super(new BasicName("stick"), array("pointy", "stick"),
          container, preposition);
  }
  
  public String des(Detail detail) {
    switch (detail) {
      case BASIC:
        return "A pointy stick as tall as a man lies here.";
     
      case DETAIL:
        return "Crude, but very pointy.";
        
      case INSIDE:
        return "You see pretty much what you would expect on the inside of"
            + " a stick.";
        
      default:
        return null;
    }
  }
  
  @Override
  protected boolean authorizeAdd(Active obj, String preposition, Active actor,
                                 Report r) {
    r.report("You cannot do that.");
    return false;
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Report r) {
    return false; // nothing happens
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Active indirect,
                             Report r) {
    return false;
  }
  
  @Override
  public boolean used(Active actor, String verb, Selection target, Report r) {
    return false; // nothing happens
  }

  @Override
  public double lengthLimit() {
    return 0;
  }

  @Override
  public double widthLimit() {
    return 0;
  }

  @Override
  public double availableSize() {
    return 0;
  }

  @Override
  public double size() {
    return 5. * 5. * 200. * CU_CM;
  }

  @Override
  public double baseWeight() {
    return 2 * KG;
  }

  @Override
  public double length() {
    return 2 * METER;
  }

  @Override
  public double width() {
    return 5 * CM;
  }
}