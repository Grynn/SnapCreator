// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupParams.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopierOptions;

public class BackupParams
{

    public BackupParams()
    {
        zip = false;
        zipStore = false;
        sync = false;
        redologs = false;
        skipDb = false;
        skipSearchIndex = false;
        skipBlobs = false;
        skipSecondaryBlobs = false;
        blobsSyncToken = 0;
        fcOpts = null;
    }

    public boolean zip;
    public boolean zipStore;
    public boolean sync;
    public boolean redologs;
    public boolean skipDb;
    public boolean skipSearchIndex;
    public boolean skipBlobs;
    public boolean skipSecondaryBlobs;
    public int blobsSyncToken;
    public FileCopierOptions fcOpts;
}
