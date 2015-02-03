package widders.rhetoric;

/**
 * This is a class in its own right; it exhibited VERY little individualized
 * implementation because it is extended from Thing on a separate branch from
 * Entity so that Thing is not being used as the main class of in-game objects,
 * thus making inanimate things (Items) distinguishable from Entities only with
 * difficulty.
 * 
 * Now, that has changed, with the implementation of splitting and merging.
 * Since only movable objects that are not entities should pile and merge, this
 * class is the perfect base point for the implementation of these elements.
 * Probably. Unless there's some kind of problem with requiring the objects to
 * be mergeable/pileable. ///// TODO there should be a groupable indicator
 * 
 * @author widders
 */
public abstract class Item extends Thing {
  ///// TODO splitting & merging
  // may have one or more merging signatures
  
  /**
   * Creates a new instance of Item
   */
  protected Item(Name n, String[] ident, Container initialContainer,
                 String preposition) {
    super(n, ident, initialContainer, preposition);
    // this is just for debugging purposes; use instanceof to actually check
    classify("item");
  }
}