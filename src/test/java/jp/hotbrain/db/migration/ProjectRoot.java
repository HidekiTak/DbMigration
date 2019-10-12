package jp.hotbrain.db.migration;

import java.io.*;
import java.nio.file.Path;

public class ProjectRoot {

    public static Path of(String path) {
        return instance.resolve(path);
    }

    public static Path of() {
        return instance;
    }

    private static Path instance = getRoot(".").toPath();

    public static File getRoot(String start) {
        FileFilter fileFilter = pathname -> pathname.isFile() && pathname.getName().equals("build.sbt");

        File current = new File(start).getAbsoluteFile();
        File result = current;
        while (null != result && result.isDirectory()) {
            File[] files = result.listFiles(fileFilter);
            if (null != files && 0 < files.length) {
                return result;
            }
            result = result.getParentFile();
        }
        return current;
    }
}
