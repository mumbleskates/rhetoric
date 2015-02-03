package widders.rhetoric;

/**
 * Represents a name of a type of object that is not quantifiable, such as water
 * or sand or something ("there is a water here"?). This basically eliminates
 * the article, replacing it instead with "some"
 * 
 * @author widders
 */
public class FluidName extends Name {
  /**
   * The word(s) used to indicate the item's pseudomultiplicity; defaults to
   * "some"
   */
  protected String indicator;
  
  /** Creates a new instance with the given indicator */
  public FluidName(String noun, String ind) {
    super(noun, false);
    indicator = ind + " ";
  }
  
  /** Creates a new instance with the default indicator */
  public FluidName(String noun) {
    this(noun, "some");
  }
  
  /** Returns the noun preceded by the proper indicator */
  @Override
  public String getOne() {
    return getPlural();
  }
  
  /** Overrides Name behavior */
  @Override
  public String getOne(String[] adj) {
    return getPlural(adj);
  }
  
  /** Returns the noun preceded by the proper indicator */
  @Override
  public String getPlural() {
    return indicator + noun;
  }
  
  /** Returns the noun preceded by the proper indicator and adjectives */
  @Override
  public String getPlural(String[] adj) {
    StringBuilder sb;
    
    if (adj == null || adj.length == 0) {
      return getPlural();
    } else {
      sb = new StringBuilder(indicator);
      appendWords(sb, adj);
      sb.append(noun);
      
      return sb.toString();
    }
  }
}