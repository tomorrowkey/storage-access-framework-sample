
package com.example.mycloud;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class IOUtil {

    public static void write(String path, String text) throws UnsupportedEncodingException,
            FileNotFoundException, IOException {

        BufferedOutputStream outputStream = null;
        try {
            File file = new File(path);
            file.getParentFile().mkdirs();
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            outputStream.write(text.getBytes("utf-8"));
            outputStream.flush();
        } finally {
            IOUtil.forceClose(outputStream);
        }
    }

    public static void forceClose(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public static List<File> find(String filePath, String searchQuery) {
        List<File> searchResultList = new ArrayList<File>();
        for (File file : new File(filePath).listFiles()) {
            if (file.isDirectory()) {
                searchResultList.addAll(find(file.getPath(), searchQuery));
            } else {
                if (file.getName().indexOf(searchQuery) != -1) {
                    searchResultList.add(file);
                }
            }
        }

        return searchResultList;
    }
}
