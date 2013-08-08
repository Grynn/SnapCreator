// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   SharedBlobRepository.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import java.io.*;

abstract class SharedBlobRepository
{
    public static interface SharedBlobEntry
    {

        public abstract String getName();

        public abstract String getPath();

        public abstract InputStream getInputStream()
            throws IOException;

        public abstract boolean mustCloseInputStream();
    }


    protected SharedBlobRepository(int dirDepth, int charsPerDir)
    {
        mDirDepth = dirDepth;
        mCharsPerDir = charsPerDir;
    }

    public abstract void close()
        throws IOException;

    public abstract void write(String s, File file, FileCopier filecopier, FileCopierCallback filecopiercallback)
        throws IOException;

    public abstract SharedBlobEntry getEntry(String s)
        throws IOException;

    protected String digestToRelPath(String digest)
    {
        return getRelPath(digest, mDirDepth, mCharsPerDir);
    }

    public static String getRelPath(String digest, int dirDepth, int charsPerDir)
    {
        if(!$assertionsDisabled && charsPerDir <= 0)
            throw new AssertionError();
        int len = Math.min(digest.length(), dirDepth * charsPerDir);
        int start = 0;
        int end = charsPerDir;
        StringBuilder sb = new StringBuilder();
        for(; end <= len; end += charsPerDir)
        {
            sb.append(digest.substring(start, end).toLowerCase()).append(File.separator);
            start = end;
        }

        sb.append(digest).append(File.separator).append("blob.dat");
        return sb.toString();
    }

    protected static final String BLOB_DAT = "blob.dat";
    private int mDirDepth;
    private int mCharsPerDir;
    static final boolean $assertionsDisabled = !com/zimbra/cs/backup/SharedBlobRepository.desiredAssertionStatus();

}
