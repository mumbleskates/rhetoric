package widders.rhetoric;

public class DoesNotFitException extends Exception {
  private static final long serialVersionUID = 566182308688196428L;
  
  public DoesNotFitException(String description) {
    super(description);
  }
}