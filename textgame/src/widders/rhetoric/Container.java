package widders.rhetoric;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import widders.util.IterProtector;
import widders.util.RandomAccessLinkedHashSet;
import widders.util.SimpleStack;


/**
 * 
 * @author widders
 */
public abstract class Container implements Named, Iterable<Active> {
  // CONTAINS ITEMS...ALL THIS IS DONE ALREADY
  // just has a dang list of items (a HashSet actually)
  // also allows for adding and removing of items (this follows the Report
  // setup for saying exactly what happened)
  // makes sure they'll fit on the bases of length (limits single objects from
  // being added) and size (which mustn't exceed a certain maximum for all
  // objects in the container)
  // can reject items attempting to be added on arbitrary basis
  // (that way you could make sheaths and stuff and they would work) and just
  // say that they don't fit (ABSTRACT)
  // those attributes are accessible to the outside
  
  // container() RETURNS AN Active, OR NULL IF THE CONTAINER IS ACTUALLY A Room
  // room() RETURNS THE ROOM THE OBJECT IS ULTIMATELY IN
  
  /* The internal static registry of live Active objects, mapped by internal
   * name */
  private static Map<String, Container> registry =
      new Hashtable<String, Container>();
  
  /* This object's name */
  private Name name;
  
  /* The unique String used to identify this object internally */
  private String internalName;
  
  /* Constants for optimizing content table sizes */
  private static final int MAX_COMFORTABLE_CAPACITY = 192;
  private static final int SHRINK_FACTOR = 4;
  private static final int COMFORTABLE_CAPACITY = 64;
  
  // lock to enforce synchronicity on stats tracking
  private final ReentrantLock statSynchro = new ReentrantLock();
  
   /* These are only updated in the context of both this object's AND its container's synchro
   Therefore, locking your own synchro guarantees stability of the lastReported
   values of all your contained objects */
  private Stats lastReportedStats;
  
  /* Synchronized to this container's synchro */
  private ContentStats contentStats = new ContentStats();
  
  /** Lock to ensure that only one process is moving this object at a time. */
  private final ReentrantLock moveSynchro = new ReentrantLock();
  /** Signaled when the object's freeze state returns to unfrozen */
  private final Condition movementUnfreezes = moveSynchro.newCondition();
  /** Signaled when the object's movement phase ends */
  private final Condition movementEnds = moveSynchro.newCondition();
  /** Provides unique reservation IDs for object movements */
  private static final AtomicLong nextReservationID = new AtomicLong();
  /** Reservation ID of the thread currently moving this object */
  private Reservation currentReservation = null;
  /** When this is 0 the object is ok to move, -1 when an object is currently moving */
  private int moveFreeze = 0;
  
  /** The object containing this container */
  private Container container;
  
  /**
   * Simple preposition that indicates the relationship to this object's
   * container. Interned before set; comparing the actual property can use ==
   * operator
   */
  private String preposition;
  
  /** The contents of this container */
  private RandomAccessLinkedHashSet<Active> contents;
  // lazy instantiation as MOST objects will have no contents
  private int contentCountPeak;
  //private AtomicInteger contentCountDeep = new AtomicInteger();
  
  /**
   * The dates at which the object is created and incinerated, for gc
   * statistics; based on actual real-life time, not game time
   */
  @SuppressWarnings("unused")
  private long dateCreated = System.currentTimeMillis();
  @SuppressWarnings("unused")
  private long dateDoomed = Long.MIN_VALUE;
  
  /** Set to true when the object has finished initializing. */
  private volatile boolean initialized = false;
  
  /** The ID allocator counter */
  private static AtomicLong nextID = new AtomicLong();
  /** Unique ID for this Container */
  public final long iD = nextID.incrementAndGet();
  
  /** The total number of tasks queued for Containers */
  private static final AtomicInteger totalTaskCount = new AtomicInteger();
  /** The task queue for this specific container */
  private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<Runnable>();
  
  
  
  public static class Stats {
    protected double size, weight, length, width;
    
    private Stats(double size, double weight, double length, double width) {
      this.size = size;
      this.weight = weight;
      this.length = length;
      this.width = width;
    }
    
    private Stats(Stats copyThis) {
      size = copyThis.size;
      weight = copyThis.weight;
      length = copyThis.length;
      width = copyThis.width;
    }
    
    private Stats(Container init) {
      size = init.size();
      weight = init.weight();
      length = init.length();
      width = init.width();
    }
    
    public double size() {
      return size;
    }
    
    public double weight() {
      return weight;
    }
    
    public double length() {
      return length;
    }
    
    public double width() {
      return width;
    }
    
    private Stats getRemovalChange() {
      return new Stats(-size, -weight, -length, -width);
    }
    
