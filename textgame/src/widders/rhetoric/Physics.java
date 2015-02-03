package widders.rhetoric;

import static widders.rhetoric.Units.*;


/**
 * This is the class which is represented as the actor for physical events etc.
 * 
 * @author widders
 */
public final class Physics extends Active {
  
  /**
   * Creates a new instance of Physics
   */
  public Physics(Container putItHere, String preposition) {
    super(new BasicName("physics"), array("physics", "mobius", "strip",
                                          "small", "floating"),
          putItHere, preposition);
    classify("engine_object", "immortal");
  }
  
  @Override
  public String des(Detail detail) {
    switch (detail) {
      case BASIC:
        return "There is a small möbius strip here, silently quivering with the"
            + " power of everything that ever moved, burned, fell, or flew.";
        
      case DETAIL:
        return "The strip was not meant to be viewed by mortal eyes.";
        
      case INSIDE:
        return "You should not be here.";
        
      default:
        return null;
    }
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
  protected boolean authorizeAdd(Active obj, String preposition, Active actor, Report r) {
    r.report("You dare not go near.");
    return false;
  }

  @Override
  protected boolean targeted(Active actor, String verb, Report r) {
    r.report("How could you possibly do that?");
    return false;
  }
  
  @Override
  public boolean used(Active actor, String verb, Selection target, Report r) {
    r.report("How could you possibly do that?");
    return false;
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Active indirect,
                             Report r) {
    return false;
  }
  
  @Override
  public final boolean movable() {
    return false;
  }

  @Override
  public double size() {
    return 28. * CU_INCH;
  }

  @Override
  public double baseWeight() {
    return Double.POSITIVE_INFINITY;
  }

  @Override
  public double length() {
    return 10.1 * INCH;
  }

  @Override
  public double width() {
    return 6.1 * INCH;
  }
}