// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupRestoreFileCopierOptionsCLI.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.service.ServiceException;
import org.apache.commons.cli.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupRestoreCLI

public abstract class BackupRestoreFileCopierOptionsCLI extends BackupRestoreCLI
{
    protected static class SkipComponentsOptions
    {

        public boolean hasSkipSearchIndexOpt()
        {
            return mHasOptSkipSearchIndex;
        }

        public boolean getSkipSearchIndex()
        {
            return mSkipSearchIndex;
        }

        private void setSkipSearchIndex(boolean b)
        {
            mHasOptSkipSearchIndex = true;
            mSkipSearchIndex = b;
        }

        public boolean hasSkipBlobsOpt()
        {
            return mHasOptSkipBlobs;
        }

        public boolean getSkipBlobs()
        {
            return mSkipBlobs;
        }

        private void setSkipBlobs(boolean b)
        {
            mHasOptSkipBlobs = true;
            mSkipBlobs = b;
        }

        public boolean hasSkipHsmBlobsOpt()
        {
            return mHasOptSkipHsmBlobs;
        }

        public boolean getSkipHsmBlobs()
        {
            return mSkipHsmBlobs;
        }

        private void setSkipHsmBlobs(boolean b)
        {
            mHasOptSkipHsmBlobs = true;
            mSkipHsmBlobs = b;
        }

        private boolean mHasOptSkipSearchIndex;
        private boolean mSkipSearchIndex;
        private boolean mHasOptSkipBlobs;
        private boolean mSkipBlobs;
        private boolean mHasOptSkipHsmBlobs;
        private boolean mSkipHsmBlobs;




        protected SkipComponentsOptions()
        {
        }
    }


    protected BackupRestoreFileCopierOptionsCLI()
        throws ServiceException
    {
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getHiddenOptions();
        options.addOption(null, "FileCopier", true, "FileCopier options in encoded format; If this option is specified, all other FileCopier options are ignored.");
        options.addOption(null, "FileCopierMethod", true, "file copier method (PARALLEL, PIPE or SERIAL)");
        options.addOption(null, "FileCopierIOType", true, "OIO or NIO");
        options.addOption(null, "FileCopierOIOCopyBufSize", true, "copy buffer size for OIO");
        options.addOption(null, "FileCopierAsyncQueueCapacity", true, "async request queue size, for PARALLEL and PIPE");
        options.addOption(null, "FileCopierParallelWorkers", true, "number of worker threads, for PARALLEL");
        options.addOption(null, "FileCopierPipes", true, "number of pipes, for PIPE");
        options.addOption(null, "FileCopierPipeBufSize", true, "buffer size for each pipe, for PIPE");
        options.addOption(null, "FileCopierReadersPerPipe", true, "number of file reader threads, for PIPE");
        options.addOption(null, "FileCopierWritersPerPipe", true, "number of file writer threads, for PIPE");
    }

    protected static FileCopierOptions getFileCopierOptions(CommandLine cl)
        throws ServiceException
    {
        FileCopierOptions fcOpts = null;
        if(cl.hasOption("FileCopier"))
        {
            String options = cl.getOptionValue("FileCopier");
            fcOpts = new FileCopierOptions(options);
        } else
        if(cl.hasOption("FileCopierMethod"))
        {
            fcOpts = new FileCopierOptions();
            com.zimbra.common.io.FileCopierOptions.Method method = com.zimbra.common.io.FileCopierOptions.Method.parseMethod(cl.getOptionValue("FileCopierMethod"));
            fcOpts.setMethod(method);
            if(cl.hasOption("FileCopierIOType"))
            {
                com.zimbra.common.io.FileCopierOptions.IOType ioType = com.zimbra.common.io.FileCopierOptions.IOType.parseIOType(cl.getOptionValue("FileCopierIOType"));
                fcOpts.setIOType(ioType);
                if(com.zimbra.common.io.FileCopierOptions.IOType.OIO.equals(ioType) && cl.hasOption("FileCopierOIOCopyBufSize"))
                {
                    int num = FileCopierOptions.parseSize(cl.getOptionValue("FileCopierOIOCopyBufSize"));
                    fcOpts.setOIOCopyBufferSize(num);
                }
            }
            if(com.zimbra.common.io.FileCopierOptions.Method.PARALLEL.equals(method) || com.zimbra.common.io.FileCopierOptions.Method.PIPE.equals(method))
            {
                if(cl.hasOption("FileCopierAsyncQueueCapacity"))
                {
                    int num = FileCopierOptions.parseSize(cl.getOptionValue("FileCopierAsyncQueueCapacity"));
                    fcOpts.setAsyncQueueCapacity(num);
                }
                if(com.zimbra.common.io.FileCopierOptions.Method.PARALLEL.equals(method))
                {
                    if(cl.hasOption("FileCopierParallelWorkers"))
                    {
                        int num = FileCopierOptions.parseSize(cl.getOptionValue("FileCopierParallelWorkers"));
                        fcOpts.setNumParallelWorkers(num);
                    }
                } else
                {
                    if(cl.hasOption("FileCopierPipes"))
                    {
                        int num = FileCopierOptions.parseSize(cl.getOptionValue("FileCopierPipes"));
                        fcOpts.setNumPipes(num);
                    }
                    if(cl.hasOption("FileCopierPipeBufSize"))
                    {
                        int num = FileCopierOptions.parseSize(cl.getOptionValue("FileCopierPipeBufSize"));
                        fcOpts.setPipeBufferSize(num);
                    }
                    if(cl.hasOption("FileCopierReadersPerPipe"))
                    {
                        int num = FileCopierOptions.parseSize(cl.getOptionValue("FileCopierReadersPerPipe"));
                        fcOpts.setNumReadersPerPipe(num);
                    }
                    if(cl.hasOption("FileCopierWritersPerPipe"))
                    {
                        int num = FileCopierOptions.parseSize(cl.getOptionValue("FileCopierWritersPerPipe"));
                        fcOpts.setNumWritersPerPipe(num);
                    }
                }
            }
        }
        return fcOpts;
    }

