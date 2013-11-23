
package co.natsuhi.flprovider;

import java.io.FileNotFoundException;
import java.util.List;

import net.yanzm.flashairdev.FlashAirFileInfo;
import net.yanzm.flashairdev.FlashAirUtils;
import android.R.integer;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;
import co.natsuhi.flprovider.util.LogUtil;

public class FlashAirProvider extends DocumentsProvider {
    private static final String TAG = "FlashAirProvider";
    private final FlashAirProvider self = this;

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
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        LogUtil.d(TAG, "queryDocument");
        LogUtil.d(TAG, "documentId is " + documentId);
        MatrixCursor cursor = new MatrixCursor(resolveDocumentProjection(projection));
        includeFile(cursor, documentId);
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

    private void includeFile(MatrixCursor cursor, String documentId) {
        LogUtil.d(TAG, "includeFile");
        List<FlashAirFileInfo> fileInfos = FlashAirUtils.getFileList(documentId);
        if (fileInfos == null) {
            return;
        }
        LogUtil.d(TAG, "List size is " + fileInfos.size());
        for (FlashAirFileInfo flashAirFileInfo : fileInfos) {
            LogUtil.d(TAG, "display name = " + flashAirFileInfo.mFileName);
            RowBuilder row = cursor.newRow();
            row.add(Document.COLUMN_DISPLAY_NAME, flashAirFileInfo.mFileName);
            row.add(Document.COLUMN_DOCUMENT_ID, documentId + "/" + flashAirFileInfo.mFileName);
            row.add(Document.COLUMN_SIZE, flashAirFileInfo.mSize);
            int flags = 0;
            if (flashAirFileInfo.isDirectory()) {
                flags = flags | Document.FLAG_DIR_PREFERS_LAST_MODIFIED;
            }
            row.add(Document.COLUMN_FLAGS, flags);
            row.add(Document.COLUMN_LAST_MODIFIED, null);
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        }
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
        return null;
    }
}
