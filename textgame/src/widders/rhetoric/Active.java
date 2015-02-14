package widders.rhetoric;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import widders.util.Value;


/**
 * 
 * @author widders
 */
public abstract class Active extends Container {
  
  // HAS VALUE HASHTABLE
  // Value getValue(String name)
  // boolean hasValue(String name)
  // boolean setValue(String name, Value/String/int/double/boolean val)
  
  // IMPLEMENT TARGETING
  // do i do this with prepositions? i'm thinking to send the verb to the target
  // in a verb.preposition format to simplify things; this could work pretty
  // well, since i plan on using heavy regex parsing on the user input stream
  // target(), targeted() and see() are done already
  
  
  /* The set of words that identify this object. All lowercase. */
  private String[] identity;
  
  /* The set of words (adjectives only, basically) that specify this object's
   * current state (they are treated as a part of the identity in matches().
   * All lowercase. */
  private Set<String> properties;// = new HashSet<String>();
  
  /* The set of classifications that this Thing fits (such as "container").
   * These never act as part of the identity, but they can be accessed. Should
   * be all lowercase, but is NOT FORCED CASE (this is for efficiency since
   * classification won't be exposed directly to the user).
   * Classifications are interned on addition (two identical classifications
   * are guaranteed to be the same String object). */
  private Set<String> classification;// = new HashSet<String>();
  
  /* The list of Active objects monitoring this object */
  private Set<Active> watchers;// = new LinkedHashSet<Active>();
  
  /* Contains all the Active objects that this Watcher is registered with */
  private Set<Active> watching;// = new LinkedHashSet<Active>();
  
  /* The set of values associated with this object */
  private Map<String, Value> values;// = new Hashtable<String, Value>();
  
  
  protected Active(Name n, String[] ident, String[] classes,
                   String[] properties,
                   Container initialContainer, String preposition) {
    super(n, initialContainer, preposition);
    if (initialContainer == null)
      throw new NullPointerException("Active objects must have an initial"
          + " container");
    
    if (n == null || ident == null)
      throw new NullPointerException();
    
    if (ident.length == 0 || !initIdentity(ident))
      throw new IllegalArgumentException("Invalid identity: " + ident);
    addProperties(properties);
    if (classes != null)
      classify(classes);
  }
  
  protected Active(Name n, String[] ident,
                   Container initialContainer, String preposition) {
    this(n, ident, null, null, initialContainer, preposition);
  }
  
  
  /**
   * Initializes identity words and returns true if the initialization was
   * carried out
   */
  private boolean initIdentity(String[] ident) {
    if (ident == null || ident.length == 0)
      return false;
    
    String[] t = new String[ident.length];
    for (int i = 0; i < ident.length; i++)
      t[i] = ident[i].toLowerCase();
    
    Arrays.sort(t);
    identity = t;
    
    return true;
  }
  
  /** Returns the basic identity */
  public final synchronized String[] basicIdentity() {
    return Arrays.copyOf(identity, identity.length);
  }
  
  /** Returns the current identity (including identity and properties) */
  public final synchronized String[] identity() {
    if (properties == null) {
      return basicIdentity();
    } else {
      String[] x =
          properties.toArray(new String[identity.length + properties.size()]);
      System.arraycopy(identity, 0, x, properties.size(), identity.length);
      return x;
    }
  }
  
  /** Returns the current properties */
  public final synchronized String[] properties() {
    return properties == null
        ? new String[0]
        : properties.toArray(new String[properties.size()]);
  }
  
  /** Returns the number of properties */
  public final synchronized int propertyCount() {
    return properties == null
        ? 0
        : properties.size();
  }
  
  /**
   * Adds the given property and returns true if it was really added or false if
   * it was already there
   */
  public final synchronized boolean addProperty(String prop) {
    if (properties == null) properties = new HashSet<String>();
    return properties.add(prop.toLowerCase());
  }
  
