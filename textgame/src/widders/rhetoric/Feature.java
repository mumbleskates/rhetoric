package widders.rhetoric;

/**
 * Feature objects represent specific elements of an object or room.
 * They cannot be moved.
 * 
 * @author widders
 */
public abstract class Feature extends Active {
  
  /**
   * Creates a new instance of Feature
   */
  protected Feature(Name n, String[] ident, Container initialContainer,
                    String preposition) {
    super(n, ident, initialContainer, preposition);
    // this is just for debugging purposes; use instanceof to actually check
    classify("feature");
  }
  
  @Override
  public final boolean movable() {
    return false;
  }
  
  
}