package widders.rhetoric;

import java.util.*;


/**
 * 
 * @author widders
 */
public abstract class Name {
  private static Map<String, Boolean> wordRegister =
      new Hashtable<String, Boolean>();
  
  /** The unadorned singular noun */
  protected String noun;
  
  /**
   * If true, indicates that this is a word that begins with an open vowel
   * sound, invoking the need for "an" rather than "a" preceding it as an
   * unspecific singular noun
   */
  protected boolean vowel;
  
  /** Creates a new instance of Name with the given noun and vowel indicator */
  public Name(String basicNoun, boolean vowelStart) {
    noun = basicNoun;
    vowel = vowelStart;
    registerWord(noun, vowel);
  }
  
  
  /**
   * Appends the given words, each one (including the last) followed by a space,
   * to the given StringBuilder and returns the same. If the string array is
   * null or empty, nothing happens
   */
  public static final
      StringBuilder appendWords(StringBuilder sb, String[] words) {
    if (words != null) {
      for (String w : words) {
        sb.append(w);
        sb.append(" ");
      }
    }
    return sb;
  }
  
  
  @Override
  public String toString() {
    return get();
  }
  
  /** Returns the basic, unadorned singular noun of the name. */
  public String get() {
    return noun;
  }
  
  /**
   * Returns the noun with the given adjectives preceding it in order. If [adj]
   * is null or empty, just the noun is returned
   */
  public String get(String[] adj) {
    StringBuilder sb;
    
    if (adj == null || adj.length == 0) {
      return get();
    } else {
      sb = new StringBuilder();
      appendWords(sb, adj);
      sb.append(get());
      
      return sb.toString();
    }
  }
  
  /**
   * Returns the noun preceded by a singular unspecific article (Contract: If
   * the item is fluid--not spoken as if it is quantifiable, as in "water" or
   * "clay" or "sand" or something--it need not be preceded by a singular
   * unspecific article, but should be preceded by something of similar
   * grammatical function, such as "some"
   */
  public String getOne() {
    return (vowel ? "an " : "a ") + noun;
  }
  
  /**
   * Returns the noun preceded by the given adjectives in order and a singular
   * unspecific article, correct for the first adjective. If [adj] is null or
   * empty, no adjectives are added
   */
  public String getOne(String[] adj) {
    if (adj == null || adj.length == 0) {
      return getOne();
    } else {
      return appendWords(new StringBuilder(wordIsVowel(adj[0]) ? "an " : "a "),
                         adj)
          .append(noun)
          .toString();
    }
  }
  
  /** Returns the plural form of the noun */
  public abstract String getPlural();
  
  /**
   * Returns the plural form of the noun preceded by the given adjectives in
   * order. If [adj] is null or empty, no adjectives are added
   */
  public String getPlural(String[] adj) {
    if (adj == null || adj.length == 0) {
      return getPlural();
    } else {
      return appendWords(new StringBuilder(), adj)
          .append(getPlural())
          .toString();
    }
  }
  
  /**
   * Makes the string posessive according to the (for once) unwavering practice
   * of the English Language. If it's a single word or ends in one, this works
   * fine ("llama" becomes "llama's", "moss" becomes "moss'" etc.); of course,
   * it's not meant to be used on anything that ends weird, like in a space or
   * something, but it won't return an error.
   */
  public static final String makePosessive(String noun) {
    if (noun.length() == 0)
      return noun;
    char c = noun.charAt(noun.length() - 1);
    if (c == 's' || c == 'S') {
      return (noun + "'");
    } else {
      return (noun + "'s");
    }
  }
  
  /**
   * Registers the given word with the database; [vowel] specifies the type of
   * word beginning
   */
  public static void registerWord(String word, boolean vowel) {
    wordRegister.put(word.toLowerCase(), vowel);
  }
  
  /** Returns true iff the given word is registered */
  public static boolean wordRegistered(String word) {
    return wordRegister.containsKey(word.toLowerCase());
  }
  
  /**
   * Returns true iff the given word is registered AND it begins with a vowel
   * sound
   */
  public static boolean wordIsVowel(String word) {
    // see, if the word is not registered, the get() method returns null.
    return (wordRegister.get(word.toLowerCase()));
  }
}