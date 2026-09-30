package com.unicornwhodev.visiondatasetstudio;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Test APK only. The target UID can write bounded QA paths through a file descriptor.
 * Java/platform APIs only: the target's Kotlin runtime is unavailable in this process. */
public final class QaEvidenceProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/octet-stream"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            int allowed=getContext().getPackageManager().getApplicationInfo("com.unicornwhodev.visiondatasetstudio",0).uid;
            String path=uri.getPath();
            if(Binder.getCallingUid()==2000 && mode.equals("r") && path!=null &&
                    path.matches("/final01-ui/[a-f0-9]{12}/(portrait|landscape)/archive.zip")) {
                File directory=new File(getContext().getFilesDir(),"qa-evidence"+path.substring(0,path.lastIndexOf('/')));
                if(!directory.isDirectory())throw new FileNotFoundException("QA attempt missing");
                ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createPipe();
                new Thread(() -> {
                    try(ZipOutputStream zip=new ZipOutputStream(new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]))) {
                        File[] files=directory.listFiles();
                        if(files==null)throw new IOException("QA files missing");
                        Arrays.sort(files,(left,right)->left.getName().compareTo(right.getName()));
                        byte[] buffer=new byte[65536];
                        for(File file:files) {
                            if(!file.isFile() || !file.getName().matches("[A-Za-z0-9_.-]+\\.(png|txt|json)"))continue;
                            zip.putNextEntry(new ZipEntry(file.getName()));
                            try(FileInputStream input=new FileInputStream(file)) {
                                int size;while((size=input.read(buffer))!=-1)zip.write(buffer,0,size);
                            }
                            zip.closeEntry();
                        }
                    } catch(IOException error) {
                        try { pipe[1].closeWithError("QA archive interrupted"); } catch(IOException ignored) { }
                    }
                },"qa-artifact-readback").start();
                return pipe[0];
            }
            if(Binder.getCallingUid()!=allowed)throw new SecurityException("Target QA UID required");
            if(!mode.equals("w"))throw new SecurityException("QA writes only");
            if(path==null || !(path.matches("/final01-ui/[a-f0-9]{12}/(portrait|landscape)/[A-Za-z0-9_.-]+\\.(png|txt|json)") ||
                    path.equals("/installed-database-digest.json")))throw new SecurityException("QA artifact path required");
            File root=new File(getContext().getFilesDir(),"qa-evidence");
            File file=new File(root,path.substring(1));
            if(!file.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new SecurityException("QA path escaped");
            if(path.startsWith("/final01-ui/") && file.exists())throw new SecurityException("Never overwrite a UI attempt");
            if(!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs())throw new IOException("QA directory unavailable");
            return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE);
        } catch(android.content.pm.PackageManager.NameNotFoundException | IOException error) {
            throw new FileNotFoundException("QA artifact unavailable");
        }
    }
}
