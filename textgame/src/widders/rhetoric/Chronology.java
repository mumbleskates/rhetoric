package widders.rhetoric;

import static widders.rhetoric.Units.*;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Provides the ability to schedule an action on an object.
 * 
 * @author widders
 */
public final class Chronology extends Active {
  private ScheduledExecutorService scheduler;
  
  /**
   * Creates a new instance of Chronology
   */
  public Chronology(Container putItHere, String preposition, int threadCount) {
    super(new BasicName("chronology"),
          array("chronology", "hourglass", "huge"),
          putItHere, preposition);
    classify("engine_object", "immortal");
    
    scheduler = Executors.newScheduledThreadPool(threadCount);
  }
  
  /** Returns the current game time in millis UTC */
  public static long now() {
    return new Date().getTime();
  }
  
  
  
  
  ///// TODO implement task queueing
  public ScheduledFuture<?> scheduleTask(long delayMs, Runnable task) {
    return scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
  }

  ///// TODO implement task queueing
  public ScheduledFuture<?> scheduleTask(long delayMs, final Active actor,
                                     final String verb,
                                     final Active target, final Report report) {
    return scheduler.schedule(
                              new Runnable() {
                                Active a = actor, t = target;
                                String v = verb;
                                Report r = report;
                                
                                public void run() {
                                  t.target(a, v, r);
                                }
                              },
                              delayMs, TimeUnit.MILLISECONDS);
  }
  
  public ScheduledFuture<?> addEvent(long delayMs, final Active actor,
                                  final String verb,
                                  final Active indirect, final Selection target,
                                  final Report report) {
    return scheduler.schedule(
                              new Runnable() {
                                Active a = actor, i = indirect;
                                Selection t = target;
                                String v = verb;
                                Report r = report;
                                
                                public void run() {
                                  i.use(a, v, t, r);
                                }
                              },
                              delayMs, TimeUnit.MILLISECONDS);
  }
  
  public void shutDownScheduler() {
    scheduler.shutdown();
  }
  
  
  @Override
  public String des(Detail detail) {
    switch (detail) {
      case BASIC:
        return " There is an enormous unsupported hourglass, over seventy feet"
            + " high, glowing with a deep and mysterious light.";
        
      case DETAIL:
        return "The hourglass is difficult to look at.";
        
      case INSIDE:
        return "Beautiful flickering pebbles in green, blue, and ultraviolet"
            + "cascade up and down.";
        
      default:
        return null;
    }
  }
  
  @Override
  public double lengthLimit() {
    return 0;
  }
  
  @Override
  public double widthLimit() {
    return 0;
  }
  
  @Override
  public double availableSize() {
    return 0;
  }
  
  @Override
  protected boolean authorizeAdd(Active obj, String preposition, Active actor,
                                 Report r) {
    switch (preposition) {
      case "in":
        r.report("The glass is sealed.");
        break;
      
      default:
        r.report("You cannot do that.");
    }
    return false;
  }
  
  @Override
  public boolean movable() {
    return false;
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Report r) {
    r.report("How could you possibly do that?");
    return false;
  }
  
  @Override
  protected boolean targeted(Active actor, String verb, Active indirect,
                             Report r) {
    r.report("How could you possibly do that?");
    return false;
  }
  
  @Override
  public boolean used(Active actor, String verb, Selection target, Report r) {
    r.report("How could you possibly do that?");
    return false;
  }
  
  @Override
  public double size() {
    return 80. * 40. * 40. * .6 * CU_FT;
  }
  
  @Override
  public double baseWeight() {
    return 1.71e7 * TON;
  }
  
  @Override
  public double length() {
    return 85. * FT;
  }
  
  @Override
  public double width() {
    return 42 * FT;
  }
}