    private Stats getChange(Stats newValue) {
      double changeSize, changeWeight, changeLength, changeWidth;
      // size
      if (newValue.size != size) {
        changeSize = newValue.size - size;
      } else {
        changeSize = 0d; // no change signal
      }
      
      // weight
      if (newValue.weight != weight) {
        changeWeight = newValue.weight - weight;
      } else {
        changeWeight = 0d; // no change signal
      }
      
      // length
      if (newValue.length != length) {
        if (newValue.length < length) {
          changeLength = -length; // decrease signal
        } else /* if (newValue.length > Length) */{
          changeLength = newValue.length; // increase signal
        }
      } else {
        changeLength = 0d; // no change signal
      }
      
      // width
      if (newValue.width != width) {
        if (newValue.width < width) {
          changeWidth = -width; // decrease signal
        } else /* if (newValue.width > Width) */{
          changeWidth = newValue.width; // increase signal
        }
      } else {
        changeWidth = 0d; // no change signal
      }
      
      return new Stats(changeSize, changeWeight, changeLength, changeWidth);
    }
    
    private boolean isZero() {
      return size == 0d
          && weight == 0d
          && length == 0d
          && width == 0d;
    }
  }
  
  private class ContentStats extends Stats {
    // force recompute size/weight totals at least after this many removals
    private static final int RECOMPUTE_INTERVAL = 1 << 10;
    /* force recompute size/weight totals if the magnitude falls below this ratio
     * of its peak. */
    private static final double RECOMPUTE_PEAK_RATIO = 1d / (1L << 32);
    
    // number of size reductions since recomputing
    private int contentSizeRecompute = RECOMPUTE_INTERVAL;
    // magnitude of peak size since recomputing
    private double contentSizePeak = 0d;
    // number of weight reductions since recomputing
    private int contentWeightRecompute = RECOMPUTE_INTERVAL;
    // magnitude of peak weight since recomputing
    private double contentWeightPeak = 0d;
    
    private ContentStats() {
      super(0d, 0d, 0d, 0d);
    }
    
    /**
     * Updates this ContentStats. It is assumed that a lock on statSynchro for the owning
     * container is held when this method is called.
     * 
     * @param change
     *        The stats change delta
     * @return
     *        True if content stats changed in any way
     */
    private boolean modify(Stats change) {
      boolean changed = false;
      
      // size
      if (change.size != 0d) {
        double oldSize = size;
        size += change.size; // modify size
        
        if (size != oldSize) {
          changed = true;
          
          if (change.size < 0d) {
            // magnitude of weight is being reduced (size is always non-negative)
            // If there have been enough reductions to trigger a recompute,
            // or the size has been reduced enough from its maximum... 
            if (contentSizeRecompute == 0
                || size < contentSizePeak * RECOMPUTE_PEAK_RATIO) {
              // recompute size
              contentSizeRecompute = RECOMPUTE_INTERVAL;
              size = 0d;
              for (Container content : contents) {
                size += content.lastReportedStats.size;
              }
              contentSizePeak = size;
            } else {
              contentSizeRecompute--;
            }
          } else {
            contentSizePeak = Math.max(size, contentSizePeak);
          }
        }
      }
      
      // weight
      if (change.weight != 0d) {
        double oldWeight = weight;
        weight += change.weight;
        
        if (weight != oldWeight) {
          changed = true;
          
          //check for recomputes
          if (Math.abs(weight) < Math.abs(oldWeight)) {
            if (contentWeightRecompute == 0
                || Math.abs(weight) <
                contentWeightPeak * RECOMPUTE_PEAK_RATIO) {
              // recompute content weight
              contentWeightRecompute = RECOMPUTE_INTERVAL;
              weight = 0d;
              for (Container content : contents) {
                weight += content.lastReportedStats.weight;
              }
              contentWeightPeak = weight;
            } else {
              // count down to mandatory recompute
              contentWeightRecompute--;
            }
          } else {
            // magnitude of weight is being increased, no worries mate
            contentWeightPeak =
                Math.max(Math.abs(weight), contentWeightPeak);
          }
        }
      }
      
      // length
      if (change.length != 0d) {
        if (change.length > length) { // increased over current max
          length = change.length;
          changed = true;
        } else if (-change.length >= length) { // decreased from current max
          double newLongest = 0d;
          for (Container content : contents) {
            double len = content.lastReportedStats.length;
            if (len == length) {
              newLongest = len;
              break;
            }
            newLongest = Math.max(newLongest, len);
          }
          if (length != newLongest) {
            changed = true;
            length = newLongest;
          }
        }
      }
      
      // width
      if (change.width != 0d) {
        if (change.width > width) { // increased over current max
          width = change.width;
          changed = true;
        } else if (-change.width >= width) { // decreased from current max
          double newWidest = 0d;
          for (Container content : contents) {
            double wide = content.lastReportedStats.width;
            if (wide == width) {
              newWidest = wide;
              break;
            }
            newWidest = Math.max(newWidest, wide);
          }
          if (width != newWidest) {
            changed = true;
            width = newWidest;
          }
        }
      }
      
      return changed;
    }
  }
  
  
  
  
  /**
   * Creates a new Container inside the given container; if [spawnHere] is null,
   * the Container is assumed to be a Room
   * 
   * @param name
   *          the object's name
   * @param spawnHere
   *          initial location
   * @param preposition
   *          preposition for how this object is contained in the other one
   */
  protected Container(Name name, Container spawnHere, String preposition) {
    if (spawnHere == null && !(this instanceof Room))
      throw new Error("Only Rooms may init() without a container");
    container = spawnHere;
    this.preposition = (preposition == null)
        ? null
        : preposition.intern();
    this.name = name;
    internalName = name + "~" + iD;
    registry.put(internalName, this);
  }
  
