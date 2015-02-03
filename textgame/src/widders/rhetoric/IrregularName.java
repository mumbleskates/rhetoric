package widders.rhetoric;

/**
 * Represents a name that is an exception to the basic pluralization rule, such
 * as child/children, dish/dishes, or fish/fish
 * 
 * @author widders
 */
public class IrregularName extends Name {
  protected String pluralNoun;
  
  /**
   * Creates a new instance where the plural is irregular with the specified
   * singular and plural forms of the noun
   */
  public IrregularName(String noun, String plural, boolean vowel) {
    super(noun, vowel);
    pluralNoun = plural;
  }
  
  /**
   * Creates a new instance where the singular is exactly the same as the
   * plural, as in "fish" or "sheep"
   */
  public IrregularName(String noun, boolean vowel) {
    this(noun, noun, vowel);
  }
  
  /** Returns the plural noun */
  @Override
  public String getPlural() {
    return pluralNoun;
  }
}