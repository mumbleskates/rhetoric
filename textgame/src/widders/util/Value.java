package widders.util;

public abstract class Value {
//  public static final int
//      TYPE_UNSET = 0,
//      TYPE_INT = 1,
//      TYPE_FLOAT = 2,
//      TYPE_BOOLEAN = 3,
//      TYPE_STRING = 4;
  public enum ValueType {
    UNSET("Unset"),
    INT("Integer"),
    FLOAT("Floating"),
    BOOLEAN("Boolean"),
    STRING("String");
    
    String name;
    
    ValueType(String typeName) {
      name = typeName;
    }
  
    @Override
    public String toString() {
      return name;
    }
  }
  
  public static final long DEFAULT_INT = 0L;
  public static final double DEFAULT_FLOAT = 0d;
  public static final boolean DEFAULT_BOOLEAN = false;
  public static final String DEFAULT_STRING = "";
  
  /* private int type; private Object val; */
  private static final Value empty = new EmptyValue();
  
  public Value() {
  }
  
  /** Returns an empty value */
  public static Value make() {
    return empty;
  }
  
  /** Returns a new integer value */
  public static Value make(long val) {
    return new IntValue(val);
  }
  
  /** Returns a new floating point value */
  public static Value make(double val) {
    return new FloatValue(val);
  }
  
  /** Returns a new boolean value */
  public static Value make(boolean val) {
    return new BooleanValue(val);
  }
  
  /** Returns a new string value */
  public static Value make(String val) {
    return new StringValue(val);
  }
  
  /* public Value() { set(); }
   * 
   * public Value(int value) { set(value); }
   * 
   * public Value(double value) { set(value); }
   * 
   * public Value(boolean value) { set(value); }
   * 
   * public Value(String value) { set(value); }
   * 
   * public Value(Value value) { set(value); }
   * 
   * 
   * public void set() { type = TYPE_NONE; val = null; }
   * 
   * public void set(int value) { type = TYPE_INT; val = new Integer(value); }
   * 
   * public void set(double value) { if (!Double.isNaN(value)) { type =
   * TYPE_FLOAT; val = new Double(value); } else { set(); } }
   * 
   * public void set(boolean value) { type = TYPE_BOOLEAN; val =
   * Boolean.valueOf(value); }
   * 
   * public void set(String value) { if (value != null) { type = TYPE_STRING;
   * val = value; } else { set(); } }
   * 
   * public void set(Value value) { if (value != null) { type = value.type; val
   * = value.val; } else { set(); } } */
  
  /**
   * Returns the type of value (either TYPE_UNSET, TYPE_INT, TYPE_FLOAT,
   * TYPE_BOOLEAN, or TYPE_STRING)
   */
  public abstract ValueType type(); /* { return type; } */
  
  /** Returns true only if the value is unset */
  public final boolean isUnset() {
    return (type() == ValueType.UNSET);
  }
  
  public abstract long toInt(); /* { switch (type) { case TYPE_INT: return
                                    * ((Integer)val).intValue();
                                    * 
                                    * case TYPE_FLOAT: return
                                    * toInt(floatValue());
                                    * 
                                    * case TYPE_BOOLEAN: return
                                    * toInt(booleanValue());
                                    * 
                                    * case TYPE_STRING: return
                                    * toInt(stringValue());
                                    * 
                                    * default: //case TYPE_NONE: return 0; } } */
  
  public final long toInt(double x) {
    return (int)x;
  }
  
  public final long toInt(boolean x) {
    return (x ? 1 : 0);
  }
  
