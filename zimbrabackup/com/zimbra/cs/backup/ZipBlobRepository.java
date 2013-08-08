// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ZipBlobRepository.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.zip.ZipEntry;
import com.zimbra.common.util.zip.ZipOutputStream;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.backup.util.ParallelZipCopier;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

// Referenced classes of package com.zimbra.cs.backup:
//            BlobRepository, BackupLC

class ZipBlobRepository extends BlobRepository
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
            return mZipInputStream;
        }

        public boolean mustCloseInputStream()
        {
            return false;
        }

        private ZipInputStream mZipInputStream;
        private String mName;
        final ZipBlobRepository this$0;

        private Entry(ZipInputStream zis, String name)
        {
            this$0 = ZipBlobRepository.this;
            super();
            mZipInputStream = zis;
            mName = name;
        }

    }


    ZipBlobRepository(int mailboxId, File zipDir, boolean uncompressed)
        throws IOException
    {
        mCompressionMethod = 8;
        mNumOutputs = DEFAULT_NUM_ZIPFILES;
        mDir = zipDir;
        FileUtil.ensureDirExists(mDir);
        mCompressionMethod = uncompressed ? 0 : 8;
        mNumOutputs = Math.max(mNumOutputs, 1);
        mOutputs = new ZipOutputStream[mNumOutputs];
        String copierName = (new StringBuilder()).append("ZipCopier-mbox").append(Integer.toString(mailboxId)).toString();
        int numCopierThreads = Math.min(mNumOutputs, 100);
        mZipCopier = new ParallelZipCopier(copierName, numCopierThreads);
        mOpened = true;
    }

    ZipBlobRepository(File zipDir)
    {
        mCompressionMethod = 8;
        mNumOutputs = DEFAULT_NUM_ZIPFILES;
        mDir = zipDir;
        mInputs = new ArrayList();
        File files[] = mDir.listFiles(new FileFilter() {

            public boolean accept(File pathname)
            {
                return pathname.isFile() && pathname.getName().endsWith(".zip");
            }

            final ZipBlobRepository this$0;

            
            {
                this$0 = ZipBlobRepository.this;
                super();
            }
        }
);
        if(files != null)
        {
            File arr$[] = files;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                File f = arr$[i$];
                mInputs.add(f);
            }

        }
        mOpened = true;
    }

    public synchronized void close()
        throws IOException
    {
        if(mOpened)
        {
            if(mZipCopier != null)
                mZipCopier.shutdown();
            if(mOutputs != null)
            {
                for(int i = 0; i < mOutputs.length; i++)
                {
                    if(mOutputs[i] == null)
                        continue;
                    try
                    {
                        mOutputs[i].close();
                    }
                    catch(IOException e)
                    {
                        Log.backup.warn("Error closing ZipOutputStream", e);
                    }
                }

                mOutputs = null;
            }
            if(mInputs != null)
            {
                mInputs.clear();
                mInputs = null;
                if(mCurrentZipInputStream != null)
                {
                    try
                    {
                        mCurrentZipInputStream.close();
                    }
                    catch(IOException e)
                    {
                        Log.backup.warn("Error closing ZipInputStream", e);
                    }
                    mCurrentZipInputStream = null;
                }
            }
            mOpened = false;
        }
    }

    public synchronized void write(String entryName, String digest, File in, FileCopier copier, FileCopierCallback cb)
        throws IOException
    {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setMethod(mCompressionMethod);
        ZipOutputStream zos = getOutput();
        mZipCopier.copy(in, zos, entry, cb);
        if(Log.backup.isDebugEnabled())
            Log.backup.debug((new StringBuilder()).append("Submitted file ").append(entryName).append(" for zip copying").toString());
    }

    private synchronized ZipOutputStream getOutput()
        throws IOException
    {
        ZipOutputStream zos = mOutputs[mCurrentOutputIndex];
        if(zos == null)
        {
            String fname = (new StringBuilder()).append("blobs-").append(Integer.toString(mCurrentOutputIndex + 1)).append(".zip").toString();
            File zipFile = new File(mDir, fname);
            FileOutputStream fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos, mDir);
            zos.setMethod(mCompressionMethod);
            if(mCompressionMethod == 8)
            {
                int level = BackupLC.backup_zip_copier_deflate_level.intValueWithinRange(-1, 9);
                if(level != -1)
                    zos.setLevel(level);
            }
            mOutputs[mCurrentOutputIndex] = zos;
        }
        mCurrentOutputIndex++;
        mCurrentOutputIndex %= mNumOutputs;
        return zos;
    }

    public synchronized BlobRepository.BlobEntry getNextEntry()
        throws IOException
    {
        mCurrentZipEntry = null;
_L2:
        FileInputStream fis;
        if(mCurrentZipInputStream != null)
            break MISSING_BLOCK_LABEL_100;
        if(mInputs.isEmpty())
            return null;
        File file = (File)mInputs.remove(0);
        fis = new FileInputStream(file);
        mCurrentZipInputStream = new ZipInputStream(fis);
        if(mCurrentZipInputStream == null)
            try
            {
                fis.close();
            }
            catch(IOException e) { }
        break MISSING_BLOCK_LABEL_100;
        Exception exception;
        exception;
        if(mCurrentZipInputStream == null)
            try
            {
                fis.close();
            }
            catch(IOException e) { }
        throw exception;
        if(!$assertionsDisabled && mCurrentZipInputStream == null)
            throw new AssertionError();
        mCurrentZipEntry = mCurrentZipInputStream.getNextEntry();
        if(mCurrentZipEntry != null)
            return new Entry(mCurrentZipInputStream, mCurrentZipEntry.getName());
        try
        {
            mCurrentZipInputStream.close();
        }
        catch(IOException e)
        {
            Log.backup.warn("Error closing ZipInputStream", e);
        }
        mCurrentZipInputStream = null;
        if(true) goto _L2; else goto _L1
_L1:
    }

    public synchronized BlobRepository.BlobEntry getCurrentEntry()
    {
        if(mCurrentZipInputStream != null && mCurrentZipEntry != null)
            return new Entry(mCurrentZipInputStream, mCurrentZipEntry.getName());
        else
            return null;
    }

    private static final int DEFAULT_NUM_ZIPFILES;
    private File mDir;
    private boolean mOpened;
    private int mCompressionMethod;
    private ZipOutputStream mOutputs[];
    private int mNumOutputs;
    private int mCurrentOutputIndex;
    private ParallelZipCopier mZipCopier;
    private List mInputs;
    private ZipInputStream mCurrentZipInputStream;
    private java.util.zip.ZipEntry mCurrentZipEntry;
    static final boolean $assertionsDisabled = !com/zimbra/cs/backup/ZipBlobRepository.desiredAssertionStatus();

    static 
    {
        DEFAULT_NUM_ZIPFILES = BackupLC.backup_zip_copier_private_blob_zips.intValueWithinRange(1, 10000);
    }
}