  /** Batch adds properties */
  public final synchronized void addProperties(String... props) {
    if (props != null && props.length > 0) {
      if (properties == null) properties = new HashSet<String>();
      for (String p : props) {
        properties.add(p.toLowerCase());
      }
    }
  }
  
  /**
   * Removes the given property from the properties list and returns true if and
   * only if it was really there to begin with
   */
  public final synchronized boolean removeProperty(String prop) {
    return properties == null
        ? false
        : properties.remove(prop.toLowerCase());
  }
  
  public final synchronized void removeProperties(String... prop) {
    if (prop != null && properties != null)
      for (String p : prop)
        properties.remove(p.toLowerCase());
  }
  
  /** Returns true iff the object has the given property set */
  public final synchronized boolean hasProperty(String prop) {
    return properties == null
        ? false
        : properties.contains(prop.toLowerCase());
  }
  
  /** Returns true iff the object has all the given properties set */
  public final synchronized boolean hasProperties(String... properties) {
    if (properties == null)
      return true;
    
    for (String prop : properties)
      if (!hasProperty(prop))
        return false;
    return true;
  }
  
  /** Returns true iff ident is found in either identity or properties */
  public final synchronized boolean is(String ident) {
    ident = ident.toLowerCase();
    return Arrays.binarySearch(identity, ident) >= 0
        || (properties != null && properties.contains(ident));
  }
  
  /** Returns true iff this object completely matches the given identity */
  public final synchronized boolean is(String... ident) {
    if (ident == null)
      return true;
    
    for (String part : ident)
      if (!is(part))
        return false; // did not match the current identifier
    return true; // all identifiers matched
  }
  
  /** Returns the current classifications */
  public final synchronized String[] classification() {
    return classification == null
        ? new String[0]
        : classification.toArray(new String[classification.size()]);
  }
  
  /** Returns the total number of classifications on this object */
  public final synchronized int classificationCount() {
    return classification == null
        ? 0
        : classification.size();
  }
  
  /** Classifies the object as [c] and returns true only if it wasn't already */
  public final synchronized boolean classify(String c) {
    if (classification == null) classification = new HashSet<String>();
    return classification.add(c.intern());
  }
  
  /** Batch adds classifications */
  public final synchronized void classify(String... classes) {
    if (classes == null || classes.length == 0)
      return;
    if (this.classification == null)
      this.classification = new HashSet<String>(); 
    for (String c : classes)
      this.classification.add(c.intern());
  }
  
  /**
   * Declassifies the object from [c] and returns true only if it was already
   * classified as [c]
   */
  public final synchronized boolean declassify(String c) {
    return classification == null
        ? false
        : classification.remove(c);
  }
  
  /** Declassifies the object from every classification given */
  public final synchronized void declassify(String... classes) {
    if (classification != null && classes != null) {
      for (String c : classes)
        declassify(c);
    }
  }
  
  /** Returns true iff the object is classified as [c] */
  public final synchronized boolean isClassified(String c) {
    return classification == null
        ? false
        : classification.contains(c);
  }
  
  /**
   * Returns true iff this object completely matches the given identity and
   * classifications
   */
  public final boolean isClassified(String... classes) {
    if (classes == null || classes.length == 0)
      return true;
    
    for (String part : classes)
      if (!isClassified(part))
        return false;
    return true;
  }
  
  /** The default weight implementation. */
  @Override
  public double weight() {
    return baseWeight() + contentWeight();
  }
  
  /** The weight of this object before contents. */
  public abstract double baseWeight();
  
  /** Called when the object is destroyed. */
  @Override
  protected final void onDestroyed(Active actor) {
    // end all watches
    clearWatchers();
    clearWatching();
  }
  
  /**
   * Called when the object is destroyed. Override to perform additional
   * cleanup behavior.
   * 
   * @param actor
   */
  protected void cleanupOnDestroyed(Active actor) {
  }
  
  /** Returns whether this object can be moved */
  public abstract boolean movable();
  
