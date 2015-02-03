package widders.rhetoric;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import widders.util.BreadthFirstRecursionIterator;
import widders.util.IterProtector;
import widders.util.MultiIterator;
import widders.util.RandomAccessLinkedHashSet;


/**
 * Provides functionality for selecting multiple items in a given context.
 * Provides immutable sets of Active objects garnered from selection parameters.
 * 
 * @author widders
 */
public class Selection implements Set<Active> {
  private static final Selection EMPTY_SELECTION = new EmptySelection();
  
  private RandomAccessLinkedHashSet<Active> selected;
  
  private Selection(boolean empty) {
    selected = empty
        ? null //Collections.<Active> emptySet()
        : new RandomAccessLinkedHashSet<Active>();
  }
  
  /** Creates a new instance of Selection with nothing selected */
  private Selection() {
    this(false);
  }
  
  /** Returns an empty selection */
  public static Selection selectNone() {
    return EMPTY_SELECTION;
  }
  
  /** Creates a new instance of Selection containing only the given object */
  public static Selection selectOnly(Active a) {
    Selection sel = new Selection();
    sel.selected.add(a);
    return sel;
  }
  
  public static Selection selectOnly(Active[] from) {
    Selection sel = new Selection();
    for (Active a : from)
      sel.selected.add(a);
    return sel;
  }
  
  /** Selects all in the given container */
  public static Selection select(Iterable<Active> from, Predicate<Active> filter,
                                 int skip, int max) {
    Selection sel = new Selection();
    sel.selectImpl(from.iterator(), filter, skip, max);
    return sel;
  }
  
  public static Selection select(Iterable<Active> from, Predicate<Active> filter) {
    return select(from, filter, 0, 0);
  }
  
  public static Selection select(Iterable<Active>[] from, Predicate<Active> filter,
                                 int skip, int max) {
    Selection sel = new Selection();
    sel.selectImpl(MultiIterator.createFromIterables(from),
                   filter, skip, max);
    return sel;
  }
  
  public static Selection select(Iterable<Active>[] from, Predicate<Active> filter) {
    return select(from, filter, 0, 0);
  }
  
  public static Selection selectAll(Iterable<Active> from) {
    Selection sel = new Selection();
    sel.selectAllImpl(from.iterator());
    return sel;
  }
  
  public static Selection selectAll(Iterable<Active>[] from) {
    Selection sel = new Selection();
    sel.selectAllImpl(MultiIterator.createFromIterables(from));
    return sel;
  }
  
  public static Selection selectAllDeep(Iterable<Active> from) {
    Selection sel = new Selection();
    sel.selectAllImpl(new BreadthFirstRecursionIterator<Active>(from, false));
    return sel;
  }
  
  public static Selection selectAllDeep(Iterable<Active>[] from) {
    Selection sel = new Selection();
    Iterator<Active> it = MultiIterator.createFromIterables(from);
    sel.selectAllImpl(new BreadthFirstRecursionIterator<Active>(it, false));
    return sel;
  }
  
  public static Selection selectDeep(Iterable<Active> from, Predicate<Active> filter,
                                     int skip, int max) {
    Selection sel = new Selection();
    sel.selectImpl(new BreadthFirstRecursionIterator<Active>(from, false),
                   filter, skip, max);
    return sel;
  }
  
  public static Selection selectDeep(Iterable<Active> from, Predicate<Active> filter) {
    return selectDeep(from, filter, 0, 0);
  }
  
  public static Selection selectDeep(Iterable<Active>[] from, Predicate<Active> filter,
                                     int skip, int max) {
    Selection sel = new Selection();
    Iterator<Active> it = MultiIterator.createFromIterables(from);
    sel.selectImpl(new BreadthFirstRecursionIterator<Active>(it, false),
                   filter, skip, max);
    return sel;
  }
  
  public static Selection selectDeep(Iterable<Active>[] from, Predicate<Active> filter) {
    return selectDeep(from, filter, 0, 0);
  }
  
  /**
   * Selects matching objects in the given container, skipping the first [skip]
   * elements and selecting no more than [max] (unless [max] < 1), and
   * returns the number selected. If no objects were selected, returns a
   * negative value of the number of objects that remain to be skipped because
   * of the ordinal argument.
   * 
   * Duplicate objects presented while items are still being skipped still count
   * towards the total number of objects to skip.
   */
  private int selectImpl(Iterator<Active> from, Predicate<Active> filter, int skip, int max) {
    int gotten = 0;
    while (from.hasNext()) {
      Active a = from.next();
      if (filter.test(a)) {
        if (skip > 0) { // still skipping
          skip--;
        } else { // done skipping, actually grabbing items now
          if (selected.add(a)) {
            max--;
            gotten++;
            if (max == 0)
              return gotten; // finish
          }
        }
      }
    }
    
    if (skip > 0)
      return -skip;
    else
      return gotten;
  }
  
  /**
   * Selects matching objects in the given container, skipping the first [skip]
   * elements and selecting no more than [max] (unless [max] < 1), and
   * returns the number selected. If no objects were selected, returns a
   * negative value of the number of objects that remain to be skipped because
   * of the ordinal argument.
   * 
   * Duplicate objects presented while items are still being skipped still count
   * towards the total number of objects to skip.
   */
  private int selectAllImpl(Iterator<Active> from) {
    int gotten = 0;
    while (from.hasNext()) {
      Active a = from.next();
      if (selected.add(a))
        gotten++;
    }
    return gotten;
  }
  
  public Active randomElement(Random rand) {
    if (isEmpty())
      throw new NoSuchElementException();
    
    return selected.randomElement(rand);
  }
  
  /** Returns the number of objects selected */
  public int size() {
    return selected.size();
  }
  
  /** Returns an iterator over the selection list */
  public Iterator<Active> iterator() {
    return new IterProtector<Active>(selected.iterator());
  }
  
  public boolean isEmpty() {
    return selected.isEmpty();
  }
  
  
  public boolean contains(Object o) {
    return selected.contains(o);
  }
  
  
  /** Returns an array containing the selection list */
  public Active[] toArray() {
    return selected.toArray(new Active[size()]);
  }
  
  public <T> T[] toArray(T[] a) {
    return selected.toArray(a);
  }
  
  public boolean containsAll(Collection<?> c) {
    return selected.containsAll(c);
  }
  
  public boolean add(Active e) {
    throw new UnsupportedOperationException();
  }
  
  
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }
  
  
  public boolean addAll(Collection<? extends Active> c) {
    throw new UnsupportedOperationException();
  }
  
  
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }
  
  
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }
  
  
  public void clear() {
    throw new UnsupportedOperationException();
  }
  
  
  private static class EmptySelection extends Selection {
    private static final Set<Active> EMPTY = Collections.<Active> emptySet();
    
    EmptySelection() {
      super(true);
    }
    
    /** Returns the number of objects selected */
    @Override
    public int size() {
      return 0;
    }
    
    /** Returns an iterator over the selection list */
    @Override
    public Iterator<Active> iterator() {
      return EMPTY.iterator();
    }
    
    @Override
    public boolean isEmpty() {
      return true;
    }
    
    @Override
    public boolean contains(Object o) {
      return false;
    }
    
    /** Returns an array containing the selection list */
    @Override
    public Active[] toArray() {
      return new Active[] {};
    }
    
    @Override
    public <T> T[] toArray(T[] a) {
      return EMPTY.toArray(a);
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
      return c.isEmpty();
    }
    
    @Override
    public Active randomElement(Random rand) {
      throw new NoSuchElementException();
    }
  }
}