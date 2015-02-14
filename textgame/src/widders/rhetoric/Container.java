package widders.rhetoric;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
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
  
  // force recompute size/weight totals at least after this many removals
  private static final int RECOMPUTE_INTERVAL = 1 << 10;
  /* force recompute size/weight totals if the magnitude falls below this ratio
   * of its peak. */
  private static final double RECOMPUTE_PEAK_RATIO = 1d / (1L << 32);
  /* Constants for optimizing content table sizes */
  private static final int MAX_COMFORTABLE_CAPACITY = 192;
  private static final int SHRINK_FACTOR = 4;
  private static final int COMFORTABLE_CAPACITY = 64;
  
  // lock to enforce synchronicity on stats tracking
  private final ReentrantLock statSynchro = new ReentrantLock();
  
   /* These are only updated in the context of both this object's AND its container's synchro
   Therefore, locking your own synchro guarantees stability of the lastReported
   values of all your contained objects */
  private double lastReportedSize;
  private double lastReportedWeight;
  private double lastReportedLength;
  private double lastReportedWidth;
  
  /* Synchronized to this container's synchro */
  private double contentSize = 0d; // total size of contents
  private double contentWeight = 0d; // total weight of contents
  private double longestContent = 0d; // length of longest content
  private double widestContent = 0d; // width of widest content
  
  //private int contentCountDeep = 0; // recursive count of contents
  
  // number of size reductions since recomputing
  private int contentSizeRecompute = RECOMPUTE_INTERVAL;
  // magnitude of peak size since recomputing
  private double contentSizePeak = 0d;
  // number of weight reductions since recomputing
  private int contentWeightRecompute = RECOMPUTE_INTERVAL;
  // magnitude of peak weight since recomputing
  private double contentWeightPeak = 0d;
  
  /** Lock to ensure that only one process is moving this object at a time. This lock may
   * be held temporarily to 
   */
  private final ReentrantLock moveSynchro = new ReentrantLock();
  private final Condition movableCondition = moveSynchro.newCondition();
  // When this is 0 the object is ok to move
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
    
    lastReportedSize = size();
    lastReportedWeight = weight();
    lastReportedLength = length();
    lastReportedWidth = width();
    
    Report r = new Report();
    if (!container.emplace((Active)this, preposition, Main.creator, r)) {
      // failed to emplace; doom this object
      Container originalTarget = container;
      container = null;
      registry.remove(internalName);
      dateDoomed = System.currentTimeMillis();
      Main.logger.p("creation", "Could not init " + this + " in "
          + originalTarget + " : " + r.text());
      throw new DoesNotFitException(r.text());
    }
    Main.logger.p("creation", r);
    
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
    return (container == null);
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
  
  /** Updates this object's size, weight, etc. */
  protected final void updateStats() {
    if (!initialized)
      return;
    
    boolean changeSize, changeWeight, changeLength, changeWidth;
    
    statSynchro.lock();
    try {
      double newSize = size();
      double newWeight = weight();
      double newLength = length();
      double newWidth = width();
      
      // parameters to send to changeStats
      double sendSize, sendWeight, sendLength, sendWidth;
      
      changeSize = newSize != lastReportedSize;
      changeWeight = newWeight != lastReportedWeight;
      changeLength = newLength != lastReportedLength;
      changeWidth = newWidth != lastReportedWidth;
      
      if (changeSize || changeWeight || changeLength || changeWidth) {
        // synchronize upwards only
        Lock parentLock = container.statSynchro;
        parentLock.lock();
        try  { // synchronize moving upwards only (bottleneck)
          // size
          if (changeSize) {
            sendSize = newSize - lastReportedSize;
            lastReportedSize = newSize;
          } else {
            sendSize = 0d; // no change signal
          }
          
          // weight
          if (changeWeight) {
            sendWeight = newWeight - lastReportedWeight;
            lastReportedWeight = newWeight;
          } else {
            sendWeight = 0d; // no change signal
          }
          
          // length
          if (changeLength) {
            if (newLength < lastReportedLength) {
              // if this could be the container's longest object, propagate upwards
              if (lastReportedLength == container.longestContent)
                sendLength = -1d; // reduce signal
              else
                sendLength = 0d; // no change signal
            } else /* if (newLength > lastReportedLength) */{
              sendLength = newLength; // increase signal
            }
            lastReportedLength = newLength;
          } else {
            sendLength = 0d; // no change signal
          }
          
          // width
          if (changeWidth) {
            if (newWidth < lastReportedWidth) {
              // if this could be the container's longest object, propagate upwards
              if (lastReportedWidth == container.widestContent)
                sendWidth = -1d; // reduce signal
              else
                sendWidth = 0d; // no change signal
            } else /* if (newLength > lastReportedLength) */{
              sendWidth = newWidth; // increase signal
            }
            lastReportedWidth = newWidth;
          } else {
            sendWidth = 0d; // no change signal
          }
          
          // propagate upwards
          container.changeContentStats(sendSize, sendWeight, sendLength,
                                       sendWidth);
        } finally {
          parentLock.unlock();
        }
      }
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Changes content size, weight etc. and immediately propagates */
  private void changeContentStats(double sizeDelta, double weightDelta,
                                  double lengthChange, double widthChange) {
    // if (contents == null) return;
    /* contents should never be null when this method is called, because in
     * order for it to have a content that will call this, contents will have
     * been instantiated */
    boolean changed = false;
    
    // size
    if (sizeDelta != 0d) {
      changed = true;
      contentSize += sizeDelta; // modify size
      
      if (sizeDelta < 0d) {
        // magnitude of weight is being reduced (size is always non-negative)
        // If there have been enough reductions to trigger a recompute,
        // or the size has been reduced enough from its maximum... 
        if (contentSizeRecompute == 0
            || contentSize < contentSizePeak * RECOMPUTE_PEAK_RATIO) {
          // recompute size
          contentSizeRecompute = RECOMPUTE_INTERVAL;
          contentSize = 0d;
          for (Container content : contents) {
            contentSize += content.lastReportedSize;
          }
          contentSizePeak = contentSize;
        } else {
          contentSizeRecompute--;
        }
      } else {
        contentSizePeak = Math.max(contentSize, contentSizePeak);
      }
    }
    
    // weight
    if (weightDelta != 0d) {
      changed = true;
      double oldWeight = contentWeight;
      
      contentWeight += weightDelta;
      
      //check for recomputes
      if (Math.abs(contentWeight) < Math.abs(oldWeight)) {
        if (contentWeightRecompute == 0
            || Math.abs(contentWeight) <
            contentWeightPeak * RECOMPUTE_PEAK_RATIO) {
          // recompute content weight
          contentWeightRecompute = RECOMPUTE_INTERVAL;
          contentWeight = 0d;
          for (Container content : contents) {
            contentWeight += content.lastReportedWeight;
          }
          contentWeightPeak = contentWeight;
        } else {
          // count down to mandatory recompute
          contentWeightRecompute--;
        }
      } else {
        // magnitude of weight is being increased, no worries mate
        contentWeightPeak =
            Math.max(Math.abs(contentWeight), contentWeightPeak);
      }
    }
    
    // length
    if (lengthChange != 0d) {
      if (lengthChange > longestContent) { // increased over current max
        longestContent = lengthChange;
        changed = true;
      } else if (-lengthChange >= longestContent) { // decreased from current max
        double newLongest = 0d;
        for (Container content : contents) {
          double len = content.lastReportedLength;
          if (len == longestContent)
            break;
          newLongest = Math.max(newLongest, len);
        }
        longestContent = newLongest;
        changed = true;
      }
    }
    
    // width
    if (widthChange != 0d) {
      if (widthChange > widestContent) { // increased over current max
        widestContent = widthChange;
        changed = true;
      } else if (-widthChange >= widestContent) { // decreased from current max
        double newWidest = 0d;
        for (Container content : contents) {
          double wide = content.lastReportedWidth;
          if (wide == widestContent)
            break;
          newWidest = Math.max(newWidest, wide);
        }
        widestContent = newWidest;
        changed = true;
      }
    }
    
    // now update this object and so forth
    if (changed)
      updateStats();
  }
  
  /** Returns the total size of the contents */
  public final double contentSize() {
    statSynchro.lock();
    try {
      return contentSize;
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Returns the total weight of the contents */
  public final double contentWeight() {
    statSynchro.lock();
    try {
      return contentWeight;
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Returns the length of the longest contained item */
  public final double longestContent() {
    statSynchro.lock();
    try {
      return longestContent;
    } finally {
      statSynchro.unlock();
    }
  }
  
  /** Returns the width of the widest contained item */
  public final double widestContent() {
    statSynchro.lock();
    try {
      return widestContent;
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
   * Reentrant (4 billion times), persistent, does not block other processes. */
  private void freezeMovement() {
    moveSynchro.lock();
    try {
      moveFreeze++;
    } finally {
      moveSynchro.unlock();
    }
  }
  
  /** Undoes one layer of movement restriction from moveFreeze() */
  private void unfreezeMovement() {
    moveSynchro.lock();
    try {
      if (moveFreeze == 0)
        throw new Error("unfreezeMovement() called on an already unfrozen object!");
      
      moveFreeze--;
      if (moveFreeze == 0)
        movableCondition.signal();
    } finally {
      moveSynchro.unlock();
    }
  }
  
  /** Prepares the object to be moved and */
  private void beginMovement() {
    moveSynchro.lock();
    try {
      while (moveFreeze != 0)
        movableCondition.await();
    } catch (InterruptedException e) {
      System.out.println("Interrupted obtaining movement lock");
      e.printStackTrace();
    }
  }
  
  private void endMovement() {
    if (moveFreeze != 0)
      throw new Error("Process attempted to end movement phase while object was frozen");
    moveSynchro.unlock();
  }
  
  /**
   * Adds a contained object. Returns true on success
   */
  public final boolean add(Active obj, String preposition,
                           Active actor, Report r) {
    /* Laundry list:
     * obj must be initialized
     * obj must not be this object or any object containing it
     * obj must be movable
     * from and this must authorize- remove and add
     */
    
    // obj must not be this object
    if (obj == this) { // self check
      r.report("You cannot put something inside itself.");
      return false; // impossible
    }
    
    // obj must be initialized
    if (!obj.initializedActive())
      throw new Error(obj + " has not been initialized");
    
    // list of objects frozen
    SimpleStack<Container> frozen = new SimpleStack<Container>();
    
    // freeze obj for checks
    ((Container)obj).beginMovement();
    try {
      
      // obj is already here
      if (obj.container() == this) { // pre-containment check
        if (obj.preposition().equals(preposition)) {
          r.report("The " + obj.name().get() + " is already there.");
          return false; // already there
        } else { // here but with different preposition
        
          // shift the object inside this one instead of moving it in the tree
          if (!obj.movable()) { // mobility check
            r.report("The " + obj.name().get() + " is unmovable.");
            return false; // can't move
          } else if (authorizeRemove(obj, this, actor, r)
              && authorizeAdd(obj, preposition, actor, r)) { // authorization check
            enactShift(obj, preposition, actor);
            return true; // hooray
          } else {
            return false; // not authorized
          }
          
        }
      }
      
      // obj must be movable
      if (!obj.movable()) { // mobility check
        r.report("The " + obj.name().get() + " is unmovable.");
        return false; // can't move
      }
      
      // obj must not in any way contain this
      Container c = this;
      do {
        frozen.push(c);
        c.freezeMovement();
        if (c.container == obj) {
          r.report("You cannot put an object inside itself");
          return false;
        }
        c = c.container;
      } while (c != null);
      
      // obj's current container must authorize the removal
      // this container must authorize its addition
      if (obj.container().authorizeRemove(obj, this, actor, r)
          && authorizeAdd(obj, preposition, actor, r)) { // authorization check
        enactMove(obj, preposition, actor);
        return true; // hooray
      } else {
        return false; // not authorized
      }
    } finally {
      // end the movement phase for obj
      ((Container)obj).endMovement();
      
      // unfreeze movement of critical objects
      while (!frozen.isEmpty())
        frozen.pop().unfreezeMovement();
    }
  }
  
  /**
   * Removes the specified object to this container's container
   * (using the same preposition this object has in that container); if this
   * container is a room, the object is thrown into the Inferno.
   */
  public final boolean remove(Active obj, Active actor, Report r) {
    if (!contains(obj)) { // necessary presence check
      r.report("The " + obj.name().get() + " is not in the "
          + this.name().get());
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
    statSynchro.lock();
    try {
      Active[] contentArray = allContents();
      for (Active a : contentArray)
        // destroy contents first
        a.destroy(actor);
      if (container != null) {
        enactRemove((Active)this);
      }
      container = null;
      registry.remove(internalName);
      dateDoomed = System.currentTimeMillis();
      onDestroyed(actor);
    } finally {
      statSynchro.unlock();
    }
    
    Main.logger.p("destruction", this + " was incinerated by " + actor);
  }
  
  /**
   * Moves the given object into this container
   */
  private void enactMove(Active obj, String prep, Active actor) {
    Container from;
    
    ((Container)obj).statSynchro.lock();
    try {
      from = obj.container();
      from.statSynchro.lock();
      try {
        enactRemove(obj);
      } finally {
        from.statSynchro.unlock();
      }
      enactAdd(obj, prep);
    } finally {
      ((Container)obj).statSynchro.unlock();
    }
    
    Main.logger.p("movement", obj + " was moved to " + this + " by " + actor);
    from.onRemove(obj, this, actor); // notify source    
    this.onAdd(obj, actor); // notify destination
    obj.onMoved(actor); // notify moved object
  }
  
  /**
   * Concurrency v2 enactAdd method.
   * adds to this container unconditionally and propagates stats
   */
  private void enactAdd(Active obj, String prep) {
    if (this instanceof Active && !initialized)
      throw new Error(this + " has not been initialized");
    
    synchronized (statSynchro) {
      ((Container)obj).container = this;
      ((Container)obj).preposition = prep;
      if (contents == null) // lazy initialization
        contents = new RandomAccessLinkedHashSet<Active>();
      contents.add(obj);
      contentCountPeak = Math.max(contents.size(), contentCountPeak);
      
      changeContentStats(obj.size(), obj.weight(), obj.length(), obj.width());
    }
  }
  
  /**
   * Concurrency v2 enactRemove method.
   * Unconditionally removes the object from its container and propagates stats
   */
  private static void enactRemove(Active obj) {
    Container from = ((Container)obj).container;
    from.contents.remove(obj);
    from.changeContentStats(-obj.size(), -obj.weight(), -obj.length(), -obj.width());
    
    if (from.contentSizePeak > MAX_COMFORTABLE_CAPACITY
        && from.contents.size() <= from.contentCountPeak >>> SHRINK_FACTOR) {
      from.contents.shrink(COMFORTABLE_CAPACITY);
      from.contentCountPeak = from.contents.size();
    }
  }
  
  private void enactShift(Active obj, String newPreposition, Active actor) {
    String oldPrep = obj.preposition();
    ((Container)obj).preposition = newPreposition.intern();
    
    onShift(obj, actor, oldPrep); // notify this container
    obj.onShifted(actor, oldPrep); // notify the moving object
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