package widders.util;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ManualFlag extends Flag {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();
  private boolean set;
  
  public ManualFlag(boolean state) {
    set = state;
  }
  
  public ManualFlag() {
    this(false);
  }
  
  @Override
  public void set() {
    lock.lock();
    try {
      set = true;
      condition.signalAll();
    } finally {
      lock.unlock();
    }
  }
  
  public void reset() {
    lock.lock();
    try {
      set = false;
    } finally {
      lock.unlock();
    }
  }
  
  @Override
  public void await() {
    lock.lock();
    while (true) {
      try {
        while (!set)
          condition.await();
        return;
      } catch (InterruptedException ex) {
        continue;
      } finally {
        lock.unlock();
      }
    }
  }
}
