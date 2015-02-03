package widders.rhetoric;

/**
 * 
 * @author widders
 */
public interface Named {
  /**
   * Returns a description of the object with the requested level of detail.
   */
  public String des(Detail detail);
  
  /**
   * Adds the appropriate description to the given report.
   */
  public void des(Detail detail, Report r);
  
  /** Returns the name of the object */
  public Name name();
}