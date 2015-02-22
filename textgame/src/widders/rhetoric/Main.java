package widders.rhetoric;

import widders.rhetoric.content.*;
import widders.util.*;
import static widders.rhetoric.Units.*;

import java.io.File;
import java.io.PrintStream;
import java.text.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
    //// PARSE ARGS ////
    
    // No args at this time
    
    //// STATIC INIT ////
    
    try {
      // Start the logger
      try {
        logger = new Logging(new PrintStream(new File("s:/temp/rhetoric_logs/log_"
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
      
      //// RUN ////
      
      test();
    } catch (Throwable ex) {
      ex.printStackTrace();
    } finally {
      //// CLEANUP ////
      
      if (logger != null) logger.shutdown();
      else System.out.println("Logger did not instantiate");
      System.exit(0);
    }
  }
  
  ///// TODO add concurrency test
  static void test() {
    testSPQ();
    testSets();
    //testContent();
    testConcurrency();
  }
  
  static void testConcurrency() {
    final int threadCount = 2;
    final int testTime = 3; // seconds to test
    TestThread[] testers = new TestThread[threadCount];
    int[] lastMoveCount = new int[threadCount];
    int lastDeferrals = 0;
    
    log("concurrency test", "starting concurrency test");
    
    TestThread.initializeEnvironment();
    
    log("concurrency test", "environment initialized");
    
    for (int i = 0; i < threadCount; i++)
      testers[i] = new TestThread(i);
    for (int i = 0; i < threadCount; i++)
      testers[i].start();
    
    log("concurrency test", "threads started");
    System.out.println("Concurrency test: 0 (of " + testTime + ")");
    
    for (int time = 0; time < testTime; time++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        ex.printStackTrace();
        break;
      }
      for (int i = 0; i < threadCount; i++) {
        log("concurrency test", testers[i].getName() + ": " +
            (testers[i].movesCompleted - lastMoveCount[i]) + " moves");
        lastMoveCount[i] = testers[i].movesCompleted;
      }
      log("concurrency test", (Container.totalReservationDeferrals() - lastDeferrals) + " total deferrals");
      lastDeferrals = Container.totalReservationDeferrals();
      log("concurrency test", Container.activeReservations() + " active reservations");
      log("concurrency test", Container.buildingReservations() + " building reservations");
      System.out.println("Concurrency test: " + (time + 1));
    }
    log("concurrency test", "stopping test threads");
    TestThread.stop = true;
    log("concurrency test", "Current test environment shape:");
    TestThread.logStructure();
    while (true) {
      try {
        for (int i = 0; i < threadCount; i++)
          testers[i].join();
        break;
      } catch (InterruptedException ex) {
        continue;
      }
    }
    log("concurrency test", "threads stopped");
    log("concurrency test", (Container.totalReservationDeferrals() - lastDeferrals) + " total deferrals");
    log("concurrency test", Container.activeReservations() + " active reservations");
    log("concurrency test", Container.buildingReservations() + " building reservations");
  }
  
  static class TestThread extends Thread {
    static volatile boolean stop = false;
    int movesCompleted = 0;
    static final int roomCount = 4;
    static final int objectCount = 100;
    static Vector<Container> deck = new Vector<Container>();
    
    public TestThread(int number) {
      super("TestThread" + number);
    }
    
    static void initializeEnvironment() {
      for (int i = 0; i < roomCount; i++)
        TestThread.deck.add(new DebugRoom());
      
      for (int i = 0; i < objectCount; i++) {
        Active x = new PlasticBag(TestThread.deck.get(rand.nextInt(roomCount)), "in");
        TestThread.deck.add(x);
        try {
          x.init();
        } catch (DoesNotFitException ex) {
          log("concurrency test", "object did not fit! " + ex.getMessage());
        }
      }
    }
    
    static void logStructure() {
      for (int i = 0; i < roomCount; i++)
        logStructure(deck.elementAt(i), 0);
      log("concurrency test", "done outputting environment");
    }
    
    static void logStructure(Container from, int tabs) {
      log("concurrency test", new String(new char[tabs]).replace("\0", "\t") + from);
      Active[] contents = from.allContents();
      tabs++;
      for (Active child : contents)
        logStructure(child, tabs);
    }
    
    @Override
    public void run() {
      try {
        while (!stop) {
          int movingIndex = ThreadLocalRandom.current().nextInt(objectCount) + roomCount;
          int destinationIndex = ThreadLocalRandom.current().nextInt(deck.size() - 1);
          if (destinationIndex >= movingIndex) destinationIndex++;
          
          Active moving = (Active)deck.get(movingIndex);
          Container destination = deck.get(destinationIndex);
          
          log("concurrency test", getName() + " attempting to move " + moving + " into " + destination);
          if (!destination.add(moving, "in", creator, fakeReport))
            log("concurrency test", getName() + " could not move " + moving + " into " + destination);
          movesCompleted++;
        }
      } catch (Exception ex) {
        log("concurrency test", getName() + " crashed out!");
        System.out.println("Error in " + getName());
        ex.printStackTrace();
      }
    }
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
    log("content test", Container.globalQueuedTasks() + " container tasks queued");
    log("content test", "done");
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
    switch (topic) {
      case "creation":
      case "destruction":
        return;
      
      default:
        logger.p(topic, s);
    }
  }
  
  public static void log(String topic, Report r) {
    log(topic, r.text());
  }
}