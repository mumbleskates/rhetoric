package widders.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;


/**
 * A composite Set structure that maintains several concurrent substructures.
 * There are few use cases for this structure, but you may find it handy.
 * For any typical use case it exhibits the same behaviors and bounds as
 * java.util.LinkedHashSet, but it also provides array-speed O(1) access to
 * an arbitrarily-ordered set of its elements for the purpose of cherry-picking
 * random elements from the set: getByArbitraryIndex(int) and
 * getRandomElement(Random). Without concurrent modification every index
 * in this span is guaranteed to be unique and every index will return a
 * consistent element from the set. Absolutely no guarantees about the ordering
 * of the elements in this span is made; any operation requiring guaranteed
 * order should be performed with the provided iterators, getElementAfter(),
 * and getElementBefore().
 * 
 * This set DOES accept nulls, but provides a number of operations that return
 * null as a default value. When checking if an element is the first or last in
 * the ordering of the set and it is a requirement that null be handled
 * correctly, compare its value to getFirst() and getLast() instead as these
 * methods will throw exceptions when the set is empty. getElementAfter(),
 * getElementBefore(), peekFirst(), and peekLast() will return null in either
 * case, whether the element in question is actually null or there is no
 * element at that location.
 * 
 * Contains, add, and remove are all O(1). Iterator traversal and random access
 * are upper-bounded at O(1). All operations involving multiple elements such
 * as addAll(), removeAll(), and retainAll() are always O(n).
 * 
 * This class implements all methods from Set and Deque. Deque methods that do
 * not return a boolean value may fail with exception when the operation
 * does not change the set.
 * 
 * All special methods provided by this implementation are:
 * 
 * getElementAfter(Object)
 * getElementBefore(Object)
 * queryElementBefore(Object)
 * queryElementAfter(Object)
 * E getArbitraryIndex(int)
 * E randomElement(java.util.Random)
 * Iterator<E> descendingIterator()
 * ListIterator<E> listIterator()
 * ListIterator<E> listIteratorFromLast()
 * 
 * @author widders
 */
public class RandomAccessLinkedHashSet<E> implements Set<E>, Deque<E> {
  /*
  static int nextID = 0; 
  final int iD = nextID++;
  */
  
  private static final int DEFAULT_CAPACITY = 8;
  private static final int MINIMUM_CAPACITY = 4;
  private static final float DEFAULT_LOAD_FACTOR = .75f;
  
  // pointers for the linked list
  private final Element<E> head;
  // element table; first half is hash table, after that is the array
  private Element<E>[] table;
  // number of contained elements
  private int size = 0;
  
  // running hash total for this whole Set
  private int totalHash = 0;
  
  // current capacity; always power of two
  private int capacity;
  // the multiple of capacity to reach before considering a resize
  private float factor;
  // basically capacity * factor
  private int currentLimit;
  
  private transient int modCount = 0;
  
  public RandomAccessLinkedHashSet() {
    this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
  }
  
  @SuppressWarnings("unchecked")
  public RandomAccessLinkedHashSet(int initialCapacity, float loadFactor) {
    //System.out.println(iD + " new!");
    if (initialCapacity < 0)
      throw new IllegalArgumentException("Illegal initial capacity: " +
          initialCapacity);
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
      throw new IllegalArgumentException("Illegal load factor: " +
          loadFactor);
    
    // set capacity to the highest power of two that's >= capacity, but not < 4
    initialCapacity =
        Math.max(MINIMUM_CAPACITY,
                 Integer.highestOneBit(initialCapacity * 2 - 1));
    
    head = new Element<E>(null, -1, -1, null, null, null);
    head.next = head.prev = head;
    
    this.table =
        (Element<E>[])Array.newInstance(head.getClass(),
                                     tableSize(initialCapacity, loadFactor));
    this.capacity = initialCapacity;
    this.factor = loadFactor;
    currentLimit = (int)(initialCapacity * factor);
  }
  
  public RandomAccessLinkedHashSet(Collection<E> init) {
    this();
    addAll(init);
  }
  
  
  /**
   * Returns the necessary internal table size to handle the given capacity and
   * load factor
   */
  private static int tableSize(int capacity, float loadFactor) {
    return Math.max(capacity * 2, capacity + (int)(capacity * loadFactor));
  }
  
