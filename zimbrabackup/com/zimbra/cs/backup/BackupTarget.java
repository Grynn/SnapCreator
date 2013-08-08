// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupTarget.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import java.io.File;
import java.io.IOException;
import java.util.List;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupParams, BackupSet, BackupAccountSession, AccountSession, 
//            RestoreParams, BackupServiceException

public interface BackupTarget
{

    public abstract boolean isCustom();

    public abstract String getURI();

    public abstract BackupSet createFullBackupSet(String s, String s1, Account aaccount[], BackupParams backupparams)
        throws IOException, ServiceException;

    public abstract BackupSet createIncrementalBackupSet(String s, String s1, Account aaccount[], BackupParams backupparams)
        throws IOException, ServiceException;

    public abstract BackupSet getBackupSet()
        throws ServiceException;

    public abstract BackupSet getBackupSet(String s)
        throws ServiceException;

    public abstract List getBackupSets(long l, long l1)
        throws ServiceException;

    public abstract BackupAccountSession createFullBackup(BackupSet backupset, Account account)
        throws IOException, ServiceException;

    public abstract BackupAccountSession createIncrementalBackup(BackupSet backupset, Account account)
        throws IOException, ServiceException;

    public abstract AccountSession getAccountSession(String s)
        throws IOException, ServiceException;

    public abstract boolean hasAccountSession(String s);

    public abstract AccountSession getAccountSession(String s, String s1)
        throws IOException, ServiceException;

    public abstract List getBackupSets(String s, long l, long l1)
        throws ServiceException;

    public abstract void restore(String as[], String s, RestoreParams restoreparams)
        throws IOException, ServiceException;

    public abstract String[] getAccountIds(List list, String s, boolean flag)
        throws IOException, ServiceException;

    public abstract String[] getAccountIds()
        throws ServiceException;

    public abstract File[] getRedoLogFiles(long l, long l1)
        throws IOException;

    public abstract File[] getRedoLogFiles(long l)
        throws IOException;

    public abstract long getMostRecentRedoSequence()
        throws IOException;

    public abstract void deleteBackups(long l)
        throws IOException, ServiceException;

    public abstract boolean hasEnoughFreeSpace(String s);

    public abstract boolean outOfSpace();

    public abstract BackupServiceException makeOutOfSpaceException(IOException ioexception);
}
