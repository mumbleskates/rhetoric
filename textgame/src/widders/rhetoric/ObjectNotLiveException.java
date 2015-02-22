package widders.rhetoric;

/** Can be thrown when an action fails because an object is not initialized
 * or has been destroyed */
public class ObjectNotLiveException extends RuntimeException {
  private static final long serialVersionUID = 7483932058207306243L;
  
  public ObjectNotLiveException(Container problem) {
    super(problem + " is not a live object");
  }
}