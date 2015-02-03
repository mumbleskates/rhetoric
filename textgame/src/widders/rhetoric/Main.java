package widders.rhetoric;

import widders.rhetoric.content.*;
import widders.util.*;
import static widders.rhetoric.Units.*;

import java.io.File;
import java.io.PrintStream;
import java.text.*;
import java.util.*;

import com.google.common.collect.testing.MinimalCollection;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.SetFeature;

import junit.framework.TestFailure;
import junit.framework.TestResult;


/**
 * 
 * @author widders
 */
public class Main {
  
  public static final int SCHEDULER_THREAD_COUNT = 3;
  public static final int LOGGING_BUFFER = 1 << 24;
  
  /** The main Debug object */
  public static Logging logger;
  
  public static final DateFormat dateFormat =
      new SimpleDateFormat("y.MM.dd HH:mm:ss.SSS");
  public static final DateFormat fileNameDateFormat =
      new SimpleDateFormat("y-MM-dd_HH-mm-ss.SSS");
  
  /** The room for Physics, Chronology, and other engine objects */
  public static final Room debugRoom = new DebugRoom();
  /** The new object placement actor */
  public static final Active creator = new Creator(debugRoom, "in");
  /** The main Physics object */
  public static final Active physics = new Physics(debugRoom, "in");
  /** The main Chronology object */
  public static final Chronology chrono =
      new Chronology(debugRoom, "in",
                     SCHEDULER_THREAD_COUNT);
  /** The game's official random number generator */
  public static final Random rand = new Random();
  /** A dummy report for general use */
  public static final Report fakeReport = new Report() {
    @Override
    public void report(String text) {
    }
    
    @Override
    public String text() {
      return null;
    }
  };
  
  static {
    Calendar cal = new GregorianCalendar(new SimpleTimeZone(0, "GMT"));
    dateFormat.setCalendar(cal);
    fileNameDateFormat.setCalendar(cal);
  }
  
  
  /**
   * @param args
   *          the command line arguments
   */
  public static void main(String[] args) {
    ////// PARSE ARGS ///////
    
    // No args at this time
    
    ////// STATIC INIT //////
    
    try {
      // Start the logger
      try {
        logger = new Logging(new PrintStream(new File("f:/temp/rhetoric_logs/log_"
            + fileNameDateFormat.format(new Date())
            + ".log")), LOGGING_BUFFER);
        //        new Logging(System.out, LOGGING_BUFFER);
      } catch (Exception ex) {
        throw new Error("Could not initialize logger", ex);
      }
      
      
      try {
        creator.init();
        physics.init();
        chrono.init();
      } catch (DoesNotFitException ex) {
        log("STATIC INSTANTIATION", "ERROR! " + ex.getMessage());
        throw new Error("Could not instantiate base game objects in debugRoom",
                        ex);
      }
      
      ////////// RUN //////////
      
      test();
    } catch (Throwable ex) {
      ex.printStackTrace();
    } finally {
      //////// CLEANUP ////////
      
      if (logger != null) logger.shutdown();
      else System.out.println("Logger did not instantiate");
      System.exit(0);
    }
  }
  
  static void test() {
    testValues();
    testSPQ();
    testSets();
    testContent();
  }
  
  static void testSets() {
    TestResult test = new TestResult();
    
    com.google.common.collect.testing.SetTestSuiteBuilder
        .using(new TestStringSetGenerator() {
          @Override
          protected Set<String> create(String[] elements) {
            return new RandomAccessLinkedHashSet<String>(MinimalCollection
                .of(elements));
          }
        }).named("RandomAccessLinkedHashSet")
        .withFeatures(SetFeature.GENERAL_PURPOSE,
                      CollectionFeature.ALLOWS_NULL_VALUES,
                      CollectionFeature.KNOWN_ORDER,
                      CollectionSize.ANY)
        .createTestSuite()
        .run(test);
    
    log("set test", "successful: " + test.wasSuccessful()
        + " (" + test.failureCount() + " failures)");
    Enumeration<junit.framework.TestFailure> failures = test.failures();
    //test.
    while (failures.hasMoreElements()) {
      TestFailure tf = failures.nextElement();
      log("set test", "error: " + tf.exceptionMessage());
      log("set test", "trace: " + tf.trace());
    }
  }
  