    protected static SkipComponentsOptions getSkipComponentsOptions(CommandLine cl)
        throws ParseException
    {
        if(cl.hasOption("excludeSearchIndex") && cl.hasOption("includeSearchIndex"))
            throw new ParseException("--includeSearchIndex and --excludeSearchIndex are mutually exclusive");
        if(cl.hasOption("excludeBlobs") && cl.hasOption("includeBlobs"))
            throw new ParseException("--includeBlobs and --excludeBlobs are mutually exclusive");
        if(cl.hasOption("excludeHsmBlobs") && cl.hasOption("includeHsmBlobs"))
            throw new ParseException("--includeHsmBlobs and --excludeHsmBlobs are mutually exclusive");
        SkipComponentsOptions skipOpts = new SkipComponentsOptions();
        if(cl.hasOption("excludeSearchIndex"))
            skipOpts.setSkipSearchIndex(true);
        else
        if(cl.hasOption("includeSearchIndex"))
            skipOpts.setSkipSearchIndex(false);
        if(cl.hasOption("excludeBlobs"))
            skipOpts.setSkipBlobs(true);
        else
        if(cl.hasOption("includeBlobs"))
            skipOpts.setSkipBlobs(false);
        if(cl.hasOption("excludeHsmBlobs"))
            skipOpts.setSkipHsmBlobs(true);
        else
        if(cl.hasOption("includeHsmBlobs"))
            skipOpts.setSkipHsmBlobs(false);
        return skipOpts;
    }

    protected static final String O_FILE_COPIER_OPTIONS = "FileCopier";
    protected static final String O_FILE_COPIER_METHOD = "FileCopierMethod";
    protected static final String O_FILE_COPIER_IOTYPE = "FileCopierIOType";
    protected static final String O_FILE_COPIER_OIO_COPY_BUFSIZE = "FileCopierOIOCopyBufSize";
    protected static final String O_FILE_COPIER_ASYNC_QUEUE_CAPACITY = "FileCopierAsyncQueueCapacity";
    protected static final String O_FILE_COPIER_PARALLEL_WORKERS = "FileCopierParallelWorkers";
    protected static final String O_FILE_COPIER_PIPES = "FileCopierPipes";
    protected static final String O_FILE_COPIER_PIPE_BUFSIZE = "FileCopierPipeBufSize";
    protected static final String O_FILE_COPIER_READERS_PER_PIPE = "FileCopierReadersPerPipe";
    protected static final String O_FILE_COPIER_WRITERS_PER_PIPE = "FileCopierWritersPerPipe";
    protected static final String O_INCLUDE_INDEX = "includeSearchIndex";
    protected static final String O_EXCLUDE_INDEX = "excludeSearchIndex";
    protected static final String O_INCLUDE_BLOBS = "includeBlobs";
    protected static final String O_EXCLUDE_BLOBS = "excludeBlobs";
    protected static final String O_INCLUDE_HSM_BLOBS = "includeHsmBlobs";
    protected static final String O_EXCLUDE_HSM_BLOBS = "excludeHsmBlobs";
}
