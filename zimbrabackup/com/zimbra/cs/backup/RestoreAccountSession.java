// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   RestoreAccountSession.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.*;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.backup.util.Utils;
import com.zimbra.cs.db.*;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.redolog.*;
import com.zimbra.cs.redolog.logger.FileHeader;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.util.Zimbra;
import java.io.File;
import java.io.IOException;
import java.util.*;

// Referenced classes of package com.zimbra.cs.backup:
//            AccountSession, RestoreParams, BackupTarget, BlobRepository, 
//            BackupSet, XmlMeta

public abstract class RestoreAccountSession extends AccountSession
{
    private static class StringReplacement
        implements com.zimbra.cs.backup.util.Utils.Replacement
    {

        public String getValue(String oldVal)
        {
            return mNewVal;
        }

        private String mNewVal;

        StringReplacement(String newVal)
        {
            mNewVal = newVal;
        }
    }


    protected RestoreAccountSession(BackupSet bak, String acctId, Log logger)
    {
        super(bak, acctId, logger);
        mTargetMailboxId = -1;
        mRestoreTargetMboxIdSet = false;
        mFullRestoreDone = false;
    }

    protected void setTargetAccount(Account account)
    {
        mTargetAccount = account;
    }

    protected Account getTargetAccount()
    {
        return mTargetAccount;
    }

    protected void setTargetMailboxId(int id)
    {
        mTargetMailboxId = id;
    }

    protected int getTargetMailboxId()
    {
        int id = mTargetMailboxId != -1 ? mTargetMailboxId : getMailboxId();
        return id;
    }

    protected Volume getVolume()
    {
        return mRestoreParams.primaryBlobVolume;
    }

    protected Volume getSecondaryVolume()
    {
        return mRestoreParams.secondaryBlobVolume;
    }

