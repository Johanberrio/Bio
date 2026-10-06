package com.alvia.health;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
/** Test APK only: two synthetic PDFs, no user files or clinical data. */
public class FixtureProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    public String getType(Uri uri){return "application/pdf";}
    private String asset(Uri uri){return "history".equals(uri.getLastPathSegment())?"synthetic-history.pdf":"synthetic-exam.pdf";}
    public Cursor query(Uri uri,String[] p,String selection,String[] args,String order){MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{"scan_0001.pdf",1024});return c;}
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
        if(!"r".equals(mode))throw new FileNotFoundException();File f=new File(getContext().getCacheDir(),asset(uri));
        try(InputStream in=getContext().getAssets().open(asset(uri));OutputStream out=new FileOutputStream(f)){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}catch(IOException e){throw new FileNotFoundException();}
        return ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
    }
    public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
