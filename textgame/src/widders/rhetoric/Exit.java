package widders.rhetoric;

/**
 * 
 * @author widders
 */
public interface Exit {
  // GOES SOMEWHERE
  // MIGHT GO DIFFERENT PLACES; has arbitrary decision-making power over
  // the actual movement destination as well as whether travel is possible
  // has an exit direction, but this is mainly used in the data in the Room;
  // the actual functionality of the Exit object is to provide a place to go
  // in addition to flexibility in creating additional movement filtering,
  // whereas if the exit hashtable merely listed other rooms, this option
  // would not exist
  // Most exits should be Feature objects that implement the Exit interface;
  // that would be way more awesome than what I was thinking before where
  // the features are merely false fronts for description; this way, it's
  // all integrated and you don't have to dance a jig to communicate between
  // the front and the effective member since they will be the same thing
  
  /**
   * [e] attempts to traverse through the exit; if successful, returns the
   * destination room; otherwise returns null
   */
  public Room traverse(Active e, Report r);
}