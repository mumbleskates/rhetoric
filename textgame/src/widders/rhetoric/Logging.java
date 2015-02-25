package widders.rhetoric;

import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author widders
 */
public class Logging extends Thread {
  private static final int MIN_BUFFER_SIZE = 1 << 12;
  private static final int LOG_WARNING_MASK = 255;
  private static final float LOG_DEWARN_THRESHOLD = .85f;
  
  private final int maxBufferSize;
  private final int bufferDewarnSize;
  private AtomicInteger currentBufferSize = new AtomicInteger();
  private AtomicInteger bufferWarningCounter = new AtomicInteger();
//  private int currentBufferSize = 0;
//  private int bufferWarningCounter = 0;
  
  private BlockingQueue<LogEvent> queue;
  private PrintStream out;
  private volatile boolean alive = true;
  private Thread shutdownHook;
  
  /** Creates a new instance of Logging */
  public Logging(PrintStream output, int maxBuffer) {
    super("Logging");
    queue = new LinkedBlockingQueue<LogEvent>();
    maxBufferSize = Math.max(MIN_BUFFER_SIZE, maxBuffer);
    bufferDewarnSize = (int)(maxBufferSize * LOG_DEWARN_THRESHOLD);
    
    out = output;
    shutdownHook = new Thread() {
      
      @Override
      public void run() {
        outputStandard(Main.dateFormat.format(new Date(Chronology.now())) + "(-)"
               + " Logging terminating: System shutting down");
        outputShutdown();
        alive = false;
        Logging.this.interrupt();
        
        synchronized(out) {
          while (!queue.isEmpty()) // dump entire queue
            output(queue.poll());
        }
        
        outputStandard(Main.dateFormat.format(new Date(Chronology.now())) + "(-)"
            + " Logging: shut down");
      }
      
    };
    
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    outputStandard(Main.dateFormat.format(new Date(Chronology.now()))
           + "(-) Logging: started");
    start();
  }
  
  @Override
  public void run() {
    do {
      try {
        LogEvent event = queue.take();
        synchronized (out) {
          output(event);
        }
        
        // check dewarn threshold
        if (bufferWarningCounter.get() > 0
            && currentBufferSize.get() <= bufferDewarnSize) {
          // reset counter if the logger starts catching up
          bufferWarningCounter.set(0);
          outputQueueDewarn();
        }
      } catch (InterruptedException e) { }
    } while (alive);// || !queue.isEmpty());
    
    outputShutdown();
  }
  
  public void p(String topic, String s) {
    LogEvent event = new LogEvent(topic, s);
    queue.offer(event);
    currentBufferSize.addAndGet(event.size);
    
    ///// TODO test whether it's faster to recruit to expel events or to block on a has-space condition
    while (currentBufferSize.get() >= maxBufferSize) {
      // output periodic warnings
      if ((bufferWarningCounter.get() & LOG_WARNING_MASK) == 0)
        outputQueueWarning();
      bufferWarningCounter.incrementAndGet();
      
      // expel one log event to free up some memory
      synchronized (out) {
        output(queue.poll());
      }
    }
  }
  
  public void p(String topic, Report r) {
    p(topic, r.text());
  }
  
  public String loggingState() {
    return (queue.size() > 0 ? (Chronology.now() - queue.peek().time) : 0)
        + "ms delayed"
        + ", " + queue.size() + " logs to push"
        + ", ~" + currentBufferSize.get() + "B buffered"
        + " (" + ((float)currentBufferSize.get() / maxBufferSize) + "x max)"
        + ", " + bufferWarningCounter + " warns";
  }
  
  private void outputQueueWarning() {
    long now = Chronology.now();
    String warn = Main.dateFormat.format(new Date(now)) + "(-)"
        + " Logging: LOGJAM WARNING - "
        + loggingState();
    
    outputStandard(warn);
  }
  
  private void outputQueueDewarn() {
    long now = Chronology.now();
    LogEvent event = queue.peek();
    String dewarn;
    
    if (event == null) {
      dewarn = Main.dateFormat.format(new Date(now)) + "(-)"
          + " Logging: Logjam cleared... queue empty? "
          + loggingState();
    } else {
      dewarn = Main.dateFormat.format(new Date(now)) + "(-)"
          + " Logging: Logjam cleared - "
          + loggingState();
    }
    
    outputStandard(dewarn);
  }
  
  private void outputShutdown() {
    long now = Chronology.now();
    outputStandard(Main.dateFormat.format(new Date(now)) + "(-)"
        + " Logging: shutting down - "
        + loggingState());
  }
  
  private void output(LogEvent event) {
    String topic = event.topic;
    String s;
    long time;
    
//    if (topic == "creation"
//        || topic == "destruction")
//      return;
    
    s = event.s;
    time = event.time;
    outputVerbose(Main.dateFormat.format(new Date(time))
           + "(+" + (Chronology.now() - time) + ") "
           + topic + ": " + s);
    currentBufferSize.addAndGet(-event.size);
  }
  
  private void outputVerbose(String s) {
    synchronized(out) {
      out.println(s);
    }
  }
  
  private void outputStandard(String s) {
    outputVerbose(s);
    System.out.println(s);
  }
  
  public void shutdown() {
    alive = false;
    interrupt();
    Runtime.getRuntime().removeShutdownHook(shutdownHook);
    shutdownHook = null;
    try {
      join();
    } catch (InterruptedException e) { }
    
    synchronized(out) {
      while (!queue.isEmpty()) // dump entire queue
        output(queue.poll());
    }
    
    outputStandard(Main.dateFormat.format(new Date(Chronology.now()))
           + "(-) Logging: shut down");
  }
  
  
  private static class LogEvent {
    private static final int OVERHEAD = 64;
    long time;
    int size;
    String topic;
    String s;
    
    LogEvent(String topic, String s) {
      this.topic = topic;
      this.s = s;
      size = topic.length() + s.length() + OVERHEAD;
      this.time = Chronology.now();
    }
  }
}