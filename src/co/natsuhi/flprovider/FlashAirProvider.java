
package co.natsuhi.flprovider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;

import net.yanzm.flashairdev.FlashAirFileInfo;
import net.yanzm.flashairdev.FlashAirUtils;
import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Point;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;
import co.natsuhi.flprovider.util.LogUtil;

public class FlashAirProvider extends DocumentsProvider {
    private static final String TAG = "FlashAirProvider";
    @SuppressWarnings("unused")
    private final FlashAirProvider self = this;
    private static final String ROOT_DOCUMENT_ID = "flashairroot";
    private final int BUFFER_SIZE = 1024;
    private static final String MIMETYPE_UNKNOWN = "unknown/unknown";

    private byte[] buffer = new byte[BUFFER_SIZE];

    @Override
    public boolean onCreate() {
        LogUtil.d(TAG, "onCreate");
        // TODO:FlashAirへの接続準備
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        LogUtil.d(TAG, "queryRoots");
        final MatrixCursor cursor = new MatrixCursor(resolveRootProjection(projection));

        final MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(Root.COLUMN_ROOT_ID, FlashAirProvider.class.getName() + ".flashair");
        row.add(Root.COLUMN_TITLE, "FlashAir");
        row.add(Root.COLUMN_DOCUMENT_ID, ROOT_DOCUMENT_ID);
        row.add(Root.COLUMN_MIME_TYPES, "*/*");
        row.add(Root.COLUMN_AVAILABLE_BYTES, Integer.MAX_VALUE);
        row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);

        return cursor;
    }

    private String[] resolveRootProjection(String[] projection) {
        LogUtil.d(TAG, "resolveRootProjection");
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
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        LogUtil.d(TAG, "queryDocument");
        LogUtil.d(TAG, "documentId is " + documentId);
        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));
        includeRootFile(cursor, documentId);
        return cursor;
    }

    private String[] resolveDocumentProjection(String[] projection) {
        LogUtil.d(TAG, "resoveDocumentProjection");
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

    /**
     * RootDir用
     * 
     * @param cursor
     * @param documentId
     */
    private void includeRootFile(MatrixCursor cursor, String documentId) {
        LogUtil.d(TAG, "includeRootFile");
        RowBuilder row = cursor.newRow();
        row.add(Document.COLUMN_DISPLAY_NAME, "/");
        row.add(Document.COLUMN_DOCUMENT_ID, "/");
        row.add(Document.COLUMN_SIZE, null);
        row.add(Document.COLUMN_FLAGS, Document.FLAG_DIR_PREFERS_LAST_MODIFIED);
        row.add(Document.COLUMN_LAST_MODIFIED, null);
        row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
    }

    /**
     * 指定されたディレクトリの中をCursorに詰めていく
     * 
     * @param cursor
     * @param documentId
     */
    private void includeFile(MatrixCursor cursor, String documentId) {
        LogUtil.d(TAG, "includeFile");
        // Cursorに値を入れていく
        if (documentId.equals("/")) {
            documentId = "";
        }
        List<FlashAirFileInfo> fileInfos = FlashAirUtils.getFileList("/" + documentId);
        if (fileInfos == null) {
            return;
        }
        LogUtil.d(TAG, "List size is " + fileInfos.size());
        for (FlashAirFileInfo flashAirFileInfo : fileInfos) {
            LogUtil.d(TAG, "display name = " + flashAirFileInfo.mFileName);
            RowBuilder row = cursor.newRow();
            row.add(Document.COLUMN_DISPLAY_NAME, flashAirFileInfo.mFileName);
            if (documentId.equals("")) {
                row.add(Document.COLUMN_DOCUMENT_ID, "/" + flashAirFileInfo.mFileName);
            } else {
                row.add(Document.COLUMN_DOCUMENT_ID, "/" + documentId + "/"
                        + flashAirFileInfo.mFileName);
            }
            row.add(Document.COLUMN_SIZE, flashAirFileInfo.mSize);
            row.add(Document.COLUMN_LAST_MODIFIED, null);
            String mimeType = flashAirFileInfo.isDirectory() ? Document.MIME_TYPE_DIR
                    : getMimeType(flashAirFileInfo.mFileName);
            LogUtil.d(TAG, "mimetype " + mimeType);
            row.add(Document.COLUMN_MIME_TYPE, mimeType);
            int flags = 0;
            if (flashAirFileInfo.isDirectory()) {
                flags = flags | Document.FLAG_DIR_PREFERS_LAST_MODIFIED;
            }
            if (mimeType != null && mimeType.equals("image/jpeg")) {
                flags = flags | Document.FLAG_SUPPORTS_THUMBNAIL;
            }
            row.add(Document.COLUMN_FLAGS, flags);
        }
    }

    @SuppressLint("DefaultLocale")
    private String getMimeType(String filePath) {
        filePath = filePath.toLowerCase(Locale.getDefault());
        String mimeType = MIMETYPE_UNKNOWN;
        String extension = null;
        int index = filePath.lastIndexOf(".");
        if (index > 0) {
            extension = filePath.substring(index + 1);
        }
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        if (extension != null) {
            mimeType = mime.getMimeTypeFromExtension(extension);
        }
        return mimeType;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        LogUtil.d(TAG, "queryChildDocuments");
        LogUtil.d(TAG, "documentId is " + parentDocumentId);
        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(cursor, parentDocumentId);
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode,
            CancellationSignal signal)
            throws FileNotFoundException {
        LogUtil.d(TAG, "openDocument");
        // ファイルをダウンロードして渡す
        // File file = new File("http://flashair" + documentId);
        File file = new File(downloadFile(FlashAirUtils.BASE + documentId));

        int accessMode = ParcelFileDescriptor.MODE_READ_ONLY;
        return ParcelFileDescriptor.open(file, accessMode);
    }

    /**
     * 指定したURLからファイルをダウンロードする。ファイル名は時間
     * 
     * @param urlString
     * @return
     */
    private String downloadFile(String urlString) {
        // TODO:別クラスにする
        // TODO:キャッシュの仕組みを作る
        File cacheDir = getContext().getCacheDir();
        File outputFile = new File(cacheDir, String.valueOf(System.currentTimeMillis()));
        URL url;
        URLConnection urlConnection;
        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            url = new URL(urlString);
            urlConnection = url.openConnection();
            inputStream = urlConnection.getInputStream();
            bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
            fileOutputStream = new FileOutputStream(outputFile);

            int len;
            while ((len = bufferedInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outputFile.getPath();
    }

    @Override
    public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint,
            CancellationSignal signal) throws FileNotFoundException {
        LogUtil.d(TAG, "openDocumentThumbnail");
        // サムネをダウンロードして渡す
        File file = new File(downloadFile(FlashAirUtils.THUMBNAIL + "/" + documentId));

        int accessMode = ParcelFileDescriptor.MODE_READ_ONLY;
        ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, accessMode);
        int startOffset = 0;
        int length = 0;
        AssetFileDescriptor assetFileDescriptor = new AssetFileDescriptor(fd, startOffset, length);
        return assetFileDescriptor;
    }
}
