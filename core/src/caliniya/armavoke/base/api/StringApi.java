package caliniya.armavoke.base.api;

public class StringApi extends arc.util.Strings {
  public static String repeat(String str, int count) {
    if (count <= 0) return "";
    StringBuilder sb = new StringBuilder(str.length() * count);
    for (int i = 0; i < count; i++) sb.append(str);
    return sb.toString();
  }
}
