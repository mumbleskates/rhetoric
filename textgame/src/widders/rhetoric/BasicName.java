package widders.rhetoric;

/**
 * Represents a name with normal pluralization standards (just add an "s")
 * 
 * @author widders
 */
public class BasicName extends Name {
  /** Creates a new instance of BasicName */
  public BasicName(String noun, boolean vowel) {
    super(noun, vowel);
  }
  
  /** The same as calling BasicName(noun, false) */
  public BasicName(String noun) {
    this(noun, false);
  }
  
  /** Returns the plural noun */
  @Override
  public String getPlural() {
    return noun + "s";
  }
}