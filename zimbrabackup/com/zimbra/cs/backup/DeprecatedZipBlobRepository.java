// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   DeprecatedZipBlobRepository.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import com.zimbra.common.util.zip.ZipEntry;
import com.zimbra.common.util.zip.ZipOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

// Referenced classes of package com.zimbra.cs.backup:
//            BlobRepository

class DeprecatedZipBlobRepository extends BlobRepository
{
    private class Entry
        implements BlobRepository.BlobEntry
    {

        public String getName()
        {
            return mName;
        }

        public String getPath()
        {
            throw new UnsupportedOperationException();
        }

        public InputStream getInputStream()
            throws IOException
        {
            return mBlobZipInputStream;
        }

        public boolean mustCloseInputStream()
        {
            return false;
        }

        private String mName;
        final DeprecatedZipBlobRepository this$0;

        Entry(String name)
        {
            this$0 = DeprecatedZipBlobRepository.this;
            super();
            mName = name;
        }
    }


    DeprecatedZipBlobRepository(File zipFile, boolean forWrite)
        throws IOException
    {
        boolean success;
        mZipEntry = null;
        success = false;
        mOpened = true;
        mZipFile = zipFile;
        if(forWrite)
        {
            FileOutputStream fos = new FileOutputStream(mZipFile);
            mBlobZipOutputStream = new ZipOutputStream(fos, mZipFile.getParentFile());
            mBlobZipOutputStream.putNextEntry(new ZipEntry("ZM_BACKUP"));
            mBlobZipOutputStream.closeEntry();
        } else
        {
            FileInputStream fis = new FileInputStream(mZipFile);
            mBlobZipInputStream = new ZipInputStream(fis);
            java.util.zip.ZipEntry e = mBlobZipInputStream.getNextEntry();
            if(e == null || !e.getName().equals("ZM_BACKUP"))
                throw new IOException("Invalid zip file for backup blob files");
        }
        success = true;
        if(!success)
            close();
        break MISSING_BLOCK_LABEL_173;
        Exception exception;
        exception;
        if(!success)
            close();
        throw exception;
    }

    public synchronized void write(String entryName, String digest, File in, FileCopier copier, FileCopierCallback cb)
        throws IOException
    {
        InputStream is;
        ZipEntry entry = new ZipEntry(entryName);
        mBlobZipOutputStream.putNextEntry(entry);
        if(in == null)
            break MISSING_BLOCK_LABEL_87;
        is = new FileInputStream(in);
        byte buf[] = new byte[16384];
        int byteRead;
        while((byteRead = is.read(buf)) != -1) 
            mBlobZipOutputStream.write(buf, 0, byteRead);
        is.close();
        break MISSING_BLOCK_LABEL_87;
        Exception exception;
        exception;
        is.close();
        throw exception;
        mBlobZipOutputStream.closeEntry();
        return;
    }

    public synchronized void close()
        throws IOException
    {
        if(mOpened)
        {
            if(mBlobZipInputStream != null)
                try
                {
                    mBlobZipInputStream.close();
                }
                catch(IOException e) { }
            if(mBlobZipOutputStream != null)
                try
                {
                    mBlobZipOutputStream.close();
                }
                catch(IOException e) { }
            mOpened = false;
        }
    }

    public synchronized BlobRepository.BlobEntry getNextEntry()
        throws IOException
    {
        java.util.zip.ZipEntry entry = null;
        entry = mBlobZipInputStream.getNextEntry();
        mZipEntry = entry;
        break MISSING_BLOCK_LABEL_26;
        Exception exception;
        exception;
        mZipEntry = entry;
        throw exception;
        if(mZipEntry != null)
            return new Entry(mZipEntry.getName());
        else
            return null;
    }

    public synchronized BlobRepository.BlobEntry getCurrentEntry()
    {
        return new Entry(mZipEntry.getName());
    }

    private File mZipFile;
    private ZipInputStream mBlobZipInputStream;
    private ZipOutputStream mBlobZipOutputStream;
    private java.util.zip.ZipEntry mZipEntry;
    private boolean mOpened;

}
