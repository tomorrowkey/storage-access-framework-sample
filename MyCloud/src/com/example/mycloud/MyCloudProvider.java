
package com.example.mycloud;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class MyCloudProvider extends DocumentsProvider {

    public static final String LOG_TAG = MyCloudProvider.class.getSimpleName();

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        final MatrixCursor cursor = new MatrixCursor(resolveRootProjection(projection));

        final MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Root.COLUMN_ROOT_ID, MyCloudProvider.class.getName() + ".tomorrowkey");
        row.add(Root.COLUMN_SUMMARY, "tomorrowkey");
        row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE | Root.FLAG_SUPPORTS_SEARCH);
        row.add(Root.COLUMN_TITLE, "MyCloud");
        row.add(Root.COLUMN_DOCUMENT_ID, "/");
        row.add(Root.COLUMN_MIME_TYPES, "*/*");
        row.add(Root.COLUMN_AVAILABLE_BYTES, Integer.MAX_VALUE);
        row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);

        return cursor;
    }

    private String[] resolveRootProjection(String[] projection) {

        if (projection == null || projection.length == 0) {
            return new String[] {
                    Root.COLUMN_ROOT_ID,
                    Root.COLUMN_MIME_TYPES,
                    Root.COLUMN_FLAGS,
                    Root.COLUMN_ICON,
                    Root.COLUMN_TITLE,
                    Root.COLUMN_SUMMARY,
                    Root.COLUMN_DOCUMENT_ID,
                    Root.COLUMN_AVAILABLE_BYTES,
            };
        } else {
            return projection;
        }
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));

        String parentDocumentPath = getContext().getFilesDir().getPath() + "/" + parentDocumentId;
        File dir = new File(parentDocumentPath);
        for (File file : dir.listFiles()) {
            String documentId = parentDocumentId + "/" + file.getName();
            includeFile(cursor, documentId);
        }

        return cursor;
    }

    private String[] resolveDocumentProjection(String[] projection) {
        if (projection == null || projection.length == 0) {
            return new String[] {
                    Document.COLUMN_DOCUMENT_ID,
                    Document.COLUMN_MIME_TYPE,
                    Document.COLUMN_DISPLAY_NAME,
                    Document.COLUMN_LAST_MODIFIED,
                    Document.COLUMN_FLAGS,
                    Document.COLUMN_SIZE,
            };
        } else {
            return projection;
        }
    }

    @Override
    public ParcelFileDescriptor openDocument(final String documentId, String mode,
            CancellationSignal signal) throws FileNotFoundException {
        final File file = new File(getContext().getFilesDir().getPath() + "/" + documentId);

        boolean isWrite = (mode.indexOf('w') != -1);
        if (isWrite) {
            int accessMode = ParcelFileDescriptor.MODE_READ_WRITE;
            try {
                Handler handler = new Handler(getContext().getMainLooper());
                return ParcelFileDescriptor.open(file, accessMode,
                        handler, new ParcelFileDescriptor.OnCloseListener() {
                            @Override
                            public void onClose(IOException e) {
                                Log.i(LOG_TAG, "A file with id " + documentId
                                        + " has been closed! Time to " + "update the server.");
                            }

                        });
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to open document with id "
                        + documentId + " and mode " + mode);
            }
        } else {
            int accessMode = ParcelFileDescriptor.MODE_READ_ONLY;
            return ParcelFileDescriptor.open(file, accessMode);
        }
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(cursor, documentId);

        return cursor;
    }

    @Override
    public String createDocument(String parentDocumentId, String mimeType, String displayName)
            throws FileNotFoundException {
        String filePath = getContext().getFilesDir().getPath() + "/" + parentDocumentId + "/"
                + displayName;
        try {
            boolean result = new File(filePath).createNewFile();
            if (!result)
                throw new RuntimeException("Failed to make new file");

            return parentDocumentId + "/" + displayName;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        String filePath = getContext().getFilesDir().getPath() + "/" + documentId;
        File file = new File(filePath);
        boolean result = file.delete();
        if (!result)
            throw new RuntimeException("Failed to delete the file, file path=" + filePath);
    }

    @Override
    public Cursor querySearchDocuments(String rootId, String query, String[] projection)
            throws FileNotFoundException {
        String rootDocumentPath = getContext().getFilesDir().getPath() + "/";

        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));
        List<File> searchResultList = IOUtil.find(rootDocumentPath, query);
        for (File file : searchResultList) {
            String documentId = file.getPath().replace(rootDocumentPath, "");
            includeFile(cursor, documentId);
        }

        return cursor;
    }

    private void includeFile(MatrixCursor cursor, String documentId) {
        String filePath = getContext().getFilesDir().getPath() + "/" + documentId;
        File file = new File(filePath);

        RowBuilder row = cursor.newRow();
        row.add(Document.COLUMN_DOCUMENT_ID, documentId);
        if (file.isDirectory()) {
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
            row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_SUPPORTS_CREATE);
            row.add(Document.COLUMN_SIZE, 0);
        } else if (file.getName().endsWith(".txt")) {
            row.add(Document.COLUMN_MIME_TYPE, "text/plain");
            row.add(Document.COLUMN_FLAGS, Document.FLAG_SUPPORTS_DELETE
                    | Document.FLAG_SUPPORTS_WRITE);
            row.add(Document.COLUMN_SIZE, file.length());
        } else {
            throw new RuntimeException("Unknown file type, file name=" + file.getName());
        }
        row.add(Document.COLUMN_DISPLAY_NAME, file.getName());
        row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
    }

}
