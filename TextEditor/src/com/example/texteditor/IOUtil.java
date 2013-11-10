
package com.example.texteditor;

import java.io.Closeable;
import java.io.IOException;

public class IOUtil {

    /**
     * Closeableインスタンスを強制的に閉じます<br>
     * 
     * @param closeable
     */
    public static void forceClose(Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (IOException e) {
            // ignore
        }
    }

}
