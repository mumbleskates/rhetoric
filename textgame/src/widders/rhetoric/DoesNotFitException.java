package widders.rhetoric;

public class DoesNotFitException extends Exception {
  private String s;
  private static final long serialVersionUID = 566182308688196428L;
  
  public DoesNotFitException(String description) {
    s = description;
  }
  
  @Override
  public String getMessage() {
    return s;
  }
}
