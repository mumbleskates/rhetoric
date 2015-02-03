package widders.rhetoric;

/**
 * Provides unit standards for length, size (volume), and weight. Length and
 * volume are geometrically compatible, since the standards are m and m^3. (The
 * standard for weight is also kg.)
 * 
 * @author widders
 */
public abstract class Units {
  private static final int BITS_ACCURACY = 42;
  private static final double MAX_ERROR = Math.scalb(1.d, -BITS_ACCURACY);
  
  public static final double
      // length (1.0 = m)
      METER = 1.d,
      CM = .01d * METER,
      MM = .001d * METER,
      KM = 1000.d * METER,
      INCH = .0254d * METER,
      FT = .3048d * METER,
      YARD = .9144d * METER,
      MILE = 5280.d * FT,
      FURLONG = MILE / 8.d,
      MILE_NAUTICAL = 1852.d * METER,
      
      // weight (1.0 = kg)
      KG = 1.d,
      GRAM = .001d,
      METRIC_TON = 1000.d,
      LB = .45359d,
      WT_OZ = LB / 16.d, TON = LB * 2000.d,
      
      // time (1.0 = ms)
      S = 1.d,
      MS = .001d * S, //milliseconds
      MINUTE = 60.d * S,
      HOUR = 60.d * MINUTE,
      DAY = 24.d * HOUR,
      WEEK = 7.d * DAY,
      FORTNIGHT = 14.d * DAY,
      YEAR = 525949.d * MINUTE,
      YEAR_JULIAN = 525960.d * MINUTE,
      
      // area (length^2)
      SQ_METER = METER * METER,
      SQ_INCH = INCH * INCH,
      SQ_FT = FT * FT,
      SQ_YARD = YARD * YARD,
      SQ_MILE = MILE * MILE,
      SQ_CM = CM * CM,
      SQ_KM = KM * KM,
      HECTARE = 10000.d * SQ_METER,
      ACRE = 43560.d * SQ_FT,
      FOOTBALL_FIELD = 6400.d * YARD * YARD,
      
      // volume (length^3)
      CU_METER = METER * METER * METER,
      LITER = .001d * CU_METER,
      CU_CM = 1.0e-6d * CU_METER,
      CU_MM = 1.0e-9d * CU_METER,
      CU_KM = 1.0e9d * CU_METER,
      CU_FT = FT * FT * FT,
      CU_INCH = INCH * INCH * INCH,
      CU_YARD = YARD * YARD * YARD,
      CU_MILE = MILE * MILE * MILE,
      VOL_OZ = 2.95735295625e-5d * CU_METER,
      GALLON = 128.d * VOL_OZ, // ~= 231 * CU_INCH
      QUART = 32.d * VOL_OZ,
      PINT = 16.d * VOL_OZ,
      CUP = 8.d * VOL_OZ,
      TBSP = VOL_OZ / 2.d,
      TSP = VOL_OZ / 6.d,

      // velocity (length / time)
      C = 299792458.d * METER / S,
      MPH = MILE / HOUR,
          
      // acceleration (length/time^2)
      GRAVITY = 9.81d * METER / S / S,
      
      // force (N = mass*acceleration)
      NEWTON = 1.d * KG * METER / S / S,
      F_KG = KG * GRAVITY,
      F_GRAM = GRAM * GRAVITY,
      F_METRIC_TON = METRIC_TON * GRAVITY,
      F_LB = LB * GRAVITY,
      F_OZ = WT_OZ * GRAVITY,
      F_TON = TON * GRAVITY,
      
      // pressure (force / area)
      PASCAL = NEWTON / SQ_METER,
      BAR = 100000.d * PASCAL,
      KPA = 1000.d * PASCAL,
      MPA = 1.0e6d * PASCAL,
      PSI = F_LB / SQ_INCH,
      TORR = 101325.d / 760.d * PASCAL,
      MM_HG = 133.322387415d * PASCAL,
      ATMOSPHERE_TECHNICAL = 101325.d * PASCAL,
      ATMOSPHERE_STANDARD = 98066.5d * PASCAL,
      METER_SEAWATER = 10133.d * PASCAL,
      
      // density (mass/volume)
      DENSITY_WATER = 1.d * KG / LITER,
      DENSITY_SEAWATER = 1.027d * KG / LITER,
      DENSITY_MERCURY = 13.5951d * KG / LITER,
      DENSITY_AIR = 1.2922d * KG / CU_METER,
      
      // angle (radians)
      RADIAN = 1.d,
      ARC_DEGREE = Math.PI / 180.d,
      ARC_MINUTE = Math.PI / 10800.d,
      ARC_SECOND = Math.PI / 648000.d,
      
      // astronomical
      PARSEC = 3.0856774879e16d * METER, // length
      LY = C * YEAR_JULIAN, // length
      AU = 149597870700.d * METER, // length
      SOLAR_MASS = 1.98855e30d * KG, // mass
      EARTH_MASS = 5.97219e24d * KG, // mass
      
      // microscopic
      ANGSTROM = 1.e-10d * METER, // length
      PROTON_MASS = 1.672621777e-24d * KG, // mass
      ELECTRON_MASS = 9.10938291e-31d * KG, // mass
      
      // the most smallest
      PLANCK_LEN = 1.61619997e-35 * METER,
      PLANCK_TIME = PLANCK_LEN / C
      ;
  
  /**
   * Rounds value to the nearest multiple of increment. For example, a distance
   * can be rounded to the nearest tenth of an inch with:
   * roundTo(value, INCH / 10)
   */
  public static double roundTo(double value, double increment) {
    /* if value / increment is larger than the 1.0 accuracy size of a double,
     * just return the original value because it doesn't matter */
    double absVal = Math.abs(value);
    return (absVal - Math.nextAfter(absVal, Double.NEGATIVE_INFINITY)
                                                        > Math.abs(increment))
        ? value
        : Math.round(value / increment) * increment;
  }
  
  /**
   * Returns true if x and y are close enough to be basically equal, on a scale
   * defined by the scale parameter. 
   */
  public static boolean closeEnough(double x, double y, double scale) {
    return Math.abs(x - y) < scale * MAX_ERROR;
  }
  
  /**
   * Returns true if x and y are close enough to be basically equal based on
   * their own scale. 
   */
  public static boolean closeEnough(double x, double y) {
    return closeEnough(x, y, x);
  }
}