  public final long toInt(String x) {
    try {
      // supports Java decimal, hex, and octal integers
      return Long.decode(x);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
  
  public abstract double toFloat(); /* { switch (type) { case TYPE_INT:
                                     * return toFloat(intValue());
                                     * 
                                     * case TYPE_FLOAT: return
                                     * ((Double)val).doubleValue();
                                     * 
                                     * case TYPE_BOOLEAN: return
                                     * toFloat(booleanValue());
                                     * 
                                     * case TYPE_STRING: return
                                     * toFloat(stringValue());
                                     * 
                                     * default: //case TYPE_NONE: return
                                     * Double.NaN; } } */
  
  public final double toFloat(long x) {
    return (double)x;
  }
  
  public final double toFloat(boolean x) {
    return (x ? 1.0 : 0.0);
  }
  
  public final double toFloat(String x) {
    try {
      /* supports all kinds of weird interpretations of a Double in String
       * format */
      return Double.valueOf(x);
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }
  
  public abstract boolean toBoolean(); /* { switch (type) { case TYPE_INT:
                                        * return toBoolean(intValue());
                                        * 
                                        * case TYPE_FLOAT: return
                                        * toBoolean(floatValue());
                                        * 
                                        * case TYPE_BOOLEAN: return
                                        * ((Boolean)val).booleanValue();
                                        * 
                                        * case TYPE_STRING: return
                                        * toBoolean(stringValue());
                                        * 
                                        * default: //case TYPE_NONE: return
                                        * false; } } */
  
  public final boolean toBoolean(long x) {
    return (x != 0L);
  }
  
  public final boolean toBoolean(double x) {
    return (x != 0.0);
  }
  
  public final boolean toBoolean(String x) {
    return (x.equals("true") || x.equals("t") || x.equals("yes")
            || x.equals("y") || x.equals("on") || x.equals("1"));
  }
  
  @Override
  public abstract String toString(); /* { switch (type) { case TYPE_INT:
                                      * return toString(intValue());
                                      * 
                                      * case TYPE_FLOAT: return
                                      * toString(floatValue());
                                      * 
                                      * case TYPE_BOOLEAN: return
                                      * toString(booleanValue());
                                      * 
                                      * case TYPE_STRING: return (String)val;
                                      * 
                                      * default: //case TYPE_NONE: return "";
                                      * } } */
  
  public final String toString(long x) {
    return Long.toString(x);
  }
  
  public final String toString(double x) {
    return Double.toString(x);
  }
  
  public final String toString(boolean x) {
    return (x ? "true" : "false");
  }
  
  // ***************************************************************
  // ******************* BEGIN PRIVATE CLASSES *********************
  // ***************************************************************
  
  private static class EmptyValue extends Value {
    @Override
    public ValueType type() {
      return ValueType.UNSET;
    }
    
    @Override
    public long toInt() {
      return DEFAULT_INT;
    }
    
    @Override
    public double toFloat() {
      return DEFAULT_FLOAT;
    }
    
    @Override
    public boolean toBoolean() {
      return DEFAULT_BOOLEAN;
    }
    
    @Override
    public String toString() {
      return DEFAULT_STRING;
    }
  }
  
  private static class IntValue extends Value {
    private long val;
    
    public IntValue(long value) {
      val = value;
    }
    
    @Override
    public ValueType type() {
      return ValueType.INT;
    }
    
    @Override
    public long toInt() {
      return val;
    }
    
    @Override
    public double toFloat() {
      return toFloat(val);
    }
    
    @Override
    public boolean toBoolean() {
      return toBoolean(val);
    }
    
    @Override
    public String toString() {
      return toString(val);
    }
  }
  
  private static class FloatValue extends Value {
    private double val;
    
    public FloatValue(double value) {
      val = value;
    }
    
    @Override
    public ValueType type() {
      return ValueType.FLOAT;
    }
    
    @Override
    public long toInt() {
      return toInt(val);
    }
    
    @Override
    public double toFloat() {
      return val;
    }
    
    @Override
    public boolean toBoolean() {
      return toBoolean(val);
    }
    
    @Override
    public String toString() {
      return toString(val);
    }
  }
  
  private static class BooleanValue extends Value {
    private boolean val;
    
    public BooleanValue(boolean value) {
      val = value;
    }
    
    @Override
    public ValueType type() {
      return ValueType.BOOLEAN;
    }
    
    @Override
    public long toInt() {
      return toInt(val);
    }
    
    @Override
    public double toFloat() {
      return toFloat(val);
    }
    
    @Override
    public boolean toBoolean() {
      return val;
    }
    
    @Override
    public String toString() {
      return toString(val);
    }
  }
  
  private static class StringValue extends Value {
    private String val;
    
    public StringValue(String value) {
      val = (value == null ? "" : value);
    }
    
    @Override
    public ValueType type() {
      return ValueType.STRING;
    }
    
    @Override
    public long toInt() {
      return toInt(val);
    }
    
    @Override
    public double toFloat() {
      return toFloat(val);
    }
    
    @Override
    public boolean toBoolean() {
      return toBoolean(val);
    }
    
    @Override
    public String toString() {
      return val;
    }
  } // end private class
}