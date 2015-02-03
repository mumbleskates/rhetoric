package widders.rhetoric;

public class DebugRoom extends Room {
  public DebugRoom() {
    super(new BasicName("debugroom", true),
          "All is dim, with sparkles and secrets "
              + "of the universe all around.",
          Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
          Double.POSITIVE_INFINITY);
  }
}