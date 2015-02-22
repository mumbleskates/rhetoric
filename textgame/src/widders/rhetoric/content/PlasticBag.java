package widders.rhetoric.content;

import widders.rhetoric.Active;
import widders.rhetoric.BasicName;
import widders.rhetoric.Container;
import widders.rhetoric.Detail;
import widders.rhetoric.Report;
import widders.rhetoric.Selection;
import static widders.rhetoric.Units.*;

public class PlasticBag extends Active {
  public PlasticBag(Container container, String preposition) {
    super(new BasicName("bag"), array("plastic", "bag"), container, preposition);
  }

  public String des(Detail detail) {
    switch (detail) {
      case BASIC:
        return "A plain white plastic shoping bag.";
      case DETAIL:
        return "Crinkly and not very imposing.";
      case INSIDE:
        return "The bag looks pretty much the same on the inside.";
      default:
        return null;
    }
  }

  @Override
  public double baseWeight() {
    return 5.5 * GRAM;
  }

  @Override
  public boolean movable() {
    return true;
  }

  @Override
  protected boolean targeted(Active actor, String verb, Report r) {
    return false;
  }

  @Override
  protected boolean targeted(Active actor, String verb, Active indirect, Report r) {
    return false;
  }

  @Override
  protected boolean used(Active actor, String verb, Selection target, Report r) {
    return false;
  }

  @Override
  public double availableSize() {
    return 3 * GALLON - contentSize();
  }

  @Override
  public double lengthLimit() {
    return .5 * METER;
  }

  @Override
  public double widthLimit() {
    return .5 * METER;
  }

  @Override
  public double size() {
    return contentSize() + 10 * CU_CM;
  }

  @Override
  public double length() {
    return 2 * CM;
  }

  @Override
  public double width() {
    return 4 * MM;
  }
}