package edu.isi.kcap.diskproject.shared.classes.util;

public class GUID {
  private static final char[] CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

  public static String get() {
    return get(12);
  }

  /**
   * Generate a random uuid of the specified length. Example: uuid(15) returns
   * "VcydxgltxrVZSTV"
   *
   * @param len
   *            the desired number of characters
   */
  public static String get(int len) {
    return get(len, CHARS.length);
  }

  /**
   * Generate a random uuid of the specified length, and radix. Examples:
   * <ul>
   * <li>uuid(8, 2) returns "01001010" (8 character ID, base=2)
   * <li>uuid(8, 10) returns "47473046" (8 character ID, base=10)
   * <li>uuid(8, 16) returns "098F4D35" (8 character ID, base=16)
   * </ul>
   *
   * @param len
   *              the desired number of characters
   * @param radix
   *              the number of allowable values for each character (must be less
   *              that 62)
   */
  public static String get(int len, int radix) {
    if (radix > CHARS.length) {
      throw new IllegalArgumentException();
    }
    char[] uuid = new char[len];
    // Compact form
    for (int i = 0; i < len; i++) {
      uuid[i] = CHARS[(int) (Math.random() * radix)];
    }
    return new String(uuid);
  }

  public static String randomId(String prefix) {
    return prefix + "-" + GUID.get(12);
  }
}