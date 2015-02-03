package widders.util;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 
 * @author widders
 *
 * @param <E>
 */
public class CancellablePriorityQueue<E> {
  private Node[] queue;
  private int size;
  private Comparator<? super E> comparator;
  
  private static final int DEFAULT_SIZE = 11;
  
  public CancellablePriorityQueue() {
    this(DEFAULT_SIZE, null);
  }
  
  public CancellablePriorityQueue(int capacity) {
    this(capacity, null);
  }
  
  @SuppressWarnings("unchecked")
  public CancellablePriorityQueue(int capacity, Comparator<? super E> comparator) {
    queue = (Node[])Array.newInstance(Node.class, capacity);
    this.comparator = comparator;
  }
  
  /** Ensures the queue can hold at least [capacity] elements. */
  private void ensureCapacity(int capacity) {
    if (queue.length >= capacity)
      return;
    
    int oldCapacity = queue.length;
    int newCapacity = oldCapacity + ((oldCapacity < 64)
        ? (oldCapacity + 2)
        : (oldCapacity >> 1));
    queue = Arrays.copyOf(queue, newCapacity);
  }
  
  public int size() {
    return size;
  }
  
  public void add(E value) {
    addCancellable(value);
  }
  
  public Cancellable addCancellable(E value) {
    ensureCapacity(size + 1);
    Node n = new Node(value);
    siftTowardsRoot(size++, n);
    return n;
  }
  
  /**
   * Removes an arbitrary element whose value is equal to [value], cancelling
   * that entry's Cancellable.
   * Returns true if an element was removed.
   */
  public boolean remove(E value) {
    for (int i = 0; i < size; i++) {
      if (queue[i].value.equals(value)) {
        removeAt(i).cancelled = true;
        return true;
      }
    }
    return false;
  }
  
  public E peek() {
    if (size == 0) return null;
    return queue[0].value;
  }
  
  public E poll() {
    if (size == 0) return null;
    size--;
    Node result = queue[0];
    
    // grab last element in queue and sift it out from the root
    Node pullDown = queue[size];
    queue[size] = null;
    if (size > 0) // no need to do anything if it's empty now
      siftTowardsLeaf(0, pullDown);
    
    result.hasRun = true;
    return result.value;
  }
  
  /** Returns true iff this queue contains an element that equals [value]. */
  public boolean contains(E value) {
    for (Node n : queue) {
      if (n.value.equals(value)) return true;
    }
    return false;
  }
  
  /**
   * Sifts the element [key] towards the root of the queue, starting at and
   * replacing the element at [index].
   */
  private void siftTowardsRoot(int index, Node insert) {
    if (comparator == null)
      siftTowardsRootComparable(index, insert);
    else
      siftTowardsRootComparator(index, insert);
  }
  
  @SuppressWarnings("unchecked")
  private void siftTowardsRootComparable(int index, Node insert) {
    while (index > 0) { // stop traversing at the root
      int parentIndex = (index - 1) >>> 1;
      Node parent = queue[parentIndex];
      if (((Comparable<? super E>)insert.value).compareTo(parent.value) >= 0)
        break; // done sifting, parent has priority
      
      // bump parent up the tree to the child position we are visiting
      queue[index] = parent;
      parent.index = index;
      
      index = parentIndex;
    } // upon break, index contains insertion location
    queue[index] = insert;
    insert.index = index;
  }

  private void siftTowardsRootComparator(int index, Node insert) {
    while (index > 0) { // stop traversing at the root
      int parentIndex = (index - 1) >>> 1;
      Node parent = queue[parentIndex];
      if (comparator.compare(insert.value, parent.value) >= 0)
        break; // done sifting, parent has priority
      
      // bump parent up the tree to the child position we are visiting
      queue[index] = parent;
      parent.index = index;
      
      index = parentIndex;
    } // upon break, index contains insertion location
    queue[index] = insert;
    insert.index = index;
  }
  
  /**
   * Sifts the element [key] towards the leaves of the queue, starting at and
   * replacing the element at [index].
   */
  private void siftTowardsLeaf(int index, Node insert) {
    if (comparator == null)
      siftTowardsLeafComparable(index, insert);
    else
      siftTowardsLeafComparator(index, insert);
  }
  
