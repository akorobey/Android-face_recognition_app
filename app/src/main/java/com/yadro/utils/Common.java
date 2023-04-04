package com.yadro.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Common {
    public static String getResourcePath(InputStream in, String name, String ext) {
        String path = "";
        try {
            Path plugins = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                plugins= Files.createTempFile(name, ext);
            }
            Files.copy(in, plugins, StandardCopyOption.REPLACE_EXISTING);
            path = plugins.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public static Path getDirectory(InputStream in, String dirName) {
        String path = "";
        Path plugins = null;
        try {
            plugins= Files.createTempDirectory(dirName);
            //Files.copy(in, plugins, StandardCopyOption.REPLACE_EXISTING);
//            path = plugins.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return plugins;
    }
}
