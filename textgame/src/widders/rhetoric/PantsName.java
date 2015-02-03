package widders.rhetoric;

/**
 * Represents a name for some few, odd nouns such as "pants" and "scissors"
 * which, even single, come in pairs. Odd, isn't it?
 * 
 * @author widders
 */
public class PantsName extends Name {
  /** Creates a new instance of PantsName */
  public PantsName(String noun) {
    super(noun, false);
  }
  
  /**
   * Returns the noun preceded by the proper indications of singular
   * unspecificity
   */
  @Override
  public String getOne() {
    return "a pair of " + noun;
  }
  
  /** Does the same as thing as getOne(), but throws some adjectives in there */
  @Override
  public String getOne(String[] adj) {
    StringBuilder sb;
    
    if (adj == null || adj.length == 0) {
      return getOne();
    } else {
      sb = new StringBuilder("a pair of ");
      appendWords(sb, adj);
      sb.append(noun);
      
      return sb.toString();
    }
  }
  
  /** Returns the noun in all it's plural glory */
  @Override
  public String getPlural() {
    return "pairs of " + noun;
  }
  
  /** Returns the plural noun setup with adjectives */
  @Override
  public String getPlural(String[] adj) {
    if (adj == null || adj.length == 0) {
      return getPlural();
    } else {
      return appendWords(new StringBuilder(), adj)
          .append("pairs of ")
          .append(noun)
          .toString();
    }
  }
  
  @Override
  public String get() {
    return "pair of " + noun;
  }
}