public class LoadVersionUtilsTest {
  public static void main(String[] args) {
    try {
      Class.forName("plugily.projects.minigamesbox.classic.utils.version.VersionUtils");
      System.out.println("VersionUtils loaded");
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