    public void startRestore(RestoreParams params)
        throws ServiceException
    {
        com.zimbra.cs.mailbox.MailboxManager.MailboxLock lock;
        String acctId;
        mRestoreParams = params;
        if(mRestoreParams.append && mRestoreParams.method != 0)
            throw ServiceException.INVALID_REQUEST("Append mode restore cannot be combined with account object restore", null);
        if(mRestoreParams.primaryBlobVolume == null)
            mRestoreParams.primaryBlobVolume = Volume.getCurrentMessageVolume();
        if(mRestoreParams.secondaryBlobVolume == null)
        {
            mRestoreParams.secondaryBlobVolume = Volume.getCurrentSecondaryMessageVolume();
            if(mRestoreParams.secondaryBlobVolume == null)
                mRestoreParams.secondaryBlobVolume = mRestoreParams.primaryBlobVolume;
        }
        if(mRestoreParams.indexVolume == null)
            mRestoreParams.indexVolume = Volume.getCurrentIndexVolume();
        lock = null;
        acctId = null;
        int restoreMethod;
        int mboxid = getMailboxId();
        logger.info((new StringBuilder()).append("Restore started for account ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(mboxid == -1 ? "" : (new StringBuilder()).append(" original mailbox id ").append(mboxid).toString()).toString());
        mDbConn = DbPool.getConnection();
        mDbConn.setTransactionIsolation(4);
        loadAccount(false, !mRestoreParams.includeIncrementals);
        acctId = getAccountId();
        restoreMethod = mRestoreParams.method;
        if(mboxid != -1)
            break MISSING_BLOCK_LABEL_355;
        logger.info((new StringBuilder()).append("Mailbox did not exist at backup for account ").append(getAccountName()).toString());
        if(restoreMethod != 2)
        {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(acctId, false);
            IMPersona.deleteIMPersona(getAccountName());
            if(mbox != null)
                mbox.deleteMailbox();
        }
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        return;
        if(getAccount() != null)
            break MISSING_BLOCK_LABEL_428;
        logger.warn((new StringBuilder()).append("Not restoring because account ").append(getAccountName()).append(" did not exist or has been deleted").toString());
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        return;
        if(restoreMethod != 2)
            break MISSING_BLOCK_LABEL_515;
        if(mTargetAccount != null)
            break MISSING_BLOCK_LABEL_507;
        logger.warn((new StringBuilder()).append("Not restoring account ").append(getAccountName()).append("; no restore target account set").toString());
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        return;
        acctId = mTargetAccount.getId();
        boolean dbSuccess = false;
        lock = prepareTargetMailbox(restoreMethod == 2);
        if(!mRestoreParams.skipDb)
        {
            loadTables();
            if(restoreMethod != 2)
                DbBackup.updateMailboxBackupTime(mDbConn, getTargetMailboxId(), (int)(getStartTime() / 1000L));
            else
                DbBackup.updateMailboxBackupTime(mDbConn, getTargetMailboxId(), -1);
        }
        dbSuccess = true;
        if(dbSuccess)
            mDbConn.commit();
        else
            DbPool.quietRollback(mDbConn);
        break MISSING_BLOCK_LABEL_644;
        Exception exception;
        exception;
        if(dbSuccess)
            mDbConn.commit();
        else
            DbPool.quietRollback(mDbConn);
        throw exception;
        if(!mRestoreParams.skipBlobs)
            loadBlobs();
        if(!mRestoreParams.skipSearchIndex)
            loadIndex();
        mFullRestoreDone = true;
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        break MISSING_BLOCK_LABEL_930;
        MailServiceException e;
        e;
        if(e.getCode().equals("mail.WRONG_MAILBOX"))
            initError(ServiceException.FAILURE((new StringBuilder()).append("Account and mailbox IDs mismatch: account id=").append(acctId).append(" mailbox id=").append(getTargetMailboxId()).toString(), e));
        else
            initError(e);
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        break MISSING_BLOCK_LABEL_930;
        e;
        Zimbra.halt("out of memory", e);
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        break MISSING_BLOCK_LABEL_930;
        e;
        initError(e);
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        break MISSING_BLOCK_LABEL_930;
        Exception exception1;
        exception1;
        DbPool.quietClose(mDbConn);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, true);
        throw exception1;
    }

    public void endRestore()
        throws IOException, ServiceException
    {
        boolean redoReplayDone = false;
        if(mFullRestoreDone && getError() == null && mRestoreParams.includeIncrementals && getMailboxId() != -1)
        {
            logger.info("Run redo operations since last full backup");
            RedoLogManager redoMgr = RedoLogProvider.getInstance().getRedoLogManager();
            long fullbackupTime = getStartTime();
            BackupTarget target = getBackupSet().getBackupTarget();
            long seqFirst = getRedoLogFileSequence();
            File redologFiles[] = target.getRedoLogFiles(seqFirst, mRestoreParams.restoreToSequence);
            boolean notPastTimeLimit = true;
            if(redologFiles != null && redologFiles.length > 0)
            {
                checkRedoLogSequenceContinuity(redologFiles, seqFirst);
                notPastTimeLimit = playBackRedoOps(redologFiles, fullbackupTime);
            }
            if(notPastTimeLimit && mRestoreParams.replayCurrentRedologs && !mConflictingMailboxId)
            {
                long lastSeq = -1L;
                if(redologFiles.length > 0)
                {
                    FileLogReader r = new FileLogReader(redologFiles[redologFiles.length - 1]);
                    lastSeq = r.getHeader().getSequence();
                }
                logger.info((new StringBuilder()).append("Run redo operations from archived redo logs starting at sequence ").append(lastSeq).toString());
                File archFiles[] = redoMgr.getArchivedLogsFromSequence(lastSeq + 1L);
                if(mRestoreParams.offline)
                {
                    File a[] = new File[archFiles.length + 1];
                    System.arraycopy(archFiles, 0, a, 0, archFiles.length);
                    a[archFiles.length] = redoMgr.getLogFile();
                    archFiles = a;
                }
                if(archFiles.length > 0)
                {
                    boolean doit = true;
                    if(mRestoreParams.restoreToSequence < 0xffffffffL)
                    {
                        FileLogReader r = new FileLogReader(archFiles[0]);
                        long nextSeq = r.getHeader().getSequence();
                        doit = nextSeq == lastSeq + 1L;
                    }
                    if(doit)
                    {
                        File newLogs[] = checkRedoLogSequenceContinuity(archFiles, lastSeq);
                        if(newLogs != null)
                            playBackRedoOps(newLogs, fullbackupTime);
                    }
                }
            }
            redoReplayDone = true;
        } else
        {
            redoReplayDone = true;
        }
        if(getError() == null)
        {
            int oid = getMailboxId();
            int tid = getTargetMailboxId();
            logger.info((new StringBuilder()).append("Restore finished for ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(oid == -1 ? "" : (new StringBuilder()).append(" original mailbox ").append(oid).append(" to mailbox ").append(tid).toString()).toString());
        }
        if(!mFullRestoreDone || !redoReplayDone)
        {
            if(mRestoreTargetMboxIdSet)
            {
                int mid = getTargetMailboxId();
                Mailbox mbox = null;
                try
                {
                    mbox = MailboxManager.getInstance().getMailboxById(mid, true);
                }
                catch(ServiceException e)
                {
                    if(!"mail.NO_SUCH_MBOX".equals(e.getCode()))
                        logger.warn((new StringBuilder()).append("Error getting mailbox to delete (mid=").append(mid).append(")").toString(), e);
                }
                if(mbox != null && mbox.getAccountId().equalsIgnoreCase(getAccountId()))
                    try
                    {
                        logger.info((new StringBuilder()).append("Cleaning up data from failed restore (mid=").append(mid).append(")").toString());
                        mbox.deleteMailbox();
                    }
                    catch(Throwable t)
                    {
                        logger.warn((new StringBuilder()).append("Error during mailbox cleanup after failed restore (mid=").append(mid).append(")").toString(), t);
                    }
            }
            if(mTargetAccount != null && mRestoreParams.method == 2)
            {
                String acctId = mTargetAccount.getId();
                logger.info((new StringBuilder()).append("Cleaning up account created during failed restore (account=").append(acctId).append(")").toString());
                try
                {
                    Provisioning.getInstance().deleteAccount(acctId);
                }
                catch(Throwable t)
                {
                    logger.warn((new StringBuilder()).append("Error during account cleanup after failed restore (account=").append(acctId).append(")").toString(), t);
                }
            }
        }
        break MISSING_BLOCK_LABEL_1929;
        OutOfMemoryError e;
        e;
        Zimbra.halt("out of memory", e);
        if(getError() == null)
        {
            int oid = getMailboxId();
            int tid = getTargetMailboxId();
            logger.info((new StringBuilder()).append("Restore finished for ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(oid == -1 ? "" : (new StringBuilder()).append(" original mailbox ").append(oid).append(" to mailbox ").append(tid).toString()).toString());
        }
        if(!mFullRestoreDone || !redoReplayDone)
        {
            if(mRestoreTargetMboxIdSet)
            {
                int mid = getTargetMailboxId();
                Mailbox mbox = null;
                try
                {
                    mbox = MailboxManager.getInstance().getMailboxById(mid, true);
                }
                catch(ServiceException e)
                {
                    if(!"mail.NO_SUCH_MBOX".equals(e.getCode()))
                        logger.warn((new StringBuilder()).append("Error getting mailbox to delete (mid=").append(mid).append(")").toString(), e);
                }
                if(mbox != null && mbox.getAccountId().equalsIgnoreCase(getAccountId()))
                    try
                    {
                        logger.info((new StringBuilder()).append("Cleaning up data from failed restore (mid=").append(mid).append(")").toString());
                        mbox.deleteMailbox();
                    }
                    catch(Throwable t)
                    {
                        logger.warn((new StringBuilder()).append("Error during mailbox cleanup after failed restore (mid=").append(mid).append(")").toString(), t);
                    }
            }
            if(mTargetAccount != null && mRestoreParams.method == 2)
            {
                String acctId = mTargetAccount.getId();
                logger.info((new StringBuilder()).append("Cleaning up account created during failed restore (account=").append(acctId).append(")").toString());
                try
                {
                    Provisioning.getInstance().deleteAccount(acctId);
                }
                catch(Throwable t)
                {
                    logger.warn((new StringBuilder()).append("Error during account cleanup after failed restore (account=").append(acctId).append(")").toString(), t);
                }
            }
        }
        break MISSING_BLOCK_LABEL_1929;
        acctId;
        initError(acctId);
        if(getError() == null)
        {
            int oid = getMailboxId();
            int tid = getTargetMailboxId();
            logger.info((new StringBuilder()).append("Restore finished for ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(oid == -1 ? "" : (new StringBuilder()).append(" original mailbox ").append(oid).append(" to mailbox ").append(tid).toString()).toString());
        }
        if(!mFullRestoreDone || !redoReplayDone)
        {
            if(mRestoreTargetMboxIdSet)
            {
                int mid = getTargetMailboxId();
                Mailbox mbox = null;
                try
                {
                    mbox = MailboxManager.getInstance().getMailboxById(mid, true);
                }
                catch(ServiceException e)
                {
                    if(!"mail.NO_SUCH_MBOX".equals(e.getCode()))
                        logger.warn((new StringBuilder()).append("Error getting mailbox to delete (mid=").append(mid).append(")").toString(), e);
                }
                if(mbox != null && mbox.getAccountId().equalsIgnoreCase(getAccountId()))
                    try
                    {
                        logger.info((new StringBuilder()).append("Cleaning up data from failed restore (mid=").append(mid).append(")").toString());
                        mbox.deleteMailbox();
                    }
                    catch(Throwable t)
                    {
                        logger.warn((new StringBuilder()).append("Error during mailbox cleanup after failed restore (mid=").append(mid).append(")").toString(), t);
                    }
            }
            if(mTargetAccount != null && mRestoreParams.method == 2)
            {
                String acctId = mTargetAccount.getId();
                logger.info((new StringBuilder()).append("Cleaning up account created during failed restore (account=").append(acctId).append(")").toString());
                try
                {
                    Provisioning.getInstance().deleteAccount(acctId);
                }
                catch(Throwable t)
                {
                    logger.warn((new StringBuilder()).append("Error during account cleanup after failed restore (account=").append(acctId).append(")").toString(), t);
                }
            }
        }
        break MISSING_BLOCK_LABEL_1929;
        Exception exception;
        exception;
        if(getError() == null)
        {
            int oid = getMailboxId();
            int tid = getTargetMailboxId();
            logger.info((new StringBuilder()).append("Restore finished for ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(oid == -1 ? "" : (new StringBuilder()).append(" original mailbox ").append(oid).append(" to mailbox ").append(tid).toString()).toString());
        }
        if(!mFullRestoreDone || !redoReplayDone)
        {
            if(mRestoreTargetMboxIdSet)
            {
                int mid = getTargetMailboxId();
                Mailbox mbox = null;
                try
                {
                    mbox = MailboxManager.getInstance().getMailboxById(mid, true);
                }
                catch(ServiceException e)
                {
                    if(!"mail.NO_SUCH_MBOX".equals(e.getCode()))
                        logger.warn((new StringBuilder()).append("Error getting mailbox to delete (mid=").append(mid).append(")").toString(), e);
                }
                if(mbox != null && mbox.getAccountId().equalsIgnoreCase(getAccountId()))
                    try
                    {
                        logger.info((new StringBuilder()).append("Cleaning up data from failed restore (mid=").append(mid).append(")").toString());
                        mbox.deleteMailbox();
                    }
                    catch(Throwable t)
                    {
                        logger.warn((new StringBuilder()).append("Error during mailbox cleanup after failed restore (mid=").append(mid).append(")").toString(), t);
                    }
            }
            if(mTargetAccount != null && mRestoreParams.method == 2)
            {
                String acctId = mTargetAccount.getId();
                logger.info((new StringBuilder()).append("Cleaning up account created during failed restore (account=").append(acctId).append(")").toString());
                try
                {
                    Provisioning.getInstance().deleteAccount(acctId);
                }
                catch(Throwable t)
                {
                    logger.warn((new StringBuilder()).append("Error during account cleanup after failed restore (account=").append(acctId).append(")").toString(), t);
                }
            }
        }
        throw exception;
    }

    private boolean playBackRedoOps(File redologFiles[], long startTime)
        throws IOException, ServiceException
    {
        long prevLogLastOpTime;
        int i;
        prevLogLastOpTime = 0L;
        i = 0;
_L2:
        File redolog;
        RedoPlayer redoPlayer;
        if(i >= redologFiles.length)
            break; /* Loop/switch isn't completed */
        redolog = redologFiles[i];
        FileLogReader r = new FileLogReader(redolog);
        FileHeader rHdr = r.getHeader();
        long seq = rHdr.getSequence();
        if(seq > mRestoreParams.restoreToSequence || prevLogLastOpTime > mRestoreParams.restoreToTime)
            break; /* Loop/switch isn't completed */
        prevLogLastOpTime = rHdr.getLastOpTstamp();
        logger.debug((new StringBuilder()).append("Replaying redo log ").append(redolog.getAbsolutePath()).toString());
        redoPlayer = new RedoPlayer(false, true, mRestoreParams.ignoreRedoErrors, mRestoreParams.skipDeleteOps);
        HashMap mboxIDsMap;
        mboxIDsMap = new HashMap(1);
        mboxIDsMap.put(new Integer(getMailboxId()), new Integer(getTargetMailboxId()));
        redoPlayer.scanLog(redolog, true, mboxIDsMap, startTime, mRestoreParams.restoreToTime);
        break MISSING_BLOCK_LABEL_236;
        ServiceException e;
        e;
        Throwable cause = e.getCause();
        if(!(cause instanceof MailboxIdConflictException))
            break MISSING_BLOCK_LABEL_233;
        logger.info("Found mailbox id conflict.  Interrupting redolog replay", e);
        redoPlayer.shutdown();
        break; /* Loop/switch isn't completed */
        throw e;
        redoPlayer.shutdown();
        break MISSING_BLOCK_LABEL_254;
        Exception exception;
        exception;
        redoPlayer.shutdown();
        throw exception;
        i++;
        if(true) goto _L2; else goto _L1
_L1:
        if(logger.isDebugEnabled())
            logger.debug((new StringBuilder()).append("scanned ").append(redologFiles.length).append(" redo log files for mailbox ").append(getTargetMailboxId()).append(" at or after ").append(new Date(startTime)).append(mRestoreParams.restoreToTime >= 0xffffffffL ? "" : (new StringBuilder()).append(" and before ").append(new Date(mRestoreParams.restoreToTime)).toString()).toString());
        return prevLogLastOpTime <= mRestoreParams.restoreToTime;
    }

    private File[] checkRedoLogSequenceContinuity(File logs[], long seqLast)
        throws ServiceException, IOException
    {
        Pair splitLogs = Utils.splitRedoLogsAtSeq(logs, seqLast < 0L ? -1L : seqLast);
        List errors = (List)splitLogs.getSecond();
        if(errors != null && errors.size() > 0)
        {
            ServiceException first = null;
            Iterator i$ = errors.iterator();
            do
            {
                if(!i$.hasNext())
                    break;
                ServiceException err = (ServiceException)i$.next();
                logger.warn((new StringBuilder()).append("Redo log sequence error: ").append(err.getMessage()).toString(), err);
                if(first == null)
                    first = err;
            } while(true);
            if(!mRestoreParams.ignoreRedoErrors)
                throw ServiceException.FAILURE((new StringBuilder()).append("Restore encountered redo log sequence error: ").append(first.getMessage()).toString(), first);
        }
        return (File[])((Pair)splitLogs.getFirst()).getSecond();
    }

    protected void loadAccount(boolean flag, boolean flag1)
        throws IOException, ServiceException
    {
    }

    protected RestoreParams getParams()
    {
        return mRestoreParams;
    }

    protected abstract void loadTables()
        throws ServiceException, IOException;

    protected abstract void loadIndex()
        throws ServiceException, IOException;

    protected abstract void loadBlobs()
        throws ServiceException, IOException;

    protected abstract void loadBlob(String s, String s1, int i, File file, short word0)
        throws ServiceException, IOException;

    private com.zimbra.cs.mailbox.MailboxManager.MailboxLock prepareTargetMailbox(boolean usingNewAccount)
        throws ServiceException
    {
        com.zimbra.cs.mailbox.MailboxManager.MailboxLock maintenance;
        boolean success;
        maintenance = null;
        success = false;
        String acctId;
        int mboxId;
        boolean mailboxExists;
        if(usingNewAccount)
            break MISSING_BLOCK_LABEL_522;
        acctId = getAccountId();
        mboxId = getMailboxId();
        mailboxExists = false;
        Mailbox mbox;
        try
        {
            mbox = MailboxManager.getInstance().getMailboxByAccountId(acctId, com.zimbra.cs.mailbox.MailboxManager.FetchMode.DO_NOT_AUTOCREATE, true);
            if(mbox != null)
            {
                mailboxExists = true;
                if(mbox.getId() != mboxId)
                    mConflictingMailboxId = true;
                mboxId = mbox.getId();
                maintenance = MailboxManager.getInstance().beginMaintenance(acctId, mboxId);
                if(!mRestoreParams.append)
                {
                    DbMailbox.clearMailboxContent(mDbConn, mbox);
                    DbMailbox.deleteMailbox(mDbConn, mbox);
                    mailboxExists = false;
                }
            }
        }
        catch(MailServiceException e)
        {
            if(!e.getCode().equals("mail.NO_SUCH_MBOX"))
                throw e;
        }
        if(!mRestoreParams.skipDb || mailboxExists)
            break MISSING_BLOCK_LABEL_250;
        e = MailboxManager.getInstance().getMailboxByAccountId(acctId, com.zimbra.cs.mailbox.MailboxManager.FetchMode.AUTOCREATE, true);
        if(e == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to create mailbox for account ").append(acctId).toString(), null);
        mailboxExists = true;
        if(e.getId() != mboxId)
            mConflictingMailboxId = true;
        mboxId = e.getId();
        if(maintenance == null)
            break MISSING_BLOCK_LABEL_239;
        MailboxManager.getInstance().endMaintenance(maintenance, true, true);
        maintenance = null;
        break MISSING_BLOCK_LABEL_239;
        Exception exception;
        exception;
        maintenance = null;
        throw exception;
        maintenance = MailboxManager.getInstance().beginMaintenance(acctId, mboxId);
        int targetMboxId;
        if(mRestoreParams.append && mailboxExists)
        {
            if(!$assertionsDisabled && maintenance == null)
                throw new AssertionError();
            targetMboxId = mboxId;
            logger.info("Restoring in append mode to mailbox id %d for existing account %s.", new Object[] {
                Integer.valueOf(targetMboxId), getAccountId()
            });
            break MISSING_BLOCK_LABEL_595;
        }
        try
        {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
            if(mbox != null && !mbox.getAccountId().equalsIgnoreCase(acctId))
            {
                mboxId = -1;
                mConflictingMailboxId = true;
            }
        }
        catch(MailServiceException e)
        {
            if(!e.getCode().equals("mail.NO_SUCH_MBOX"))
                throw e;
        }
        catch(ServiceException e)
        {
            if(!e.getCode().equals("service.WRONG_HOST") && !e.getCode().equals("account.NO_SUCH_ACCOUNT"))
                throw e;
            mboxId = -1;
            mConflictingMailboxId = true;
        }
        com.zimbra.cs.db.DbMailbox.MailboxIdentifier newMboxId = getNextMailboxId(mboxId);
        createMailboxDatabase(newMboxId.id, newMboxId.groupId);
        targetMboxId = newMboxId.id;
        if(targetMboxId == mboxId || maintenance == null)
            break MISSING_BLOCK_LABEL_476;
        MailboxManager.getInstance().endMaintenance(maintenance, true, true);
        maintenance = null;
        break MISSING_BLOCK_LABEL_476;
        Exception exception1;
        exception1;
        maintenance = null;
        throw exception1;
        logger.info("Restoring to mailbox id %d for existing account %s.", new Object[] {
            Integer.valueOf(targetMboxId), getAccountId()
        });
        if(maintenance == null)
            maintenance = MailboxManager.getInstance().beginMaintenance(acctId, targetMboxId);
        break MISSING_BLOCK_LABEL_595;
        com.zimbra.cs.db.DbMailbox.MailboxIdentifier newMboxId = getNextMailboxId(-1);
        createMailboxDatabase(newMboxId.id, newMboxId.groupId);
        targetMboxId = newMboxId.id;
        logger.info("Restoring to mailbox id %d for new account %s.", new Object[] {
            Integer.valueOf(targetMboxId), mTargetAccount.getId()
        });
        maintenance = MailboxManager.getInstance().beginMaintenance(mTargetAccount.getId(), targetMboxId);
        setTargetMailboxId(targetMboxId);
        mRestoreTargetMboxIdSet = true;
        ZimbraLog.addToContext("mid", Integer.toString(targetMboxId));
        success = true;
        newMboxId = maintenance;
        if(!success && maintenance != null)
            MailboxManager.getInstance().endMaintenance(maintenance, success, true);
        return newMboxId;
        Exception exception2;
        exception2;
        if(!success && maintenance != null)
            MailboxManager.getInstance().endMaintenance(maintenance, success, true);
        throw exception2;
    }

    public static com.zimbra.cs.db.DbMailbox.MailboxIdentifier getNextMailboxId(int mailboxId)
        throws ServiceException
    {
        com.zimbra.cs.db.DbPool.Connection conn = null;
        com.zimbra.cs.db.DbMailbox.MailboxIdentifier mailboxidentifier;
        conn = DbPool.getConnection();
        com.zimbra.cs.db.DbMailbox.MailboxIdentifier newId = DbMailbox.getNextMailboxId(conn, mailboxId);
        conn.commit();
        mailboxidentifier = newId;
        DbPool.quietClose(conn);
        return mailboxidentifier;
        Exception exception;
        exception;
        DbPool.quietClose(conn);
        throw exception;
    }

    public static void createMailboxDatabase(int mailboxId, int groupId)
        throws ServiceException
    {
        com.zimbra.cs.db.DbPool.Connection conn = null;
        conn = DbPool.getConnection();
        DbMailbox.createMailboxDatabase(conn, mailboxId, groupId);
        conn.commit();
        DbPool.quietClose(conn);
        break MISSING_BLOCK_LABEL_30;
        Exception exception;
        exception;
        DbPool.quietClose(conn);
        throw exception;
    }

    protected void loadTablesFromLocalFile(File targetDir, boolean usingNewAccount)
        throws IOException, ServiceException
    {
        File schemaFile = new File(targetDir, "db_schema.xml");
        Map dbSchema = XmlMeta.readTableSchema(schemaFile);
        int mboxId = getMailboxId();
        int targetMboxId = getTargetMailboxId();
        int groupId = DbMailbox.calculateMailboxGroupId(targetMboxId);
        mDbConn.disableForeignKeyConstraints();
        String acctName = usingNewAccount ? mTargetAccount.getName() : getAccountName();
        DbMailbox.removeFromDeletedAccount(mDbConn, acctName);
        Map rmapMbox = new HashMap(3);
        rmapMbox.put(Integer.valueOf(DbMailbox.CI_ID - 1), new StringReplacement(String.valueOf(targetMboxId)));
        rmapMbox.put(Integer.valueOf(DbMailbox.CI_GROUP_ID - 1), new StringReplacement(String.valueOf(groupId)));
        rmapMbox.put(Integer.valueOf(DbMailbox.CI_INDEX_VOLUME_ID - 1), new StringReplacement(String.valueOf(mRestoreParams.indexVolume.getId())));
        if(usingNewAccount)
        {
            rmapMbox.put(Integer.valueOf(DbMailbox.CI_ACCOUNT_ID - 1), new StringReplacement(mTargetAccount.getId()));
            rmapMbox.put(Integer.valueOf(DbMailbox.CI_COMMENT - 1), new StringReplacement(mTargetAccount.getName()));
        }
        loadTableHelper(targetDir, "mailbox", "mailbox", rmapMbox, dbSchema);
        DbBackup.fixupIndexVolume(mDbConn, targetMboxId);
        Map rmapMeta = new HashMap(1);
        rmapMeta.put(Integer.valueOf(0), new StringReplacement(String.valueOf(targetMboxId)));
        loadTableHelper(targetDir, "mailbox_metadata", "mailbox_metadata", rmapMeta, dbSchema);
        Map rmapSchedTask = new HashMap(1);
        rmapSchedTask.put(Integer.valueOf(2), new StringReplacement(String.valueOf(targetMboxId)));
        loadTableHelper(targetDir, "scheduled_task", "scheduled_task", rmapSchedTask, dbSchema);
        Map rmapOufOfOffice = new HashMap(1);
        rmapOufOfOffice.put(Integer.valueOf(0), new StringReplacement(String.valueOf(targetMboxId)));
        loadTableHelper(targetDir, "out_of_office", "out_of_office", rmapOufOfOffice, dbSchema);
        Map rmapMobileDevices = new HashMap(1);
        rmapMobileDevices.put(Integer.valueOf(0), new StringReplacement(String.valueOf(targetMboxId)));
        loadTableHelper(targetDir, "mobile_devices", "mobile_devices", rmapMobileDevices, dbSchema);
        Map replMap = new HashMap(1);
        replMap.put(Integer.valueOf(0), new StringReplacement(String.valueOf(targetMboxId)));
        replMap.put(Integer.valueOf(9), new com.zimbra.cs.backup.util.Utils.Replacement() {

            public String getValue(String oldVolId)
            {
                AccountSession.VolumeInfo info = getVolumeInfo(oldVolId);
                if(info == null)
                    if("\\N".equalsIgnoreCase(oldVolId))
                        return oldVolId;
                    else
                        return mVolId;
                if(!info.isSecondary())
                    return mVolId;
                else
                    return m2ndVolId;
            }

            private String mVolId;
            private String m2ndVolId;
            final RestoreAccountSession this$0;

            
            {
                this$0 = RestoreAccountSession.this;
                super();
                Volume vol = getVolume();
                if(vol != null)
                    mVolId = String.valueOf(vol.getId());
                vol = getSecondaryVolume();
                if(vol != null)
                    m2ndVolId = String.valueOf(vol.getId());
                else
                    m2ndVolId = mVolId;
            }
        }
);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getMailItemTableName(mboxId, 0, false)), DbMailItem.getMailItemTableName(targetMboxId, groupId, false), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getMailItemTableName(mboxId, 0, true)), DbMailItem.getMailItemTableName(targetMboxId, groupId, true), replMap, dbSchema);
        replMap.remove(Integer.valueOf(9));
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getConversationTableName(mboxId, 0)), DbMailItem.getConversationTableName(targetMboxId, groupId), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getCalendarItemTableName(mboxId, 0, false)), DbMailItem.getCalendarItemTableName(targetMboxId, groupId, false), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getCalendarItemTableName(mboxId, 0, true)), DbMailItem.getCalendarItemTableName(targetMboxId, groupId, true), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getRevisionTableName(mboxId, 0, false)), DbMailItem.getRevisionTableName(targetMboxId, groupId, false), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getRevisionTableName(mboxId, 0, true)), DbMailItem.getRevisionTableName(targetMboxId, groupId, true), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbMailItem.getTombstoneTableName(mboxId, 0)), DbMailItem.getTombstoneTableName(targetMboxId, groupId), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbDataSource.getTableName(mboxId, 0)), DbDataSource.getTableName(targetMboxId, groupId), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbImapFolder.getTableName(mboxId, 0)), DbImapFolder.getTableName(targetMboxId, groupId), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbImapMessage.getTableName(mboxId, 0)), DbImapMessage.getTableName(targetMboxId, groupId), replMap, dbSchema);
        loadTableHelper(targetDir, DbBackup.removeDatabasePrefix(DbPop3Message.getTableName(mboxId, 0)), DbPop3Message.getTableName(targetMboxId, groupId), replMap, dbSchema);
        DbBackup.updateConstraints(mDbConn);
        mDbConn.enableForeignKeyConstraints();
        break MISSING_BLOCK_LABEL_857;
        Exception exception;
        exception;
        mDbConn.enableForeignKeyConstraints();
        throw exception;
    }

    private void loadTableHelper(File targetDir, String srcTableName, String targetTableName, Map replMap, Map schemaMap)
        throws IOException, ServiceException
    {
        com.zimbra.cs.db.DbBackup.TableInfo ti;
        File targetFile;
        File srcFile = DbBackup.getDbFile(targetDir, srcTableName);
        if(!srcFile.exists() || srcFile.length() <= 0L)
            break MISSING_BLOCK_LABEL_79;
        ti = (com.zimbra.cs.db.DbBackup.TableInfo)schemaMap.get(srcTableName);
        targetFile = Utils.fixDbDumpFile(srcFile, replMap);
        DbBackup.loadTable(mDbConn, targetTableName, ti, targetFile);
        targetFile.delete();
        break MISSING_BLOCK_LABEL_79;
        Exception exception;
        exception;
        targetFile.delete();
        throw exception;
    }

    protected int loadBlobsImpl(BlobRepository blobRepo, boolean deleteOldBlobs)
        throws ServiceException, IOException
    {
        int count = 0;
        int mboxId = getTargetMailboxId();
        Volume primaryVol = getVolume();
        File primaryMboxPath = new File(primaryVol.getMailboxDir(mboxId, 1));
        Volume secondaryVol = getSecondaryVolume();
        File secondaryMboxPath = null;
        if(secondaryVol != null)
        {
            secondaryMboxPath = new File(secondaryVol.getMailboxDir(mboxId, 1));
        } else
        {
            secondaryVol = primaryVol;
            secondaryMboxPath = primaryMboxPath;
        }
        Set oldMailboxPaths = new HashSet();
        BlobRepository.BlobEntry be = null;
        do
        {
            if((be = blobRepo.getNextEntry()) == null)
                break;
            logger.debug((new StringBuilder()).append("Next blob entry: ").append(be.getName()).toString());
            String vid = decodeVolume(be.getName());
            String path = decodePath(be.getName());
            AccountSession.VolumeInfo volInfo = getVolumeInfo(vid);
            Volume vol;
            File mboxPath;
            if(!volInfo.isSecondary())
            {
                vol = primaryVol;
                mboxPath = primaryMboxPath;
            } else
            {
                if(mRestoreParams.skipSecondaryBlobs)
                    continue;
                vol = secondaryVol;
                mboxPath = secondaryMboxPath;
            }
            File encodedMsgFile = new File(mboxPath, path);
            String encodedName = encodedMsgFile.getName();
            String digest = decodeDigest(encodedName);
            String msgId = decodeMessageId(encodedName);
            int linkCount = decodeLinkCount(encodedName);
            File dir = encodedMsgFile.getParentFile();
            if(deleteOldBlobs)
            {
                String oldPath = (new File(volInfo.getMailboxPath())).getCanonicalPath();
                if(!oldMailboxPaths.contains(oldPath))
                {
                    logger.info((new StringBuilder()).append("Deleting directory ").append(oldPath).toString());
                    FileUtil.deleteDir(new File(oldPath));
                    oldMailboxPaths.add(oldPath);
                }
            }
            loadBlob(digest, msgId, linkCount, dir, vol.getId());
            count++;
        } while(true);
        return count;
    }

    protected abstract String decodeDigest(String s);

    protected abstract String decodeMessageId(String s)
        throws IOException;

    protected abstract int decodeLinkCount(String s);

    protected String decodePath(String name)
    {
        int pos = name.indexOf(File.separator);
        return name.substring(pos + 1);
    }

    protected String decodeVolume(String name)
    {
        int pos = name.indexOf(File.separator);
        String vol = name.substring(0, pos);
        return vol;
    }

    protected short getIndexVolumeId()
        throws ServiceException
    {
        com.zimbra.cs.mailbox.Mailbox.MailboxData data = DbMailbox.getMailboxStats(mDbConn, mTargetMailboxId);
        if(data == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("can't lookup index volume id for mailbox ").append(mTargetMailboxId).toString(), null);
        else
            return data.indexVolumeId;
    }

    public static final int DIGEST_LEN = 28;
    private com.zimbra.cs.db.DbPool.Connection mDbConn;
    private Account mTargetAccount;
    private int mTargetMailboxId;
    private RestoreParams mRestoreParams;
    private boolean mRestoreTargetMboxIdSet;
    private boolean mFullRestoreDone;
    private boolean mConflictingMailboxId;
    static final boolean $assertionsDisabled = !com/zimbra/cs/backup/RestoreAccountSession.desiredAssertionStatus();

}