  /**
   * Used to emplace a created object into the world.
   * 
   * This is a separate method because it must be done after all constructors
   * have finished, as the objects size & weight may depend on values that
   * the subclass has not yet initialized.
   * 
   * @throws DoesNotFitException
   */
  public final void init() throws DoesNotFitException {
    if (initialized)
      return;
    if (!(this instanceof Active))
      throw new Error("Only Active objects should be initialized with init()");
    
    lastReportedStats = new Stats(this);
    
    Report r = new Report();
    if (!container.emplace((Active)this, preposition, Main.creator, r)) {
      // failed to emplace; doom this object
      Container originalTarget = container;
      container = null;
      registry.remove(internalName);
      dateDoomed = System.currentTimeMillis();
      Main.log("creation", "Could not init " + this + " in "
          + originalTarget + " : " + r.text());
      throw new DoesNotFitException(r.text());
    }
    Main.log("creation", r);
    
    initialized = true;
  }
  
  /**
   * @return
   *         true only if this is an Active object in the world which has
   *         been successfully emplaced with init().
   */
  public boolean initializedActive() {
    return initialized;
  }
  
  /** Places an initializing object in this one */
  private boolean emplace(Active obj, String preposition,
                          Active actor, Report r) {
    if (!authorizeAdd(obj, preposition, actor, r))
      return false;
    
    ((Container)obj).beginMovement(null);
    enactAdd(obj, preposition);
    
    r.report(obj + " was placed '" + preposition + "' " + this);
    
    return true;
  }
  
  /**
   * Returns the default assumed width for an object with the given size and
   * length
   */
  public static double defaultWidth(double size, double length) {
    return Math.min(Math.sqrt(size / length), length);
  }
  
  /** Utility method for creating identities with varargs */
  public static final String[] array(String... ident) {
    return ident;
  }
  
  /**
   * Retrieves a Container by internal name. If there is no existing object
   * by the given name, null is returned
   */
  public static final Container getByName(String name) {
    return registry.get(name);
  }
  
  /**
   * @return
   *         this object's unique ID
   */
  public final long getID() {
    return iD;
  }
  
  @Override
  public final String toString() {
    return internalName;
  }
  
  public final Name name() {
    return name;
  }
  
  /** Returns the preposition for this object in its container */
  public final String preposition() {
    return preposition;
  }
  
  /** Writes the appropriate description of the object to the given report */
  public final void des(Detail detail, Report r) {
    r.report(des(detail));
  }
  
  /** Appends descriptions of this items' contents. */
  public final StringBuilder readContents(StringBuilder sb, Detail detail) {
    for (Active content : this)
      sb.append('\n').append(content.des(detail));
    return sb;
  }
  
  /** Returns an iterator over the contained items */
  @Override
  public final Iterator<Active> iterator() {
    return contents == null
        ? Collections.<Active> emptyIterator()
        : new IterProtector<Active>(contents.iterator());
  }
  
  public final Active[] allContents() {
    return contents == null
        ? new Active[0]
        : contents.toArray(new Active[contents.size()]);
  }
  
  /** Returns the number of contained items */
  public final int contentCount() {
    return contents == null
        ? 0
        : contents.size();
  }
  
  /** Returns the number of items contained recursively */
  public final int contentCountDeep() {
    if (contents == null)
      return 0;
    int t = contents.size();
    for (Active content : contents)
      t += content.contentCountDeep();
    return t;
    //return contentCountDeep.get();
  }
  
  public final Container container() {
    return container;
  }
  
  /**
   * Returns the container if this is not directly a room, otherwise returns
   * null
   */
  public final Active activeContainer() {
    return (isDirectlyInRoom() ? null : (Active)container);
  }
  
