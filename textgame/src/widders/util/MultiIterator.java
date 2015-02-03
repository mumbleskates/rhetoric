package widders.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides tools to create iterators that traverse over all the elements in a
 * set of sub-iterators, either from an array or iterator of iterators, or from
 * an array or iterator of Iterable objects.
 * 
 * @author widders
 */
public abstract class MultiIterator<E> implements Iterator<E> {
  protected Iterator<E> getHere = null;
  protected boolean complete = false; //whether the iterator has finished
  private Iterator<E> lastGot = null;
  private boolean advanced = false; //whether the iterator state is current
  
  public static <E> MultiIterator<E>
      createFromIterables(Iterable<E>[] iterables) {
    return new IterableArrayIterator<E>(iterables);
  }
  
  public static <E> MultiIterator<E>
      createFromIterators(Iterator<E>[] iterators) {
    return new IteratorArrayIterator<E>(iterators);
  }
  
  public static <E> MultiIterator<E>
      createFromIterables(Iterator<? extends Iterable<E>> iterables) {
    return new IterableIteratorIterator<E>(iterables);
  }
  
  public static <E> MultiIterator<E>
      createFromIterators(Iterator<? extends Iterator<E>> iterators) {
    return new IteratorIteratorIterator<E>(iterators);
  }
  
  /**
   * Makes the state of the iterator current.
   * After executing this method either getHere.hasNext() must return true or
   * complete must be true.
   */
  protected abstract void advance();
  
  public final boolean hasNext() {
    if (!advanced) {
      advance();
      advanced = true; //this operation doesn't modify, will still be current
    }
    return !complete;
  }
  
  public final E next() {
    if (!advanced) advance();
    if (complete) throw new NoSuchElementException("No more elements");
    
    lastGot = getHere;
    advanced = false; //after calling next(), will no longer be current
    return getHere.next();
  }
  
  public final void remove() {
    if (lastGot == null)
      throw new IllegalStateException("Iterator not yet incremented");
    lastGot.remove();
  }
}
  
  
class IterableArrayIterator<E> extends MultiIterator<E> {
  private Iterable<E>[] internal;
  private int position = 0;
  
  IterableArrayIterator(Iterable<E>[] iterables) {
    internal = iterables;
  }
  
  @Override
  protected void advance() {
    if (complete) return;
    
    while (getHere == null || !getHere.hasNext()) {
      if (++position == internal.length) {
        complete = true; //advanced past end of array
        return;
      }
      Iterable<E> x = internal[position];
      if (x != null) getHere = x.iterator();
    }
  }
}



class IteratorArrayIterator<E> extends MultiIterator<E> {
  private Iterator<E>[] internal;
  private int position = 0;
  
  IteratorArrayIterator(Iterator<E>[] iterators) {
    internal = iterators;
  }
  
  @Override
  protected void advance() {
    if (complete) return;
    
    while (getHere == null || !getHere.hasNext()) {
      if (++position == internal.length) {
        complete = true; //advanced past end of array
        return;
      }
      getHere = internal[position];
    }
  }
}



class IterableIteratorIterator<E> extends MultiIterator<E> {
  private Iterator<? extends Iterable<E>> internal;
  
  IterableIteratorIterator(Iterator<? extends Iterable<E>> iterables) {
    internal = iterables;
  }
  
  @Override
  protected void advance() {
    if (complete) return;
    
    while (getHere == null || !getHere.hasNext()) {
      if (internal.hasNext()) {
        Iterable<E> x = internal.next();
        if (x != null) getHere = x.iterator();
      } else {
        complete = true;
        return;
      }
    }
  }
}



class IteratorIteratorIterator<E> extends MultiIterator<E> {
  private Iterator<? extends Iterator<E>> internal;
  
  IteratorIteratorIterator(Iterator<? extends Iterator<E>> iterators) {
    internal = iterators;
  }
  
  @Override
  protected void advance() {
    if (complete) return;
    
    while (getHere == null || !getHere.hasNext()) {
      if (internal.hasNext()) {
        getHere = internal.next();
      } else {
        complete = true;
        return;
      }
    }
  }
}