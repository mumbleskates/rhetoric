package widders.rhetoric;

/**
 * 
 * @author widders
 */
public abstract class Thing extends Active {
  //  private boolean flexible; // whether size and length vary by contents
  //  private double baseSize;
  //  private double baseLength;
  //  private double baseWidth;
  
  /**
   * Creates a new instance of Thing
   */
  protected Thing(Name n, String[] ident, Container initialContainer,
                  String preposition) {
    super(n, ident, initialContainer, preposition);
    //    baseSize = size;
    //    baseLength = length;
    //    baseWidth = width;
    //    flexible = flexibleContainer;
  }
  
  
  /*@Override
  public double lengthLimit() {
    return flexible
        ? Math.min(container().lengthLimit(), super.lengthLimit())
        : super.lengthLimit();
  }
  
  @Override
  public double widthLimit() {
    return flexible
        ? Math.min(container().maxWidthLimit(), super.maxWidthLimit())
        : super.maxWidthLimit();
  }
  
  @Override
  public double availableSize() {
    return flexible
        ? Math.min(container().availableSize(), super.availableSize())
        : super.availableSize();
  }
  
  @Override
  protected void onAdd(Active obj, Active actor) {
    if (flexible) {
      updateSize(baseSize + contentSize());
      if (obj.length() > baseLength && obj.length() > length())
        updateLength(obj.length());
      if (obj.width() > baseWidth && obj.width() > widestContent())
        updateWidth(obj.length());
    }
  }
  
  @Override
  protected void onRemove(Active obj, Container wentTo, Active actor) {
    if (flexible) {
      updateSize(baseSize + contentSize());
      if (obj.length() > baseLength && obj.length() == length()) {
        updateLongestContent();
        updateLength(longestContent());
      }
      if (obj.width() > baseWidth && obj.width() > width()) {
        updateWidestContent();
        updateWidth(widestContent());
      }
    }
  }*/
  
  @Override
  public boolean movable() {
    return true;
  }
}