  /** Returns true iff this Container is a room */
  public final boolean isRoom() {
    return (!initialized && container == null);
  }
  
  /**
   * Returns true iff this object is directly in a room. If this object is a
   * room itself, returns false
   */
  public final boolean isDirectlyInRoom() {
    return (isRoom() ? false : container.isRoom());
  }
  
  /** Returns the room that this object is ultimately in */
  public final Room room() {
    return (isRoom() ? (Room)this : container.room());
  }
  
  /** Returns true iff the given object is contained by this one */
  public final boolean contains(Active obj) {
    return obj.container() == this;
  }
  
  /**
   * Returns true iff the given object is contained by this one or any of its
   * contents, reiteratively. Returns false for itself.
   */
  public final boolean containsDeep(Container obj) {
    for (Container c = ((Container)obj).container; c != null; c = c.container) {
      if (c == this)
        return true;
    }
    return false;
  }
  
  /**
   * Returns the innermost common container of this object and the given one, or
   * null if there is no common container. If they are the same object or one is
   * a direct or recursive parent of the other, returns the outermost of the
   * two.
   */
  public final Container commonContainer(Container obj) {
    for (Container possible = this; possible != null; possible = possible.container) {
      if (possible == obj || possible.containsDeep(obj))
        return possible;
    }
    return null;
  }
  
  /**
   * Returns the available size space for contained items.
   * Can be overridden to implement flexiblity!
   */
  public abstract double availableSize();
  
  /** Returns the maximum length allowed for contained items. */
  public abstract double lengthLimit();
  
  /** Returns the maximum width allowed for contained items. */
  public abstract double widthLimit();
  
  /** Returns the size of the container object */
  public abstract double size();
  
  /** Returns the weight of the container object */
  public abstract double weight();
  
  /** Returns the length of the object along its longest axis */
  public abstract double length();
  
  /** Returns the width of the object */
  public abstract double width();
  
