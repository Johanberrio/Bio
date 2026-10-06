package com.alvia.health;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import org.json.JSONObject;
import java.io.*;

/** Grants seekable, read-only access to one document, without a plaintext disk copy. */
public class DocumentProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private JSONObject document(Uri uri) throws Exception {
        for(JSONObject doc:new Vault(getContext()).list("documents")) if(doc.optString("id").equals(uri.getLastPathSegment())) return doc;
        throw new FileNotFoundException();
    }
    public String getType(Uri uri){try{return document(uri).optString("mime");}catch(Exception e){return "application/octet-stream";}}
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        try { JSONObject doc=document(uri); String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection; MatrixCursor cursor=new MatrixCursor(cols); Object[] row=new Object[cols.length]; for(int i=0;i<cols.length;i++) row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?doc.optString("name"):OpenableColumns.SIZE.equals(cols[i])?doc.optLong("size"):null; cursor.addRow(row);return cursor; }catch(Exception e){return null;}
    }
    public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode)) throw new FileNotFoundException("Solo lectura");
        try { JSONObject doc=document(uri); byte[] bytes=new Vault(getContext()).readDocument(doc.getString("id"));
            android.os.storage.StorageManager storage=getContext().getSystemService(android.os.storage.StorageManager.class);
            return storage.openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,new android.os.ProxyFileDescriptorCallback(){
                @Override public long onGetSize(){return bytes.length;}
                @Override public int onRead(long offset,int size,byte[] output){if(offset>=bytes.length)return 0;int n=(int)Math.min(size,bytes.length-offset);System.arraycopy(bytes,(int)offset,output,0,n);return n;}
                @Override public void onRelease(){java.util.Arrays.fill(bytes,(byte)0);}
            },new android.os.Handler(android.os.Looper.getMainLooper()));
        }catch(Exception e){throw new FileNotFoundException("No se pudo abrir el documento");}
    }
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
