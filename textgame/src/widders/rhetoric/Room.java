package widders.rhetoric;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 * @author widders
 */
public class Room extends Container {
  /** The list of exits to connected rooms, listed by direction */
  protected Hashtable<String, Exit> exits = new Hashtable<String, Exit>();
  private String des;
  
  /** Maximum total size of contents */
  protected double sizeLimit;
  /** Maximum length of contents */
  protected double lengthLimit;
  /** Maximum width of contents */
  protected double widthLimit;
  
  // features are added to the room normally and are just contained therein, as
  // are entities etc.
  // FINISH EXITS IMPL: structure finished
  // //LATER: LIGHT LVL
  // Iterates population
  // //much later: sound propagation
  // //has waiting queue to get in? something like that would be nice; perhaps
  // there could be provision for pushing your way in or something, but this
  // would probably be best saved for later
  
  /** Creates a new instance of Room */
  public Room(Name name, String description,
              double size, double length, double width) {
    super(name, null, null);
    des = description;
    sizeLimit = size;
    lengthLimit = length;
    widthLimit = width;
  }
  
  public Room(Name name, String description,
              double size, double lengthLimit) {
    this(name, description, size, lengthLimit, defaultWidth(size, lengthLimit));
  }
  
  @Override
  public String des(Detail detail) {
    return des;
  }
  
  public final Iterator<Entity> Entities() {
    return new EntityIterator();
  }
  
  private class EntityIterator implements Iterator<Entity> {
    /* states:
     * next is null -> no more entities
     * next is valid -> at least this value exists */
    
    Iterator<Active> it;
    Entity next;
    
    private EntityIterator() {
      it = iterator();
      findNext();
    }
    
    public boolean hasNext() {
      return next != null;
    }
    
    public Entity next() {
      if (next == null) {
        throw new NoSuchElementException();
      }
      Entity e = next;
      findNext();
      return e;
    }
    
    private void findNext() {
      while (it.hasNext()) {
        Active a = it.next();
        if (a instanceof Entity) {
          next = (Entity)a;
          return;
        }
      }
      // no more found
      next = null;
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public double lengthLimit() {
    return lengthLimit;
  }

  @Override
  public double widthLimit() {
    return widthLimit;
  }

  @Override
  public double availableSize() {
    return sizeLimit == Double.POSITIVE_INFINITY
        ? Double.POSITIVE_INFINITY
        : sizeLimit - contentSize();
  }

  @Override
  public final double weight() {
    return contentWeight();
  }

  @Override
  public final double size() {
    return contentSize();
  }
  
  @Override
  public double length() {
    return lengthLimit;
  }
  
  @Override
  public double width() {
    return widthLimit;
  }
}