  static void testContent() {
    final long time = System.currentTimeMillis();
    final Room wablroom = new Room(new BasicName("wablroom"), "wablwablwabl",
                                   1000d * CU_METER, 14d * METER, 8d * METER);
    Active fridge = null;
    log("content test", "dimensions - width=" + wablroom.widthLimit() + "m");
    try {
      (fridge = new Refrigerator(wablroom, "in")).init();
      new Refrigerator(wablroom, "in").init();
      new Refrigerator(wablroom, "in").init();
      new Refrigerator(wablroom, "in").init();
      try {
        for (;;)
          //(int i = 0; i < 10; i++)
          new PointyStick(wablroom, "in").init();
      } catch (DoesNotFitException ex) { }
      try {
        for (;;)
          //(int i = 0; i < 10; i++)
          new PointyStick(fridge, "in").init();
      } catch (DoesNotFitException ex) { }
      new Refrigerator(wablroom, "in").init();
    } catch (DoesNotFitException ex) { }
    log("content test", wablroom + " has " + wablroom.contentWeight()
        + "kg of contents and has " + wablroom.availableSize()
        + " m3 of space left inside (" + wablroom.contentSize()
        + "m3 of contents)");
    log("content test", wablroom + " contains " + wablroom.contentCount()
        + " items (" + wablroom.contentCountDeep() + " total)");
    log("content test", fridge + " weighs " + fridge.weight() + "kg"
        + " (" + fridge.contentWeight() + "kg of contents)");
    
    log("content test", System.currentTimeMillis() - time + "ms elapsed");
    log("content test", "destroying all contents");
    //Selection sel = new Selection();
    //sel.select(wablroom, null, null, 0, -1);
    Selection sel = Selection.selectAll(wablroom);
    for (Active a : sel)
      a.destroy(creator);
    
    log("content test", System.currentTimeMillis() - time + "ms elapsed");
    log("content test", wablroom + " has " + wablroom.contentWeight()
        + "kg of contents and has " + wablroom.availableSize()
        + " m3 of space left inside (" + wablroom.contentSize()
        + "m3 of contents)");
    log("content test", wablroom + " contains " + wablroom.contentCount()
        + " items (" + wablroom.contentCountDeep() + " total)");
    
    log("content test", "executing cube fill");
    log("content test", "created " + cubeFill(wablroom, 5 * METER, 6)
        + " cubes");
    
    log("content test", System.currentTimeMillis() - time + "ms elapsed");
    log("content test", wablroom + " has " + wablroom.contentWeight()
        + "kg of contents and has " + wablroom.availableSize()
        + " m3 of space left inside (" + wablroom.contentSize()
        + "m3 of contents)");
    log("content test", System.currentTimeMillis() - time + "ms elapsed");
    log("content test", wablroom + " contains " + wablroom.contentCount()
        + " items (" + wablroom.contentCountDeep() + " total)");
    
    log("content test", System.currentTimeMillis() - time + "ms elapsed");
    log("content test", "destroying all contents");
    wablroom.destroy(creator);
    log("content test", System.currentTimeMillis() - time + "ms elapsed");
    
    log("content test", wablroom + " has " + wablroom.contentWeight()
        + "kg of contents and has " + wablroom.availableSize()
        + " m3 of space left inside (" + wablroom.contentSize()
        + "m3 of contents)");
    log("content test", wablroom + " contains " + wablroom.contentCount()
        + " items (" + wablroom.contentCountDeep() + " total)");
    log("content test", "done");
  }
  
  private static void testValues() {
    Value v = null;
    int loops = 0;
    while(true) {
      loops++;
      switch(rand.nextInt(5)) {
        case 0:
          v = Value.make();
          break;

        case 1:
          v = Value.make(rand.nextInt(1) == 1);
          break;

        case 2:
          v = Value.make(rand.nextLong());
          break;
        
        case 3:
          v = Value.make(rand.nextDouble());
          break;
        
        case 4:
          v = Value.make(rand.nextInt(2) == 1 ? "yes" : "no");
          break;
      }
      
      System.out.println("Value: " + v + " (" + v.toBoolean() + ") - type: " + v.type());
      
      if (v.toString().equals("yes")) break;
    }
    System.out.println(loops + " loops");
  }
  
  static int cubeFill(Container cont, double size, int iterations) {
    int created = 0;
    Active temp;
    try {
      for (int i = 0; i < 8; i++) {
        (temp = new DebugCube(cont, "in", size)).init();
        created++;
        if (iterations > 1)
          created += cubeFill(temp, size * .436, iterations - 1);
      }
    } catch (DoesNotFitException ex) { }
    return created;
  }
  
  static void testSPQ() {
    final int WORD_LENGTH = 4;
    boolean failed = false;
    
    CancellablePriorityQueue<String> cpq =
        new CancellablePriorityQueue<String>();
    
    for (int additions = 0; additions < 2000; additions++) {
      StringBuilder sb = new StringBuilder(WORD_LENGTH);
      for (int i = 0; i < WORD_LENGTH; i++) {
        sb.append((char)('a' + rand.nextInt(26)));
      }
      cpq.add(sb.toString());
    }
    String last = cpq.poll();
    String next;
    while ((next = cpq.poll()) != null) {
      if (last.compareTo(next) > 0) {
        log("CPQ test", "ERROR! " + last + " came before " + next);
        failed = true;
      }
      last = next;
    }
    
    log("CPQ test", failed ? "TESTS FAILED!" : "Test successful.");
  }
  
  
  /** Wraps the main debug's print method */
  public static void log(String topic, String s) {
    logger.p(topic, s);
  }
}