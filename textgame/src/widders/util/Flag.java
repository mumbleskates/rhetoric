package widders.util;

public abstract class Flag {
  public abstract void set();
  
  public abstract void await();
  
  public static void awaitAll(Flag[] flags) {
    for (int i = 0; i < flags.length; i++)
      flags[i].await();
  }
  
  public static void awaitAll(WithFlag[] flags) {
    for (int i = 0; i < flags.length; i++)
      flags[i].getFlag().await();
  }
}