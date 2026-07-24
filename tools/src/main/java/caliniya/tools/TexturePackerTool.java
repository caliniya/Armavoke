package caliniya.tools;

import arc.packer.TexturePacker;
import arc.packer.TexturePacker.Settings;

import java.io.File;

public class TexturePackerTool {
  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println(
          "TexturePackerTool work need: <inputDir> <outputDir> <packName> [settingsFile]");
      System.exit(1);
    }

    File inputDir = new File(args[0]);
    File outputDir = new File(args[1]);
    String packName = args[2];

    if (!inputDir.exists() || !inputDir.isDirectory()) {
      System.err.println("Error: Input directory invalid.");
      System.exit(1);
    }

    // 创建 Arc 的设置对象
    Settings settings = new Settings();

    settings.silent = true;

    settings.maxWidth = 2048;
    settings.maxHeight = 2048;
    
    settings.combineSubdirectories = true;

    settings.pot = false;

    settings.paddingX = 2;
    settings.paddingY = 2;
    settings.edgePadding = false;

    settings.alias = true;
    settings.flattenPaths = true;

    settings.stripWhitespaceX = false;
    settings.stripWhitespaceY = false;

    if (!outputDir.exists() && !outputDir.mkdirs()) {
      System.err.println("Failed to create output directory");
      System.exit(1);
    }

    try {
      long startTime = System.currentTimeMillis();
      System.out.println("Start Packing textures");

      // 清理旧产物
      cleanPreviousOutput(outputDir, packName);

      // 执行打包
      TexturePacker.process(
          settings, inputDir.getAbsolutePath(), outputDir.getAbsolutePath(), packName);

      float duration = (System.currentTimeMillis() - startTime) / 1000f;
      System.out.println(
          String.format(
              "Done in %.2fs. Output: %s/%s.aatls", duration, outputDir.getName(), packName));

      // 仅在发现 .9 图时提示
      checkNinePatchFiles(inputDir);

    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace(); // 只有出错时才打印详细堆栈
      System.exit(1);
    }
  }

  private static void checkNinePatchFiles(File inputDir) {
    int ninePatchCount = countNinePatchFiles(inputDir);
    if (ninePatchCount > 0) {
      System.out.println("Note: Found " + ninePatchCount + " NinePatch (.9.png) files.");
    }
  }

  private static int countNinePatchFiles(File dir) {
    int count = 0;
    if (dir.exists() && dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            count += countNinePatchFiles(file);
          } else if (file.getName().endsWith(".9.png")) {
            count++;
          }
        }
      }
    }
    return count;
  }

  private static void cleanPreviousOutput(File outputDir, String packName) {
    if (!outputDir.exists()) return;
    deleteFile(new File(outputDir, packName + ".png"));
    deleteFile(new File(outputDir, packName + ".aatls"));
  }

  private static void deleteFile(File file) {
    if (file.exists()) {
      file.delete();
    }
  }
}