  @SuppressWarnings("unchecked")
  private void siftTowardsLeafComparable(int index, Node insert) {
    int half = size >>> 1;
    while (index < half) { // stop traversing at a leaf
    
      // get children's indices
      int childIndex = (index << 1) + 1;
      Node child = queue[childIndex];
      int rightIndex = childIndex + 1;
      if (rightIndex < size //don't go out of bounds
          && ((Comparable<? super E>)child.value)
              .compareTo(queue[rightIndex].value) > 0) {
        child = queue[childIndex = rightIndex]; // index points to priority child
      }
      
      if (((Comparable<? super E>)insert.value).compareTo(child.value) <= 0)
        break; // stop traversing when key is higher priority than the children
        
      // bump child down the tree to the parent position we are visiting
      queue[index] = child;
      child.index = index;
      
      index = childIndex;
    }
    queue[index] = insert;
    insert.index = index;
  }
  
  private void siftTowardsLeafComparator(int index, Node insert) {
    int half = size >>> 1;
    while (index < half) { // stop traversing at a leaf
    
      // get children's indices
      int childIndex = (index << 1) + 1;
      Node child = queue[childIndex];
      int rightIndex = childIndex + 1;
      if (rightIndex < size && //don't go out of bounds
          comparator.compare(child.value, queue[rightIndex].value) > 0) {
        child = queue[childIndex = rightIndex]; // index points to priority child
      }
      
      if (comparator.compare(insert.value, child.value) <= 0)
        break; // stop traversing when key is higher priority than the children
      
      // bump child down the tree to the parent position we are visiting
      queue[index] = child;
      child.index = index;
      
      index = childIndex;
    }
    queue[index] = insert;
    insert.index = index;
  }
  
  /** Removes the element at [index] from the queue, then returns it. */
  private Node removeAt(int index) {
    Node ret = queue[index];
    
    size--;
    if (index == size) {
      queue[index] = null;
    } else {
      // grab last element in queue
      Node pullDown = queue[size];
      queue[size] = null;
      // sift it outwards leaves...
      siftTowardsLeaf(index, pullDown);
      // and then, if it didn't move, back towards the root
      if (queue[index] == pullDown)
        siftTowardsRoot(index, pullDown);
    }
    
    ret.index = -1; // has been removed from queue, break the index
    return ret;
  }
  
  
  
  private class Node implements Cancellable {
    boolean hasRun = false;
    boolean cancelled = false;
    E value;
    int index;
    
    Node(E value) {
      this.value = value;
    }
    
    @Override
    public boolean hasRun() {
      return hasRun;
    }
    
    @Override
    public boolean isCancelled() {
      return cancelled;
    }
    
    @Override
    public boolean cancel() {
      if (cancelled) return true;
      if (hasRun) return false;
      removeAt(index);
      return (cancelled = true);
    }
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CancellablePriorityQueue\n");
    if (size == 0) {
      sb.append("[[EMPTY]]");
    } else {
      convertToStringBrackets(sb, 0);
      //convertToStringTabbed(sb, 0, 1);
    }
    return sb.toString();
  }
  
  private void convertToStringBrackets(StringBuilder sb, int index) {
    sb.append('[');
    sb.append(queue[index].value.toString());
    if ((index << 1) + 1 < size) {
      sb.append(' ');
      convertToStringBrackets(sb, (index << 1) + 1);
      if ((index << 1) + 2 < size) {
        sb.append(" / ");
        convertToStringBrackets(sb, (index << 1) + 2);
      }
    }
    sb.append(']');
  }
  
  /* private
      void convertToStringTabbed(StringBuilder sb, int index, int iteration) {
    for (int i = 0; i < iteration; i++)
      sb.append('\t');
    sb.append(queue[index].value.toString());
    if ((index << 1) + 1 < size) {
      sb.append('\n');
      convertToStringTabbed(sb, (index << 1) + 1, iteration + 1);
      if ((index << 1) + 2 < size) {
        sb.append("\n");
        convertToStringTabbed(sb, (index << 1) + 2, iteration + 1);
      }
    }
  } */
}