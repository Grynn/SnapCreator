// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupSet.java

package com.zimbra.cs.backup;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.*;
import com.zimbra.cs.account.*;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.*;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.Zimbra;
import java.io.*;
import java.util.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupServiceException, BackupParams, BackupTarget, BackupAccountSession, 
//            BackupLC, BackupManager

public abstract class BackupSet
    implements Comparable
{
    private static class AccountMap
    {

        public void put(String email, String id)
        {
            mEmail2Id.put(email, id);
            mId2Email.put(id, email);
        }

        public int size()
        {
            return mEmail2Id.size();
        }

        public boolean hasId(String id)
        {
            return mId2Email.containsKey(id);
        }

        public boolean hasEmail(String email)
        {
            return mEmail2Id.containsKey(email);
        }

        public String getId(String email)
        {
            return (String)mEmail2Id.get(email);
        }

        public String getEmail(String id)
        {
            return (String)mId2Email.get(id);
        }

        public Map getEmail2IdMap()
        {
            return mEmail2Id;
        }

        public String[] getEmailsAsArray()
        {
            return (String[])mEmail2Id.keySet().toArray(new String[0]);
        }

        public String[] getIdsAsArray()
        {
            return (String[])mId2Email.keySet().toArray(new String[0]);
        }

        private Map mEmail2Id;
        private Map mId2Email;

        private AccountMap(int size)
        {
            mEmail2Id = new HashMap(size);
            mId2Email = new HashMap(size);
        }

    }

    public static class LiveBackupStatus
    {

        private synchronized void beginBackup(String email)
        {
            mInProgress.add(email);
        }

        private synchronized void endBackup(String email, boolean success)
        {
            mInProgress.remove(email);
            if(success)
                mNumCompletedAccts++;
            else
                mNumErrorAccts++;
        }

        public int getNumTotalAccts()
        {
            return mNumTotalAccts;
        }

        public synchronized int getNumInProgress()
        {
            return mInProgress.size();
        }

        public synchronized int getNumCompletedAccts()
        {
            return mNumCompletedAccts;
        }

        public synchronized int getNumErrorAccts()
        {
            return mNumErrorAccts;
        }

        public synchronized int getNumNotStartedAccts()
        {
            return mNumTotalAccts - mNumCompletedAccts - mNumErrorAccts - mInProgress.size();
        }

        public synchronized SortedSet getInProgressAccts()
        {
            SortedSet set = new TreeSet(mInProgress);
            return set;
        }

        private int mNumTotalAccts;
        private int mNumCompletedAccts;
        private int mNumErrorAccts;
        private SortedSet mInProgress;



        private LiveBackupStatus(int numTotal)
        {
            mNumTotalAccts = numTotal;
            mInProgress = new TreeSet();
        }

    }

    private static class Errors
    {

        public synchronized boolean hasErrors()
        {
            return !mSystemErrors.isEmpty() || !mAccountErrors.isEmpty();
        }

        public synchronized int getNumErrors()
        {
            return mSystemErrors.size() + mAccountErrors.size();
        }

        public synchronized List getAllErrors()
        {
            int num = getNumErrors();
            List copy = new ArrayList(num);
            copy.addAll(mSystemErrors);
            copy.addAll(mAccountErrors);
            return copy;
        }

        public synchronized List getSystemErrors()
        {
            List copy = new ArrayList(mSystemErrors);
            return copy;
        }

        public synchronized Map getAccountErrors()
        {
            Map errs = new HashMap(mAccountErrors.size());
            ErrorInfo err;
            String accountName;
            for(Iterator i$ = mAccountErrors.iterator(); i$.hasNext(); errs.put(accountName, err))
            {
                err = (ErrorInfo)i$.next();
                accountName = err.getAccountName();
            }

            return errs;
        }

        public synchronized ErrorInfo getFirstError()
        {
            if(!mSystemErrors.isEmpty())
                return (ErrorInfo)mSystemErrors.get(0);
            if(!mAccountErrors.isEmpty())
                return (ErrorInfo)mAccountErrors.get(0);
            else
                return null;
        }

        public synchronized boolean accountHasError(String acctName)
        {
            for(Iterator i$ = mAccountErrors.iterator(); i$.hasNext();)
            {
                ErrorInfo err = (ErrorInfo)i$.next();
                if(err.getAccountName().equalsIgnoreCase(acctName))
                    return true;
            }

            return false;
        }

        public synchronized void addSystemError(Throwable t)
        {
            ErrorInfo err = new ErrorInfo("system", t);
            mSystemErrors.add(err);
            checkOutOfDisk(t);
        }

        public synchronized void addSystemError(ErrorInfo ei)
        {
            mSystemErrors.add(ei);
            checkOutOfDisk(ei.getError());
        }

        public synchronized void addAccountError(ErrorInfo err)
        {
            mAccountErrors.add(err);
            checkOutOfDisk(err.getError());
        }

        private void checkOutOfDisk(Throwable t)
        {
            if(mOutOfDiskException == null && (t instanceof BackupServiceException))
            {
                BackupServiceException bse = (BackupServiceException)t;
                if("backup.OUT_OF_DISK".equals(bse.getCode()))
                    mOutOfDiskException = bse;
            }
        }

        public synchronized boolean outOfDisk()
        {
            return mOutOfDiskException != null;
        }

        public synchronized BackupServiceException getOutOfDiskException()
        {
            return mOutOfDiskException;
        }

        public static final String SYSTEM = "system";
        private List mSystemErrors;
        private List mAccountErrors;
        private BackupServiceException mOutOfDiskException;

        public Errors()
        {
            mSystemErrors = new ArrayList();
            mAccountErrors = new ArrayList();
        }
    }

    public static class ErrorInfo
    {

        public String getAccountName()
        {
            return mAccountName;
        }

        public String getMessage()
        {
            return mMessage;
        }

        public String getStacktrace()
        {
            return mStacktrace;
        }

        public Throwable getError()
        {
            return mError;
        }

        private String mAccountName;
        private String mMessage;
        private String mStacktrace;
        private Throwable mError;

        public ErrorInfo(String acctName, Throwable t)
        {
            mAccountName = acctName;
            mMessage = t.getMessage();
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            mStacktrace = sw.toString();
            mError = t;
        }

        public ErrorInfo(String acctName, String message, String stackTrace)
        {
            mAccountName = acctName;
            mMessage = message;
            mStacktrace = stackTrace;
        }

        public ErrorInfo()
        {
        }
    }


    public static String getTypeLabel(int type)
    {
        if(type == 1)
            return "full";
        if(type == 2)
            return "incremental";
        if(type == 3)
            return "mboxmove";
        else
            return "unknown";
    }

    public static int parseTypeLabel(String type)
    {
        if("full".equalsIgnoreCase(type))
            return 1;
        if("incremental".equalsIgnoreCase(type))
            return 2;
        return !"mboxmove".equalsIgnoreCase(type) ? 0 : 3;
    }

    protected BackupSet(String lbl, Log logger)
    {
        mType = 0;
        mMinRedoSeq = -1L;
        mMaxRedoSeq = -1L;
        mSharedBlobsZipNameDigestChars = DEFAULT_SHARED_BLOBS_ZIP_NAME_DIGEST_CHARS;
        mSharedBlobsDirDepth = DEFAULT_SHARED_BLOBS_DIR_DEPTH;
        mSharedBlobsCharsPerDir = DEFAULT_SHARED_BLOBS_CHARS_PER_DIR;
        mErrors = new Errors();
        this.logger = logger;
        mReadOnly = true;
        mLabel = lbl;
    }

    protected BackupSet(String lbl, String desc, Account accounts[], int type, BackupParams params, Log logger)
        throws ServiceException
    {
        mType = 0;
        mMinRedoSeq = -1L;
        mMaxRedoSeq = -1L;
        mSharedBlobsZipNameDigestChars = DEFAULT_SHARED_BLOBS_ZIP_NAME_DIGEST_CHARS;
        mSharedBlobsDirDepth = DEFAULT_SHARED_BLOBS_DIR_DEPTH;
        mSharedBlobsCharsPerDir = DEFAULT_SHARED_BLOBS_CHARS_PER_DIR;
        mErrors = new Errors();
        this.logger = logger;
        mReadOnly = false;
        mZCSRelease = BuildInfo.FULL_VERSION;
        mLabel = lbl;
        mType = type;
        mParams = params;
        mDescription = desc;
        if(!BackupLC.backup_debug_use_old_zip_format.booleanValue())
            mSharedBlobsZipped = params.zip;
        else
            mSharedBlobsZipped = false;
        mStartTime = BackupManager.getLabelDate(lbl);
        mAccounts = accounts;
        mAcctMap = new AccountMap(accounts.length);
        mAccountStatusMap = new TreeMap();
        Account arr$[] = accounts;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            Account acct = arr$[i$];
            String acctName = acct.getName().toLowerCase();
            mAcctMap.put(acctName, acct.getId());
            mAccountStatusMap.put(acctName, com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.NOTSTARTED);
        }

        mLiveBackupStatus = new LiveBackupStatus(accounts.length);
    }

    public BackupParams getParams()
    {
        return mParams;
    }

    public boolean isLive()
    {
        return !mReadOnly;
    }

    public String getDescription()
    {
        return mDescription;
    }

    public long getEndTime()
    {
        return mEndTime;
    }

    public String getLabel()
    {
        return mLabel;
    }

    public String getZCSRelease()
    {
        return mZCSRelease;
    }

    public int getType()
    {
        return mType;
    }

    public long getStartTime()
    {
        return mStartTime;
    }

    public long getMinRedoSequence()
    {
        return mMinRedoSeq;
    }

    protected void setMinRedoSequence(long seq)
    {
        mMinRedoSeq = seq;
    }

    public long getMaxRedoSequence()
    {
        return mMaxRedoSeq;
    }

    protected void setMaxRedoSequence(long seq)
    {
        mMaxRedoSeq = seq;
    }

    public boolean sharedBlobsZipped()
    {
        return mSharedBlobsZipped;
    }

    public int sharedBlobsZipNameDigestChars()
    {
        return mSharedBlobsZipNameDigestChars;
    }

    public int sharedBlobsDirectoryDepth()
    {
        return mSharedBlobsDirDepth;
    }

    public int sharedBlobsCharsPerDirectory()
    {
        return mSharedBlobsCharsPerDir;
    }

    public String[] getAccountIds()
    {
        if(mAcctMap != null)
            return mAcctMap.getIdsAsArray();
        else
            return new String[0];
    }

    public String[] getAccountNames()
    {
        if(mAcctMap != null)
            return mAcctMap.getEmailsAsArray();
        else
            return new String[0];
    }

    public boolean hasAccount(String acctName)
    {
        return mAcctMap != null && mAcctMap.hasEmail(acctName) && !mErrors.accountHasError(acctName);
    }

    public boolean hasAccountId(String acctId)
    {
        return mAcctMap != null && mAcctMap.hasId(acctId) && !mErrors.accountHasError(acctId);
    }

    public int getNumAccounts()
    {
        if(mAcctMap != null)
            return mAcctMap.size();
        else
            return 0;
    }

    public SortedMap getAccountStatusMap(com.zimbra.cs.service.backup.BackupService.AccountBackupStatus filter)
    {
        if(mAccountStatusMap == null)
            break MISSING_BLOCK_LABEL_137;
        SortedMap sortedmap = mAccountStatusMap;
        JVM INSTR monitorenter ;
        if(com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.ALL.equals(filter))
        {
            SortedMap copy = new TreeMap(mAccountStatusMap);
            return copy;
        }
        SortedMap subset;
        subset = new TreeMap();
        Iterator iter = mAccountStatusMap.entrySet().iterator();
        do
        {
            if(!iter.hasNext())
                break;
            java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
            com.zimbra.cs.service.backup.BackupService.AccountBackupStatus status = (com.zimbra.cs.service.backup.BackupService.AccountBackupStatus)entry.getValue();
            if(status.equals(filter))
                subset.put(entry.getKey(), status);
        } while(true);
        subset;
        sortedmap;
        JVM INSTR monitorexit ;
        return;
        Exception exception;
        exception;
        throw exception;
        return null;
    }

    public com.zimbra.cs.service.backup.BackupService.AccountBackupStatus getAccountStatus(String email)
    {
        SortedMap sortedmap = mAccountStatusMap;
        JVM INSTR monitorenter ;
        return (com.zimbra.cs.service.backup.BackupService.AccountBackupStatus)mAccountStatusMap.get(email);
        Exception exception;
        exception;
        throw exception;
    }

    public Map getAccountNameIdMap()
    {
        return mAcctMap == null ? null : mAcctMap.getEmail2IdMap();
    }

    public void startFullBackup()
        throws ServiceException, IOException
    {
        BackupTarget target;
        int every;
        int i;
        if(mReadOnly || getType() != 1)
            throw new IllegalStateException();
        logger.info((new StringBuilder()).append("Full backup started for backup set; label: ").append(getLabel()).toString());
        target = getBackupTarget();
        storeSystemData();
        every = BackupLC.backup_progress_report_threshold.intValue();
        i = 0;
_L3:
        if(i >= mAccounts.length) goto _L2; else goto _L1
_L1:
        Throwable err;
        BackupAccountSession acctBak;
        String acctName;
        err = null;
        acctBak = null;
        acctName = mAccounts[i].getName().toLowerCase();
        if(!isAborted())
            break MISSING_BLOCK_LABEL_135;
        if(!hasErrorOccurred())
            addError(acctName, BackupServiceException.ABORTED_BY_COMMAND());
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break; /* Loop/switch isn't completed */
        ZimbraLog.addToContext("name", acctName);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(mAccounts[i], true);
        ZimbraLog.addToContext("mid", Integer.toString(mbox.getId()));
        Account account;
        account = Provisioning.getInstance().get(com.zimbra.cs.account.Provisioning.AccountBy.id, mAccounts[i].getId());
        if(account != null)
            break MISSING_BLOCK_LABEL_242;
        synchronized(mAccountStatusMap)
        {
            mAccountStatusMap.remove(acctName);
        }
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        continue; /* Loop/switch isn't completed */
        acctBak = target.createFullBackup(this, account);
        break MISSING_BLOCK_LABEL_287;
        BackupServiceException e;
        e;
        if(e.getCode().equals("backup.ACCOUNT_BACKUP_EXISTS"))
        {
            ZimbraLog.removeFromContext("name");
            ZimbraLog.removeFromContext("mid");
            continue; /* Loop/switch isn't completed */
        }
        throw e;
        synchronized(mAccountStatusMap)
        {
            mAccountStatusMap.put(acctName, com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.INPROGRESS);
        }
        mLiveBackupStatus.beginBackup(acctName);
        acctBak.startFullBackup(mParams);
        acctBak.endFullBackup();
        break MISSING_BLOCK_LABEL_360;
        Exception exception2;
        exception2;
        acctBak.endFullBackup();
        throw exception2;
        err = acctBak.getError();
        if(err != null)
            logger.error((new StringBuilder()).append("Error while backing up account ").append(acctName).append(": ").append(err.getMessage()).toString(), err);
        long seq = acctBak.getRedoLogFileSequence();
        if(mMinRedoSeq < 0L || seq < mMinRedoSeq)
            mMinRedoSeq = seq;
        if(seq > mMaxRedoSeq)
            mMaxRedoSeq = seq;
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_649;
        AccountServiceException e;
        e;
        if(!e.getCode().equals("account.NO_SUCH_ACCOUNT"))
            break MISSING_BLOCK_LABEL_537;
        synchronized(mAccountStatusMap)
        {
            mAccountStatusMap.remove(acctName);
        }
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        continue; /* Loop/switch isn't completed */
        err = e;
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_649;
        e;
        if(FileUtil.isOutOfDiskError(e) || getBackupTarget().outOfSpace())
        {
            err = getBackupTarget().makeOutOfSpaceException(e);
            addError(err);
        } else
        {
            err = e;
        }
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_649;
        e;
        err = e;
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_649;
        Exception exception4;
        exception4;
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        throw exception4;
        if(err != null)
        {
            addError(acctName, err);
            synchronized(mAccountStatusMap)
            {
                mAccountStatusMap.put(acctName, com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.ERROR);
            }
            mLiveBackupStatus.endBackup(acctName, false);
        } else
        {
            synchronized(mAccountStatusMap)
            {
                mAccountStatusMap.put(acctName, com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.COMPLETED);
            }
            mLiveBackupStatus.endBackup(acctName, true);
        }
        if(every > 0)
        {
            int howmany = i + 1;
            if(howmany % every == 0 || howmany == mAccounts.length)
                logger.info((new StringBuilder()).append("Finished backing up ").append(howmany).append(" of ").append(mAccounts.length).append(" accounts").toString());
        }
        if(hadOutOfDiskError())
            break; /* Loop/switch isn't completed */
        i++;
          goto _L3
_L2:
        if(!hadOutOfDiskError() && mParams.redologs && RedoConfig.redoLogEnabled())
            try
            {
                RedoLogProvider.getInstance().getRedoLogManager().forceRollover();
                backupRedoLogs();
            }
            catch(Exception e)
            {
                if((e instanceof IOException) && (FileUtil.isOutOfDiskError((IOException)e) || getBackupTarget().outOfSpace()))
                    addError(getBackupTarget().makeOutOfSpaceException((IOException)e));
                else
                    addError(e);
            }
        return;
    }

    public void endFullBackup()
        throws IOException, ServiceException
    {
        if(mReadOnly || getType() != 1)
        {
            throw new IllegalStateException();
        } else
        {
            mEndTime = System.currentTimeMillis();
            logger.info((new StringBuilder()).append("Full backup finished for backup set; label: ").append(getLabel()).toString());
            return;
        }
    }

    public synchronized void abortFullBackup()
        throws ServiceException
    {
        if(getType() != 1)
            throw ServiceException.INVALID_REQUEST("The backup set is not a full backup", null);
        if(mReadOnly)
        {
            throw BackupServiceException.CANNOT_ABORT_COMPLETED_BACKUP();
        } else
        {
            mAborted = true;
            return;
        }
    }

    public synchronized boolean isAborted()
    {
        return mAborted;
    }

    public abstract BackupTarget getBackupTarget();

    public String getAccountId(String name)
    {
        if(mAcctMap != null)
            return mAcctMap.getId(name);
        else
            return null;
    }

    public int getNumErrors()
    {
        return mErrors.getNumErrors();
    }

    public List getAllErrors()
    {
        return mErrors.getAllErrors();
    }

    public List getSystemErrors()
    {
        return mErrors.getSystemErrors();
    }

    public Map getAccountErrors()
    {
        return mErrors.getAccountErrors();
    }

    public boolean hasErrorOccurred()
    {
        return mErrors.hasErrors();
    }

    static void raiseError(Throwable err)
        throws IOException, ServiceException
    {
        if(err != null)
        {
            if(err instanceof IOException)
                throw (IOException)err;
            if(err instanceof ServiceException)
                throw (ServiceException)err;
            else
                throw ServiceException.FAILURE(err.getMessage(), err);
        } else
        {
            return;
        }
    }

    void raiseError()
        throws IOException, ServiceException
    {
        ErrorInfo err = mErrors.getFirstError();
        if(err != null)
            raiseError(err.getError());
    }

    public abstract void storeSystemData()
        throws ServiceException, IOException;

    public abstract void loadSystemData()
        throws ServiceException, IOException;

    public int compareTo(BackupSet that)
    {
        long diff = getStartTime() - that.getStartTime();
        int sgn;
        if(diff < 0L)
            sgn = -1;
        else
        if(diff == 0L)
            sgn = 0;
        else
            sgn = 1;
        return sgn;
    }

    public void decodeMetadata(Element backupSetElem)
        throws ServiceException
    {
        mZCSRelease = backupSetElem.getAttribute("zcsRelease", null);
        mStartTime = backupSetElem.getAttributeLong("startTime");
        mEndTime = backupSetElem.getAttributeLong("endTime");
        mMinRedoSeq = backupSetElem.getAttributeLong("minRedoSeq");
        mMaxRedoSeq = backupSetElem.getAttributeLong("maxRedoSeq");
        mType = parseTypeLabel(backupSetElem.getAttribute("type"));
        mAborted = backupSetElem.getAttributeBool("aborted", false);
        mSharedBlobsZipped = backupSetElem.getAttributeBool("sharedBlobsZipped", false);
        mSharedBlobsZipNameDigestChars = (int)backupSetElem.getAttributeLong("sharedBlobsZipNameDigestChars", 1L);
        mSharedBlobsDirDepth = (int)backupSetElem.getAttributeLong("sharedBlobsDirectoryDepth", 1L);
        mSharedBlobsCharsPerDir = (int)backupSetElem.getAttributeLong("sharedBlobsCharsPerDirectory", 1L);
        Element descElem = backupSetElem.getOptionalElement("desc");
        if(descElem != null)
            mDescription = descElem.getText();
        mAcctMap = new AccountMap(100);
        mAccountStatusMap = new TreeMap();
        Element accountsElem = backupSetElem.getOptionalElement("accounts");
        if(accountsElem != null)
        {
            String email;
            com.zimbra.cs.service.backup.BackupService.AccountBackupStatus status;
            for(Iterator iter = accountsElem.elementIterator("account"); iter.hasNext(); mAccountStatusMap.put(email, status))
            {
                Element acctElem = (Element)iter.next();
                String zimbraId = acctElem.getAttribute("zimbraId");
                email = acctElem.getAttribute("email");
                mAcctMap.put(email, zimbraId);
                String statusStr = acctElem.getAttribute("status");
                status = BackupService.lookupAccountBackupStatus(statusStr);
            }

        }
        Element errorsElem = backupSetElem.getOptionalElement("errors");
        if(errorsElem != null)
        {
            for(Iterator iter = errorsElem.elementIterator("error"); iter.hasNext();)
            {
                Element errorElem = (Element)iter.next();
                String email = errorElem.getAttribute("email");
                String message = null;
                Element messageElem = errorElem.getOptionalElement("message");
                if(messageElem != null)
                    message = messageElem.getText();
                String stackTrace = null;
                Element stackTraceElem = errorElem.getOptionalElement("stackTrace");
                if(stackTraceElem != null)
                    stackTrace = stackTraceElem.getText();
                ErrorInfo ei = new ErrorInfo(email, message, stackTrace);
                if("system".equals(email))
                    mErrors.addSystemError(ei);
                else
                    mErrors.addAccountError(ei);
            }

        }
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("BackupSet: {");
        sb.append((new StringBuilder()).append("label: ").append(mLabel).append("}").toString());
        return sb.toString();
    }

    public void startIncrementalBackup()
        throws IOException, ServiceException
    {
        int i;
        if(mType != 2)
            throw new IllegalStateException();
        logger.info((new StringBuilder()).append("Incremental backup started for backup set; label: ").append(getLabel()).toString());
        storeSystemData();
        i = 0;
_L3:
        if(i >= mAccounts.length) goto _L2; else goto _L1
_L1:
        Throwable err;
        String acctName;
        err = null;
        acctName = mAccounts[i].getName().toLowerCase();
        Account account;
        ZimbraLog.addToContext("name", acctName);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(mAccounts[i], false);
        if(mbox != null)
            ZimbraLog.addToContext("mid", Integer.toString(mbox.getId()));
        account = Provisioning.getInstance().get(com.zimbra.cs.account.Provisioning.AccountBy.id, mAccounts[i].getId());
        if(account != null)
            break MISSING_BLOCK_LABEL_185;
        synchronized(mAccountStatusMap)
        {
            mAccountStatusMap.remove(acctName);
        }
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        continue; /* Loop/switch isn't completed */
        BackupAccountSession incrAcctBak = getBackupTarget().createIncrementalBackup(this, account);
        if(incrAcctBak != null)
        {
            synchronized(mAccountStatusMap)
            {
                mAccountStatusMap.put(acctName, com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.INPROGRESS);
            }
            mLiveBackupStatus.beginBackup(acctName);
            try
            {
                incrAcctBak.incrementalBackup();
            }
            catch(OutOfMemoryError e)
            {
                Zimbra.halt("out of memory", e);
            }
            catch(Throwable t)
            {
                incrAcctBak.initError(t);
            }
            err = incrAcctBak.getError();
            if(err != null)
                logger.error((new StringBuilder()).append("Error while backing up account ").append(acctName).append(": ").append(err.getMessage()).toString(), err);
        }
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_507;
        AccountServiceException e;
        e;
        if(!e.getCode().equals("account.NO_SUCH_ACCOUNT"))
            break MISSING_BLOCK_LABEL_400;
        synchronized(mAccountStatusMap)
        {
            mAccountStatusMap.remove(acctName);
        }
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        continue; /* Loop/switch isn't completed */
        err = e;
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_507;
        e;
        if(FileUtil.isOutOfDiskError(e) || getBackupTarget().outOfSpace())
        {
            err = getBackupTarget().makeOutOfSpaceException(e);
            addError(err);
        } else
        {
            err = e;
        }
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_507;
        e;
        err = e;
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        break MISSING_BLOCK_LABEL_507;
        Exception exception3;
        exception3;
        ZimbraLog.removeFromContext("name");
        ZimbraLog.removeFromContext("mid");
        throw exception3;
        if(err != null)
        {
            addError(acctName, err);
            synchronized(mAccountStatusMap)
            {
                mAccountStatusMap.put(acctName, com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.ERROR);
            }
            mLiveBackupStatus.endBackup(acctName, false);
        } else
        {
            synchronized(mAccountStatusMap)
            {
                mAccountStatusMap.put(acctName, com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.COMPLETED);
            }
            mLiveBackupStatus.endBackup(acctName, true);
        }
        if(hadOutOfDiskError())
            break; /* Loop/switch isn't completed */
        i++;
          goto _L3
_L2:
        if(!hadOutOfDiskError())
            try
            {
                RedoLogProvider.getInstance().getRedoLogManager().forceRollover();
                backupRedoLogs();
            }
            catch(Exception e)
            {
                if((e instanceof IOException) && (FileUtil.isOutOfDiskError((IOException)e) || getBackupTarget().outOfSpace()))
                    addError(getBackupTarget().makeOutOfSpaceException((IOException)e));
                else
                    addError(e);
            }
        return;
    }

    protected abstract void backupRedoLogs()
        throws IOException, ServiceException;

    public void endIncrementalBackup()
        throws IOException, ServiceException
    {
        if(mReadOnly)
        {
            throw new IllegalStateException();
        } else
        {
            mEndTime = System.currentTimeMillis();
            logger.info((new StringBuilder()).append("Incremental backup finished for backup set; label: ").append(getLabel()).toString());
            return;
        }
    }

    protected void addError(String accountName, Throwable t)
    {
        mErrors.addAccountError(new ErrorInfo(accountName, t));
    }

    protected void addError(Throwable t)
    {
        mErrors.addSystemError(t);
    }

    protected boolean hadOutOfDiskError()
    {
        return mErrors.outOfDisk();
    }

    protected void setStartTime(long t)
    {
        mStartTime = t;
    }

    public abstract List getStats();

    public LiveBackupStatus getLiveBackupStatus()
    {
        return mLiveBackupStatus;
    }

    public volatile int compareTo(Object x0)
    {
        return compareTo((BackupSet)x0);
    }

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_FULL = 1;
    public static final int TYPE_INCREMENTAL = 2;
    public static final int TYPE_MBOXMOVE = 3;
    public static final String TYPE_LABEL_UNKNOWN = "unknown";
    public static final String TYPE_LABEL_FULL = "full";
    public static final String TYPE_LABEL_INCREMENTAL = "incremental";
    public static final String TYPE_LABEL_MBOXMOVE = "mboxmove";
    private static final int DEFAULT_SHARED_BLOBS_ZIP_NAME_DIGEST_CHARS;
    private static final int DEFAULT_SHARED_BLOBS_DIR_DEPTH;
    private static final int DEFAULT_SHARED_BLOBS_CHARS_PER_DIR;
    protected Log logger;
    private String mZCSRelease;
    private String mLabel;
    private int mType;
    private BackupParams mParams;
    private String mDescription;
    private long mStartTime;
    private long mEndTime;
    private long mMinRedoSeq;
    private long mMaxRedoSeq;
    private boolean mSharedBlobsZipped;
    private int mSharedBlobsZipNameDigestChars;
    private int mSharedBlobsDirDepth;
    private int mSharedBlobsCharsPerDir;
    private Errors mErrors;
    private AccountMap mAcctMap;
    private Account mAccounts[];
    private final boolean mReadOnly;
    private boolean mAborted;
    private SortedMap mAccountStatusMap;
    private LiveBackupStatus mLiveBackupStatus;

    static 
    {
        DEFAULT_SHARED_BLOBS_ZIP_NAME_DIGEST_CHARS = BackupLC.backup_shared_blobs_zip_name_digest_chars.intValueWithinRange(1, 4);
        DEFAULT_SHARED_BLOBS_DIR_DEPTH = BackupLC.backup_shared_blobs_dir_depth.intValueWithinRange(1, 28);
        DEFAULT_SHARED_BLOBS_CHARS_PER_DIR = BackupLC.backup_shared_blobs_chars_per_dir.intValueWithinRange(1, 28);
    }
}
