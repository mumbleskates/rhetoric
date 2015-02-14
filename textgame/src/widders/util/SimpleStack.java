package widders.util;

import java.util.Arrays;

public class SimpleStack<E> {
  private Object[] elements;
  private int count;
  
  public SimpleStack() {
    elements = new Object[10];
    count = 0;
  }
  
  public void push(E obj) {
    if (count + 1 == elements.length)
      elements = Arrays.copyOf(elements, elements.length * 2);
    
    elements[count++] = obj;
  }
  
  @SuppressWarnings("unchecked")
  public E pop() {
    return (E)elements[--count];
  }
  
  public boolean isEmpty() {
    return count == 0;
  }
}