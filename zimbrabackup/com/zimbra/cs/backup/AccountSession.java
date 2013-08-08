// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   AccountSession.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.Volume;
import java.util.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupSet

public abstract class AccountSession
{
    protected static class VolumeInfo
    {

        public short getId()
        {
            return mId;
        }

        public boolean isSecondary()
        {
            return mSecondary;
        }

        public String getMailboxPath()
        {
            return mMailboxPath;
        }

        private short mId;
        private boolean mSecondary;
        private String mMailboxPath;

        public VolumeInfo(Volume vol, String mboxPath)
        {
            mId = vol.getId();
            mSecondary = vol.getType() == 2;
            mMailboxPath = mboxPath;
        }

        public VolumeInfo(short volId, String mboxPath, boolean isSecondary)
        {
            mId = volId;
            mMailboxPath = mboxPath;
            mSecondary = isSecondary;
        }

        public VolumeInfo()
        {
        }
    }


    protected AccountSession(BackupSet bak, String acctId, Log logger)
    {
        this.logger = logger;
        mBackupSet = bak;
        mAccountId = acctId;
        mVolumeInfo = new HashMap();
    }

    public BackupSet getBackupSet()
    {
        return mBackupSet;
    }

    public Account getAccount()
    {
        return mAccount;
    }

    public void setAccount(Account a)
    {
        mAccount = a;
    }

    public String getAccountId()
    {
        return mAccountId;
    }

    public String getAccountName()
    {
        return mAccountName;
    }

    public void setAccountName(String n)
    {
        mAccountName = n;
    }

    public boolean isAccountOnly()
    {
        return mAccountOnly;
    }

    public void setAccountOnly(boolean b)
    {
        mAccountOnly = b;
    }

    public long getStartTime()
    {
        return mStartTime;
    }

    public void setStartTime(long t)
    {
        mStartTime = t;
    }

    public long getEndTime()
    {
        return mEndTime;
    }

    public void setEndTime(long t)
    {
        mEndTime = t;
    }

    public Mailbox getMailbox()
    {
        return mMailbox;
    }

    public void setMailbox(Mailbox mbox)
    {
        mMailbox = mbox;
    }

    public int getMailboxId()
    {
        return mMailboxId;
    }

    public void setMailboxId(int id)
    {
        mMailboxId = id;
    }

    public long getRedoLogFileSequence()
    {
        return mRedoSequence;
    }

    public void setRedoLogFileSequence(long seq)
    {
        mRedoSequence = seq;
    }

    public String getServer()
    {
        return mServer;
    }

    public void setServer(String s)
    {
        mServer = s;
    }

    public boolean blobsZipped()
    {
        return mBlobsZipped;
    }

    public void setBlobsZipped(boolean b)
    {
        mBlobsZipped = b;
    }

    public boolean isBlobCompressedDeprecated()
    {
        return mBlobCompressedDeprecated;
    }

    public void setBlobCompressedDeprecated(boolean b)
    {
        mBlobCompressedDeprecated = b;
    }

    public VolumeInfo getVolumeInfo(String id)
    {
        return (VolumeInfo)mVolumeInfo.get(id);
    }

    public void addVolumeInfo(String id, VolumeInfo info)
    {
        mVolumeInfo.put(id, info);
    }

    public Iterator getVolumeInfoIterator()
    {
        return mVolumeInfo.values().iterator();
    }

    public final Throwable getError()
    {
        Object obj = mErrorGuard;
        JVM INSTR monitorenter ;
        return mError;
        Exception exception;
        exception;
        throw exception;
    }

    protected final void initError(Throwable err)
    {
        synchronized(mErrorGuard)
        {
            if(mError == null)
                mError = err;
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer("AccountSession: {");
        sb.append((new StringBuilder()).append(mBackupSet.toString()).append("}").toString());
        return sb.toString();
    }

    public void decodeMetadata(Element acctBackupElem)
        throws ServiceException
    {
        mAccountName = acctBackupElem.getAttribute("email");
        mMailboxId = (int)acctBackupElem.getAttributeLong("mailboxId");
        mStartTime = acctBackupElem.getAttributeLong("startTime");
        mEndTime = acctBackupElem.getAttributeLong("endTime");
        mRedoSequence = acctBackupElem.getAttributeLong("redoSeq");
        mServer = acctBackupElem.getAttribute("server");
        mBlobsZipped = acctBackupElem.getAttributeBool("blobsZipped", false);
        mBlobCompressedDeprecated = acctBackupElem.getAttributeBool("blobsCompressed", false);
        mAccountOnly = acctBackupElem.getAttributeBool("accountOnly", false);
        Element volumesElem = acctBackupElem.getOptionalElement("volumes");
        if(volumesElem != null)
        {
            mVolumeInfo = new HashMap();
            short volId;
            VolumeInfo volInfo;
            for(Iterator iter = volumesElem.elementIterator("volume"); iter.hasNext(); mVolumeInfo.put(Short.toString(volId), volInfo))
            {
                Element volElem = (Element)iter.next();
                volId = (short)(int)volElem.getAttributeLong("id");
                String path = volElem.getAttribute("path");
                boolean isSecondary = volElem.getAttributeBool("isSecondary", false);
                volInfo = new VolumeInfo(volId, path, isSecondary);
            }

        }
    }

    public static final int MAILBOX_ID_NONEXISTENT = -1;
    protected Log logger;
    private BackupSet mBackupSet;
    private Mailbox mMailbox;
    private Account mAccount;
    private String mAccountId;
    private final Object mErrorGuard = new Object();
    private Throwable mError;
    private int mMailboxId;
    private String mServer;
    private long mStartTime;
    private long mEndTime;
    private long mRedoSequence;
    private boolean mBlobCompressedDeprecated;
    private boolean mBlobsZipped;
    private String mAccountName;
    private Map mVolumeInfo;
    private boolean mAccountOnly;
}
