// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   DirBlobRepository.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import com.zimbra.common.util.FileUtil;
import java.io.*;
import java.util.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BlobRepository

class DirBlobRepository extends BlobRepository
{
    private class DirBlobEntry
        implements BlobRepository.BlobEntry
    {

        public String getName()
        {
            return mName;
        }

        public String getPath()
        {
            return mBlobFile;
        }

        public InputStream getInputStream()
            throws IOException
        {
            return new FileInputStream(mBlobFile);
        }

        public boolean mustCloseInputStream()
        {
            return true;
        }

        private String mBlobFile;
        private String mName;
        final DirBlobRepository this$0;

        DirBlobEntry(String file)
        {
            this$0 = DirBlobRepository.this;
            super();
            mBlobFile = file;
            mName = file.substring(mDirPath.length());
        }
    }


    DirBlobRepository(File dir, boolean forWrite)
        throws IOException
    {
        mDir = dir;
        String path = dir.getPath();
        path = path.replaceAll("[/\\\\]", (new StringBuilder()).append("\\").append(File.separator).toString());
        if(!path.endsWith(File.separator))
            path = (new StringBuilder()).append(path).append(File.separator).toString();
        mDirPath = path;
        if(forWrite)
        {
            FileUtil.ensureDirExists(mDir);
        } else
        {
            if(mDir.exists())
            {
                if(!mDir.isDirectory())
                    throw new IOException((new StringBuilder()).append(mDir.getPath()).append(" is not a directory").toString());
                Set fileDirs = new HashSet();
                getFileDirs(mDir, fileDirs);
                mFileDirs = fileDirs.iterator();
                mBlobFiles = new File[0];
            }
            mCurrentIndex = 0;
            mCurrentEntry = null;
        }
    }

    private void getFileDirs(File root, Set fileDirs)
    {
        File files[] = root.listFiles();
        for(int i = 0; i < files.length; i++)
        {
            if(files[i].isDirectory())
            {
                getFileDirs(files[i], fileDirs);
                continue;
            }
            File parent = files[i].getParentFile();
            if(!fileDirs.contains(parent))
                fileDirs.add(parent);
        }

    }

    public void write(String entryName, String digest, File in, FileCopier copier, FileCopierCallback cb)
        throws IOException
    {
        File file = new File(mDir, entryName);
        if(in != null)
        {
            copier.copy(in, file, cb, file);
        } else
        {
            FileUtil.ensureDirExists(file.getParentFile());
            file.createNewFile();
        }
    }

    public void close()
        throws IOException
    {
    }

    public BlobRepository.BlobEntry getCurrentEntry()
    {
        return mCurrentEntry;
    }

    public BlobRepository.BlobEntry getNextEntry()
    {
        BlobRepository.BlobEntry e = getNext();
        if(e != null)
            return e;
        if(mFileDirs != null && mFileDirs.hasNext())
        {
            File dir = (File)mFileDirs.next();
            mBlobFiles = dir.listFiles(new FileFilter() {

                public boolean accept(File file)
                {
                    return file.isFile();
                }

                final DirBlobRepository this$0;

            
            {
                this$0 = DirBlobRepository.this;
                super();
            }
            }
);
            mCurrentIndex = 0;
            return getNext();
        } else
        {
            return null;
        }
    }

    private BlobRepository.BlobEntry getNext()
    {
        if(mBlobFiles != null && mCurrentIndex < mBlobFiles.length)
        {
            mCurrentEntry = new DirBlobEntry(mBlobFiles[mCurrentIndex++].getPath());
            return getCurrentEntry();
        } else
        {
            return null;
        }
    }

    private File mDir;
    private File mBlobFiles[];
    private int mCurrentIndex;
    private BlobRepository.BlobEntry mCurrentEntry;
    private Iterator mFileDirs;
    private String mDirPath;

}