  /**
   * Targets this object with an action; returns true if something happened,
   * otherwise returns false
   */
  public final boolean target(Active actor, String verb, Report r) {
    if (!initializedActive())
      throw new Error(this + " has not been initialized");
    
    if (targeted(actor, verb, r)) { // notify and verify with this object
      notifyWatchers(actor, verb, null); // notify watchers
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Targets this object with an action; returns true if something happened,
   * otherwise returns false
   */
  public final boolean target(Active actor, String verb) {
    return target(actor, verb, Main.fakeReport);
  }
  
  /**
   * Called when this object is used as an indirect object in an action call.
   * Must return true if and only if something happens.
   */
  public final boolean use(Active actor, String verb, Selection target,
                           Report r) {
    if (!initializedActive())
      throw new Error(this + " has not been initialized");
    
    if (used(actor, verb, target, r)) {
      for (Active a : target) {
        if (a.targeted(actor, verb, this, r))
          a.notifyWatchers(actor, verb, this);
      }
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Called when this object is targeted with an action that has no indirect
   * object. Must return true if and only if something happens.
   */
  protected abstract boolean targeted(Active actor, String verb, Report r);
  
  /**
   * Called when this object is targeted with an action using an indirect
   * object.
   */
  protected abstract boolean targeted(Active actor, String verb,
                                      Active indirect, Report r);
  
  /**
   * Called when this object is used as an indirect object in an action call.
   * Returns true iff this object can be used in such a way.
   */
  protected abstract boolean used(Active actor, String verb, Selection target,
                                  Report r);
  
  /**
   * Called when something happens to a watched object OR this object is
   * targeted with an indirect-object action that goes through
   * 
   * Override to implement behavior!
   * 
   * @param actor
   *            the object causing the action
   * @param verb
   *            the action taking place
   * @param target
   *            the target of the action
   * @param indirect
   *            the indirect object; for instance, the object being used
   *            (can be null for none)
   */
  public void see(Active actor, String verb, Active target,
                           Active indirect) {
  }
  
  /**
   * Called when this object is moved to a different container.
   * Can be overridden!
   * 
   * @param actor
   *          the object that caused the move
   */
  public void onMoved(Active actor) {
    notifyWatchers(actor, "move", null);
  }
  
  /**
   * Called when this object is shifted (changes its preposition) inside its container.
   * Can be overridden!
   * 
   * @param actor
   *          the object that caused the move
   * @param oldPreposition
   *          this object's previous containment preposition
   */
  public void onShifted(Active actor, String oldPreposition) {
    notifyWatchers(actor, "shift", null);
  }
  
  /** Notifies the watching objects that an action was taken upon this object */
  private void notifyWatchers(Active actor, String verb, Active indirect) {
    if (watchers == null)
      return;
    for (Active w : watchers) {
      w.see(actor, verb, this, indirect);
    }
  }
  
  /**
   * Adds [w] to the Watchers list and returns whether the operation was
   * successful
   */
  public final boolean addWatcher(Active w) {
    if (!initializedActive())
      throw new Error(this + " has not been initialized");
    
    if (w == null || w == this)
      return false;
    if (w.watching == null)
      w.watching = new LinkedHashSet<Active>();
    w.watching.add(this);
    if (watchers == null)
      watchers = new LinkedHashSet<Active>();
    watchers.add(w);
    return true;
  }
  
  /**
   * Removes [w] from the Watchers list and returns true iff it was there to
   * begin with
   */
  public final boolean removeWatcher(Active w) {
    if (watchers == null || w == null)
      return false;
    if (w.watching != null && w.watching.remove(this)) {
      watchers.remove(w);
      return true;
    } else {
      return false;
    }
  }
  
  /** Returns true iff this Active object is reporting to the given object */
  public final boolean hasWatcher(Active w) {
    return watchers != null && watchers.contains(w);
  }
  
  /** Returns an array of all this object's watchers */
  public final Active[] allWatchers() {
    return watchers == null
        ? new Active[0]
        : watchers.toArray(new Active[watchers.size()]);
  }
  
  /** Returns the number of objects watching this one */
  public final int watcherCount() {
    return watchers == null
        ? 0
        : watchers.size();
  }
  
  /**
   * Clears all watches on this object from others.
   * 
   * @return
   *         the number of watchers this object had
   */
  public final int clearWatchers() {
    if (watchers == null)
      return 0;
    int n = watchers.size();
    for (Active w : watchers) {
      w.watching.remove(this);
    }
    watchers = null;
    return n;
  }
  
  /**
   * Begins watching Active object [a] and returns whether the operation was
   * successful
   */
  public final boolean startWatching(Active a) {
    return a.addWatcher(this);
  }
  
  /**
   * Stops watching Active object [a] and returns true iff it was there to begin
   * with
   */
  public final boolean stopWatching(Active a) {
    return a.removeWatcher(this);
  }
  
  /** Returns true iff this Active object is watching the given object */
  public final boolean isWatching(Active a) {
    return watching != null && watching.contains(a);
  }
  
  /** Returns an array of everything this object is currently watching */
  public final Active[] allWatching() {
    return watching == null
        ? new Active[0]
        : watching.toArray(new Active[watching.size()]);
  }
  
  /** Returns the number of objects this object is watching */
  public final int watchingCount() {
    return watching == null
        ? 0
        : watching.size();
  }
  
  /**
   * Removes all watches this object has on others.
   * 
   * @returns
   *          the number of watches this object had
   */
  public final int clearWatching() {
    if (watching == null)
      return 0;
    int n = watching.size();
    for (Active w : watching) {
      w.watchers.remove(this);
    }
    watching = null;
    return n;
  }
  
  /**
   * Returns the value of the given name; if the value is not set, an empty
   * value is returned
   */
  public final Value getValue(String name) {
    if (values == null)
      return Value.make();
    Value v = values.get(name);
    return v == null ? Value.make() : v;
  }
  
  /**
   * Sets the value of the given name to the given value. Returns the previous
   * value, if it was set, otherwise returns an empty value
   */
  public final Value setValue(String name, String val) {
    if (values == null)
      values = new Hashtable<String, Value>();
    Value x = values.put(name, Value.make(val));
    return (x == null ? Value.make() : x);
  }
  
  /**
   * Sets the value of the given name to the given value. Returns the previous
   * value, if it was set, otherwise returns an empty value
   */
  public final Value setValue(String name, long val) {
    if (values == null)
      values = new Hashtable<String, Value>();
    Value x = values.put(name, Value.make(val));
    return (x == null ? Value.make() : x);
  }
  
  /**
   * Sets the value of the given name to the given value. Returns the previous
   * value, if it was set, otherwise returns an empty value
   */
  public final Value setValue(String name, double val) {
    if (values == null)
      values = new Hashtable<String, Value>();
    Value x = values.put(name, Value.make(val));
    return (x == null ? Value.make() : x);
  }
  
  /**
   * Sets the value of the given name to the given value. Returns the previous
   * value, if it was set, otherwise returns an empty value
   */
  public final Value setValue(String name, boolean val) {
    if (values == null)
      values = new Hashtable<String, Value>();
    Value x = values.put(name, Value.make(val));
    return (x == null ? Value.make() : x);
  }
  
  /**
   * Sets the value of the given name to the given value. Returns the previous
   * value, if it was set, otherwise returns an empty value
   */
  public final Value setValue(String name, Value val) {
    if (val == null || val.isUnset()) {
      return deleteValue(name);
    } else {
      if (values == null)
        values = new Hashtable<String, Value>();
      Value x = values.put(name, val);
      return (x == null ? Value.make() : x);
    }
  }
  
  /** Returns true if and only if the value of the given name is set */
  public final boolean hasValue(String name) {
    return values != null && values.containsKey(name);
  }
  
  /**
   * Deletes the value entry of the given name. Returns the previous value, if
   * it was set, otherwise returns an empty value
   */
  public final Value deleteValue(String name) {
    if (values == null)
      return Value.make();
    Value x = values.remove(name);
    return (x == null ? Value.make() : x);
  }
}