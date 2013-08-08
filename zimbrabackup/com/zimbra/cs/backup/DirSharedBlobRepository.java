// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   DirSharedBlobRepository.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import com.zimbra.common.util.FileUtil;
import java.io.*;

// Referenced classes of package com.zimbra.cs.backup:
//            SharedBlobRepository

class DirSharedBlobRepository extends SharedBlobRepository
{
    private static class Entry
        implements SharedBlobRepository.SharedBlobEntry
    {

        public String getName()
        {
            return mName;
        }

        public String getPath()
        {
            return mFile.getAbsolutePath();
        }

        public InputStream getInputStream()
            throws IOException
        {
            return new FileInputStream(mFile);
        }

        public boolean mustCloseInputStream()
        {
            return true;
        }

        private File mFile;
        private String mName;

        private Entry(File file, String name)
        {
            mFile = file;
            mName = name;
        }

    }


    DirSharedBlobRepository(File dir, int dirDepth, int charsPerDir, boolean forWrite)
        throws IOException
    {
        super(dirDepth, charsPerDir);
        mDir = dir;
        if(forWrite)
            FileUtil.ensureDirExists(mDir);
    }

    public void close()
        throws IOException
    {
    }

    public void write(String digest, File in, FileCopier copier, FileCopierCallback cb)
        throws IOException
    {
        String relPath = digestToRelPath(digest);
        File file = new File(mDir, relPath);
        if(in != null)
        {
            copier.copy(in, file, cb, file);
        } else
        {
            FileUtil.ensureDirExists(file.getParentFile());
            file.createNewFile();
        }
    }

    public SharedBlobRepository.SharedBlobEntry getEntry(String digest)
        throws IOException
    {
        String relPath = digestToRelPath(digest);
        File file = new File(mDir, relPath);
        if(file.exists())
            return new Entry(file, relPath);
        else
            return null;
    }

    private File mDir;
}
