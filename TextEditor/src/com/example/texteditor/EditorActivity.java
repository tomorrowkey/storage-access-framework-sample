
package com.example.texteditor;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class EditorActivity extends Activity {

    private static final int CREATE_DOCUMENT_REQUEST = 1;

    private Uri mUri;

    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mUri = intent.getData();

        initTitle(mUri);
        initEditText(mUri);
        initSaveButton();
    }

    private void initEditText(Uri uri) {
        mEditText = (EditText) findViewById(R.id.edit_text);

        if (uri == null)
            return;

        InputStream inputStream = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            inputStream = getContentResolver().openInputStream(mUri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }

            mEditText.setText(stringBuilder.toString());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtil.forceClose(inputStream);
        }

    }

    private void initTitle(Uri uri) {
        if (uri == null) {
            setTitle("Untitled");
            return;
        } else {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);

            try {
                cursor.moveToFirst();

                String[] columnNames = cursor.getColumnNames();
                for (String columnName : columnNames) {
                    Log.d("EditorActivity", columnName);
                }

                String displayName = cursor.getString(cursor
                        .getColumnIndex(OpenableColumns.DISPLAY_NAME));
                setTitle(displayName);
            } finally {
                IOUtil.forceClose(cursor);
            }
        }

    }

    private void initSaveButton() {
        Button button = (Button) findViewById(R.id.save_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mUri == null) {
                    createNewFile();
                } else {
                    try {
                        String text = mEditText.getText().toString();
                        save(mUri, text);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void createNewFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "Untitled.txt");
        startActivityForResult(intent, CREATE_DOCUMENT_REQUEST);
    }

    /**
     * 指定されたUriのファイルにテキストを追記します<br>
     * TODO 挙動は追記で問題ない？
     * 
     * @param uri 書き込みファイルのUri
     * @param text 書き込む内容
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void save(Uri uri, String text) throws FileNotFoundException, IOException {
        OutputStream outputStream = null;
        try {
            outputStream = getContentResolver().openOutputStream(uri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            writer.write(text);
            writer.flush();
        } finally {
            IOUtil.forceClose(outputStream);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CREATE_DOCUMENT_REQUEST) {
            if (resultCode != RESULT_OK)
                return;

            try {
                String text = mEditText.getText().toString();
                mUri = data.getData();
                initTitle(mUri);
                save(mUri, text);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                boolean result = delete(mUri);

                if (result) {
                    Toast.makeText(getApplicationContext(), "The file has been deleted",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), "Failed to delete the file",
                            Toast.LENGTH_SHORT).show();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean delete(Uri uri) {
        boolean result = DocumentsContract.deleteDocument(getContentResolver(), uri);
        return result;
    }

}
