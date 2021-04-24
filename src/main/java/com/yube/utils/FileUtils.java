package com.yube.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

public class FileUtils {

    public static String deleteExtension(String fileName) {
        int index = fileName.indexOf('.');
        return fileName.substring(0, index);
    }

    public static File getRandomFileFromDirectory(String directory) {
        String[] fileNames = getResourceFolderFileNames(directory);
        String filePath = FileUtils.class.getClassLoader()
                .getResource(directory + "/" + fileNames[(int) (fileNames.length * Math.random())]).getPath();
        return new File(filePath);
    }

    public static File getFileFromDirectoryByPartialName(String directory, String name) throws UnsupportedEncodingException {
        String[] fileNames = getResourceFolderFileNames(directory);
        for (String fileName : fileNames) {
            if (fileName.contains(name)) {
                return getFileFromDirectoryByName(directory, fileName);
            }
        }
        return null;
    }

    public static File getFileFromDirectoryByName(String directory, String name) throws UnsupportedEncodingException {
        String filePath = URLDecoder.decode(FileUtils.class.getClassLoader()
                .getResource(directory + "/" + name).getPath(), "UTF-8");
        return new File(filePath);
    }

    public static String[] getResourceFolderFileNames(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        return new File(url.getPath()).list();
    }
}