  /** Returns the current stats of this object */
  public final Stats stats() {
    statSynchro.lock();
    try {
      return new Stats(this);
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Updates this object's size, weight, etc. */
  protected final void updateStats() {
    if (!initialized)
      return;
    
    Stats newStats, sendStats;
    
    statSynchro.lock();
    try {
      // update this object's lastreported stats
      newStats = new Stats(this);
      sendStats = lastReportedStats.getChange(newStats);
      
      // if local stats didn't change, exit
      if (sendStats.isZero())
        return;
      
      // freeze this object's movement
      this.freezeMovement(null);
      
      // obtain stat lock of next container up to enforce ordering
      container.statSynchro.lock();
      
      // update lastreported stats under this lock
      lastReportedStats = newStats;
      
    } finally {
      // release stat lock
      statSynchro.unlock();
    }
    
    /* at this point we hold statSynchro locks only on the container, and this
     * object is move-frozen */
    
    // propagate upwards
    container.propagateContentStats(sendStats, this);
  }
  
  /** Changes content stats and immediately propagates.
   * 
   * The lock on this object's statSynchro must be held when this method is called.
   * 
   * propagatingFrom must be move-frozen or null; if it is non-null, it will be
   * unfrozen once. */
  private void propagateContentStats(Stats change, Container propagatingFrom) {
    /* laundry list:
     * the contents must stay the same while the parent is updating content
     * stats
     * after content stats are updated the contents are free to move again
     * 
     * - content stats can only change when statsynchro is held
     * - local stats can only change when the object is move-frozen
     * - local stats are dependent on content stats
     * - therefore lastreported stats members must be updated while frozen and
     * synced
     * 
     * 1. modify container's content stats from parameters
     * 2. unfreeze content's movement if there was one
     * 3. if this is a room, release stat lock and exit
     * 4. if content stats did not change, release stat lock and exit
     * 5. freeze this object's movement (undone by 7, at next level 2)
     * 8. obtain stat lock of next container up to ensure order (undone at next level by 3, 4, 7)
     * 6. update this object's lastreported stats
     * 7. if local stats didn't change, unfreeze unlock and exit
     * 9. release stat lock
     * 10. propagate the next container up
     */
    
    Stats newStats, sendStats;
    
    /* on entry to this method, propagatingFrom is either null or move-frozen and
     * the lock to this object's statSynchro is already held */
    try {
      // modify content stats from parameters
      boolean contentChanged = contentStats.modify(change);
          //changeContentStats(change);
      
      // unfreeze content's movement if there was one
      if (propagatingFrom != null)
        propagatingFrom.unfreezeMovement();
      
      // if contents did not change, exit (releasing stat lock)
      if (!contentChanged)
        return;
      
      // if this is a room, exit (releasing stat lock)
      if (container == null)
        return;
      
      // update this object's lastreported stats
      newStats = new Stats(this);
      sendStats = lastReportedStats.getChange(newStats);
      
      // if local stats didn't change, exit
      if (sendStats.isZero())
        return;
      
      // freeze this object's movement
      this.freezeMovement(null);
      
      // obtain stat lock of next container up to enforce ordering
      container.statSynchro.lock();
      
      // update lastreported stats under this lock
      lastReportedStats = newStats;
      
    } finally {
      // release stat lock
      statSynchro.unlock();
    }
    
    /* at this point we hold statSynchro locks only on the container, and this
     * object is move-frozen */
    
    // propagate upwards
    container.propagateContentStats(sendStats, this);
  }
  
  /** Returns the total size of the contents */
  public final double contentSize() {
    statSynchro.lock();
    try {
      return contentStats.size;
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Returns the total weight of the contents */
  public final double contentWeight() {
    statSynchro.lock();
    try {
      return contentStats.weight;
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Returns the length of the longest contained item */
  public final double longestContent() {
    statSynchro.lock();
    try {
      return contentStats.length;
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Returns the width of the widest contained item */
  public final double widestContent() {
    statSynchro.lock();
    try {
      return contentStats.width;
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Returns the collective stats of this object's contents */
  public final Stats contentStats() {
    statSynchro.lock();
    try {
      return new Stats(contentStats);
    } finally {
      statSynchro.unlock();
    }
  }
  
  /**
   * Called whenever the size of this container's contents changes.
   * Override to take action when this occurs.
   * Notifies are called on outermost objects first, in the following order:
   * size, weight, length, width.
   */
  protected void onContentSizeChanged() {
  }
  
  /**
   * Called whenever the weight of this container's contents changes.
   * Override to take action when this occurs.
   * Notifies are called on outermost objects first, in the following order:
   * size, weight, length, width.
   */
  protected void onContentWeightChanged() {
  }
  
  /**
   * Called whenever the maximum length of this container's contents changes.
   * Override to take action when this occurs.
   * Notifies are called on outermost objects first, in the following order:
   * size, weight, length, width.
   */
  protected void onContentLengthChanged() {
  }
  
  /**
   * Called whenever the maximum width of this container's contents changes.
   * Override to take action when this occurs.
   * Notifies are called on outermost objects first, in the following order:
   * size, weight, length, width.
   */
  protected void onContentWidthChanged() {
  }
  
  /** Returns true iff the object can be fit into this container */
  public final boolean canFit(Active obj, Report r) {
    if (obj.width() > widthLimit()) { // width check
      r.report("The " + obj.name() + " is too wide.");
      return false;
    }
    if (obj.length() > lengthLimit()) { // length check
      r.report("The " + obj.name() + " is too long.");
      return false;
    }
    if (obj.size() > availableSize()) { // size check
      r.report("The " + obj.name() + " is too large.");
      return false;
    }
    return true;
  }
  
  /** Returns true iff the object can be fit into this container */
  public final boolean canFit(Active obj) {
    return (obj.width() <= widthLimit()
        && obj.length() <= lengthLimit()
        && obj.size() <= availableSize());
  }
  
  /** Prevents the object from being moved until moveUnfreeze() is called.
   * Reentrant (4 billion times).
   * 
   * @param res
   *            The movement reservation currently claiming locks with this call,
   *            or null to always wait.
   * @returns
   *            If a reservation is passed and an older
   *            reservation is already in place to move this object, returns the
   *            superceding reservation, otherwise returns null (for success). */
  private Reservation freezeMovement(Reservation res) {
    moveSynchro.lock();
    try {
      if (moveFreeze == -1) { // object is being moved
        // if the lock we encountered is held by an older reservation...
        if (res != null && currentReservation != null
            && res.getID() > currentReservation.getID()) {
          // fail and return reservation to defer to
          return currentReservation;
        } else {
          // either we have precedence or we don't care, force a wait
          movementEnds.await();
        }
      }
      moveFreeze++;
      return null; // success
    } catch (InterruptedException ex) {
      throw new Error("Interrupted while waiting to freeze an object", ex);
    } finally {
      moveSynchro.unlock();
    }
  }
  
  /** Undoes one layer of movement restriction from moveFreeze() */
  private void unfreezeMovement() {
    moveSynchro.lock();
    try {
      if (moveFreeze == 0)
        throw new IllegalMonitorStateException("unfreezeMovement() called on an already"
            + " unfrozen object!");
      if (moveFreeze == -1)
        throw new IllegalMonitorStateException("unfreezeMovement() called on an object"
            + " in its move phase!");
      
      moveFreeze--;
      if (moveFreeze == 0)
        movementUnfreezes.signal();
    } finally {
      moveSynchro.unlock();
    }
  }
  
  /** Prepares the object to be moved and prevents it from being frozen
   * until endMovement() is called */
  private void beginMovement(Reservation res) {
    moveSynchro.lock();
    try {
      while (moveFreeze != 0)
        movementUnfreezes.await();
      
      moveFreeze = -1; // set object to locked state
      currentReservation = res;
    } catch (InterruptedException e) {
      System.out.println("Interrupted obtaining movement lock");
      e.printStackTrace();
    } finally {
      moveSynchro.unlock();
    }
  }
  
  /** Releases the lock on the object and allows it to be frozen in place again */
  private void endMovement() {
    moveSynchro.lock();
    try {
      ///// possibly add thread safety tracker here
      if (moveFreeze != -1)
        throw new IllegalMonitorStateException("Process attempted to end nonexistent"
            + " movement phase");
      
      moveFreeze = 0;
      currentReservation = null;
      
     // unpark waiting threads
      movementUnfreezes.signal();
      movementEnds.signalAll();
    } finally {
      moveSynchro.unlock();
    }
  }
  
  // atomic version of { endMovement(); freezeMovement(); }
  /** Releases the lock on the object but puts it into a frozen state. This is the
   * equivalent of an atomic call to first endMovement() then freezeMovement().
   * After this is called, it is imperative that unfreezeMovement() be called when finished. */
  private void endMovementAndFreeze() {
    moveSynchro.lock();
    try {
      if (moveFreeze != -1)
        throw new IllegalMonitorStateException("Process attempted to end nonexistent"
            + " movement phase");
      
      moveFreeze = 1;
      currentReservation = null;
      
      // unpark waiting threads
      movementEnds.signalAll();
    } finally {
      moveSynchro.unlock();
    }
  }
  
  /** Provides functionality for obtaining locks for movement of objects
   * from one place to another */
  private static class Reservation {
    private final Container moving;
    private final SimpleStack<Container> frozen;
    private final long reservationID;
    private final Thread owner = Thread.currentThread();
    
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition finishCondition = lock.newCondition();
    private boolean finished = false;
    
    /** Locks and freezes necessary objects and provides a Reservation that can unlock them
     * Returns null if the destination object is inside the moving object */
    public static Reservation create(Container moving, Container destination) {
      final SimpleStack<Container> frozen = new SimpleStack<Container>();
      
      final Reservation res = new Reservation(moving, frozen,
                                              nextReservationID.incrementAndGet());
      
      while (true) { // let's make this work you and me
        // Lock the moving object
        moving.beginMovement(res);
        
        // we don't need to freeze if it's a shift
        if (moving.container == destination)
          break;
        
        // freeze the destination & containers
        Container c = destination;
        while (c.container != null) {
          Reservation priorReservation = c.freezeMovement(res);
          
          if (priorReservation != null) { // we need to defer to the other reservation
            // unlock everything
            moving.endMovement();
            while (!frozen.isEmpty())
              frozen.pop().unfreezeMovement();
            
            // defer and try again
            priorReservation.defer();
            continue;
          } else { // we're good to go
            frozen.push(c);
            if (c.container == moving) { // destination is inside Moving!
              // unlock everything
              while (!frozen.isEmpty())
                frozen.pop().unfreezeMovement();
              moving.endMovement();
              return null;
            }
          }
          c = c.container;
        }
        break;
      }

      
      return res;
    }
    
    private Reservation(Container moving, SimpleStack<Container> frozen, long reservationID) {
      this.moving = moving;
      this.frozen = frozen;
      this.reservationID = reservationID;
    }
    
    /** Provides the reservation ID */
    public long getID() {
      return reservationID;
    }
    
    /** Ends the reservation.
     * The destination's containers are unfrozen and the moving object's move state is
     * ended, leaving it in a frozen state. */
    public void end() {
      if (Thread.currentThread() != owner)
        throw new IllegalMonitorStateException("Non-owning thread attempted to end a"
            + " move reservation");
      
      if (finished)
        return;
      
      lock.lock();
      try {
        moving.endMovementAndFreeze();
        while (!frozen.isEmpty())
          frozen.pop().unfreezeMovement();
        
        // release deferring threads
        finished = true;
        finishCondition.signalAll();
      } finally {
        lock.unlock();
      }
    }
    
    /** Blocks until this reservation is over */
    private void defer() {
      lock.lock();
      try {
        while (!finished)
          finishCondition.await();
      } catch (InterruptedException ex) {
        throw new Error("Interrupted during move reservation deferral", ex);
      } finally {
        lock.unlock();
      }
    }
  }
  
  ///// TODO consider adding an expected from container
  /** Adds a contained object. Returns true on success */
  public final boolean add(Active obj, String prep,
                           Active actor, Report r) {
    /* Laundry list:
     * obj must be initialized
     * obj must not be destroyed
     * obj must not be this object or any object containing it
     * obj must be movable
     * from and this must authorize- remove and add
     */
    
    // obj must be initialized
    if (!((Container)obj).initialized)
      throw new IllegalArgumentException(obj + " has not been initialized");
    
    // obj must not be this object
    if (obj == this) { // self check
      r.report("You cannot put something inside itself.");
      return false; // impossible
    }
    
    Container from;
    
    Reservation reservation = Reservation.create(obj, this);
    if (reservation == null) {
      r.report("You cannot put something inside itself");
      return false; // impossibru
    }
    try {
      from = obj.container();
        
      // obj must not be here already
      if (from == this) { // pre-containment check
        // obj will not be moving
        reservation.end();
        
        if (obj.preposition().equals(prep)) {
          // exit
          ((Container)obj).unfreezeMovement();
          r.report("The " + obj.name() + " is already there.");
          return false; // already there
        } else { // here but with different preposition
          // shift the object inside this one instead of moving it in the tree
          if (!obj.movable()) { // mobility check
            // exit
            ((Container)obj).unfreezeMovement();
            r.report("The " + obj.name() + " is unmovable.");
            return false; // can't move
          } else if (authorizeRemove(obj, this, actor, r)
              && authorizeAdd(obj, prep, actor, r)) { // authorization check
            enactShift(obj, prep, actor);
            ((Container)obj).unfreezeMovement();
            return true; // hooray
          } else {
            ((Container)obj).unfreezeMovement();
            return false; // not authorized
          }
          
        }
      }
      
      // obj must not be destroyed
      if (from == null) {
        reservation.end();
        ((Container)obj).unfreezeMovement();
        r.report("The " + obj.name() + " no longer exists.");
        return false;
      }
      
      // obj must be movable
      if (!obj.movable()) {
        reservation.end();
        ((Container)obj).unfreezeMovement();
        r.report("The " + obj.name() + " is unmovable.");
        return false; // can't move
      }
      
      // obj's current container must authorize the removal
      // this container must authorize its addition
      if (!from.authorizeRemove(obj, this, actor, r)
          || !authorizeAdd(obj, prep, actor, r)) {
        reservation.end();
        ((Container)obj).unfreezeMovement();
        return false; // not authorized
      } else {
        
        
        // NOW WE ACTUALLY DO THE MOVING BECAUSE IT'S OK
        enactMove(obj, prep, actor);
      }
    } finally {
      reservation.end();
    }
    
    ((Container)obj).unfreezeMovement();
    
    return true;
  }
  
  /**
   * Removes the specified object to this container's container
   * (using the same preposition this object has in that container); if this
   * container is a room, the object is thrown into the Inferno.
   */ /////TODO wat? fix this probably idk
  public final boolean remove(Active obj, Active actor, Report r) {
    if (!contains(obj)) { // necessary presence check
      r.report("The " + obj.name() + " is not in the "
          + this.name());
      return false;
    } else if (isRoom()) { // this is a room, destroy instead
      obj.destroy(actor);
      return true;
    } else {
      return container.add(obj, this.preposition, actor, r);
    }
  }
  
  /** Destroys this object. */
  public final void destroy(Active actor) {
    beginMovement(null);
    statSynchro.lock();
    try {
      if (container != null) {
        enactRemove((Active)this);
        container.task(() -> container.onRemove((Active)this, null, Main.physics));
        container = null;
      }
      if (contents != null)
        while (!contents.isEmpty())
          // destroy contents first
          contents.peekLast().destroy(actor);
      registry.remove(internalName);
      dateDoomed = System.currentTimeMillis();
      onDestroyed(actor);
    } finally {
      statSynchro.unlock();
      endMovement();
    }
    
    Main.log("destruction", this + " was incinerated by " + actor);
  }
  
  ///// TODO the fuck is going on here
  /**
   * Moves the given object into this container
   */
  private void enactMove(Active obj, String prep, Active actor) {
    Container from = obj.container();
    
    ((Container)obj).statSynchro.lock();
    try {
      from.statSynchro.lock();
      enactRemove(obj);
      enactAdd(obj, prep);
    } finally {
      ((Container)obj).statSynchro.unlock();
    }
    
    Main.log("movement", obj + " was moved to " + this + " by " + actor);
    from.task(() -> from.onRemove(obj, this, actor)); // notify source    
    this.task(() -> this.onAdd(obj, actor)); // notify destination
    ((Container)obj).task(() -> obj.onMoved(actor)); // notify moved object
  }
  
  /**
   * Concurrency v2 enactAdd method.
   * adds to this container unconditionally and propagates stats.
   */
  private void enactAdd(Active obj, String prep) {
    if (this instanceof Active && !initialized)
      throw new Error(this + " has not been initialized");
    
    ((Container)obj).container = this;
    ((Container)obj).preposition = prep;
    if (contents == null) // lazy initialization
      contents = new RandomAccessLinkedHashSet<Active>();
    contents.add(obj);
    contentCountPeak = Math.max(contents.size(), contentCountPeak);
    
    ((Container)obj).endMovementAndFreeze();
    propagateContentStats(((Container)obj).lastReportedStats, obj);
  }
  
  /**
   * Concurrency v2 enactRemove method.
   * Unconditionally removes the object from its container and propagates stats.
   */
  private static void enactRemove(Active obj) {
    Container from = ((Container)obj).container;
    from.contents.remove(obj);
    
    if (from.contentCountPeak > MAX_COMFORTABLE_CAPACITY
        && from.contents.size() <= from.contentCountPeak >>> SHRINK_FACTOR) {
      from.contents.shrink(COMFORTABLE_CAPACITY);
      from.contentCountPeak = from.contents.size();
    }
    from.propagateContentStats(((Container)obj).lastReportedStats.getRemovalChange(), null);
  }
  
  /**
   * @param actor
   *          The object that caused the move
   */
  private void enactShift(Active obj, String newPreposition, Active actor) {
    final String oldPrep = obj.preposition();
    ((Container)obj).preposition = newPreposition.intern();
    
    //onShift(obj, actor, oldPrep); // notify this container
    //obj.onShifted(actor, oldPrep); // notify the moving object /////TODO new notification model
    task(() -> onShift(obj, actor, oldPrep));
    ((Container)obj).task(() -> obj.onShifted(actor, oldPrep));
  }
  
  /**
   * Determines what can be added and how, and what and why not. To be
   * overridden by implementing classes if any special behavior is desired.
   * By default it only checks to see if the object will fit in the container
   * by standard rules.
   * 
   * @param obj
   *          the object being added
   * @param preposition
   *          the preposition with which this object wants to be added
   * @param actor
   *          the responsible actor for this event
   * @param r
   *          reporting stream
   */
  protected boolean authorizeAdd(Active obj, String preposition,
                                 Active actor, Report r) {
    if (preposition == null || !preposition.equals("in")) {
      r.report("Cannot put something '" + preposition + "' this.");
      return false;
    }
    if (!canFit(obj, r))
      return false;
    return true;
  }
  
  /**
   * Determines what can be moved and how, and what can't and why not; to be
   * overridden by implementing classes if any special behavior is desired.
   * By default all removals are authorized.
   * 
   * @param obj
   *          the object being removed
   * @param moveTo
   *          the container the object is moving to
   * @param actor
   *          the responsible actor for this event
   * @param r
   *          reporting stream
   */
  protected boolean authorizeRemove(Active obj, Container moveTo, Active actor,
                                    Report r) {
    return true;
  }
  
  private void task(Runnable task) {
    totalTaskCount.incrementAndGet();
    Tasking.queue(() -> {
      task.run();
      totalTaskCount.decrementAndGet();
      if (!taskQueue.isEmpty())
        Tasking.queue(taskQueue.poll());
    });
  }
  
  public static int globalQueuedTasks() {
    return totalTaskCount.get();
  }
  
  /**
   * Add event; called when something is added directly to this container
   * 
   * @param obj
   *          the object being added
   * @param actor
   *          the responsible actor for this event
   */
  protected void onAdd(Active obj, Active actor) {
  }
  
  /**
   * Shift event; called when something inside changes its preposition
   * 
   * @param obj
   *          the object that was shifted
   * @param actor
   *          the responsible actor for this event
   * @param oldPreposition
   *          the previous preposition of the object
   */
  protected void onShift(Active obj, Active actor, String oldPreposition) {
  }
  
  /**
   * Remove event
   * 
   * @param obj
   *          the object being removed
   * @param wentTo
   *          the container the object moved to
   * @param actor
   *          the responsible actor for this event
   */
  protected void onRemove(Active obj, Container wentTo, Active actor) {
  }
  
  /**
   * Called when the object is destroyed. Override to perform cleanup.
   * 
   * @param actor
   */
  protected void onDestroyed(Active actor) {
  }
  
  @Override
  protected final void finalize() throws Throwable {
    //preFinalize();
    /* Main.debug.p("finalized", toString()
     * + " / lived for " + (dateDoomed - dateCreated) + "ms"
     * + ", doomed for " + (System.currentTimeMillis() - dateDoomed) + "ms"); */
  }
}