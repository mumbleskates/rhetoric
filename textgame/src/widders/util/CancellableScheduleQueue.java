package widders.util;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * 
 * @author widders
 *
 * @param <E>
 */
public class CancellableScheduleQueue<E> {
  private Node[] queue;
  private int size;
  
  private static final int DEFAULT_SIZE = 11;
  
  public CancellableScheduleQueue() {
    this(DEFAULT_SIZE);
  }
  
  @SuppressWarnings("unchecked")
  public CancellableScheduleQueue(int capacity) {
    queue = (Node[])Array.newInstance(Node.class, capacity);
  }
  
  public int size() {
    return size;
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
  
  public Cancellable add(E value, long time) {
    ensureCapacity(size + 1);
    Node n = new Node(value, time);
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
  
  public long peekTime() {
    if (size == 0) return Long.MAX_VALUE;
    return queue[0].time;
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
    while (index > 0) { // stop traversing at the root
      int parentIndex = (index - 1) >>> 1;
      Node parent = queue[parentIndex];
      if (insert.time >= parent.time)
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
    int half = size >>> 1;
    while (index < half) { // stop traversing at a leaf
      
      // get children's indices
      int childIndex = (index << 1) + 1;
      Node child = queue[childIndex];
      int rightIndex = childIndex + 1;
      if (rightIndex < size && //don't go out of bounds
          child.time > queue[rightIndex].time) {
        child = queue[childIndex = rightIndex]; // index points to priority child
      }
      
      if (insert.time <= child.time)
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
    long time;
    E value;
    int index;
    
    Node(E value, long time) {
      this.value = value;
      this.time = time;
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
}