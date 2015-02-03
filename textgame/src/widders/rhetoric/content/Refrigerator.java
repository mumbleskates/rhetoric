package widders.rhetoric.content;

import static widders.rhetoric.Units.*;
import widders.rhetoric.Active;
import widders.rhetoric.BasicName;
import widders.rhetoric.Container;
import widders.rhetoric.Detail;
import widders.rhetoric.Item;
import widders.rhetoric.Main;
import widders.rhetoric.Report;
import widders.rhetoric.Selection;

public class Refrigerator extends Item {
  public Refrigerator(Container initialContainer, String preposition) {
    super(new BasicName("refrigerator"),
          array("huge", "large", "hilarious", "refrigerator"),
          initialContainer, preposition);
  }
  
  public String des(Detail detail) {
    switch (detail) {
      case BASIC:
        return "There is a comically large refrigerator here, the size of a"
            + " very small house.";
        
      case DETAIL:
        return "It's simply massive, like a regular refrigerator seen by a"
            + " small cat.";
        
      case INSIDE:
        StringBuilder sb = new StringBuilder("The inside of the refrigerator"
            + " is shockingly spacious.");
        for (Active content : this)
          sb.append(content.des(Detail.BASIC));
        return sb.toString();
        
        ///// TODO make describing stuff easier. this is really clunky for end-developer
        
      default:
        return null;
    }
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
  protected void onAdd(Active obj, Active actor) {
    obj.target(this, "chill", Main.fakeReport);
  }
  
  @Override
  public double lengthLimit() {
    return 9 * METER;
  }
  
  @Override
  public double widthLimit() {
    return 4 * METER;
  }
  
  @Override
  public double availableSize() {
    return 3 * 4 * 9 * CU_METER - contentSize();
  }
  
  @Override
  public double size() {
    return 4 * 5 * 10 * METER;
  }
  
  @Override
  public double baseWeight() {
    return 12 * METRIC_TON;
  }

  @Override
  public double length() {
    return 10.5 * METER;
  }

  @Override
  public double width() {
    return 5 * METER;
  }
}