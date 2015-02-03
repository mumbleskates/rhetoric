package widders.rhetoric;

/**
 * This is used internally to allow for extensive, flexible status reporting
 * after calling an action such as an Active target, so that objects can easily
 * report the result text to the specific stream that the action was called
 * from.
 * 
 * @author widders
 */
public class Report {
  private StringBuilder text;
  
  public Report() {
    text = new StringBuilder();
  }
  
  public Report(String t) {
    text = new StringBuilder(t);
  }
  
  public void report(String t) {
    if (text.length() != 0)
      text.append("\n");
    text.append(t);
  }
  
  public String text() {
    return text.toString();
  }
}