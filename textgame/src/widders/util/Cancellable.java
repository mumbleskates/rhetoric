package widders.util;

public interface Cancellable {
  /** Whether the action has already been executed */
  boolean hasRun();
  
  /** Whether the action was canceled */ 
  boolean isCancelled();
  
  /**
   * Cancels the action if it has not yet been run.
   * Returns true iff the action has not been executed yet.
   */
  boolean cancel();
}