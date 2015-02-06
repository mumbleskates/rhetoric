package widders.rhetoric;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import widders.util.IterProtector;
import widders.util.RandomAccessLinkedHashSet;


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
  
  // SOLVED: what if you add a bunch of empty bags to another identical bag,
  // then fill up all the inside bags so that the limits are actually
  // exceeded?
  // PROBLEM ALREADY SOLVED (by seeding flexibleSize() from this class and using
  // that to determine extended fitting etc.)
  // ^ WOW that was an old problem. It's so much more beautiful now, you should see it.
  
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
  private static final int RECOMPUTE_INTERVAL = 1 << 12;
  /* force recompute size/weight totals if the magnitude falls below this ratio
   * of its peak. */
  private static final double RECOMPUTE_PEAK_RATIO = 1d / (1L << 32);
  /* Constants for optimizing content table sizes */
  private static final int MAX_COMFORTABLE_CAPACITY = 192;
  private static final int SHRINK_FACTOR = 4;
  private static final int COMFORTABLE_CAPACITY = 64;
  
  
  // lock to enforce synchronicity on dimension tracking
  private Object synchro = new Object();
  
  // These are only updated in the context of both this object's and its container's synchro
  private double lastReportedSize;
  private double lastReportedWeight;
  private double lastReportedLength;
  private double lastReportedWidth;
  
  /* Every instance in which these are accessed is synchronized to this object's synchro */
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
  
  /** The object containing this container */
  private Container container;
  
  /**
   * Simple preposition that indicates the relationship to this object's
   * container. Interned before set; comparing the actual property can use ==
   * operator
   */
  private String preposition;
  
  /** The contents of this container */
  private RandomAccessLinkedHashSet<Active> contents =
      new RandomAccessLinkedHashSet<Active>();
  //new LinkedHashSet<Active>();
  private int contentCountPeak;
  
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
  private static long nextID = 0;
  /** Unique ID for this Container */
  public final long iD = nextID++;
  
  
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
    this.preposition = preposition == null
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
  
  /** Places the object from init() */
  private synchronized boolean emplace(Active obj, String preposition,
                                       Active actor, Report r) {
    if (!authorizeAdd(obj, preposition, actor, r))
      return false;
    
    enactAdd(obj, preposition, obj.size(), obj.weight(),
             obj.length(), obj.width());
    
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
    return new IterProtector<Active>(contents.iterator());
  }
  
  public final Active[] allContents() {
    return contents.toArray(new Active[contents.size()]);
  }
  
  /** Returns the number of contained items */
  public final int contentCount() {
    return contents.size();
  }
  
  /** Returns the number of items contained recursively */
  public final int contentCountDeep() {
    int t;
    t = contents.size();
    for (Active content : contents) {
      t += content.contentCountDeep();
    }
    return t;
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
  public final boolean containsDeep(Active obj) {
    for (Container c = ((Container)obj).container; c != null; c = c.container) {
      if (c == this)
        return true;
    }
    return false;
  }
  
  /**
   * Returns the deepest common container of this object and the given one, or
   * null if there is no common container. If they are the same object or one is
   * a
   * direct or recursive parent of the other, returns the outermost of the two.
   */
  public final Container commonContainer(Active obj) {
    Container possible = this;
    do {
      if (possible == obj || possible.containsDeep(obj))
        return possible;
      possible = possible.container;
    } while (possible != null);
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
    
    synchronized (synchro) {
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
        synchronized (container.synchro) { // synchronize moving upwards only (bottleneck)
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
          
          container.changeContentStats(sendSize, sendWeight, sendLength, sendWidth);
        }
      }
    }
  }
  
  /** Changes the content size, weight, length etc. */
  private void changeContentStats(double sizeDelta, double weightDelta, double lengthChange, double widthChange) {
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
      changed = true;
      if (lengthChange > 0d) { // content increased in length
        longestContent = Math.max(longestContent, lengthChange);
      } else if (lengthChange < 0d) { // content decreased in length
        double newLongest = 0d;
        for (Container content : contents) {
          double len = content.lastReportedLength;
          if (len == longestContent) break;
          newLongest = Math.max(newLongest, len);
        }
        longestContent = newLongest;
      }
    }
    
    // width
    if (widthChange != 0d) {
      changed = true;
      if (widthChange > 0d) { // content increased in length
        widestContent = Math.max(widestContent, widthChange);
      } else if (widthChange < 0d) { // content decreased in length
        double newWidest = 0d;
        for (Container content : contents) {
          double wide = content.lastReportedWidth;
          if (wide == widestContent) break;
          newWidest = Math.max(newWidest, wide);
        }
        widestContent = newWidest;
      }
    }
    
    // now update this object and so forth
    if (changed) updateStats();
  }
  
  @Deprecated
  /** Updates this object's size */
  protected final void updateSize() {
    if (!initialized)
      return;
    
    synchronized (synchro) {
      double newSize = size();
      if (newSize != lastReportedSize) {
        // synchronize upwards only
        synchronized (container.synchro) { ///// TODO synchro check
          lastReportedSize = newSize;
          container.changeContentSize(newSize - lastReportedSize);
        }
      }
    }
  }
  
  @Deprecated
  /** Updates this object's weight */
  protected final void updateWeight() {
    if (!initialized)
      return;
    
    synchronized (synchro) {
      double newWeight = weight();
      if (newWeight != lastReportedWeight) {
        synchronized (container.synchro) { ///// TODO synchro check
          lastReportedWeight = newWeight;
          container.changeContentWeight(newWeight - lastReportedWeight);
        }
      }
    }
  }
  
  @Deprecated
  protected final void updateLength() {
    if (!initialized)
      return;
    
    synchronized (synchro) {
      double newLength = length();
      if (newLength == lastReportedLength)
        return;
      
      synchronized (container.synchro) { ///// TODO synchro check
        lastReportedLength = newLength;
        
        if (newLength < lastReportedLength) {
          // if this could be the container's longest object, propagate upwards
          if (lastReportedLength == container.longestContent)
            container.updateLongestContentReduce();
        } else /* if (newLength > lastReportedLength) */{
          container.updateLongestContentIncrease(newLength);
        }
      }
    }
    
    onContentLengthChanged(); ///// TODO this doesn't seem right
  }
  
  @Deprecated
  protected final void updateWidth() {
    if (!initialized)
      return;
    
    synchronized (synchro) {
      double newWidth = width();
      if (newWidth == lastReportedWidth)
        return;
      synchronized (container.synchro) { ///// TODO synchro check
        lastReportedWidth = newWidth;
        
        if (newWidth < lastReportedWidth) {
          // if this could be the container's widest content, propagate upwards
          if (lastReportedWidth == container.widestContent)
            container.updateWidestContentReduce();
        } else /* if (newWidth > lastReportedWidth) */{
          lastReportedWidth = newWidth;
          container.updateWidestContentIncrease(newWidth);
        }
      }
    }
    
    onContentWidthChanged(); ///// TODO this doesn't seem right
  }
  
  /** Returns the total size of the contents */
  public final double contentSize() {
    synchronized (synchro) {
      return contentSize;
    }
  }
  
  /** Returns the total weight of the contents */
  public final double contentWeight() {
    synchronized (synchro) {
      return contentWeight;
    }
  }
  
  /** Returns the length of the longest contained item */
  public final double longestContent() {
    synchronized (synchro) {
      return longestContent;
    }
  }
  
  /** Returns the width of the widest contained item */
  public final double widestContent() {
    synchronized (synchro) {
      return widestContent;
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
  
  @Deprecated
  /**
   * Modifies the tallied total content size without necessarily recomputing.
   * Negative object sizes are not supported!
   * 
   * Only called from synchronized context.
   * 
   * @param deltaSize
   */
  private void changeContentSize(double deltaSize) {
    if (deltaSize == 0d)
      return;
    
    synchronized (synchro) {
      double oldSize = contentSize;
      
      contentSize += deltaSize; // modify size
      
      if (deltaSize < 0) {
        // magnitude of weight is being reduced (size is always non-negative)
        if (contentSizeRecompute == 0
            || contentSize < contentSizePeak * RECOMPUTE_PEAK_RATIO) {
          recomputeContentSize();
        } else {
          contentSizeRecompute--;
        }
      } else {
        contentSizePeak = Math.max(contentSize, contentSizePeak);
      }
      updateSize();
      if (contentSize != oldSize)
        onContentSizeChanged();
    }
  }
  
  @Deprecated
  /**
   * Updates the size of this container's contents. 
   *  
   * Only called from synchronized context
   */
  private void recomputeContentSize() {
    contentSizeRecompute = RECOMPUTE_INTERVAL;
    double x = 0d;
    for (Active content : contents) {
      x += content.size();
    }
    contentSizePeak = contentSize = x;
  }
  
  @Deprecated
  /**
   * Modifies the tallied total content weight without necessarily recomputing.
   * Supports negative weight.
   * 
   * Only called from synchronized context.
   * 
   * @param deltaWeight
   */
  private void changeContentWeight(double deltaWeight) {
    if (deltaWeight == 0d)
      return;
    
    synchronized (synchro) {
      double oldWeight = contentWeight;
      
      contentWeight += deltaWeight;
      
      if (Math.abs(contentWeight) < Math.abs(oldWeight)) {
        // magnitude of weight is being reduced
        if (contentWeightRecompute == 0
            || Math.abs(contentWeight) <
            contentWeightPeak * RECOMPUTE_PEAK_RATIO) {
          recomputeContentWeight();
        } else {
          contentWeightRecompute--;
        }
      } else {
        // magnitude of weight is being increased, no worries mate
        contentWeightPeak =
            Math.max(Math.abs(contentWeight), contentWeightPeak);
      }
      updateWeight();
      if (contentWeight != oldWeight)
        onContentWeightChanged();
    }
  }
  
  @Deprecated
  /**
   * Updates the weight of this container's contents.
   * 
   * Only called from synchronized context
   */
  private void recomputeContentWeight() {
    contentWeightRecompute = RECOMPUTE_INTERVAL;
    double newWeight = 0d;
    for (Container content : contents) {
      newWeight += content.lastReportedWeight;
    }
    contentWeightPeak = contentWeight = newWeight;
  }
  
  @Deprecated
  /** Only called from synchronized context */
  private void updateLongestContentIncrease(double length) {
    synchronized (synchro) {
      if (length > longestContent) {
        longestContent = length;
        updateLength();
      }
    }
  }
  
  @Deprecated
  /**
   * updates the length on object's removal with knowledge of the old length
   * 
   * Only called from synchronized context
   */
  private void updateLongestContentReduce() {
    double longest = 0d;
    synchronized (synchro) {
      for (Active content : contents) {
        double len = content.length();
        if (len == longestContent)
          return;
        longest = Math.max(longest, len);
      }
      longestContent = longest;
    }
    updateLength();
  }
  
  @Deprecated
  /** Only called from synchronized context */
  private void updateWidestContentIncrease(double width) {
    synchronized (synchro) {
      if (width > widestContent) {
        widestContent = width;
        updateWidth();
      }
    }
  }
  
  @Deprecated
  /**
   * updates the width on object's removal with knowledge of the old width
   * 
   * Only called from synchronized context
   */
  private void updateWidestContentReduce() {
    double widest = 0d;
    synchronized (synchro) {
      for (Active content : contents) {
        double width = content.width();
        if (width == widestContent)
          return;
        widest = Math.max(widest, width);
      }
      widestContent = widest;
      updateWidth();
    }
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
  
  /**
   * Adds a contained object. Returns true on success
   */
  public final synchronized boolean add(Active obj, String preposition,
                                        Active actor, Report r) {
    if (!obj.initializedActive()) // object is not initialized
      throw new Error(obj + " has not been initialized");
    
    if (obj == this) { // self check
      r.report("You cannot put something inside itself.");
      return false; // impossible
    } else if (contents.contains(obj)) { // pre-containment check
      if (obj.preposition().equals(preposition)) {
        r.report("The " + obj.name().get() + " is already there.");
        return false; // already there
      } else { // here but with different preposition
      
        // shift
        if (!obj.movable()) { // mobility check
          r.report("The " + obj.name().get() + " is unmovable.");
          return false; // can't move
        } else if (authorizeRemove(obj, this, actor, r)
            && authorizeAdd(obj, preposition, actor, r)) { // authorization check
          String oldPrep = obj.preposition();
          ((Container)obj).preposition = preposition.intern();
          
          onShift(obj, actor, oldPrep); // notify this container
          obj.onMoved(actor); // notify the moving object
          return true; // hooray
        } else {
          return false; // not authorized
        }
        
      }
    } else if (!obj.movable()) { // mobility check
      r.report("The " + obj.name().get() + " is unmovable.");
      return false; // can't move
    }
    
    if (obj.container().authorizeRemove(obj, this, actor, r)
        && authorizeAdd(obj, preposition, actor, r)) { // authorization check
      enactMove(obj, preposition, actor);
      return true; // hooray
    } else {
      return false; // not authorized
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
  public final synchronized void destroy(Active actor) {
    Active[] contentArray = contents.toArray(new Active[contents.size()]);
    for (Active a : contentArray)
      // destroy contents first
      a.destroy(actor);
    if (container != null)
      enactRemove((Active)this, size(), weight(), length(), width());
    container = null;
    registry.remove(internalName);
    dateDoomed = System.currentTimeMillis();
    onDestroyed(actor);
    
    Main.logger.p("destruction", this + " was incinerated by " + actor);
  }
  
  /**
   * Moves the given object into this container
   * 
   * Only called from synchronized context
   */
  private void enactMove(Active obj, String preposition, Active actor) {
    double size = obj.size(), weight = obj.weight(), length = obj.length(), width =
        obj.width();
    
    // REMOVE FROM OLD CONTAINER
    enactRemove(obj, size, weight, length, width);
    
    obj.container().onRemove(obj, this, actor); // notify source
    
    // ADD TO NEW CONTAINER
    enactAdd(obj, preposition, size, weight, length, width);
    
    Main.logger.p("movement", obj + " was moved to " + this + " by " + actor);
    onAdd(obj, actor); // notify destination
    obj.onMoved(actor); // notify moved object
  }
  
  /** Only called from synchronized context */
  private void enactAdd(Active obj, String preposition,
                        double size, double weight,
                        double length, double width) {
    if (this instanceof Active && !initialized)
      throw new Error(this + " has not been initialized");
    
    ((Container)obj).container = this;
    ((Container)obj).preposition = preposition.intern();
    this.contents.add(obj);
    this.contentCountPeak = Math.max(contents.size(), this.contentCountPeak);
    changeContentSize(size);
    changeContentWeight(weight);
    updateLongestContentIncrease(length);
    updateWidestContentIncrease(width);
  }
  
  ///// TODO onContentWeightChanged etc. events shouldn't be called twice if a container containsdeep an object that's being moved...
  ///// TODO do you really need these dimensions or can you use lastReportedWeight etc.? there's no way they're really needed...
  private static void enactRemove(Active obj, double size, double weight,
                                  double length, double width) {
    Container from = ((Container)obj).container;
    synchronized (from) {
      if (from.contents.remove(obj)) {
        
        ///// TODO gotta use the new system 
        from.changeContentSize(-size);
        from.changeContentWeight(-weight);
        if (length == from.longestContent)
          from.updateLongestContentReduce();
        if (width == from.widestContent)
          from.updateWidestContentReduce();
        if (from.contentSizePeak > MAX_COMFORTABLE_CAPACITY
            && from.contents.size() <= from.contentCountPeak >>> SHRINK_FACTOR) {
          from.contents.shrink(COMFORTABLE_CAPACITY);
          from.contentCountPeak = from.contents.size();
        }
      }
    }
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
    /*Main.debug.p("finalized", toString()
     * + " / lived for " + (dateDoomed - dateCreated) + "ms"
     * + ", doomed for " + (System.currentTimeMillis() - dateDoomed) + "ms"); */
  }
}