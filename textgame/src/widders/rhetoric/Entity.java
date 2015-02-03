package widders.rhetoric;

import java.util.Map;

/**
 * Entity objects represent players, NPCs, and other animate objects.
 * 
 * @author widders
 */
public abstract class Entity extends Thing {
  protected Map<String, Feature> slots;
  
  // IS WEARING & HOLDING ITEMS
  // ( TODO: implement with Feature objects? oshit where will stuff be contained then that won't work )
  /**
   * Creates a new instance of Entity
   */
  public Entity(Name n, String[] ident,
                Container initialContainer, String preposition) {
    super(n, ident, initialContainer, preposition);
    // this is just for debugging purposes; use instanceof to actually check
    classify("entity");
  }
}