  /** Converts a hashcode to an index in the table */
  private static int hashToIndex(int hash, int capacity) {
    // This function ensures that hashCodes that differ only by
    // constant multiples at each bit position have a bounded
    // number of collisions (approximately 8 at default load factor).
    // Borrowed from java.util.HashMap.
    hash ^= (hash >>> 20) ^ (hash >>> 12);
    hash ^= (hash >>> 7) ^ (hash >>> 4);
    return hash & (capacity - 1);
  }
  
  
  @SuppressWarnings("unchecked")
  private void resize(int newCapacity) {
    Element<E>[] newTable =
        (Element<E>[])Array.newInstance(head.getClass(),
                                     tableSize(newCapacity, factor));
    
    // copy the array
    System.arraycopy(table, capacity, newTable, newCapacity, size);
    
    // now rehash the elements
    for (Element<E> e = head.next; e != head; e = e.next) {
      // convert hash to table index
      int index = hashToIndex(e.hash, newCapacity);
      // put element into table
      e.bucketNext = newTable[index];
      newTable[index] = e;
    }
    
    table = newTable;
    capacity = newCapacity;
    currentLimit = (int)(capacity * factor);
  }
  
  public int size() {
    return size;
  }
  
  /**
   * Reduces the size of the internal table if needed. Similar in application
   * to java.util.ArrayList's trimToSize() method.
   * 
   * @param minimum
   *          the minimum desired capacity
   */
  public void shrink(int minimum) {
    // assume we'd like to hold at least 150% of our current size without
    // resizing again
    minimum = Math.max(MINIMUM_CAPACITY,
                       Integer.highestOneBit(minimum * 2 - 1));
    int goodCapacity =
        Math.max(minimum, Integer.highestOneBit(size * 3 - 1));
    if (goodCapacity < capacity)
      resize(goodCapacity);
  }
  
  public boolean isEmpty() {
    return size == 0;
  }
  
  public boolean contains(Object o) {
    return findElement(o) != null;
  }
  
  /**
   * Returns the element after the given one in the set's ordering.
   * 
   * @return
   *         the element after obj
   * @throws IllegalArgumentException
   *           if this set does not contain obj
   * @throws NoSuchElementException
   *           if obj is the last element in this set
   */
  public E getElementAfter(Object obj) {
    Element<E> find = findElement(obj);
    if (find == null)
      throw new IllegalArgumentException("No such element in this set");
    if ((find = find.next) == head)
      throw new NoSuchElementException("Last element in the set");
    return find.val;
  }
  
  /**
   * Returns the element before the given one in the set's ordering.
   * 
   * @return
   *         the element before obj
   * @throws IllegalArgumentException
   *           if this set does not contain obj
   * @throws NoSuchElementException
   *           if obj is the first element in this set
   */
  public E getElementBefore(Object obj) {
    Element<E> find = findElement(obj);
    if (find == null)
      throw new IllegalArgumentException("No such element in this set");
    if ((find = find.prev) == head)
      throw new NoSuchElementException("Last element in the set");
    return find.val;
  }
  
  /**
   * Returns the element after the given one in the set's ordering, or null
   * if there is no such element.
   * 
   * Use getElementAfter(Object) instead if nulls are important.
   * 
   * @return
   *         the element after obj, or null if either obj is not contained in
   *         this set or is the last element in the set's ordering
   */
  public E queryElementAfter(Object obj) {
    Element<E> find = findElement(obj);
    if (find == null 
        || (find = find.next) == head)
      return null;
    return find.val;
  }
  
  /**
   * Returns the element before the given one in the set's ordering, or null
   * if there is no such element.
   * 
   * Use getElementBefore(Object) instead if nulls are important.
   * 
   * @return
   *         the element before obj, or null if either obj is not contained in
   *         this set or is the first element in the set's ordering
   */
  public E queryElementBefore(Object obj) {
    Element<E> find = findElement(obj);
    if (find == null
        || (find = find.prev) == head)
      return null;
    return find.val;    
  }
  
  private Element<E> findElement(Object obj) {
    if (obj == null) return findNullElement();
    
    int hash = obj.hashCode();
    for (Element<E> bucket = table[hashToIndex(hash, capacity)];
        bucket != null;
        bucket = bucket.bucketNext) {
      E v;
      if (hash == bucket.hash
          && (obj == (v = bucket.val) || obj.equals(v))) {
        //System.out.println(iD + " contains " + o + " (true)");
        return bucket; // found it
      }
    }
    //System.out.println(iD + " contains " + o + " (false)");
    return null;
  }
  
  private Element<E> findNullElement() {
    for (Element<E> bucket = table[0];
        bucket != null;
        bucket = bucket.bucketNext)
      if (bucket.val == null)
        return bucket;
    return null;
  }
  
