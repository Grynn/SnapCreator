// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   RestoreParams.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.cs.store.file.Volume;
import java.util.SortedSet;
import java.util.TreeSet;

public class RestoreParams
{
    public static class Result
    {

        public boolean isRebuiltSchema()
        {
            return rebuiltSchema;
        }

        public void setRebuiltSchema(boolean rebuiltSchema)
        {
            this.rebuiltSchema = rebuiltSchema;
        }

        public boolean isResetRedoSequence()
        {
            return resetRedoSequence;
        }

        public void setResetRedoSequence(boolean resetRedoSequence)
        {
            this.resetRedoSequence = resetRedoSequence;
        }

        public SortedSet getFailedAccounts()
        {
            return failedAccounts;
        }

        public void addFailedAccount(String accountName)
        {
            if(failedAccounts == null)
                failedAccounts = new TreeSet();
            failedAccounts.add(accountName);
        }

        public void setStatus(String status)
        {
            this.status = status;
        }

        public String getStatus()
        {
            return status;
        }

        private String status;
        private boolean rebuiltSchema;
        private boolean resetRedoSequence;
        private SortedSet failedAccounts;

        public Result()
        {
            failedAccounts = null;
        }
    }


    public RestoreParams()
    {
        includeIncrementals = true;
        method = 0;
        offline = false;
        systemData = false;
        replayCurrentRedologs = true;
        continueOnError = false;
        skipDeletedAccounts = false;
        restoreToTime = 0xffffffffL;
        restoreToSequence = 0xffffffffL;
        ignoreRedoErrors = false;
        skipDb = false;
        skipSearchIndex = false;
        skipBlobs = false;
        skipSecondaryBlobs = false;
        skipDeleteOps = false;
        append = false;
        primaryBlobVolume = null;
        secondaryBlobVolume = null;
        indexVolume = null;
        fcOpts = null;
        mResult = new Result();
    }

    public Result getResult()
    {
        return mResult;
    }

    public static final int METHOD_RESTORE_MAILBOX = 0;
    public static final int METHOD_RESTORE_ACCOUNT = 1;
    public static final int METHOD_CREATE_ACCOUNT = 2;
    public String prefix;
    public boolean includeIncrementals;
    public int method;
    public boolean offline;
    public boolean systemData;
    public boolean replayCurrentRedologs;
    public boolean continueOnError;
    public boolean skipDeletedAccounts;
    public long restoreToTime;
    public long restoreToSequence;
    public String restoreToIncrementalLabel;
    public boolean ignoreRedoErrors;
    public boolean skipDb;
    public boolean skipSearchIndex;
    public boolean skipBlobs;
    public boolean skipSecondaryBlobs;
    public boolean skipDeleteOps;
    public boolean append;
    public Volume primaryBlobVolume;
    public Volume secondaryBlobVolume;
    public Volume indexVolume;
    public FileCopierOptions fcOpts;
    private Result mResult;
}
