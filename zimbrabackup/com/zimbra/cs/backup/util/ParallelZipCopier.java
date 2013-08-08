// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ParallelZipCopier.java

package com.zimbra.cs.backup.util;

import com.zimbra.common.io.FileCopierCallback;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.zip.ZipEntry;
import com.zimbra.common.util.zip.ZipOutputStream;
import com.zimbra.cs.backup.BackupLC;
import com.zimbra.cs.util.Zimbra;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Referenced classes of package com.zimbra.cs.backup.util:
//            Log, ZipUtil

public class ParallelZipCopier
{
    private class ZipCopierThread extends Thread
    {

        public void run()
        {
            byte buf[] = new byte[ParallelZipCopier.COPYBUF_SIZE];
_L2:
            ZipEntryTask task;
            File srcFile;
            Throwable err;
            try
            {
                task = (ZipEntryTask)mTaskQueue.take();
            }
            catch(InterruptedException e)
            {
                break; /* Loop/switch isn't completed */
            }
            if(task.isShutdownTask())
                break; /* Loop/switch isn't completed */
            srcFile = task.getSrc();
            err = null;
            if(hadError())
            {
                FileCopierCallback cb = task.getCallback();
                if(cb != null)
                    cb.fileCopierCallbackEnd(srcFile, err);
                continue; /* Loop/switch isn't completed */
            }
            ZipEntry zentry;
            String entryName;
            long fileLen;
            zentry = task.getZipEntry();
            entryName = zentry.getName();
            if(zentry.getMethod() != 0)
                break MISSING_BLOCK_LABEL_279;
            if(srcFile == null)
                break MISSING_BLOCK_LABEL_261;
            fileLen = srcFile.length();
            if(fileLen != 0L || srcFile.exists())
                break MISSING_BLOCK_LABEL_178;
            Log.backup.debug((new StringBuilder()).append("Skipping concurrently deleted blob ").append(srcFile.getAbsolutePath()).toString());
            FileCopierCallback cb = task.getCallback();
            if(cb != null)
                cb.fileCopierCallbackEnd(srcFile, err);
            continue; /* Loop/switch isn't completed */
            zentry.setSize(fileLen);
            zentry.setCompressedSize(fileLen);
            try
            {
                zentry.setCrc(ZipUtil.computeCRC32(srcFile));
                break MISSING_BLOCK_LABEL_279;
            }
            catch(FileNotFoundException e)
            {
                Log.backup.debug((new StringBuilder()).append("Skipping concurrently deleted blob ").append(srcFile.getAbsolutePath()).toString());
            }
            FileCopierCallback cb = task.getCallback();
            if(cb != null)
                cb.fileCopierCallbackEnd(srcFile, err);
            continue; /* Loop/switch isn't completed */
            zentry.setSize(0L);
            zentry.setCompressedSize(0L);
            zentry.setCrc(0L);
            InputStream is = null;
            if(srcFile == null)
                break MISSING_BLOCK_LABEL_358;
            try
            {
                is = new FileInputStream(srcFile);
                break MISSING_BLOCK_LABEL_358;
            }
            catch(FileNotFoundException e)
            {
                Log.backup.debug((new StringBuilder()).append("Skipping concurrently deleted blob ").append(srcFile.getAbsolutePath()).toString());
            }
            ByteUtil.closeStream(is);
            FileCopierCallback cb = task.getCallback();
            if(cb != null)
                cb.fileCopierCallbackEnd(srcFile, err);
            continue; /* Loop/switch isn't completed */
            ZipOutputStream zos = task.getZipOutputStream();
            synchronized(zos)
            {
                zos.putNextEntry(zentry);
                int byteRead;
                if(is != null)
                    while((byteRead = is.read(buf)) != -1) 
                        zos.write(buf, 0, byteRead);
                zos.closeEntry();
            }
            ByteUtil.closeStream(is);
            break MISSING_BLOCK_LABEL_444;
            Exception exception1;
            exception1;
            ByteUtil.closeStream(is);
            throw exception1;
            if(Log.backup.isDebugEnabled())
                Log.backup.debug((new StringBuilder()).append("Added ").append(entryName).append(" to zip file").toString());
            FileCopierCallback cb = task.getCallback();
            if(cb != null)
                cb.fileCopierCallbackEnd(srcFile, err);
            continue; /* Loop/switch isn't completed */
            OutOfMemoryError oome;
            oome;
            Zimbra.halt("Out of memory while copying to zip file", oome);
            FileCopierCallback cb = task.getCallback();
            if(cb != null)
                cb.fileCopierCallbackEnd(srcFile, err);
            continue; /* Loop/switch isn't completed */
            Throwable e;
            e;
            Log.backup.error("Unable to write to zip file", e);
            raiseError(e);
            err = e;
            FileCopierCallback cb = task.getCallback();
            if(cb != null)
                cb.fileCopierCallbackEnd(srcFile, err);
            if(true) goto _L2; else goto _L1
            Exception exception2;
            exception2;
            FileCopierCallback cb = task.getCallback();
            if(cb != null)
                cb.fileCopierCallbackEnd(srcFile, err);
            throw exception2;
_L1:
        }

