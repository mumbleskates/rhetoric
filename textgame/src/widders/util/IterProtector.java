package widders.util;

import java.util.Iterator;

/**
 * This class wraps an Iterator, disabling the remove() method and protecting
 * the contents of the underlying whatever-it-is.
 * 
 * @author widders
 */
public class IterProtector<E> implements Iterator<E> {
  private Iterator<E> wrap;
  
  /** Creates a new instance of IterProtector */
  public IterProtector(Iterator<E> wrapThis) {
    wrap = wrapThis;
  }
  
  public boolean hasNext() {
    return wrap.hasNext();
  }
  
  public E next() {
    return wrap.next();
  }
  
  public void remove() {
    throw new UnsupportedOperationException("This Iterator is protected.");
  }
}