  public Iterator<E> iterator() {
    return new ElementIterator();
  }
  
  public Object[] toArray() {
    Object[] output = new Object[size];
    int i = 0;
    for (Element<?> t = head.next; t != head; t = t.next) {
      output[i++] = t.val;
    }
    //System.out.println(iD + " toarray " + Arrays.toString(output));
    return output;
  }
  
  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    //System.out.println(iD + " totypedarray " + a);
    T[] output = (a.length >= size)
        ? a
        : (T[])Array.newInstance(a.getClass().getComponentType(), size);
    int i = 0;
    for (Element<E> t = head.next; t != head; t = t.next, i++)
      output[i] = (T)t.val;
    
    //standards require that if the passed array is used and is oversized
    //for the number of elements output, the set must be null-terminated
    if (i < output.length)
      output[i] = null;
    return output;
  }
  
  public boolean add(E obj) {
    return add(obj, head);
  }
  
  private boolean add(E e, Element<E> before) {
    if (e == null) return addNull(before);
    
    int hash = e.hashCode();
    int index = hashToIndex(hash, capacity);
    
    for (Element<E> bucket = table[index];
        bucket != null;
        bucket = bucket.bucketNext) {
      //e.hash == hash && ((k = e.key) == key || key.equals(k))
      E v;
      if (hash == bucket.hash
          && (e == (v = bucket.val) || e.equals(v))) {
        //System.out.println(iD + " add " + e + " (false)");
        return false; // already contained e
      }
    }
    // Add e & return true
    addNew(hash, e, index, before);
    //System.out.println(iD + " add " + e + " (true)");
    return true;
  }
  
  private boolean addNull(Element<E> before) {
    for (Element<E> bucket = table[0];
        bucket != null;
        bucket = bucket.bucketNext) {
      if (bucket.val == null)
        return false; // already contained null
    }
    // Add null & return true
    addNew(0, null, 0, before);
    return true;
  }
  
  /** Adds a new Element entry into the bucket at index */
  private void addNew(int hash, E value, int index, Element<E> before) {
    modCount++;
    if (size + capacity >= table.length
        || (size >= currentLimit && table[index] != null)) {
      resize(capacity * 2);
      index = hashToIndex(hash, capacity);
    }
    
    before.prev = before.prev.next = table[capacity + size] = table[index] =
        new Element<E>(value, hash, size, table[index], before.prev, before);
    size++;
    totalHash += hash;
  }
  
  public boolean remove(Object o) {
    if (o == null) return removeNull();
    
    int hash = o.hashCode();
    int index = hashToIndex(hash, capacity);
    
    for (Element<E> bucket = table[index], previous = bucket;
        bucket != null;
        previous = bucket, bucket = bucket.bucketNext) {
      E v;
      if (hash == bucket.hash && (o == (v = bucket.val) || o.equals(v))) {
        // found element
        modCount++;
        removeElement(bucket, previous, index);
        totalHash -= bucket.hash;
        //System.out.println(iD + " remove " + o + " (true)");
        return true;
      }
    }
    //System.out.println(iD + " remove " + o + " (false)");
    return false;
  }
  
  private boolean removeNull() {
    for (Element<E> bucket = table[0], previous = bucket;
        bucket != null;
        previous = bucket, bucket = bucket.bucketNext) {
      if (bucket.val == null) {
        // found element
        modCount++;
        removeElement(bucket, previous, 0);
        return true;
      }
    }
    return false;
  }
  
  private void removeElement(Element<E> element, Element<E> bucketPrevious,
                             int bucketIndex) {
    size--;
    // remove from bucket
    if (bucketPrevious == element)
      table[bucketIndex] = element.bucketNext;
    else
      bucketPrevious.bucketNext = element.bucketNext;
    
    // remove from array
    // move the last element in the array to this one's position
    int arrayIndex = element.index;
    Element<E> moving = table[capacity + size];
    table[capacity + size] = null;
    table[capacity + arrayIndex] = moving;
    // element must know its new location in the array
    moving.index = arrayIndex;
    
    // remove from linked list
    element.prev.next = element.next;
    element.next.prev = element.prev;
  }
  
  public boolean containsAll(Collection<?> c) {
    if (c == this) return true;
    
    for (Object obj : c)
      if (!contains(obj))
        return false;
    return true;
  }
  
  public boolean addAll(Collection<? extends E> c) {
    if (c == this) return false;
    
    boolean changed = false;
    for (E obj : c)
      if (add(obj))
        changed = true;
    return changed;
  }
  
  public boolean retainAll(Collection<?> c) {
    if (c == this) return false;
    
    boolean changed = false;
    for (Element<E> e = head.next; e != head; e = e.next) {
      if (!c.contains(e.val)) {
        remove(e.val);
        changed = true;
      }
    }
    return changed;
  }
  
  public boolean removeAll(Collection<?> c) {
    if (c == this) {
      if (isEmpty()) {
        return false;
      } else {
        clear();
        return true;
      }
    }
    
    boolean changed = false;
    for (Object obj : c)
      if (remove(obj))
        changed = true;
    return changed;
  }
  
  public void clear() {
    //System.out.println(iD + " clear");
    modCount++;
    Arrays.fill(table, null);
    head.next = head.prev = head;
    size = 0;
    totalHash = 0;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    boolean first = true;
    for (E e : this) {
      if (first)
        first = false;
      else
        sb.append(", ");
      sb.append(e);
    }
    sb.append(']');
    return sb.toString();
  }
  
  @Override
  public int hashCode() {
    return totalHash;
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Set))
      return false;
    if (((Set<?>)o).size() != size)
      return false;
    if (!containsAll((Set<?>)o))
        return false;
    return true;
  }
  
  public E getArbitraryIndex(int index) {
    if (index < 0 || index >= size)
      throw new IndexOutOfBoundsException();
    return table[capacity + index].val;
  }
  
  /**
   * Returns a random element using the given random number generator.
   * Returns null if the set is empty.
   */
  public E randomElement(Random rand) {
    return size > 0
        ? table[capacity + rand.nextInt(size)].val
        : null;
  }
  
  //***** METHODS FROM DEQUE 
  
  public void addFirst(E obj) {
    if (!add(obj, head.next))
      throw new IllegalArgumentException("The element is already in the set.");
  }
  
  public void addLast(E obj) {
    if (!add(obj, head))
      throw new IllegalArgumentException("The element is already in the set.");
  }
  
  public Iterator<E> descendingIterator() {
    return new ReverseElementIterator();
  }
  
  public E element() {
    return getFirst();
  }
  
  public E getFirst() {
    if (size == 0)
      throw new NoSuchElementException();
    return head.val;
  }
  
  public E getLast() {
    if (size == 0)
      throw new NoSuchElementException();
    return head.prev.val;
  }
  
  public boolean offer(E obj) {
    return add(obj, head);
  }
  
  public boolean offerFirst(E obj) {
    return add(obj, head.next);
  }
  
  public boolean offerLast(E obj) {
    return add(obj, head);
  }
  
  public E peek() {
    return peekFirst();
  }
  
  public E peekFirst() {
    return size == 0
        ? null
        : head.next.val;
  }
  
  public E peekLast() {
    return size == 0
        ? null
        : head.prev.val;
  }
  
  public E poll() {
    return pollFirst();
  }
  
  public E pollFirst() {
    if (size == 0)
      return null;
    E value = head.next.val;
    remove(value);
    return value;
  }
  
  public E pollLast() {
    if (size == 0)
      return null;
    E value = head.prev.val;
    remove(value);
    return value;
  }
  
  public E pop() {
    return removeFirst();
  }
  
  public void push(E obj) {
    addFirst(obj);
  }
  
  public E remove() {
    return removeFirst();
  }
  
  public E removeFirst() {
    if (size == 0)
      throw new NoSuchElementException();
    E value = head.next.val;
    remove(value);
    return value;
  }
  
  public boolean removeFirstOccurrence(Object obj) {
    return remove(obj);
  }
  
  public E removeLast() {
    if (size == 0)
      throw new NoSuchElementException();
    E value = head.prev.val;
    remove(value);
    return value;
  }
  
  public boolean removeLastOccurrence(Object obj) {
    return remove(obj);
  }
  
  public ListIterator<E> listIterator() {
    return new ElementListIterator(true);
  }
  
  public ListIterator<E> listIteratorFromLast() {
    return new ElementListIterator(false);
  }
  
  
  //***** INNER CLASSES
  
  private static class Element<E> {
    E val; // value
    int hash; // hashcode
    int index; // index in array
    Element<E> bucketNext; // next bucket element
    Element<E> next; // next linkedlist item
    Element<E> prev; // previous linkedlist item
    
    Element(E value, int hashCode, int arrayIndex,
            Element<E> bucketNextElement,
            Element<E> previousElement, Element<E> nextElement) {
      val = value;
      hash = hashCode;
      index = arrayIndex;
      bucketNext = bucketNextElement;
      prev = previousElement;
      next = nextElement;
    }
  }

  private class ElementIterator implements Iterator<E> {
    /*
    static int nextID = 0; 
    final int iD = nextID++;
    */
    
    //RandomAccessLinkedHashSet<E> set;
    Element<E> current, next;
    int expectedModCount;
    
    ElementIterator() { //RandomAccessLinkedHashSet<E> set) {
      //System.out.println(set.iD + "it" + iD + " iterator!");
      
      //this.set = set;
      next = head.next;
      expectedModCount = modCount;
    }
    
    public boolean hasNext() {
      //System.out.println(set.iD + "it" + iD + " hasnext");
      return next != head;
    }
    
    public E next() {
      if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
      if (next == head)
        throw new NoSuchElementException();
      current = next;
      next = current.next;
      //System.out.println(set.iD + "it" + iD + " next (" + current.val + ")");
      return current.val;
    }
    
    public void remove() {
      //System.out.println(set.iD + "it" + iD + " remove");
      if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
      if (current == null)
        throw new IllegalStateException();
      RandomAccessLinkedHashSet.this.remove(current.val);
      current = null;
      expectedModCount = modCount;
    }
  }

  private class ReverseElementIterator implements Iterator<E> {
    /*
    static int nextID = 0; 
    final int iD = nextID++;
    */
    
    //RandomAccessLinkedHashSet<E> set;
    Element<E> current, prev;
    int expectedModCount;
    
    ReverseElementIterator() {//RandomAccessLinkedHashSet<E> set) {
      //System.out.println(set.iD + "it" + iD + " iterator!");
      
      //this.set = set;
      prev = head.prev;
      expectedModCount = modCount;
    }
    
    public boolean hasNext() {
      //System.out.println(set.iD + "it" + iD + " hasnext");
      return prev != head;
    }
    
    public E next() {
      if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
      if (prev == head)
        throw new NoSuchElementException();
      current = prev;
      prev = prev.prev;
      //System.out.println(set.iD + "it" + iD + " next (" + current.val + ")");
      return current.val;
    }
    
    public void remove() {
      //System.out.println(set.iD + "it" + iD + " remove");
      if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
      if (current == null)
        throw new IllegalStateException();
      RandomAccessLinkedHashSet.this.remove(current.val);
      current = null;
      expectedModCount = modCount;
    }
  }

  private class ElementListIterator implements ListIterator<E> {
    //RandomAccessLinkedHashSet<E> set;
    Element<E> prev, current, next;
    int prevIndex, nextIndex;//, currentIndex;
    int expectedModCount;
    
    ElementListIterator(//RandomAccessLinkedHashSet<E> set,
                               Boolean fromFirst) {
      //this.set = set;
      expectedModCount = modCount;
      if (fromFirst) {
        next = head.next;
        prev = head;
        prevIndex = -1;
        nextIndex = 0;
      } else {
        next = head;
        prev = head.prev;
        nextIndex = size;
        prevIndex = nextIndex - 1;
      }
    }
  
    public boolean hasNext() {
      return next != head;
    }
  
    public boolean hasPrevious() {
      return prev != head;
    }
  
    public E next() {
      if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
      if (next == head)
        throw new NoSuchElementException();
      current = next;
      //currentIndex = nextIndex;
      //nextIndex = currentIndex + 1;
      nextIndex++;
      prevIndex++;
      prev = current.prev;
      next = current.next;
      return current.val;
    }
  
    public E previous() {
      if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
      if (prev == head)
        throw new NoSuchElementException();
      current = prev;
      //currentIndex = prevIndex;
      //nextIndex = currentIndex + 1;
      nextIndex = prevIndex + 1;
      prevIndex--;
      prev = current.prev;
      next = current.next;
      return current.val;
    }
    
    public int nextIndex() {
      return nextIndex;
    }
  
    public int previousIndex() {
      return prevIndex;
    }
    
    public void remove() {
      if (expectedModCount != modCount)
        throw new ConcurrentModificationException();
      if (current == null)
        throw new IllegalStateException();
      RandomAccessLinkedHashSet.this.remove(current.val);
      current = null;
      nextIndex--;
      expectedModCount = modCount;
    }
    
    public void add(E arg0) {
      throw new UnsupportedOperationException();
    }
  
    public void set(E arg0) {
      throw new UnsupportedOperationException();
    }
  }
}