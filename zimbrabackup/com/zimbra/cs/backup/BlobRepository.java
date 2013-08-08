// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BlobRepository.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import java.io.*;

public abstract class BlobRepository
{
    public static interface BlobEntry
    {

        public abstract String getName();

        public abstract String getPath();

        public abstract InputStream getInputStream()
            throws IOException;

        public abstract boolean mustCloseInputStream();
    }


    public BlobRepository()
    {
    }

    public abstract void write(String s, String s1, File file, FileCopier filecopier, FileCopierCallback filecopiercallback)
        throws IOException;

    public abstract BlobEntry getCurrentEntry();

    public abstract BlobEntry getNextEntry()
        throws IOException;

    public abstract void close()
        throws IOException;
}