        final ParallelZipCopier this$0;

        private ZipCopierThread()
        {
            this$0 = ParallelZipCopier.this;
            super();
        }

    }

    private static class ShutdownTask extends ZipEntryTask
    {

        public boolean isShutdownTask()
        {
            return true;
        }

        public ShutdownTask()
        {
            super(null, null, null, null);
        }
    }

    private static class ZipEntryTask
    {

        public File getSrc()
        {
            return mSrcFile;
        }

        public ZipEntry getZipEntry()
        {
            return mZipEntry;
        }

        public ZipOutputStream getZipOutputStream()
        {
            return mZOS;
        }

        public FileCopierCallback getCallback()
        {
            return mCallback;
        }

        public boolean isShutdownTask()
        {
            return false;
        }

        private File mSrcFile;
        private ZipEntry mZipEntry;
        private ZipOutputStream mZOS;
        private FileCopierCallback mCallback;

        public ZipEntryTask(File src, ZipEntry zentry, ZipOutputStream zos, FileCopierCallback cb)
        {
            mSrcFile = src;
            mZipEntry = zentry;
            mZOS = zos;
            mCallback = cb;
        }
    }


    public ParallelZipCopier(String threadName, int numThreads)
    {
        mError = null;
        Log.backup.debug((new StringBuilder()).append("Starting ParallelZipCopier for ").append(threadName).toString());
        numThreads = Math.max(numThreads, 1);
        int capacity = Math.max(QUEUE_CAPACITY, numThreads);
        mTaskQueue = new LinkedBlockingQueue(capacity);
        mZipCopierThreads = new ZipCopierThread[numThreads];
        for(int i = 0; i < numThreads; i++)
        {
            String name = (new StringBuilder()).append(threadName).append("-").append(Integer.toString(i)).toString();
            ZipCopierThread copier = new ZipCopierThread();
            mZipCopierThreads[i] = copier;
            copier.setName(name);
            copier.start();
        }

    }

    public void shutdown()
    {
        Log.backup.debug("Shutting down ParallelZipCopier");
        if(hadError())
            mTaskQueue.clear();
        for(int i = 0; i < mZipCopierThreads.length; i++)
        {
            ShutdownTask t = new ShutdownTask();
            try
            {
                mTaskQueue.put(t);
            }
            catch(InterruptedException e) { }
        }

        for(int i = 0; i < mZipCopierThreads.length; i++)
            try
            {
                mZipCopierThreads[i].join();
            }
            catch(InterruptedException e) { }

        Log.backup.debug("ParallelZipCopier shutdown complete");
    }

    public void copy(File src, ZipOutputStream dest, ZipEntry zentry, FileCopierCallback cb)
        throws IOException
    {
        checkError();
        ZipEntryTask task = new ZipEntryTask(src, zentry, dest, cb);
        if(cb != null)
        {
            boolean okay = cb.fileCopierCallbackBegin(src);
            if(!okay)
                throw new IOException("Operation rejected by callback");
        }
        try
        {
            mTaskQueue.put(task);
        }
        catch(InterruptedException e) { }
    }

    private void raiseError(Throwable t)
    {
        synchronized(mErrorLock)
        {
            mError = t;
        }
    }

    private void checkError()
        throws IOException
    {
        synchronized(mErrorLock)
        {
            if(mError != null)
            {
                IOException e = new IOException((new StringBuilder()).append("ParallelZipCopier stopped due to earlier error: ").append(mError.getMessage()).toString());
                e.initCause(mError);
                throw e;
            }
        }
    }

    private boolean hadError()
    {
        Object obj = mErrorLock;
        JVM INSTR monitorenter ;
        return mError != null;
        Exception exception;
        exception;
        throw exception;
    }

    private static final int COPYBUF_SIZE;
    private static final int QUEUE_CAPACITY;
    private BlockingQueue mTaskQueue;
    private ZipCopierThread mZipCopierThreads[];
    private Throwable mError;
    private final Object mErrorLock = new Object();

    static 
    {
        COPYBUF_SIZE = (BackupLC.backup_zip_copier_copy_buffer_size.intValueWithinRange(4096, 0x100000) / 4096) * 4096;
        QUEUE_CAPACITY = BackupLC.backup_zip_copier_queue_capacity.intValueWithinRange(1, 10000);
    }




}
