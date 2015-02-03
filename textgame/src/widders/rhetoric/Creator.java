package widders.rhetoric;

import static widders.rhetoric.Units.*;


public final class Creator extends Active {
  
  public Creator(Container initialContainer, String preposition) {
    super(new BasicName("creator"), array("creator", "machine", "huge"),
          initialContainer, preposition);
    classify("engine_object", "immortal");
  }
  
  public String des(Detail detail) {
    switch (detail) {
      case BASIC:
        return "An immense ethereal machine squats here, flickering gently"
            + " in and out of solidity, looming with quiescent power.";
        
      case DETAIL:
        return "The machine's silent, engraved flanks betray no"
            + " secrets.";
        
      case INSIDE:
        return "You cannot see inside.";
        
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
    return false;
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Report r) {
    r.report("How could you possibly do that?");
    return false;
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Active indirect,
                             Report r) {
    return false;
  }
  
  @Override
  public boolean used(Active actor, String verb, Selection target, Report r) {
    r.report("How could you possibly do that?");
    return false;
  }
  
  @Override
  public void see(Active actor, String verb, Active target, Active indirect) {
  }
  
  @Override
  public boolean movable() {
    return false;
  }
  
  @Override
  public double size() {
    return 6. * 5. * 8. * CU_METER;
  }
  
  @Override
  public double baseWeight() {
    return 2.97e8 * METRIC_TON;
  }
  
  @Override
  public double length() {
    return 8. * METER;
  }
  
  @Override
  public double width() {
    return 6. * METER;
  }
}
