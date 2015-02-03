package widders.util;

import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Acts as a heap which sorts by time, returning items with the lowest time
 * number first, without limiting time numbers in any way. Events with the same
 * time value have no guaranteed return order.
 * 
 * @author widders
 */
public class ScheduleQueue<E> implements Iterable<E> {
  private PriorityQueue<Node<E>> queue;
  
  ScheduleQueue() {
    queue = new PriorityQueue<Node<E>>();
  }
  
  ScheduleQueue(int initialSize) {
    queue = new PriorityQueue<Node<E>>(initialSize);
  }
  
  /** Returns the number of items in the heap */
  public int size() {
    return queue.size();
  }
  
  /** Adds the given value to the heap with the given time */
  public void add(long time, E value) {
    queue.add(new Node<E>(time, value));
  }
  
  /**
   * Returns the time of the event that's next up for dequeueing, or
   * Long.MAX_VALUE if there is no event pending
   */
  public long peekTime() {
    Node<E> peek = queue.peek();
    return peek == null ? Long.MAX_VALUE : peek.time;
  }
  
  /**
   * Returns the value of the event that's next up for dequeueing, or null if
   * there is no event pending
   */
  public E peek() {
    Node<E> peek = queue.peek();
    return peek == null ? null : peek.value;
  }
  
  /**
   * Dequeues the next pending event; returns null if there is no event
   * pending
   */
  public E pop() {
    Node<E> poll = queue.poll();
    return poll == null ? null : poll.value;
  }
  
  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      Iterator<Node<E>> it = queue.iterator();
      public boolean hasNext() { return it.hasNext(); }
      public E next() { return it.next().value; }
      public void remove() { throw new UnsupportedOperationException(); }
    };
  }
  
  
  
  private static class Node<E> implements Comparable<Node<E>> {
    long time;
    E value;
    
    Node(long t, E v) {
      time = t;
      value = v;
    }
    
    @Override
    public int compareTo(Node<E> o) {
      if (time < o.time) return -1;
      if (time == o.time) return 0;
      return 1;
    }
  }
}