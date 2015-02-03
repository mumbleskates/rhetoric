package widders.util;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class BreadthFirstRecursionIterator<E extends Iterable<E>>
implements Iterator<E> {
  private Iterator<E> getHere = null;
  private boolean complete = false; //whether the iterator has finished
  private Iterator<E> lastGot = null;
  private boolean lastGotHasChild = false; //whether an iterator was enqueued
  /* whether to snip the branch of the tree stemming from that particular
   * element when remove() is called */
  private final boolean snipTree;
  private boolean advanced = false; //whether the iterator state is current
  Deque<Iterator<E>> queue = new LinkedList<Iterator<E>>();
  
  public BreadthFirstRecursionIterator(Iterator<E> fromThis,
                                       boolean snipRemovedBranches) {
    getHere = fromThis;
    snipTree = snipRemovedBranches;
  }
  
  public BreadthFirstRecursionIterator(Iterable<E> fromThis,
                                       boolean snipRemovedBranches) {
    this(fromThis.iterator(), snipRemovedBranches);
  }

  private void advance() {
    if (complete) return;
    
    while (!getHere.hasNext()) {
      getHere = queue.poll();
      if (getHere == null) {
        complete = true;
        return;
      }
    }
  }
  
  public boolean hasNext() {
    if (!advanced) {
      advance();
      advanced = true; //this operation doesn't modify, will still be current
    }
    return !complete;
  }
  
  public E next() {
    if (!advanced) advance();
    if (complete) throw new NoSuchElementException("No more elements");
    
    lastGot = getHere;
    advanced = false; //after calling next(), will no longer be current
    E next = getHere.next();
    Iterator<E> it = next.iterator();
    if ((lastGotHasChild = it.hasNext())) // if this object has anything in it,
      queue.add(it); // queue it up
    return next;
  }
  
  public void remove() {
    if (lastGot == null)
      throw new IllegalStateException("Iterator not yet incremented");
    lastGot.remove();
    if (snipTree && lastGotHasChild) // cut any children from the tree
      queue.removeLast();
  }
}