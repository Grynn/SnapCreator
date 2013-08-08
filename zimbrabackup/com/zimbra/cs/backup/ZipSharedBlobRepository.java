// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ZipSharedBlobRepository.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.zip.ZipEntry;
import com.zimbra.common.util.zip.ZipFile;
import com.zimbra.common.util.zip.ZipOutputStream;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.backup.util.ParallelZipCopier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// Referenced classes of package com.zimbra.cs.backup:
//            SharedBlobRepository, BackupLC

class ZipSharedBlobRepository extends SharedBlobRepository
{
    private class Entry
        implements SharedBlobRepository.SharedBlobEntry
    {

        public String getName()
        {
            return mName;
        }

        public String getPath()
        {
            return mZipEntry.getName();
        }

        public InputStream getInputStream()
            throws IOException
        {
            return mZipFile.getInputStream(mZipEntry);
        }

        public boolean mustCloseInputStream()
        {
            return true;
        }

        private ZipFile mZipFile;
        private ZipEntry mZipEntry;
        private String mName;
        final ZipSharedBlobRepository this$0;

        private Entry(ZipFile zf, ZipEntry zentry, String name)
        {
            this$0 = ZipSharedBlobRepository.this;
            super();
            mZipFile = zf;
            mZipEntry = zentry;
            mName = name;
        }

    }


    ZipSharedBlobRepository(File zipDir, boolean uncompressed, int dirDepth, int charsPerDir, int zipNameDigestChars)
        throws IOException
    {
        super(dirDepth, charsPerDir);
        mCompressionMethod = 8;
        mDir = zipDir;
        FileUtil.ensureDirExists(mDir);
        mCompressionMethod = uncompressed ? 0 : 8;
        mZipNameDigestChars = zipNameDigestChars;
        mOutputs = new HashMap();
        int numThreads = BackupLC.backup_shared_blobs_zip_copier_threads.intValueWithinRange(1, 100);
        mZipCopier = new ParallelZipCopier("ZipCopier-SharedBlobs", numThreads);
        mOpened = true;
    }

    ZipSharedBlobRepository(File zipDir, int dirDepth, int charsPerDir, int zipNameDigestChars)
    {
        super(dirDepth, charsPerDir);
        mCompressionMethod = 8;
        mDir = zipDir;
        mZipNameDigestChars = zipNameDigestChars;
        mInputs = new HashMap();
        mOpened = true;
    }

    public synchronized void close()
        throws IOException
    {
        if(mOpened)
        {
            if(mOutputs != null)
            {
                if(mZipCopier != null)
                    mZipCopier.shutdown();
                for(Iterator i$ = mOutputs.entrySet().iterator(); i$.hasNext();)
                {
                    java.util.Map.Entry entry = (java.util.Map.Entry)i$.next();
                    ZipOutputStream zos = (ZipOutputStream)entry.getValue();
                    try
                    {
                        zos.close();
                    }
                    catch(IOException e)
                    {
                        Log.backup.warn((new StringBuilder()).append("Error closing ZipOutputStream for ").append((String)entry.getKey()).toString(), e);
                    }
                }

                mOutputs.clear();
            }
            if(mInputs != null)
            {
                for(Iterator i$ = mInputs.entrySet().iterator(); i$.hasNext();)
                {
                    java.util.Map.Entry entry = (java.util.Map.Entry)i$.next();
                    ZipFile zf = (ZipFile)entry.getValue();
                    try
                    {
                        zf.close();
                    }
                    catch(IOException e)
                    {
                        Log.backup.warn((new StringBuilder()).append("Error closing ZipFile ").append((String)entry.getKey()).toString(), e);
                    }
                }

                mInputs.clear();
            }
            mOpened = false;
        }
    }

    public synchronized void write(String digest, File in, FileCopier copier, FileCopierCallback cb)
        throws IOException
    {
        String relPath = digestToRelPath(digest);
        ZipEntry entry = new ZipEntry(relPath);
        entry.setMethod(mCompressionMethod);
        ZipOutputStream zos = getOutput(digest);
        mZipCopier.copy(in, zos, entry, cb);
        if(Log.backup.isDebugEnabled())
            Log.backup.debug((new StringBuilder()).append("Submitted file ").append(entry.getName()).append(" for zip copying").toString());
    }

    public synchronized SharedBlobRepository.SharedBlobEntry getEntry(String digest)
        throws IOException
    {
        String relPath = digestToRelPath(digest);
        ZipFile zf = getInput(digest);
        if(zf != null)
        {
            ZipEntry zentry = zf.getEntry(relPath);
            if(zentry != null)
                return new Entry(zf, zentry, relPath);
        }
        return null;
    }

    private String getZipFileForDigest(String digest)
    {
        String prefix = digest.substring(0, mZipNameDigestChars).toLowerCase();
        return (new StringBuilder()).append("blobs-").append(prefix).append(".zip").toString();
    }

    private synchronized ZipOutputStream getOutput(String digest)
        throws IOException
    {
        String fname = getZipFileForDigest(digest);
        ZipOutputStream zos = (ZipOutputStream)mOutputs.get(fname);
        if(zos != null)
            return zos;
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
        mOutputs.put(fname, zos);
        return zos;
    }

    private synchronized ZipFile getInput(String digest)
        throws IOException
    {
        String fname = getZipFileForDigest(digest);
        ZipFile zf = (ZipFile)mInputs.get(fname);
        if(zf != null)
            return zf;
        File zipFile = new File(mDir, fname);
        if(zipFile.exists())
        {
            zf = new ZipFile(zipFile);
            mInputs.put(fname, zf);
            return zf;
        } else
        {
            return null;
        }
    }

    private File mDir;
    private boolean mOpened;
    private int mCompressionMethod;
    private int mZipNameDigestChars;
    private Map mOutputs;
    private ParallelZipCopier mZipCopier;
    private Map mInputs;
}
