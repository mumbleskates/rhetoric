package widders.rhetoric;

import java.util.concurrent.LinkedBlockingQueue;

public class Tasking {
  private static final LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
  
  public static int queueCount() {
    return taskQueue.size();
  }
  
  public static void queue(Runnable task) {
    taskQueue.add(task);
  }
  
  private static void processTask()
      throws InterruptedException {
    taskQueue.take().run();
  }
}