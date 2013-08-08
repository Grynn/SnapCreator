// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   FileBackupTarget.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.AbstractFileCopierMonitor;
import com.zimbra.common.io.FileCopier;
import com.zimbra.common.io.FileCopierCallback;
import com.zimbra.common.io.FileCopierFactory;
import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.ZAttrProvisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.backup.util.Utils;
import com.zimbra.cs.db.DbBackup;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbSharedBlobDigest;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.RolloverManager;
import com.zimbra.cs.redolog.logger.FileHeader;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.util.Zimbra;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

// Referenced classes of package com.zimbra.cs.backup:
//            AbstractBackupTarget, BackupSet, RestoreParams, BackupServiceException, 
//            BackupLC, BackupManager, XmlMeta, BackupParams, 
//            BackupAccountSession, AccountSession, ZipBlobRepository, DeprecatedZipBlobRepository, 
//            DirBlobRepository, FileBackupStats, Counter, BlobRepository, 
//            SharedBlobRepository, ZipSharedBlobRepository, DirSharedBlobRepository, BackupTarget, 
//            RestoreAccountSession

public class FileBackupTarget extends AbstractBackupTarget
{
    private static class BackupSetWorkerThread extends Thread
    {
        private static final class Stage extends Enum
        {

            public static Stage[] values()
            {
                return (Stage[])$VALUES.clone();
            }

            public static Stage valueOf(String name)
            {
                return (Stage)Enum.valueOf(com/zimbra/cs/backup/FileBackupTarget$BackupSetWorkerThread$Stage, name);
            }

            public static final Stage DB;
            public static final Stage BLOB;
            public static final Stage INDEX;
            private static final Stage $VALUES[];

            static 
            {
                DB = new Stage("DB", 0);
                BLOB = new Stage("BLOB", 1);
                INDEX = new Stage("INDEX", 2);
                $VALUES = (new Stage[] {
                    DB, BLOB, INDEX
                });
            }

            private Stage(String s, int i)
            {
                super(s, i);
            }
        }


        public void run()
        {
_L3:
label0:
            {
                try
                {
                    int idx = mStartBarrier.await();
                    if(Log.backup.isDebugEnabled())
                        Log.backup.debug((new StringBuilder()).append("startBarrier.await() = ").append(idx).toString());
                }
                catch(InterruptedException e)
                {
                    Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" was interrupted while waiting for start barrier.").toString(), e);
                }
                catch(BrokenBarrierException e)
                {
                    Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" got BrokenBarrierException while waiting for start barrier.").toString(), e);
                }
                synchronized(this)
                {
                    if(!mShutdown)
                        break label0;
                }
                break; /* Loop/switch isn't completed */
            }
            BackupAcctSession session = mAccountSession;
            backupsetworkerthread;
            JVM INSTR monitorexit ;
              goto _L1
            exception;
            throw exception;
_L1:
            ZimbraLog.addToContext("name", mAccountSession.getAccountName());
            ZimbraLog.addToContext("mid", Integer.toString(mAccountSession.getMailboxId()));
            if(Log.backup.isDebugEnabled())
                Log.backup.debug((new StringBuilder()).append("started ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).toString());
            static class _cls3
            {

                static final int $SwitchMap$com$zimbra$cs$backup$FileBackupTarget$BackupSetWorkerThread$Stage[];

                static 
                {
                    $SwitchMap$com$zimbra$cs$backup$FileBackupTarget$BackupSetWorkerThread$Stage = new int[BackupSetWorkerThread.Stage.values().length];
                    try
                    {
                        $SwitchMap$com$zimbra$cs$backup$FileBackupTarget$BackupSetWorkerThread$Stage[BackupSetWorkerThread.Stage.DB.ordinal()] = 1;
                    }
                    catch(NoSuchFieldError ex) { }
                    try
                    {
                        $SwitchMap$com$zimbra$cs$backup$FileBackupTarget$BackupSetWorkerThread$Stage[BackupSetWorkerThread.Stage.BLOB.ordinal()] = 2;
                    }
                    catch(NoSuchFieldError ex) { }
                    try
                    {
                        $SwitchMap$com$zimbra$cs$backup$FileBackupTarget$BackupSetWorkerThread$Stage[BackupSetWorkerThread.Stage.INDEX.ordinal()] = 3;
                    }
                    catch(NoSuchFieldError ex) { }
                }
            }

            switch(_cls3..SwitchMap.com.zimbra.cs.backup.FileBackupTarget.BackupSetWorkerThread.Stage[mStage.ordinal()])
            {
            case 1: // '\001'
                session.storeTablesStage();
                break;

            case 2: // '\002'
                session.storeBlobsStage();
                break;

            case 3: // '\003'
                session.storeIndexStage();
                break;
            }
            BrokenBarrierException e;
            if(Log.backup.isDebugEnabled())
            {
                Throwable err = getError();
                if(err == null)
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).toString());
                else
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).append(" with error: ").append(err.getMessage()).toString());
            }
            try
            {
                int idx = mFinishBarrier.await();
                if(Log.backup.isDebugEnabled())
                    Log.backup.debug((new StringBuilder()).append("finishBarrier.await() = ").append(idx).toString());
            }
            // Misplaced declaration of an exception variable
            catch(BrokenBarrierException e)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" was interrupted while waiting for finish barrier.").toString(), e);
            }
            // Misplaced declaration of an exception variable
            catch(BrokenBarrierException e)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" got BrokenBarrierException while waiting for finish barrier.").toString(), e);
            }
            continue; /* Loop/switch isn't completed */
            e;
            Zimbra.halt("out of memory", e);
            if(Log.backup.isDebugEnabled())
            {
                Throwable err = getError();
                if(err == null)
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).toString());
                else
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).append(" with error: ").append(err.getMessage()).toString());
            }
            try
            {
                int idx = mFinishBarrier.await();
                if(Log.backup.isDebugEnabled())
                    Log.backup.debug((new StringBuilder()).append("finishBarrier.await() = ").append(idx).toString());
            }
            // Misplaced declaration of an exception variable
            catch(int idx)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" was interrupted while waiting for finish barrier.").toString(), idx);
            }
            // Misplaced declaration of an exception variable
            catch(int idx)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" got BrokenBarrierException while waiting for finish barrier.").toString(), idx);
            }
            continue; /* Loop/switch isn't completed */
            Throwable t;
            t;
            setError(t);
            if(Log.backup.isDebugEnabled())
            {
                Throwable err = getError();
                if(err == null)
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).toString());
                else
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).append(" with error: ").append(err.getMessage()).toString());
            }
            try
            {
                int idx = mFinishBarrier.await();
                if(Log.backup.isDebugEnabled())
                    Log.backup.debug((new StringBuilder()).append("finishBarrier.await() = ").append(idx).toString());
            }
            // Misplaced declaration of an exception variable
            catch(int idx)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" was interrupted while waiting for finish barrier.").toString(), idx);
            }
            // Misplaced declaration of an exception variable
            catch(int idx)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" got BrokenBarrierException while waiting for finish barrier.").toString(), idx);
            }
            if(true) goto _L3; else goto _L2
            Exception exception1;
            exception1;
            if(Log.backup.isDebugEnabled())
            {
                Throwable err = getError();
                if(err == null)
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).toString());
                else
                    Log.backup.debug((new StringBuilder()).append("finished ").append(mStage).append(" stage for account ").append(mAccountSession.getAccountName()).append(" with error: ").append(err.getMessage()).toString());
            }
            try
            {
                int idx = mFinishBarrier.await();
                if(Log.backup.isDebugEnabled())
                    Log.backup.debug((new StringBuilder()).append("finishBarrier.await() = ").append(idx).toString());
            }
            catch(InterruptedException e)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" was interrupted while waiting for finish barrier.").toString(), e);
            }
            catch(BrokenBarrierException e)
            {
                Log.backup.warn((new StringBuilder()).append("Thread ").append(getName()).append(" got BrokenBarrierException while waiting for finish barrier.").toString(), e);
            }
            throw exception1;
_L2:
        }

        public synchronized Throwable getError()
        {
            return mError;
        }

        private synchronized void setError(Throwable t)
        {
            mError = t;
        }

        public synchronized void signalShutdown()
        {
            mShutdown = true;
        }

        public synchronized void doAccountSession(BackupAcctSession session)
        {
            mError = null;
            mAccountSession = session;
        }

        private Stage mStage;
        private BackupAcctSession mAccountSession;
        private Throwable mError;
        private CyclicBarrier mStartBarrier;
        private CyclicBarrier mFinishBarrier;
        private boolean mShutdown;

        public BackupSetWorkerThread(Stage stage, CyclicBarrier startBarrier, CyclicBarrier finishBarrier)
        {
            setName((new StringBuilder()).append("BackupSetWorkerThread-").append(stage.toString()).toString());
            mStage = stage;
            mStartBarrier = startBarrier;
            mFinishBarrier = finishBarrier;
        }
    }

    private static class RestoreFileCopierMonitor extends AbstractFileCopierMonitor
        implements FileCopierCallback
    {

        protected boolean fileCopierMonitorBegin(Object cbarg)
        {
            return true;
        }

        protected synchronized void fileCopierMonitorEnd(Object cbarg, Throwable err)
        {
            if(err != null)
            {
                denyFutureOperations();
                if(mFirstError == null)
                {
                    mFirstError = err;
                    mAccountSession.initError(err);
                }
                Log.backup.error((new StringBuilder()).append("Error reported by FileCopier: ").append(err.getMessage()).toString(), err);
            }
        }

        public synchronized void waitForCompletion()
        {
            super.waitForCompletion();
            Log.backup.info((new StringBuilder()).append("Account ").append(mAccountSession.getAccountName()).append(": All pending file IO completed (").append(getCompleted()).append(" out of ").append(getRequested()).append(")").toString());
        }

        private Throwable mFirstError;
        private RestoreAcctSession mAccountSession;

        public RestoreFileCopierMonitor(RestoreAcctSession acct)
        {
            mAccountSession = acct;
        }
    }

    private static class BackupFileCopierMonitor extends AbstractFileCopierMonitor
        implements FileCopierCallback
    {

        protected boolean fileCopierMonitorBegin(Object cbarg)
        {
            return true;
        }

        protected synchronized void fileCopierMonitorEnd(Object cbarg, Throwable err)
        {
            if(err != null)
            {
                if((err instanceof IOException) && (FileUtil.isOutOfDiskError((IOException)err) || mBackupSet.getBackupTarget().outOfSpace()))
                    err = mBackupSet.getBackupTarget().makeOutOfSpaceException((IOException)err);
                denyFutureOperations();
                if(mFirstError == null)
                {
                    mFirstError = err;
                    if(mAccountSession != null)
                    {
                        mAccountSession.initError(err);
                        mAccountSession.markInterrupted();
                    } else
                    {
                        mBackupSet.addError(err);
                    }
                }
                Log.backup.error((new StringBuilder()).append("Error reported by FileCopier: ").append(err.getMessage()).toString(), err);
            }
        }

        public synchronized void waitForCompletion()
        {
            super.waitForCompletion();
            Log.backup.info((new StringBuilder()).append(mName).append(": All pending file IO completed (").append(getCompleted()).append(" out of ").append(getRequested()).append(")").toString());
        }

        private Throwable mFirstError;
        private FileBackupSet mBackupSet;
        private BackupAcctSession mAccountSession;
        private String mName;

        public BackupFileCopierMonitor(FileBackupSet set)
        {
            this(set, null);
        }

        public BackupFileCopierMonitor(FileBackupSet set, BackupAcctSession acct)
        {
            mBackupSet = set;
            mAccountSession = acct;
            if(mAccountSession != null)
                mName = (new StringBuilder()).append("Account ").append(mAccountSession.getAccountName()).append(" in backup set ").append(mBackupSet.getLabel()).toString();
            else
                mName = (new StringBuilder()).append("Backup set ").append(mBackupSet.getLabel()).toString();
        }
    }

    private static class LabelFilenameFilter
        implements FilenameFilter
    {

        public boolean accept(File dir, String name)
        {
            if(mLabelType == null)
                return false;
            LabelType type = FileBackupTarget.getLabelType(name);
            if(mLabelType.equals(LabelType.ALL))
                return LabelType.FULL.equals(type) || LabelType.INCREMENTAL.equals(type);
            else
                return mLabelType.equals(type);
        }

        private LabelType mLabelType;

        LabelFilenameFilter(LabelType type)
        {
            mLabelType = type;
        }
    }

    private static final class LabelType extends Enum
    {

        public static LabelType[] values()
        {
            return (LabelType[])$VALUES.clone();
        }

        public static LabelType valueOf(String name)
        {
            return (LabelType)Enum.valueOf(com/zimbra/cs/backup/FileBackupTarget$LabelType, name);
        }

        public static final LabelType ALL;
        public static final LabelType FULL;
        public static final LabelType INCREMENTAL;
        public static final LabelType INVALID;
        private static final LabelType $VALUES[];

        static 
        {
            ALL = new LabelType("ALL", 0);
            FULL = new LabelType("FULL", 1);
            INCREMENTAL = new LabelType("INCREMENTAL", 2);
            INVALID = new LabelType("INVALID", 3);
            $VALUES = (new LabelType[] {
                ALL, FULL, INCREMENTAL, INVALID
            });
        }

        private LabelType(String s, int i)
        {
            super(s, i);
        }
    }

    private static class LabelComparator
        implements Comparator
    {

        public int compare(File d1, File d2)
        {
            long t1;
            long t2;
            t1 = BackupManager.getLabelDate(d1.getName());
            t2 = BackupManager.getLabelDate(d2.getName());
            if(t1 < t2)
                return LabelSortOrder.OLD_TO_NEW.equals(mSortOrder) ? -1 : 1;
            if(t1 > t2)
                return LabelSortOrder.OLD_TO_NEW.equals(mSortOrder) ? 1 : -1;
            try
            {
                return 0;
            }
            catch(ServiceException e)
            {
                Log.backup.error("Error while comparing backup label directories", e);
            }
            return 0;
        }

        public volatile int compare(Object x0, Object x1)
        {
            return compare((File)x0, (File)x1);
        }

        private LabelSortOrder mSortOrder;

        LabelComparator(LabelSortOrder sortOrder)
        {
            mSortOrder = sortOrder;
        }
    }

    private static final class LabelSortOrder extends Enum
    {

        public static LabelSortOrder[] values()
        {
            return (LabelSortOrder[])$VALUES.clone();
        }

        public static LabelSortOrder valueOf(String name)
        {
            return (LabelSortOrder)Enum.valueOf(com/zimbra/cs/backup/FileBackupTarget$LabelSortOrder, name);
        }

        public static final LabelSortOrder OLD_TO_NEW;
        public static final LabelSortOrder NEW_TO_OLD;
        private static final LabelSortOrder $VALUES[];

        static 
        {
            OLD_TO_NEW = new LabelSortOrder("OLD_TO_NEW", 0);
            NEW_TO_OLD = new LabelSortOrder("NEW_TO_OLD", 1);
            $VALUES = (new LabelSortOrder[] {
                OLD_TO_NEW, NEW_TO_OLD
            });
        }

        private LabelSortOrder(String s, int i)
        {
            super(s, i);
        }
    }

    class RestorePathCache
    {

        String get(short volId, String digest)
            throws IOException, ServiceException
        {
            if(mEnabled)
            {
                Map volDigestToPath = getDigestToPath(volId);
                String path = (String)volDigestToPath.get(digest);
                File linkTarget = null;
                if(path != null)
                {
                    linkTarget = new File(path);
                    if(!linkTarget.exists())
                    {
                        remove(volId, digest);
                        path = null;
                    }
                }
                return path;
            } else
            {
                return null;
            }
        }

        void put(short volId, String digest, String path)
            throws IOException, ServiceException
        {
            if(mEnabled)
            {
                Map volDigestToPath = getDigestToPath(volId);
                volDigestToPath.put(digest, path);
            }
        }

        void remove(short volId, String digest)
            throws IOException, ServiceException
        {
            if(mEnabled)
            {
                Map volDigestToPath = getDigestToPath(volId);
                volDigestToPath.remove(digest);
            }
        }

        void save()
            throws IOException, ServiceException
        {
            if(mEnabled)
            {
                Iterator i$ = mVolMap.entrySet().iterator();
                do
                {
                    if(!i$.hasNext())
                        break;
                    java.util.Map.Entry entry = (java.util.Map.Entry)i$.next();
                    Map digestPathMap = (Map)entry.getValue();
                    if(!digestPathMap.isEmpty())
                    {
                        Short volId = (Short)entry.getKey();
                        FileUtil.ensureDirExists(mBase);
                        File file = getFileForVolume(volId);
                        XmlMeta.writeRestorePathCache(volId.shortValue(), digestPathMap, file);
                    }
                } while(true);
            }
        }

        private Map getDigestToPath(short id)
            throws IOException, ServiceException
        {
            Short volId = new Short(id);
            Map digestPathMap = (Map)mVolMap.get(volId);
            if(digestPathMap != null)
                return digestPathMap;
            File file = getFileForVolume(volId);
            if(file.exists())
                digestPathMap = XmlMeta.readRestorePathCache(file);
            else
                digestPathMap = new HashMap();
            mVolMap.put(volId, digestPathMap);
            return digestPathMap;
        }

        private File getFileForVolume(Short volId)
        {
            return new File(mBase, (new StringBuilder()).append(volId.toString()).append(".xml").toString());
        }

        private boolean mEnabled;
        private Map mVolMap;
        private File mBase;
        final FileBackupTarget this$0;

        RestorePathCache()
            throws ServiceException
        {
            this$0 = FileBackupTarget.this;
            super();
            mEnabled = !BackupLC.backup_disable_shared_blobs.booleanValue();
            if(mEnabled)
            {
                mBase = BackupManager.getInstance().getRestoreCacheDir();
                if(!mBase.exists())
                    mBase.mkdirs();
                mVolMap = new HashMap();
            }
        }
    }

    class RestoreAcctSession extends RestoreAccountSession
    {

        void setFileCopier(FileCopier copier)
        {
            mCopier = copier;
        }

        private File getBlobsDir()
        {
            return new File(mDir, "blobs");
        }

        protected void loadAccount(boolean attrsOnly, boolean fullOnly)
            throws IOException, ServiceException
        {
            Provisioning prov = Provisioning.getInstance();
            RestoreParams restParams = getParams();
            com.zimbra.cs.db.DbMailbox.DeletedAccount deletedAccount = BackupManager.getInstance().getDeletedAccount(getAccountName());
            if(deletedAccount != null && restParams.skipDeletedAccounts)
            {
                if(restParams.method != 2)
                {
                    String acctId = deletedAccount.getAccountId();
                    try
                    {
                        Account existing = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.id, acctId);
                        if(existing != null)
                        {
                            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(existing, false);
                            try
                            {
                                IMPersona.deleteIMPersona(existing.getName());
                                mbox.deleteMailbox();
                            }
                            catch(ServiceException e)
                            {
                                Log.backup.warn((new StringBuilder()).append("Exception while deleting mailbox ").append(mbox.getId()).append(" of deleted account ").append(getAccountName()).toString(), e);
                            }
                        }
                        prov.deleteAccount(acctId);
                    }
                    catch(AccountServiceException ase)
                    {
                        if(!ase.getCode().equals("account.NO_SUCH_ACCOUNT"))
                            throw ase;
                    }
                }
                return;
            }
            File acctFile;
            if(!fullOnly)
            {
                acctFile = new File(mDir, "ldap_latest.xml");
                if(!acctFile.exists())
                    acctFile = new File(mDir, "ldap.xml");
            } else
            {
                acctFile = new File(mDir, "ldap.xml");
            }
            XmlMeta.AccountLdapMeta meta = XmlMeta.readAccountLdap(acctFile);
            String acctName = meta.getEmail();
            String acctId = meta.getId();
            String acctNameOverride = null;
            String prefix = restParams.prefix;
            if(prefix != null)
                acctNameOverride = (new StringBuilder()).append(prefix).append(acctName).toString();
            Account account = null;
            String an = acctName;
            if(restParams.method == 2)
            {
                an = acctNameOverride;
            } else
            {
                account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.id, acctId);
                if(account != null)
                    if(Utils.refreshAccount(account))
                    {
                        if(restParams.method != 1)
                        {
                            account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.id, acctId);
                            setAccount(account);
                            return;
                        }
                        if(!attrsOnly)
                        {
                            prov.deleteAccount(acctId);
                            account = null;
                        }
                    } else
                    {
                        account = null;
                    }
                Account currAcctByName = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, acctName);
                if(currAcctByName != null && Utils.refreshAccount(currAcctByName))
                {
                    Log.backup.info((new StringBuilder()).append("account by the name ").append(acctName).append(" exists; id=").append(currAcctByName.getId()).toString());
                    if(!currAcctByName.getId().equals(acctId))
                    {
                        if(acctNameOverride == null)
                            throw new IOException((new StringBuilder()).append("Account name has been reused by another account: ").append(acctName).toString());
                        an = acctNameOverride;
                        Log.backup.info((new StringBuilder()).append("Recreate account with name override: ").append(acctNameOverride).toString());
                    }
                }
            }
            Map attrs = meta.getAttrs();
            String newMailHost = prov.getLocalServer().getAttr("zimbraServiceHostname");
            if(account == null)
            {
                Map m = new HashMap(1);
                m.put("zimbraMailHost", newMailHost);
                account = prov.restoreAccount(an, null, m, attrs);
            }
            setAccount(account);
            String cosId = (String)attrs.get("zimbraCOSId");
            if(cosId != null)
            {
                com.zimbra.cs.account.Cos c = prov.get(com.zimbra.cs.account.Provisioning.CosBy.id, cosId);
                if(c == null)
                {
                    Log.backup.warn((new StringBuilder()).append("COS ").append(cosId).append(" used by account ").append(an).append(" is no longer available.  Creating account without specifying COS.  ").append("Account will inherit COS from domain.").toString());
                    attrs.remove("zimbraCOSId");
                }
            }
            attrs.remove("uid");
            if(restParams.method == 2)
            {
                attrs.remove("zimbraId");
                attrs.remove("mail");
                attrs.remove("zimbraMailAlias");
                attrs.put("zimbraMailDeliveryAddress", an);
                attrs.put("mail", an);
                setTargetAccount(account);
                attrs.remove("zimbraPrefReplyToAddress");
            }
            attrs.put("zimbraMailHost", newMailHost);
            prov.modifyAttrs(account, attrs, false, false);
            if(restParams.method != 2)
                account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.id, acctId);
            String userType = (String)attrs.get("zimbraAccountCalendarUserType");
            if(com.zimbra.cs.account.ZAttrProvisioning.AccountCalendarUserType.RESOURCE.toString().equals(userType))
            {
                String zimbraId = account.getId();
                if(prov instanceof LdapProvisioning)
                    ((LdapProvisioning)prov).removeFromCache(account);
                account = prov.getCalendarResourceById(zimbraId);
                setAccount(account);
                if(restParams.method == 2)
                    setTargetAccount(account);
            }
            String aliases[] = meta.getAliases();
            if(aliases != null && aliases.length > 0 && restParams.method != 2)
            {
                String arr$[] = aliases;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    String alias = arr$[i$];
                    try
                    {
                        prov.addAlias(account, alias);
                        continue;
                    }
                    catch(AccountServiceException e)
                    {
                        if(e.getCode().equals("account.ACCOUNT_EXISTS"))
                            Log.backup.warn((new StringBuilder()).append("account already exists when adding alias ").append(alias).toString());
                        else
                            throw e;
                    }
                }

            }
            List identities = meta.getIdentities();
            if(identities != null && identities.size() > 0)
            {
                XmlMeta.IdentityData iden;
                for(Iterator i$ = identities.iterator(); i$.hasNext(); prov.restoreIdentity(account, iden.getName(), iden.getAttrs()))
                    iden = (XmlMeta.IdentityData)i$.next();

            }
            List dataSources = meta.getDataSources();
            if(dataSources != null && dataSources.size() > 0)
            {
                XmlMeta.DataSourceData ds;
                for(Iterator i$ = dataSources.iterator(); i$.hasNext(); prov.restoreDataSource(account, ds.getType(), ds.getName(), ds.getAttrs()))
                    ds = (XmlMeta.DataSourceData)i$.next();

            }
            List signatures = meta.getSignatures();
            if(signatures != null && signatures.size() > 0)
            {
                XmlMeta.SignatureData sig;
                for(Iterator i$ = signatures.iterator(); i$.hasNext(); prov.restoreSignature(account, sig.getName(), sig.getAttrs()))
                    sig = (XmlMeta.SignatureData)i$.next();

            }
            String distributionLists[] = meta.getDistributionLists();
            if(distributionLists != null && distributionLists.length > 0)
            {
                String acctEmail[] = {
                    account.getName()
                };
                String arr$[] = distributionLists;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    String dlEmail = arr$[i$];
                    DistributionList dl = prov.get(com.zimbra.cs.account.Provisioning.DistributionListBy.name, dlEmail);
                    if(dl != null)
                        prov.addMembers(dl, acctEmail);
                }

            }
        }

        public void loadTables()
            throws ServiceException, IOException
        {
            int restoreMethod = getParams().method;
            File dbDir = new File(mDir, "db");
            loadTablesFromLocalFile(dbDir, restoreMethod == 2);
        }

        private int copyDirectory(File srcDir, File destDir)
            throws IOException
        {
            int filesCopied = 0;
            File files[] = srcDir.listFiles();
            if(files == null)
                return 0;
            File arr$[] = files;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                File file = arr$[i$];
                if(file.isFile())
                {
                    File dest = new File(destDir, file.getName());
                    mCopier.copy(file, dest, mFCMonitor, dest);
                    filesCopied++;
                }
            }

            arr$ = files;
            len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                File file = arr$[i$];
                if(!file.isDirectory())
                    continue;
                String name = file.getName();
                if(name != "." && name != "..")
                {
                    File subDestDir = new File(destDir, file.getName());
                    filesCopied += copyDirectory(file, subDestDir);
                }
            }

            return filesCopied;
        }

        public void loadIndex()
            throws ServiceException, IOException
        {
            int mboxId = getTargetMailboxId();
            short volId = getIndexVolumeId();
            Volume vol = Volume.getById(volId);
            File srcDir = new File(mDir, "index");
            File destDir = new File(vol.getMailboxDir(mboxId, 10));
            Log.backup.info((new StringBuilder()).append("Deleting directory ").append(destDir.getAbsolutePath()).toString());
            FileUtil.deleteDir(destDir);
            int count = copyDirectory(srcDir, destDir);
            Log.backup.info((new StringBuilder()).append("loaded ").append(count).append(" index files during restore").toString());
        }

        public void loadBlobs()
            throws ServiceException, IOException
        {
            if(blobsZipped())
                mBlobRepo = new ZipBlobRepository(getBlobsDir());
            else
            if(isBlobCompressedDeprecated())
                mBlobRepo = new DeprecatedZipBlobRepository(new File(mDir, "blobs.zip"), false);
            else
                mBlobRepo = new DirBlobRepository(getBlobsDir(), false);
            if(mFullBackupSet.sharedBlobsZipped())
                mSharedBlobRepo = new ZipSharedBlobRepository(mFullBackupSet.mSharedBlobsDir, mFullBackupSet.sharedBlobsDirectoryDepth(), mFullBackupSet.sharedBlobsCharsPerDirectory(), mFullBackupSet.sharedBlobsZipNameDigestChars());
            else
                mSharedBlobRepo = new DirSharedBlobRepository(mFullBackupSet.mSharedBlobsDir, mFullBackupSet.sharedBlobsDirectoryDepth(), mFullBackupSet.sharedBlobsCharsPerDirectory(), false);
            boolean deleteOldBlobs = getTargetMailboxId() == getMailboxId();
            int count = loadBlobsImpl(mBlobRepo, deleteOldBlobs);
            Log.backup.info((new StringBuilder()).append("loaded ").append(count).append(" blobs during restore").toString());
            try
            {
                if(mBlobRepo != null)
                    mBlobRepo.close();
            }
            catch(IOException e) { }
            mBlobRepo = null;
            try
            {
                if(mSharedBlobRepo != null)
                    mSharedBlobRepo.close();
            }
            catch(IOException e) { }
            mSharedBlobRepo = null;
            break MISSING_BLOCK_LABEL_318;
            Exception exception;
            exception;
            try
            {
                if(mBlobRepo != null)
                    mBlobRepo.close();
            }
            catch(IOException e) { }
            mBlobRepo = null;
            try
            {
                if(mSharedBlobRepo != null)
                    mSharedBlobRepo.close();
            }
            catch(IOException e) { }
            mSharedBlobRepo = null;
            throw exception;
        }

        public void loadBlob(String digest, String msgId, int linkCount, File targetDir, short volId)
            throws ServiceException, IOException
        {
            File msgFile;
            String recomputedDigest;
            InputStream in;
            if(!targetDir.exists() && !targetDir.mkdirs())
                throw new IOException((new StringBuilder()).append("cannot create directory for ").append(targetDir.getPath()).toString());
            msgFile = new File(targetDir, (new StringBuilder()).append(msgId).append(".msg").toString());
            boolean restored = false;
            String path = mRestorePathCache.get(volId, digest);
            if(path != null)
            {
                File inBackup = new File(path);
                if(!msgFile.getAbsolutePath().equals(inBackup.getAbsolutePath()))
                {
                    Utils.link(msgFile, inBackup);
                    restored = true;
                }
            } else
            {
                boolean done = false;
                if(linkCount < 2)
                {
                    BlobRepository.BlobEntry entry = mBlobRepo.getCurrentEntry();
                    if(!blobsZipped() && !isBlobCompressedDeprecated())
                    {
                        File src = new File(entry.getPath());
                        if(src.length() > 0L || "2jmj7l5rSw0yVb,vlWAYkK,YBwk=".equals(digest))
                        {
                            if(!mVerifyRestoredBlobDigest)
                                mCopier.copy(src, msgFile, mFCMonitor, msgFile);
                            else
                                FileUtil.copy(src, msgFile);
                            done = true;
                        }
                    } else
                    {
                        InputStream is = entry.getInputStream();
                        FileUtil.copy(is, entry.mustCloseInputStream(), msgFile);
                        done = msgFile.length() > 0L || "2jmj7l5rSw0yVb,vlWAYkK,YBwk=".equals(digest);
                    }
                }
                restored = done;
                if(!done)
                {
                    SharedBlobRepository.SharedBlobEntry sharedBlobEntry = mSharedBlobRepo.getEntry(digest);
                    if(sharedBlobEntry != null)
                    {
                        if(Log.backup.isDebugEnabled())
                            Log.backup.debug((new StringBuilder()).append("Restoring shared blob ").append(sharedBlobEntry.getPath()).append(" to ").append(msgFile.getAbsolutePath()).toString());
                        InputStream is = sharedBlobEntry.getInputStream();
                        FileUtil.copy(is, sharedBlobEntry.mustCloseInputStream(), msgFile);
                        mRestorePathCache.put(volId, digest, msgFile.getPath());
                        restored = true;
                    } else
                    {
                        Log.backup.warn((new StringBuilder()).append("Missing shared blob file for digest \"").append(digest).append("\" in backup; Unable to restore ").append(msgFile.getAbsolutePath()).toString());
                    }
                }
            }
            if(!restored || !mVerifyRestoredBlobDigest)
                break MISSING_BLOCK_LABEL_627;
            recomputedDigest = null;
            in = null;
            in = new FileInputStream(msgFile);
            recomputedDigest = ByteUtil.getSHA1Digest(in, true);
            if(in != null)
                try
                {
                    in.close();
                }
                catch(IOException e) { }
            break MISSING_BLOCK_LABEL_534;
            Exception exception;
            exception;
            if(in != null)
                try
                {
                    in.close();
                }
                catch(IOException e) { }
            throw exception;
            if(digest.equals(recomputedDigest))
                Log.backup.info((new StringBuilder()).append("Restored blob ").append(msgFile.getAbsolutePath()).append(" has correct digest").toString());
            else
                throw ServiceException.FAILURE((new StringBuilder()).append("Restored blob ").append(msgFile.getAbsolutePath()).append(" has wrong digest; expected ").append(digest).append("; got ").append(recomputedDigest).toString(), null);
        }

        protected String decodeDigest(String filename)
        {
            String digest = filename.substring(0, 28);
            return digest;
        }

        protected int decodeLinkCount(String filename)
        {
            int dot = filename.lastIndexOf('.');
            int linkCount = Integer.parseInt(filename.substring(dot + 4));
            return linkCount;
        }

        protected String decodeMessageId(String name)
            throws IOException
        {
            int pos = name.indexOf('.');
            if(pos == -1)
            {
                throw new IOException((new StringBuilder()).append("Unexpected blob entry from backup: ").append(name).toString());
            } else
            {
                String msgId = name.substring(28, pos);
                return msgId;
            }
        }

        public Mailbox getMailbox()
        {
            throw new IllegalStateException();
        }

        public void endRestore()
            throws IOException, ServiceException
        {
            mFCMonitor.waitForCompletion();
            super.endRestore();
        }

        private FileBackupSet mFullBackupSet;
        private File mDir;
        private BlobRepository mBlobRepo;
        private SharedBlobRepository mSharedBlobRepo;
        private FileCopier mCopier;
        private RestoreFileCopierMonitor mFCMonitor;
        private static final String DIGEST_EMPTY_FILE = "2jmj7l5rSw0yVb,vlWAYkK,YBwk=";
        private boolean mVerifyRestoredBlobDigest;
        final FileBackupTarget this$0;


        RestoreAcctSession(BackupSet bak, String accountId)
            throws IOException, ServiceException
        {
            this$0 = FileBackupTarget.this;
            super(bak, accountId, Log.backup);
            mVerifyRestoredBlobDigest = BackupLC.backup_verify_restored_blob_digest.booleanValue();
            mFCMonitor = new RestoreFileCopierMonitor(this);
            mFullBackupSet = (FileBackupSet)bak;
            mDir = mFullBackupSet.getAccountDir(accountId);
            if(!mDir.exists())
                throw BackupServiceException.NO_SUCH_ACCOUNT_BACKUP(accountId);
            File metafile = new File(mDir, "meta.xml");
            try
            {
                Element acctMetaElem = XmlMeta.readAccountBackup(metafile);
                decodeMetadata(acctMetaElem);
            }
            catch(Exception e)
            {
                throw Utils.IOException((new StringBuilder()).append("unable to read metadata for account ").append(accountId).append(" backup ").append(bak.getLabel()).toString(), e);
            }
        }
    }

    class BackupAcctSession extends BackupAccountSession
    {

        public void markInterrupted()
        {
            synchronized(mInterruptedGuard)
            {
                mInterrupted = true;
            }
        }

        protected void storeBlobsBeforeMaintenanceMode()
            throws ServiceException
        {
            Mailbox mbox;
            com.zimbra.cs.mailbox.MailboxManager.MailboxLock lock;
            if(getParams().skipBlobs)
                return;
            synchronized(this)
            {
                mInInitialCopyPhase = true;
            }
            mbox = getMailbox();
            lock = null;
            long start;
            lock = MailboxManager.getInstance().beginMaintenance(getAccountId(), mbox.getId());
            mMboxChangeIdAtStart = mbox.getLastChangeID();
            Log.backup.debug((new StringBuilder()).append("Mailbox ").append(mbox.getId()).append(" at changeId ").append(mMboxChangeIdAtStart).append(" at backup start").toString());
            start = System.currentTimeMillis();
            loadBlobDigestMap();
            long now = System.currentTimeMillis();
            mBackupSet.mStats.mDbDigestMapTime.add(now - start);
            start = now;
            break MISSING_BLOCK_LABEL_171;
            Exception exception1;
            exception1;
            long now = System.currentTimeMillis();
            mBackupSet.mStats.mDbDigestMapTime.add(now - start);
            start = now;
            throw exception1;
            if(lock != null)
                MailboxManager.getInstance().endMaintenance(lock, getError() == null, false);
            break MISSING_BLOCK_LABEL_227;
            Exception exception2;
            exception2;
            if(lock != null)
                MailboxManager.getInstance().endMaintenance(lock, getError() == null, false);
            throw exception2;
            mBackupSet.mBlobWorkerThread.doAccountSession(this);
            waitForBlobWorkerCompletion();
            mFCMonitor.waitForCompletion();
            synchronized(this)
            {
                mInInitialCopyPhase = false;
            }
            return;
        }

        protected void storeAccount()
            throws IOException, ServiceException
        {
            long start = System.currentTimeMillis();
            Account account = getAccount();
            File acctFile = new File(mDir, "ldap.xml");
            Provisioning prov = Provisioning.getInstance();
            List identities = null;
            List allIdentities = prov.getAllIdentities(account);
            if(allIdentities != null)
            {
                identities = new ArrayList();
                Iterator i$ = allIdentities.iterator();
                do
                {
                    if(!i$.hasNext())
                        break;
                    Identity iden = (Identity)i$.next();
                    String name = iden.getName();
                    if(name != null && !name.equalsIgnoreCase("DEFAULT"))
                        identities.add(iden);
                } while(true);
            }
            List signatures = null;
            List allSignatures = prov.getAllSignatures(account);
            if(allSignatures != null)
            {
                String sigIdOnAcct = account.getAttr("zimbraSignatureId", null);
                signatures = new ArrayList();
                Iterator i$ = allSignatures.iterator();
                do
                {
                    if(!i$.hasNext())
                        break;
                    Signature sig = (Signature)i$.next();
                    String sigId = sig.getId();
                    if(sigId != null && !sigId.equalsIgnoreCase(sigIdOnAcct))
                        signatures.add(sig);
                } while(true);
            }
            String distributionLists[] = null;
            List dlList = prov.getDistributionLists(account, true, null);
            if(dlList != null && dlList.size() > 0)
            {
                distributionLists = new String[dlList.size()];
                int i = 0;
                for(Iterator i$ = dlList.iterator(); i$.hasNext();)
                {
                    DistributionList dl = (DistributionList)i$.next();
                    distributionLists[i] = dl.getName();
                    i++;
                }

            }
            XmlMeta.writeAccountLdap(account, account.getMailAlias(), identities, prov.getAllDataSources(account), signatures, distributionLists, acctFile);
            mBackupSet.mStats.mLdapBytes.add(acctFile.length());
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mLdapTime.add(end - start);
            break MISSING_BLOCK_LABEL_400;
            Exception exception;
            exception;
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mLdapTime.add(end - start);
            throw exception;
        }

        public synchronized void storeTables()
        {
            mBackupSet.mDbWorkerThread.doAccountSession(this);
        }

        void storeTablesStage()
            throws IOException, ServiceException
        {
            long start = System.currentTimeMillis();
            verifyStateForBackup();
            File dbDir = new File(mDir, "db");
            FileUtil.ensureDirExists(dbDir);
            long bytes = storeTablesToLocalFile(dbDir);
            mBackupSet.mStats.mDbBytes.add(bytes);
            long now = System.currentTimeMillis();
            mBackupSet.mStats.mDbTime.add(now - start);
            break MISSING_BLOCK_LABEL_99;
            Exception exception;
            exception;
            long now = System.currentTimeMillis();
            mBackupSet.mStats.mDbTime.add(now - start);
            throw exception;
        }

        public synchronized void storeIndex()
        {
            if(!getParams().skipSearchIndex)
                mBackupSet.mIndexWorkerThread.doAccountSession(this);
        }

        void storeIndexStage()
            throws ServiceException, IOException
        {
            long start = System.currentTimeMillis();
            verifyStateForBackup();
            storeIndexImpl();
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mIndexTime.add(end - start);
            break MISSING_BLOCK_LABEL_62;
            Exception exception;
            exception;
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mIndexTime.add(end - start);
            throw exception;
        }

        public synchronized void storeBlobs()
            throws ServiceException, IOException
        {
            storeBlobsModifiedSince(mMboxChangeIdAtStart);
        }

        void storeBlobsStage()
            throws ServiceException, IOException
        {
            long start = System.currentTimeMillis();
            verifyStateForBackup();
            if(blobsZipped())
                mBlobRepo = new ZipBlobRepository(getMailboxId(), mBlobsDir, getBackupSet().getParams().zipStore);
            else
            if(isBlobCompressedDeprecated())
                mBlobRepo = new DeprecatedZipBlobRepository(new File(mDir, "blobs.zip"), true);
            else
                mBlobRepo = new DirBlobRepository(mBlobsDir, true);
            storeBlobsImpl();
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mMsgsTime.add(end - start);
            break MISSING_BLOCK_LABEL_152;
            Exception exception;
            exception;
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mMsgsTime.add(end - start);
            throw exception;
        }

        void storeBlobsStageCleanup()
        {
            mFCMonitor.waitForCompletion();
            try
            {
                if(mBlobRepo != null)
                    mBlobRepo.close();
            }
            catch(IOException e)
            {
                Log.backup.warn("Error while closing blob repository", e);
            }
            mBlobRepo = null;
        }

        public void storeBlob(String digest, Volume vol, File msgFile)
            throws ServiceException, IOException
        {
            String msgFileAbsPath;
            long length;
            long linkCount;
            String relPath;
            synchronized(mInterruptedGuard)
            {
                if(mInterrupted)
                    throw new IOException("Backup account session interrupted");
            }
            msgFileAbsPath = msgFile.getAbsolutePath();
            String msgFileName;
            String parentFileName;
            StringBuilder relPathSb;
            try
            {
                length = msgFile.length();
                File parentFile = msgFile.getParentFile();
                msgFileName = msgFile.getName();
                parentFileName = parentFile == null ? null : parentFile.getName();
                if(length == 0L && !msgFile.exists() || parentFileName == null)
                {
                    Log.backup.debug((new StringBuilder()).append("Skipping concurrently deleted blob ").append(msgFileAbsPath).toString());
                    return;
                }
            }
            catch(FileNotFoundException e)
            {
                Log.backup.debug((new StringBuilder()).append("Skipping concurrently deleted blob ").append(msgFileAbsPath).toString());
                return;
            }
            linkCount = mBackupSet.mDisableSharedBlobs ? 1L : getLinkCount(msgFile);
            relPathSb = new StringBuilder();
            relPathSb.append(vol.getId()).append(File.separator);
            relPathSb.append(parentFileName).append(File.separator).append(digest).append(msgFileName);
            relPathSb.append(String.valueOf(linkCount));
            relPath = relPathSb.toString();
            boolean shared;
            long start;
            shared = linkCount > 1L && !mBackupSet.mDisableSharedBlobs;
            if(!shared)
                break MISSING_BLOCK_LABEL_633;
            if(!DbSharedBlobDigest.contains(digest))
            {
                DbSharedBlobDigest.add(digest);
                SharedBlob sharedBlob = mBackupSet.getSharedBlobFile(digest);
                File sharedFile = sharedBlob.getFile();
                boolean linked = false;
                if(!getBackupSet().sharedBlobsZipped())
                {
                    File fromAnotherBackup = mBackupSet.findSharedFileInAnotherBackupSet(sharedBlob);
                    if(fromAnotherBackup != null)
                    {
                        boolean checkLinkTargetExists;
                        synchronized(this)
                        {
                            checkLinkTargetExists = !mInInitialCopyPhase;
                        }
                        if(!checkLinkTargetExists || !sharedFile.exists())
                        {
                            long start = System.currentTimeMillis();
                            mBackupSet.mCopier.link(fromAnotherBackup, sharedFile, mFCMonitor, sharedFile);
                            long end = System.currentTimeMillis();
                            mBackupSet.mStats.mMsgLinkTime.add(end - start);
                        }
                        linked = true;
                    }
                }
                if(!linked)
                {
                    if(Log.backup.isDebugEnabled())
                        Log.backup.debug((new StringBuilder()).append("Copying ").append(msgFileAbsPath).append(" as shared blob ").append(sharedFile.getAbsolutePath()).toString());
                    long start = System.currentTimeMillis();
                    mBackupSet.mSharedBlobRepo.write(digest, msgFile, mBackupSet.mCopier, mFCMonitor);
                    long end = System.currentTimeMillis();
                    mBackupSet.mStats.mMsgCopyTime.add(end - start);
                    mBackupSet.mStats.mMsgCopyBytes.add(length);
                }
            }
            start = System.currentTimeMillis();
            mBlobRepo.write(relPath, digest, null, mBackupSet.mCopier, mFCMonitor);
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mMsgLinkTime.add(end - start);
            break MISSING_BLOCK_LABEL_959;
            Exception exception2;
            exception2;
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mMsgLinkTime.add(end - start);
            throw exception2;
            long start;
            boolean linked = false;
            if(!blobsZipped() && !isBlobCompressedDeprecated() && mPrevBlobsDir != null)
            {
                File prev = new File(mPrevBlobsDir, relPath);
                if(prev.exists())
                {
                    long prevLen = prev.length();
                    boolean valid;
                    if(shared)
                        valid = prevLen == 0L;
                    else
                        valid = prevLen == length;
                    if(valid)
                    {
                        File link = new File(mBlobsDir, relPath);
                        boolean checkLinkTargetExists;
                        synchronized(this)
                        {
                            checkLinkTargetExists = !mInInitialCopyPhase;
                        }
                        if(!checkLinkTargetExists || !link.exists())
                        {
                            long start = System.currentTimeMillis();
                            mBackupSet.mCopier.link(prev, link, mFCMonitor, link);
                            long end = System.currentTimeMillis();
                            mBackupSet.mStats.mMsgLinkTime.add(end - start);
                        }
                        linked = true;
                    }
                }
            }
            if(linked)
                break MISSING_BLOCK_LABEL_959;
            start = System.currentTimeMillis();
            mBlobRepo.write(relPath, digest, msgFile, mBackupSet.mCopier, mFCMonitor);
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mMsgCopyTime.add(end - start);
            mBackupSet.mStats.mMsgCopyBytes.add(length);
            break MISSING_BLOCK_LABEL_959;
            Exception exception4;
            exception4;
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mMsgCopyTime.add(end - start);
            mBackupSet.mStats.mMsgCopyBytes.add(length);
            throw exception4;
        }

        protected void storeIndexFile(File src, String relPathDest)
            throws IOException
        {
            synchronized(mInterruptedGuard)
            {
                if(mInterrupted)
                    throw new IOException("Backup account session interrupted");
            }
            StringBuilder destPath = new StringBuilder();
            destPath.append(mDir.getAbsolutePath()).append(File.separator);
            destPath.append("index").append(File.separator);
            destPath.append(relPathDest);
            File dest = new File(destPath.toString());
            long start = System.currentTimeMillis();
            mBackupSet.mCopier.copy(src, dest, mFCMonitor, dest);
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mIndexTime.add(end - start);
            mBackupSet.mStats.mIndexBytes.add(src.length());
        }

        protected Thread[] getWorkerThreads()
        {
            return mBackupSet.getWorkerThreads();
        }

        private void waitForBlobWorkerCompletion()
            throws ServiceException
        {
            try
            {
                int idx = mBackupSet.mBlobStartBarrier.await();
                Log.backup.debug((new StringBuilder()).append("BlobStartBarrier.await() = ").append(idx).toString());
            }
            catch(InterruptedException e)
            {
                throw ServiceException.FAILURE("InterruptedException while waiting for start barrier.", e);
            }
            catch(BrokenBarrierException e)
            {
                throw ServiceException.FAILURE("BrokenBarrierException while waiting for start barrier.", e);
            }
            try
            {
                int idx = mBackupSet.mBlobFinishBarrier.await();
                Log.backup.debug((new StringBuilder()).append("BlobFinishBarrier.await() = ").append(idx).toString());
            }
            catch(InterruptedException e)
            {
                throw ServiceException.FAILURE("InterruptedException while waiting for finish barrier.", e);
            }
            catch(BrokenBarrierException e)
            {
                throw ServiceException.FAILURE("BrokenBarrierException while waiting for finish barrier.", e);
            }
        }

        protected void waitForFullBackupCompletion()
            throws ServiceException
        {
            try
            {
                int idx = mBackupSet.mDbIndexStartBarrier.await();
                Log.backup.debug((new StringBuilder()).append("DbIndexStartBarrier.await() = ").append(idx).toString());
            }
            catch(InterruptedException e)
            {
                Log.backup.warn("InterruptedException while waiting for start barrier.", e);
            }
            catch(BrokenBarrierException e)
            {
                Log.backup.warn("BrokenBarrierException while waiting for start barrier.", e);
            }
            try
            {
                int idx = mBackupSet.mDbIndexFinishBarrier.await();
                Log.backup.debug((new StringBuilder()).append("DbIndexFinishBarrier.await() = ").append(idx).toString());
            }
            catch(InterruptedException e)
            {
                Log.backup.warn("InterruptedException while waiting for finish barrier.", e);
            }
            catch(BrokenBarrierException e)
            {
                Log.backup.warn("BrokenBarrierException while waiting for finish barrier.", e);
            }
            Throwable err = null;
            BackupSetWorkerThread arr$[] = mBackupSet.getWorkerThreads();
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                BackupSetWorkerThread worker = arr$[i$];
                if(err != null)
                    continue;
                err = worker.getError();
                if(err != null)
                {
                    initError(err);
                    markInterrupted();
                }
            }

            storeBlobsStageCleanup();
            break MISSING_BLOCK_LABEL_197;
            Exception exception;
            exception;
            storeBlobsStageCleanup();
            throw exception;
        }

        public void endFullBackup()
            throws IOException, ServiceException
        {
            storeBlobsStageCleanup();
            super.endFullBackup();
            if(getError() == null)
            {
                mBackupSet.markAccountBackupTime(getMailboxId(), (int)(getStartTime() / 1000L));
                File bakFile = new File(mDir, "meta.xml");
                XmlMeta.writeAccountBackup(this, bakFile);
                String label = getBackupSet().getLabel();
                String acctId = getAccountId();
                mLatestSessionForAccount.put(acctId, label);
                mLatestAcctNameId.put(getAccount().getName(), acctId);
            } else
            {
                Log.backup.warn((new StringBuilder()).append("Error occurred during full backup for account ").append(getAccountName()).append("; aborted backup and deleting ").append(mDir.getPath()).toString(), getError());
                FileUtil.deleteDir(mDir);
            }
            long start = getStartTime();
            long end = getEndTime();
            mBackupSet.mStats.mAccountsTime.add(end - start);
            break MISSING_BLOCK_LABEL_224;
            Exception exception;
            exception;
            long start = getStartTime();
            long end = getEndTime();
            mBackupSet.mStats.mAccountsTime.add(end - start);
            throw exception;
        }

        public void incrementalBackup()
            throws IOException, ServiceException
        {
            long start = System.currentTimeMillis();
            super.incrementalBackup();
            if(getError() == null && mFullSession != null)
            {
                FileBackupSet fullBackupSet = (FileBackupSet)mFullSession.getBackupSet();
                File fullAcctDir = fullBackupSet.getAccountDir(getAccountId());
                if(fullAcctDir.exists())
                {
                    File fullAcctFile = new File(fullAcctDir, "ldap_latest.xml");
                    File acctFile = new File(mBackupSet.getAccountDir(getAccountId()), "ldap.xml");
                    Utils.link(fullAcctFile, acctFile);
                }
            }
            mFCMonitor.waitForCompletion();
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mAccountsTime.add(end - start);
            break MISSING_BLOCK_LABEL_157;
            Exception exception;
            exception;
            mFCMonitor.waitForCompletion();
            long end = System.currentTimeMillis();
            mBackupSet.mStats.mAccountsTime.add(end - start);
            throw exception;
        }

        private FileBackupSet mBackupSet;
        private File mDir;
        private BlobRepository mBlobRepo;
        private final Object mInterruptedGuard = new Object();
        private boolean mInterrupted;
        private BackupFileCopierMonitor mFCMonitor;
        private File mBlobsDir;
        private File mPrevBlobsDir;
        private int mMboxChangeIdAtStart;
        private boolean mInInitialCopyPhase;
        final FileBackupTarget this$0;

        BackupAcctSession(BackupSet bak, Account account)
            throws IOException, ServiceException
        {
            this$0 = FileBackupTarget.this;
            super(bak, account, bak.getType(), Log.backup);
            mMboxChangeIdAtStart = -1;
            mBackupSet = (FileBackupSet)bak;
            String accountId = account.getId();
            mDir = mBackupSet.getAccountDir(accountId);
            if(mDir.exists())
                throw BackupServiceException.ACCOUNT_BACKUP_EXISTS(accountId);
            if(!mDir.mkdirs())
                throw new IOException((new StringBuilder()).append("Cannot create backup directory for account ").append(accountId).toString());
            mFCMonitor = new BackupFileCopierMonitor(mBackupSet, this);
            mBlobsDir = new File(mDir, "blobs");
            if(!BackupLC.backup_disable_links.booleanValue())
            {
                RestoreAcctSession sess = null;
                try
                {
                    sess = (RestoreAcctSession)getAccountSession(accountId);
                }
                catch(ServiceException e) { }
                if(sess != null)
                    mPrevBlobsDir = sess.getBlobsDir();
            }
        }
    }

    class FileBackupSet extends BackupSet
    {

        private void startWorkerThreads()
        {
            BackupParams params = getParams();
            List workers = new ArrayList();
            int blobBarrierCount = 2;
            if(params.skipBlobs)
                blobBarrierCount--;
            mBlobStartBarrier = new CyclicBarrier(blobBarrierCount);
            mBlobFinishBarrier = new CyclicBarrier(blobBarrierCount);
            if(!params.skipBlobs)
            {
                mBlobWorkerThread = new BackupSetWorkerThread(BackupSetWorkerThread.Stage.BLOB, mBlobStartBarrier, mBlobFinishBarrier);
                workers.add(mBlobWorkerThread);
            }
            int dbIndexBarrierCount = 3;
            if(params.skipSearchIndex)
                dbIndexBarrierCount--;
            mDbIndexStartBarrier = new CyclicBarrier(dbIndexBarrierCount);
            mDbIndexFinishBarrier = new CyclicBarrier(dbIndexBarrierCount);
            mDbWorkerThread = new BackupSetWorkerThread(BackupSetWorkerThread.Stage.DB, mDbIndexStartBarrier, mDbIndexFinishBarrier);
            workers.add(mDbWorkerThread);
            if(!params.skipSearchIndex)
            {
                mIndexWorkerThread = new BackupSetWorkerThread(BackupSetWorkerThread.Stage.INDEX, mDbIndexStartBarrier, mDbIndexFinishBarrier);
                workers.add(mIndexWorkerThread);
            }
            mWorkerThreads = new BackupSetWorkerThread[workers.size()];
            mWorkerThreads = (BackupSetWorkerThread[])workers.toArray(new BackupSetWorkerThread[0]);
            BackupSetWorkerThread arr$[] = mWorkerThreads;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                BackupSetWorkerThread thread = arr$[i$];
                thread.start();
            }

            mWorkerThreadsStarted = true;
        }

        private void stopWorkerThreads()
        {
            if(!mWorkerThreadsStarted)
                return;
            BackupSetWorkerThread arr$[] = mWorkerThreads;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                BackupSetWorkerThread thread = arr$[i$];
                thread.signalShutdown();
            }

            try
            {
                int idx = mBlobStartBarrier.await();
                Log.backup.debug((new StringBuilder()).append("BlobStartBarrier.await() = ").append(idx).toString());
            }
            catch(InterruptedException e)
            {
                Log.backup.warn("InterruptedException while waiting for worker threads to stop", e);
            }
            catch(BrokenBarrierException e)
            {
                Log.backup.warn("BrokenBarrierException while waiting for worker threads to stop", e);
            }
            try
            {
                int idx = mDbIndexStartBarrier.await();
                Log.backup.debug((new StringBuilder()).append("DbIndexStartBarrier.await() = ").append(idx).toString());
            }
            catch(InterruptedException e)
            {
                Log.backup.warn("InterruptedException while waiting for worker threads to stop", e);
            }
            catch(BrokenBarrierException e)
            {
                Log.backup.warn("BrokenBarrierException while waiting for worker threads to stop", e);
            }
            e = mWorkerThreads;
            len$ = e.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                BackupSetWorkerThread t = e[i$];
                try
                {
                    t.join();
                }
                catch(InterruptedException e)
                {
                    Log.backup.warn((new StringBuilder()).append("InterruptedException while waiting for worker thread ").append(t.getName()).append(" to stop").toString(), e);
                }
            }

        }

        BackupSetWorkerThread[] getWorkerThreads()
        {
            return mWorkerThreads;
        }

        private void createPlaceholderFile()
            throws IOException
        {
            mPlaceholderFile = new File(mDir, "placeholder.tmp");
            RandomAccessFile placeholder = new RandomAccessFile(mPlaceholderFile, "rw");
            placeholder.setLength(0xa00000L);
            placeholder.close();
        }

        private void deletePlaceholderFile()
        {
            if(mPlaceholderFile != null && mPlaceholderFile.exists())
            {
                boolean deleted = mPlaceholderFile.delete();
                if(!deleted)
                    Log.backup.warn((new StringBuilder()).append("Unable to delete placeholder file ").append(mPlaceholderFile.getAbsolutePath()).toString());
                mPlaceholderFile = null;
            }
        }

        private void markAccountBackupTime(int mboxId, int timeSec)
        {
            mBackupTstamps.put(Integer.valueOf(mboxId), Integer.valueOf(timeSec));
        }

        private void saveAccountBackupTimes()
            throws ServiceException
        {
            com.zimbra.cs.db.DbPool.Connection conn;
            boolean success;
            conn = null;
            success = false;
            conn = DbPool.getConnection();
            int mboxId;
            int timeSec;
            for(Iterator i$ = mBackupTstamps.entrySet().iterator(); i$.hasNext(); DbBackup.updateMailboxBackupTime(conn, mboxId, timeSec))
            {
                java.util.Map.Entry entry = (java.util.Map.Entry)i$.next();
                mboxId = ((Integer)entry.getKey()).intValue();
                timeSec = ((Integer)entry.getValue()).intValue();
            }

            success = true;
            if(conn == null)
                break MISSING_BLOCK_LABEL_200;
            if(success)
                conn.commit();
            else
                DbPool.quietRollback(conn);
            DbPool.quietClose(conn);
            break MISSING_BLOCK_LABEL_200;
            ServiceException e;
            e;
            Log.backup.warn("Error during commit/rollback to save backup times", e);
            DbPool.quietClose(conn);
            break MISSING_BLOCK_LABEL_200;
            Exception exception;
            exception;
            DbPool.quietClose(conn);
            throw exception;
            Exception exception1;
            exception1;
            if(conn == null)
                break MISSING_BLOCK_LABEL_197;
            if(success)
                conn.commit();
            else
                DbPool.quietRollback(conn);
            DbPool.quietClose(conn);
            break MISSING_BLOCK_LABEL_197;
            ServiceException e;
            e;
            Log.backup.warn("Error during commit/rollback to save backup times", e);
            DbPool.quietClose(conn);
            break MISSING_BLOCK_LABEL_197;
            Exception exception2;
            exception2;
            DbPool.quietClose(conn);
            throw exception2;
            throw exception1;
        }

        public void startFullBackup()
            throws IOException, ServiceException
        {
            try
            {
                AbstractBackupTarget.mRunningBackups.put(getKey(getLabel()), this);
                DbSharedBlobDigest.initialize();
                mCopier.start();
                startWorkerThreads();
            }
            catch(Exception e)
            {
                addError(e);
                throw ServiceException.FAILURE("Unable to start full backup", e);
            }
            if(sharedBlobsZipped())
            {
                BackupParams params = getParams();
                boolean noCompression = params == null ? false : params.zipStore;
                mSharedBlobRepo = new ZipSharedBlobRepository(mSharedBlobsDir, noCompression, sharedBlobsDirectoryDepth(), sharedBlobsCharsPerDirectory(), sharedBlobsZipNameDigestChars());
            } else
            {
                mSharedBlobRepo = new DirSharedBlobRepository(mSharedBlobsDir, sharedBlobsDirectoryDepth(), sharedBlobsCharsPerDirectory(), true);
            }
            super.startFullBackup();
            try
            {
                if(mSharedBlobRepo != null)
                    mSharedBlobRepo.close();
            }
            catch(IOException e) { }
            mSharedBlobRepo = null;
            break MISSING_BLOCK_LABEL_190;
            Exception exception;
            exception;
            try
            {
                if(mSharedBlobRepo != null)
                    mSharedBlobRepo.close();
            }
            catch(IOException e) { }
            mSharedBlobRepo = null;
            throw exception;
            if(!hadOutOfDiskError())
                backupLdap();
            return;
        }

        public void endFullBackup()
            throws IOException, ServiceException
        {
            File newAccountsXml;
            stopWorkerThreads();
            if(mCopier != null)
            {
                mFCMonitor.waitForCompletion();
                mCopier.shutdown();
            }
            AbstractBackupTarget.mRunningBackups.remove(getKey(getLabel()));
            super.endFullBackup();
            long start = getStartTime();
            long end = getEndTime();
            mStats.mTotalTime.add(end - start);
            newAccountsXml = null;
            deletePlaceholderFile();
            XmlMeta.writeBackupSet(this, mSessionMetaFile);
            String tmpFname = "accounts.xml.new";
            File xml = new File(mBaseDir, tmpFname);
            XmlMeta.writeAccountMaps(mLatestAcctNameId, mLatestSessionForAccount, xml);
            newAccountsXml = xml;
            renameSessionMetaFile();
            if(hasErrorOccurred())
                raiseError();
            try
            {
                DbSharedBlobDigest.cleanup();
            }
            catch(ServiceException e)
            {
                Log.backup.warn("Error cleaning up shared blob digests table", e);
            }
            if(!hadOutOfDiskError())
            {
                moveSession();
                if(!getBackupTarget().isCustom())
                    saveAccountBackupTimes();
                if(newAccountsXml != null)
                {
                    File accountsXml = new File(mBaseDir, "accounts.xml");
                    File renamedOld = new File(mBaseDir, "accounts.xml.old");
                    if(accountsXml.exists() && !accountsXml.renameTo(renamedOld))
                        throw new IOException((new StringBuilder()).append("Unable to rename ").append(accountsXml.getAbsolutePath()).append(" to ").append(renamedOld.getAbsolutePath()).toString());
                    boolean renamed = newAccountsXml.renameTo(accountsXml);
                    if(!renamed)
                        throw new IOException((new StringBuilder()).append("Unable to rename ").append(newAccountsXml.getAbsolutePath()).append(" to ").append(accountsXml.getAbsolutePath()).toString());
                }
                deleteBackedUpRedoLogs();
            } else
            {
                Log.backup.info((new StringBuilder()).append("Discarding current backup because of out-of-disk error.  Deleting ").append(mDir.getAbsolutePath()).toString());
                FileUtil.deleteDir(mDir);
                if(newAccountsXml != null)
                    newAccountsXml.delete();
            }
            break MISSING_BLOCK_LABEL_681;
            Exception exception;
            exception;
            try
            {
                DbSharedBlobDigest.cleanup();
            }
            catch(ServiceException e)
            {
                Log.backup.warn("Error cleaning up shared blob digests table", e);
            }
            if(!hadOutOfDiskError())
            {
                moveSession();
                if(!getBackupTarget().isCustom())
                    saveAccountBackupTimes();
                if(newAccountsXml != null)
                {
                    File accountsXml = new File(mBaseDir, "accounts.xml");
                    File renamedOld = new File(mBaseDir, "accounts.xml.old");
                    if(accountsXml.exists() && !accountsXml.renameTo(renamedOld))
                        throw new IOException((new StringBuilder()).append("Unable to rename ").append(accountsXml.getAbsolutePath()).append(" to ").append(renamedOld.getAbsolutePath()).toString());
                    boolean renamed = newAccountsXml.renameTo(accountsXml);
                    if(!renamed)
                        throw new IOException((new StringBuilder()).append("Unable to rename ").append(newAccountsXml.getAbsolutePath()).append(" to ").append(accountsXml.getAbsolutePath()).toString());
                }
                deleteBackedUpRedoLogs();
            } else
            {
                Log.backup.info((new StringBuilder()).append("Discarding current backup because of out-of-disk error.  Deleting ").append(mDir.getAbsolutePath()).toString());
                FileUtil.deleteDir(mDir);
                if(newAccountsXml != null)
                    newAccountsXml.delete();
            }
            throw exception;
        }

        private void renameSessionMetaFile()
        {
            File dir = mSessionMetaFile.getParentFile();
            File newName = new File(dir, "session.xml");
            String oldName = mSessionMetaFile.getAbsolutePath();
            boolean renamed = mSessionMetaFile.renameTo(newName);
            if(!renamed)
            {
                IOException ioe = new IOException((new StringBuilder()).append("Unable to rename ").append(oldName).append(" to ").append(newName.getAbsolutePath()).toString());
                addError(ioe);
            }
        }

        public void startIncrementalBackup()
            throws IOException, ServiceException
        {
            AbstractBackupTarget.mRunningBackups.put(getKey(getLabel()), this);
            mCopier.start();
            super.startIncrementalBackup();
            if(!hadOutOfDiskError())
                backupLdap();
        }

        public void endIncrementalBackup()
            throws IOException, ServiceException
        {
            if(mCopier != null)
            {
                mFCMonitor.waitForCompletion();
                mCopier.shutdown();
            }
            AbstractBackupTarget.mRunningBackups.remove(getKey(getLabel()));
            super.endIncrementalBackup();
            long start = getStartTime();
            long end = getEndTime();
            mStats.mTotalTime.add(end - start);
            deletePlaceholderFile();
            XmlMeta.writeBackupSet(this, mSessionMetaFile);
            renameSessionMetaFile();
            if(hasErrorOccurred())
                raiseError();
            if(!hadOutOfDiskError())
            {
                moveSession();
                if(!getBackupTarget().isCustom())
                    saveAccountBackupTimes();
                deleteBackedUpRedoLogs();
            } else
            {
                Log.backup.info((new StringBuilder()).append("Discarding current backup because of out-of-disk error.  Deleting ").append(mDir.getAbsolutePath()).toString());
                FileUtil.deleteDir(mDir);
            }
            break MISSING_BLOCK_LABEL_249;
            Exception exception;
            exception;
            if(!hadOutOfDiskError())
            {
                moveSession();
                if(!getBackupTarget().isCustom())
                    saveAccountBackupTimes();
                deleteBackedUpRedoLogs();
            } else
            {
                Log.backup.info((new StringBuilder()).append("Discarding current backup because of out-of-disk error.  Deleting ").append(mDir.getAbsolutePath()).toString());
                FileUtil.deleteDir(mDir);
            }
            throw exception;
        }

        private void deleteBackedUpRedoLogs()
        {
            if(mRedoLogsToDelete != null)
            {
                File arr$[] = mRedoLogsToDelete;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    File file = arr$[i$];
                    Log.backup.info((new StringBuilder()).append("Deleting redo log ").append(file.getPath()).toString());
                    boolean deleted = file.delete();
                    if(!deleted)
                        Log.backup.warn((new StringBuilder()).append("Failed to delete redo log ").append(file.getPath()).append(" after copying to incremental backup").toString());
                }

                mRedoLogsToDelete = null;
            }
        }

        private void moveSession()
            throws IOException
        {
            File targetPath = new File(mSessionsDir, getLabel());
            if(!mSessionsDir.exists())
                Log.backup.warn((new StringBuilder()).append(mSessionsDir.getPath()).append(" doesn't exist! try to create it AGAIN: ").append(" successful? ").append(mSessionsDir.mkdirs()).toString());
            if(!mDir.renameTo(targetPath))
            {
                throw new IOException((new StringBuilder()).append("Unable to rename ").append(mDir.getPath()).append(" to ").append(targetPath).toString());
            } else
            {
                mDir = targetPath;
                return;
            }
        }

        private File getAccountDir(String accountId)
        {
            String subdir = Utils.accountIdToBucketizedPath(accountId, mAccountDirDepth);
            return new File(mDir, (new StringBuilder()).append("accounts").append(File.separator).append(subdir).toString());
        }

        private SharedBlob getSharedBlobFile(String digest)
        {
            String relPath = SharedBlobRepository.getRelPath(digest, sharedBlobsDirectoryDepth(), sharedBlobsCharsPerDirectory());
            return new SharedBlob(mSharedBlobsDir, relPath);
        }

        private File findSharedFileInAnotherBackupSet(SharedBlob shareDest)
        {
            String relPath = shareDest.getRelativePath();
            for(Iterator i$ = mSharedBlobsDirList.iterator(); i$.hasNext();)
            {
                File shareRoot = (File)i$.next();
                File shareSrc = new File(shareRoot, relPath);
                if(shareSrc.exists() && shareSrc.isFile())
                    return shareSrc;
            }

            return null;
        }

        public void storeSystemData()
            throws IOException, ServiceException
        {
            long start;
            long bytes;
            File dir;
            com.zimbra.cs.db.DbPool.Connection conn;
            start = System.currentTimeMillis();
            bytes = 0L;
            dir = mSysDir;
            conn = null;
            long end;
            try
            {
                conn = DbPool.getConnection();
                conn.setTransactionIsolation(4);
                Set sysTables = DbBackup.getSystemTables(conn);
                sysTables.removeAll(FileBackupTarget.excludedSystemTables);
                File schemaFile = new File(dir, "db_schema.xml");
                List tableInfos = new ArrayList(sysTables.size());
                com.zimbra.cs.db.DbBackup.TableInfo tinfo;
                for(Iterator i$ = sysTables.iterator(); i$.hasNext(); tableInfos.add(tinfo))
                {
                    String tname = (String)i$.next();
                    tinfo = DbBackup.getTableInfo(conn, tname);
                }

                XmlMeta.writeTablesSchema(tableInfos, schemaFile);
                bytes += schemaFile.length();
                for(Iterator i$ = tableInfos.iterator(); i$.hasNext();)
                {
                    com.zimbra.cs.db.DbBackup.TableInfo tinfo = (com.zimbra.cs.db.DbBackup.TableInfo)i$.next();
                    bytes += DbBackup.saveTable(conn, tinfo, dir);
                }

                conn.commit();
                File zimbraConf = new File(LC.zimbra_home.value(), "conf");
                File targetConf = new File(dir, "localconfig.xml");
                FileUtil.copy(new File(zimbraConf, "localconfig.xml"), targetConf);
                bytes += targetConf.length();
            }
            catch(ServiceException e)
            {
                DbPool.quietRollback(conn);
                throw e;
            }
            DbPool.quietClose(conn);
            end = System.currentTimeMillis();
            mStats.mSysDbTime.add(end - start);
            mStats.mSysDbBytes.add(bytes);
            break MISSING_BLOCK_LABEL_334;
            Exception exception;
            exception;
            DbPool.quietClose(conn);
            long end = System.currentTimeMillis();
            mStats.mSysDbTime.add(end - start);
            mStats.mSysDbBytes.add(bytes);
            throw exception;
        }

        public void loadSystemData()
            throws IOException, ServiceException
        {
            boolean success;
            com.zimbra.cs.db.DbPool.Connection conn;
            success = false;
            conn = null;
            Exception exception;
            try
            {
                conn = DbPool.getConnection();
                conn.setTransactionIsolation(4);
                conn.disableForeignKeyConstraints();
                File dir = mSysDir;
                if(!dir.exists())
                    throw new FileNotFoundException((new StringBuilder()).append("unable to load system data: ").append(dir.getPath()).append(" not found").toString());
                File schemaFile = new File(dir, "db_schema.xml");
                Map dbSchema = XmlMeta.readTableSchema(schemaFile);
                Set zimbraTables = getZimbraTables();
                Iterator it = zimbraTables.iterator();
                do
                {
                    if(!it.hasNext())
                        break;
                    String tableName = (String)it.next();
                    File dbFile = DbBackup.getDbFile(mSysDir, tableName);
                    if(!FileBackupTarget.excludedSystemTables.contains(tableName) && dbFile.exists())
                        DbBackup.loadTable(conn, tableName, (com.zimbra.cs.db.DbBackup.TableInfo)dbSchema.get(tableName), dbFile, true);
                } while(true);
                DbBackup.updateConstraints(conn);
                Volume.reloadVolumes();
                File zimbraConf = new File(LC.zimbra_home.value(), "conf");
                FileUtil.copy(new File(dir, "localconfig.xml"), new File(zimbraConf, "localconfig.xml.restored"));
                success = true;
            }
            catch(ServiceException e)
            {
                DbPool.quietRollback(conn);
                throw e;
            }
            if(conn == null)
                break MISSING_BLOCK_LABEL_322;
            if(success)
                conn.commit();
            else
                DbPool.quietRollback(conn);
            conn.enableForeignKeyConstraints();
            break MISSING_BLOCK_LABEL_264;
            exception;
            conn.enableForeignKeyConstraints();
            throw exception;
            DbPool.quietClose(conn);
            break MISSING_BLOCK_LABEL_322;
            Exception exception1;
            exception1;
            if(conn == null)
                break MISSING_BLOCK_LABEL_319;
            if(success)
                conn.commit();
            else
                DbPool.quietRollback(conn);
            conn.enableForeignKeyConstraints();
            break MISSING_BLOCK_LABEL_315;
            Exception exception2;
            exception2;
            conn.enableForeignKeyConstraints();
            throw exception2;
            DbPool.quietClose(conn);
            throw exception1;
        }

        public BackupTarget getBackupTarget()
        {
            return FileBackupTarget.this;
        }

        protected void backupRedoLogs()
            throws IOException, ServiceException
        {
            long start;
            long bytes;
            start = System.currentTimeMillis();
            bytes = 0L;
            File logs[] = RedoLogProvider.getInstance().getRedoLogManager().getArchivedLogs();
            if(logs == null || logs.length == 0)
            {
                long end = System.currentTimeMillis();
                mStats.mRedologsTime.add(end - start);
                mStats.mRedologsBytes.add(bytes);
                return;
            }
            Log.backup.info((new StringBuilder()).append("Found ").append(logs.length).append(" redo logs to backup").toString());
            List toDelete = new ArrayList(logs.length);
            long keepTimeMS = BackupLC.backup_archived_redolog_keep_time.longValue() * 1000L;
            long keepThreshold = System.currentTimeMillis() - keepTimeMS;
            long seqLastBackedUp = getMostRecentRedoSequence();
            Log.backup.info((new StringBuilder()).append("Last backed-up redo log sequence = ").append(seqLastBackedUp).toString());
            Pair logSplit = Utils.splitRedoLogsAtSeq(logs, seqLastBackedUp);
            File oldLogs[] = (File[])((Pair)logSplit.getFirst()).getFirst();
            File newLogs[] = (File[])((Pair)logSplit.getFirst()).getSecond();
            List errors = (List)logSplit.getSecond();
            if(errors != null && errors.size() > 0)
            {
                ServiceException err;
                for(Iterator i$ = errors.iterator(); i$.hasNext(); addError(err))
                    err = (ServiceException)i$.next();

            }
            if(oldLogs != null)
            {
                File arr$[] = oldLogs;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    File log = arr$[i$];
                    FileLogReader reader = new FileLogReader(log);
                    if(reader.getHeader().getLastOpTstamp() < keepThreshold)
                    {
                        Log.backup.info((new StringBuilder()).append("Marking redo log ").append(log.getAbsolutePath()).append(" for deletion").toString());
                        toDelete.add(log);
                    }
                }

            }
            File redologsDir = new File(mDir, "redologs");
            if(newLogs != null && newLogs.length > 0)
            {
                File arr$[] = newLogs;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    File src = arr$[i$];
                    FileUtil.ensureDirExists(redologsDir);
                    File dest = new File(redologsDir, src.getName());
                    Log.backup.info((new StringBuilder()).append("Copying redo log ").append(src.getPath()).append(" to ").append(dest.getPath()).toString());
                    FileUtil.copy(src, dest);
                    bytes += src.length();
                    FileLogReader reader = new FileLogReader(src);
                    if(reader.getHeader().getLastOpTstamp() < keepThreshold)
                    {
                        Log.backup.info((new StringBuilder()).append("Marking redo log ").append(src.getAbsolutePath()).append(" for deletion").toString());
                        toDelete.add(src);
                    } else
                    {
                        Log.backup.info((new StringBuilder()).append("Retaining recent redo log ").append(src.getAbsolutePath()).toString());
                    }
                }

                setMinRedoSequence(RolloverManager.getSeqForFile(newLogs[0]));
                setMaxRedoSequence(RolloverManager.getSeqForFile(newLogs[newLogs.length - 1]));
            }
            mRedoLogsToDelete = new File[toDelete.size()];
            toDelete.toArray(mRedoLogsToDelete);
            long end = System.currentTimeMillis();
            mStats.mRedologsTime.add(end - start);
            mStats.mRedologsBytes.add(bytes);
            break MISSING_BLOCK_LABEL_737;
            Exception exception;
            exception;
            long end = System.currentTimeMillis();
            mStats.mRedologsTime.add(end - start);
            mStats.mRedologsBytes.add(bytes);
            throw exception;
        }

        public void decodeMetadata(Element backupSetElem)
            throws ServiceException
        {
            super.decodeMetadata(backupSetElem);
            Element statsElem = backupSetElem.getOptionalElement("stats");
            if(statsElem != null)
            {
                String name;
                Counter counter;
                for(Iterator iter = statsElem.elementIterator("counter"); iter.hasNext(); mStats.setCounter(name, counter))
                {
                    Element counterElem = (Element)iter.next();
                    name = counterElem.getAttribute("name");
                    long numSamples = counterElem.getAttributeLong("numSamples", 0L);
                    long sum = counterElem.getAttributeLong("sum", 0L);
                    String unit = counterElem.getAttribute("unit", null);
                    counter = new Counter(name, unit, sum, numSamples);
                }

            }
            mAccountDirDepth = (int)backupSetElem.getAttributeLong("accountsDirectoryDepth", 2L);
        }

        public List getStats()
        {
            return mStats.toList();
        }

        public int getAccountDirDepth()
        {
            return mAccountDirDepth;
        }

        protected void backupLdap()
        {
            try
            {
                boolean hasLdap = false;
                Server localServer = Provisioning.getInstance().getLocalServer();
                String enabledSvcs[] = localServer.getMultiAttr("zimbraServiceEnabled");
                String arr$[] = enabledSvcs;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    String svc = arr$[i$];
                    if(svc.equals("ldap"))
                        hasLdap = true;
                }

                if(hasLdap)
                {
                    String outdir = (new StringBuilder()).append(mDir).append(File.separator).append("ldap").toString();
                    outdir = outdir.replace('\\', '/');
                    String cmd = (new StringBuilder()).append("zmbackupldap --outdir ").append(outdir).toString();
                    Log.backup.debug((new StringBuilder()).append("Invoking: ").append(cmd).toString());
                    RemoteManager remote = RemoteManager.getRemoteManager(localServer);
                    remote.execute(cmd);
                }
            }
            catch(Exception e)
            {
                addError(ServiceException.FAILURE((new StringBuilder()).append("LDAP backup failed: ").append(e.getMessage()).toString(), e));
            }
        }

        private static final String SUBDIR_SYS = "sys";
        private static final String SUBDIR_ACCOUNTS = "accounts";
        private static final String SUBDIR_REDOLOGS = "redologs";
        private static final String SUBDIR_SHARED_BLOBS = "shared_blobs";
        private static final String FILE_SESSION_TEMP = "session.xml.tmp";
        private static final String FILE_PLACEHOLDER = "placeholder.tmp";
        private File mDir;
        private File mSysDir;
        private File mSharedBlobsDir;
        private File mSessionMetaFile;
        private List mSharedBlobsDirList;
        private SharedBlobRepository mSharedBlobRepo;
        private boolean mDisableSharedBlobs;
        private File mRedoLogsToDelete[];
        private FileCopier mCopier;
        private BackupFileCopierMonitor mFCMonitor;
        private FileBackupStats mStats;
        private int mAccountDirDepth;
        private Map mBackupTstamps;
        private CyclicBarrier mBlobStartBarrier;
        private CyclicBarrier mBlobFinishBarrier;
        private CyclicBarrier mDbIndexStartBarrier;
        private CyclicBarrier mDbIndexFinishBarrier;
        private BackupSetWorkerThread mDbWorkerThread;
        private BackupSetWorkerThread mBlobWorkerThread;
        private BackupSetWorkerThread mIndexWorkerThread;
        private BackupSetWorkerThread mWorkerThreads[];
        private boolean mWorkerThreadsStarted;
        private File mPlaceholderFile;
        final FileBackupTarget this$0;


















        FileBackupSet(String lbl, String desc, Account accounts[], int type, BackupParams params)
            throws ServiceException, IOException
        {
            this$0 = FileBackupTarget.this;
            super(lbl, desc, accounts, type, params, Log.backup);
            mStats = new FileBackupStats();
            mAccountDirDepth = Utils.getDefaultAccountDirDepth();
            mDir = new File(mSessionsTmpDir, lbl);
            if(mDir.exists())
                throw new IOException((new StringBuilder()).append("Cannot create backup session: ").append(lbl).append(" already exists").toString());
            mSysDir = new File(mDir, "sys");
            FileUtil.ensureDirExists(mSysDir);
            FileUtil.ensureDirExists(new File(mDir, "accounts"));
            if(type == 1)
            {
                mSharedBlobsDir = new File(mDir, "shared_blobs");
                FileUtil.ensureDirExists(mSharedBlobsDir);
            }
            mSessionMetaFile = new File(mDir, "session.xml.tmp");
            if(!mSessionMetaFile.createNewFile())
                throw new IOException((new StringBuilder()).append("Unable to create file ").append(mSessionMetaFile.getAbsolutePath()).toString());
            createPlaceholderFile();
            mSharedBlobsDirList = getSharedBlobsDirs();
            FileCopierOptions fcOpts = params.fcOpts;
            if(fcOpts != null)
                mCopier = FileCopierFactory.createCopier(fcOpts);
            else
                mCopier = FileBackupTarget.createFileCopierFromLC();
            mCopier.setIgnoreMissingSource(true);
            mFCMonitor = new BackupFileCopierMonitor(this);
            mDisableSharedBlobs = BackupLC.backup_disable_shared_blobs.booleanValue();
            mBackupTstamps = new HashMap(accounts.length);
        }

        public FileBackupSet(String label)
            throws ServiceException
        {
            this$0 = FileBackupTarget.this;
            super(label, Log.backup);
            mStats = new FileBackupStats();
            mAccountDirDepth = Utils.getDefaultAccountDirDepth();
            if(LabelType.INVALID.equals(FileBackupTarget.getLabelType(label)))
                throw BackupServiceException.INVALID_BACKUP_LABEL(label);
            mDir = new File(mSessionsDir, label);
            mSysDir = new File(mDir, "sys");
            if(!mDir.exists())
                throw BackupServiceException.NO_SUCH_BACKUP_LABEL(label, (new StringBuilder()).append(mDir.getPath()).append(" not found").toString());
            File sharedBlobsDir = new File(mDir, "shared_blobs");
            if(sharedBlobsDir.exists())
                mSharedBlobsDir = sharedBlobsDir;
            mSessionMetaFile = new File(mDir, "session.xml");
            if(!mSessionMetaFile.exists())
            {
                File tempFile = new File(mDir, "session.xml.tmp");
                if(tempFile.exists())
                    throw BackupServiceException.INCOMPLETE_BACKUP(label, (new StringBuilder()).append(tempFile.getAbsolutePath()).append(" was not renamed to ").append("session.xml").append("; backup did not complete successfully").toString());
                else
                    throw BackupServiceException.INCOMPLETE_BACKUP(label, (new StringBuilder()).append(mSessionMetaFile.getAbsolutePath()).append(" not found; backup did not complete successfully").toString());
            }
            try
            {
                Element backupSetElem = XmlMeta.readBackupSet(mSessionMetaFile);
                decodeMetadata(backupSetElem);
            }
            catch(ServiceException e)
            {
                throw ServiceException.FAILURE((new StringBuilder()).append("Unable to retrieve backup ").append(label).toString(), e);
            }
        }
    }

    private static class SharedBlob
    {

        public File getFile()
        {
            return mFile;
        }

        public String getRelativePath()
        {
            return mRelativePath;
        }

        private File mFile;
        private String mRelativePath;

        public SharedBlob(File base, String relPath)
        {
            mFile = new File(base, relPath);
            mRelativePath = relPath;
        }
    }

    class InvalidBackupSet extends BackupSet
    {

        public BackupTarget getBackupTarget()
        {
            return FileBackupTarget.this;
        }

        public void storeSystemData()
            throws ServiceException, IOException
        {
        }

        public void loadSystemData()
            throws ServiceException, IOException
        {
        }

        protected void backupRedoLogs()
            throws IOException, ServiceException
        {
        }

        public List getStats()
        {
            return null;
        }

        private File mDir;
        final FileBackupTarget this$0;


        private InvalidBackupSet(String label, Exception err)
        {
            this$0 = FileBackupTarget.this;
            super(label, Log.backup);
            mDir = new File(mSessionsDir, label);
            addError(err);
            try
            {
                long t = BackupManager.getLabelDate(label);
                setStartTime(t);
            }
            catch(ServiceException e)
            {
                if(mDir.exists())
                    setStartTime(mDir.lastModified());
                else
                    setStartTime(System.currentTimeMillis());
            }
        }

    }


    public FileBackupTarget(File base, boolean create, boolean customDest)
        throws ServiceException, IOException
    {
        mRestorePathCache = null;
        mCustomDest = false;
        mBackupSetCache = new HashMap();
        mCustomDest = customDest;
        mBaseDir = base;
        mSessionsDir = new File(base, "sessions");
        mSessionsTmpDir = new File(base, "tmp");
        if(!base.exists())
            throw BackupServiceException.INVALID_BACKUP_TARGET((new StringBuilder()).append("Backup target ").append(base.getPath()).append(" does not exist").toString());
        if(!base.isDirectory())
            throw BackupServiceException.INVALID_BACKUP_TARGET((new StringBuilder()).append("Backup target ").append(base.getPath()).append("is not a directory").toString());
        if(create)
        {
            if(!base.canWrite())
                throw BackupServiceException.INVALID_BACKUP_TARGET((new StringBuilder()).append("Backup target ").append(base.getPath()).append(" is not writable").toString());
            FileUtil.ensureDirExists(mSessionsDir);
            FileUtil.ensureDirExists(mSessionsTmpDir);
        }
        if(!base.canRead())
        {
            throw BackupServiceException.INVALID_BACKUP_TARGET((new StringBuilder()).append("Backup target ").append(base.getPath()).append(" is not readable").toString());
        } else
        {
            reloadLatestAccountMaps();
            return;
        }
    }

    public boolean isCustom()
    {
        return mCustomDest;
    }

    public boolean hasEnoughFreeSpace(String threshold)
    {
        return !Utils.freeSpaceLessThan(mBaseDir, threshold);
    }

    public boolean outOfSpace()
    {
        String threshold = BackupLC.backup_out_of_disk_threshold.value();
        return Utils.freeSpaceLessThan(mBaseDir, threshold);
    }

    public BackupServiceException makeOutOfSpaceException(IOException cause)
    {
        return BackupServiceException.OUT_OF_DISK(mBaseDir, cause);
    }

    public String getURI()
    {
        return mBaseDir.getPath();
    }

    public BackupSet createFullBackupSet(String label, String desc, Account accounts[], BackupParams params)
        throws IOException, ServiceException
    {
        return new FileBackupSet(label, desc, accounts, 1, params);
    }

    public BackupSet getBackupSet()
        throws ServiceException
    {
        BackupSet bak = lookupLastBackupSession();
        if(bak == null)
            throw ServiceException.FAILURE("Previous backup session not found", null);
        else
            return bak;
    }

    public BackupSet getBackupSet(String label)
        throws ServiceException
    {
        BackupSet bak = super.getBackupSet(label);
        if(bak == null)
        {
            bak = (BackupSet)mBackupSetCache.get(label);
            if(bak == null)
            {
                bak = new FileBackupSet(label);
                mBackupSetCache.put(label, bak);
            }
        }
        return bak;
    }

    public List getBackupSets(long from, long to)
        throws ServiceException
    {
        List result = super.getBackupSets(from, to);
        File sessionDirs[] = mSessionsDir.listFiles(new LabelFilenameFilter(LabelType.ALL));
        if(sessionDirs != null)
        {
            Arrays.sort(sessionDirs, new LabelComparator(LabelSortOrder.NEW_TO_OLD));
            for(int i = 0; i < sessionDirs.length; i++)
            {
                String label = sessionDirs[i].getName();
                BackupSet bak;
                try
                {
                    bak = getBackupSet(label);
                }
                catch(Exception e)
                {
                    bak = new InvalidBackupSet(label, e);
                }
                long startTimeInMetadata = bak.getStartTime();
                if(startTimeInMetadata >= from && startTimeInMetadata <= to)
                {
                    result.add(bak);
                    continue;
                }
                long labelDate = BackupManager.getLabelDate(bak.getLabel());
                if(labelDate >= from && labelDate <= to)
                    result.add(bak);
            }

        }
        return result;
    }

    public BackupAccountSession createFullBackup(BackupSet bak, Account account)
        throws ServiceException, IOException
    {
        return new BackupAcctSession(bak, account);
    }

    public AccountSession getAccountSession(String accountId)
        throws IOException, ServiceException
    {
        String label = lookupLastBackupSession(accountId);
        if(label == null)
            return null;
        else
            return getAccountSession(accountId, label);
    }

    private AccountSession getAccountSession(String accountId, long atOrBefore)
        throws IOException, ServiceException
    {
        String label;
label0:
        {
            label = null;
            List baks = getBackupSets(0x0L, atOrBefore);
            if(baks == null || baks.isEmpty())
                break label0;
            Iterator i$ = baks.iterator();
            BackupSet bak;
            do
            {
                if(!i$.hasNext())
                    break label0;
                bak = (BackupSet)i$.next();
            } while(bak.getType() != 1 || !bak.hasAccountId(accountId));
            label = bak.getLabel();
        }
        if(label == null)
            return null;
        else
            return getAccountSession(accountId, label);
    }

    public boolean hasAccountSession(String accountId)
    {
        String label = lookupLastBackupSession(accountId);
        return label != null;
    }

    public AccountSession getAccountSession(String accountId, String label)
        throws IOException, ServiceException
    {
        BackupSet bak = getBackupSet(label);
        if(bak.getType() != 1)
            throw BackupServiceException.FULL_BACKUP_SESSION_REQUIRED(label);
        else
            return new RestoreAcctSession(bak, accountId);
    }

    public List getBackupSets(String accountEmail, long from, long to)
        throws ServiceException
    {
        List sets = getBackupSets(from, to);
        Iterator iter = sets.iterator();
        do
        {
            if(!iter.hasNext())
                break;
            BackupSet set = (BackupSet)iter.next();
            if(!set.hasAccount(accountEmail))
                iter.remove();
        } while(true);
        return sets;
    }

    public void restore(String accountIds[], String label, RestoreParams params)
        throws IOException, ServiceException
    {
        boolean schemaRecreated;
        FileCopier copier;
        mRestorePathCache = new RestorePathCache();
        schemaRecreated = initDb();
        params.getResult().setRebuiltSchema(schemaRecreated);
        copier = null;
        if(params.fcOpts != null)
            copier = FileCopierFactory.createCopier(params.fcOpts);
        else
            copier = createFileCopierFromLC();
        List acctSources;
        RestoreParams.Result result;
        Iterator it;
        copier.start();
        BackupSet bakSet = null;
        if(label != null)
        {
            bakSet = getBackupSet(label);
            if(bakSet == null)
                throw BackupServiceException.NO_SUCH_BACKUP_LABEL(label, null);
            if(bakSet.getStartTime() > params.restoreToTime)
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("backup label ").append(label).append(" is from a later time than restore-to point").toString(), null);
        }
        BackupSet latestBak = null;
        acctSources = new ArrayList();
        for(int i = 0; i < accountIds.length; i++)
        {
            String acctId = accountIds[i];
            RestoreAcctSession acctBakSource = null;
            String errorText = null;
            if(label == null)
            {
                if(params.restoreToTime == 0xffffffffL)
                {
                    acctBakSource = (RestoreAcctSession)getAccountSession(acctId);
                    if(acctBakSource == null)
                        errorText = (new StringBuilder()).append("Full backup not found for account ").append(acctId).toString();
                } else
                {
                    acctBakSource = (RestoreAcctSession)getAccountSession(acctId, params.restoreToTime);
                    if(acctBakSource == null)
                        errorText = (new StringBuilder()).append("Missing full backup earlier than restore-to time for account ").append(acctId).toString();
                }
                if(acctBakSource != null)
                    Log.backup.info((new StringBuilder()).append("Using full backup ").append(acctBakSource.getBackupSet().getLabel()).append(" as starting point for restoring account ").append(acctId).toString());
            } else
            {
                acctBakSource = (RestoreAcctSession)getAccountSession(acctId, label);
                if(acctBakSource == null)
                    errorText = (new StringBuilder()).append("Full backup ").append(label).append(" not found for account ").append(acctId).toString();
            }
            if(acctBakSource == null)
            {
                if(accountIds.length == 1)
                    throw BackupServiceException.NO_SUCH_ACCOUNT_BACKUP(errorText);
                Log.backup.warn(errorText);
                continue;
            }
            acctSources.add(acctBakSource);
            if(label != null)
                continue;
            BackupSet bak = acctBakSource.getBackupSet();
            if(!$assertionsDisabled && bak.hasErrorOccurred())
                throw new AssertionError();
            if(latestBak == null || bak.getStartTime() > latestBak.getStartTime())
                latestBak = bak;
        }

        if(label == null)
            bakSet = latestBak;
        if(params.systemData || schemaRecreated)
        {
            if(bakSet == null)
                bakSet = getBackupSet();
            Log.backup.info((new StringBuilder()).append("restoring system-wide tables and local config from backup ").append(bakSet.getLabel()).toString());
            bakSet.loadSystemData();
        }
        RedoLogManager redoMgr = RedoLogProvider.getInstance().getRedoLogManager();
        if(!params.offline)
            redoMgr.forceRollover();
        result = params.getResult();
        it = acctSources.iterator();
_L2:
        RestoreAcctSession acctBakSource;
        if(!it.hasNext())
            break; /* Loop/switch isn't completed */
        acctBakSource = null;
        if(!BackupManager.getInstance().isRestoreRunning())
        {
            Log.backup.info("Restore interrupted");
            if(result.getStatus() == null)
                result.setStatus("interrupted");
            break; /* Loop/switch isn't completed */
        }
        acctBakSource = (RestoreAcctSession)it.next();
        acctBakSource.setFileCopier(copier);
        ZimbraLog.addToContext("name", acctBakSource.getAccountName());
        acctBakSource.startRestore(params);
        acctBakSource.endRestore();
        break MISSING_BLOCK_LABEL_660;
        Exception exception;
        exception;
        acctBakSource.endRestore();
        throw exception;
        Throwable err = acctBakSource.getError();
        if(err != null)
        {
            Log.backup.warn((new StringBuilder()).append("Error occurred during restore account ").append(acctBakSource.getAccountName()).append(" (").append(acctBakSource.getAccountId()).append(")").toString(), err);
            ZimbraLog.removeFromContext("name");
            ZimbraLog.removeFromContext("mid");
            result.setStatus("err");
            if(!params.continueOnError)
                BackupSet.raiseError(err);
        } else
        {
            it.remove();
        }
        if(true) goto _L2; else goto _L1
        Exception exception1;
        exception1;
        Throwable err = acctBakSource.getError();
        if(err != null)
        {
            Log.backup.warn((new StringBuilder()).append("Error occurred during restore account ").append(acctBakSource.getAccountName()).append(" (").append(acctBakSource.getAccountId()).append(")").toString(), err);
            ZimbraLog.removeFromContext("name");
            ZimbraLog.removeFromContext("mid");
            result.setStatus("err");
            if(!params.continueOnError)
                BackupSet.raiseError(err);
        } else
        {
            it.remove();
        }
        throw exception1;
_L1:
        if(acctSources.isEmpty())
            result.setStatus("ok");
        else
            for(it = acctSources.iterator(); it.hasNext(); result.addFailedAccount(((RestoreAcctSession)it.next()).getAccountName()));
        copier.shutdown();
        mRestorePathCache.save();
        break MISSING_BLOCK_LABEL_967;
        Exception exception2;
        exception2;
        copier.shutdown();
        mRestorePathCache.save();
        throw exception2;
    }

    private void checkMissingAccountsMapFile()
        throws ServiceException
    {
        if(mLatestAcctNameId.isEmpty() && mLatestSessionForAccount.isEmpty())
        {
            File mapFile = new File(mBaseDir, "accounts.xml");
            if(!mapFile.exists())
            {
                String msg = String.format("Account map file %s is missing.  Specify a full backup label to restore from.", new Object[] {
                    mapFile.getAbsolutePath()
                });
                throw ServiceException.INVALID_REQUEST(msg, null);
            }
        }
    }

    public String[] getAccountIds()
        throws ServiceException
    {
        checkMissingAccountsMapFile();
        Set acctIdSet = mLatestSessionForAccount.keySet();
        return (String[])acctIdSet.toArray(new String[0]);
    }

    public String[] getAccountIds(List accountNames, String label, boolean bail)
        throws IOException, ServiceException
    {
        int sz = accountNames.size();
        List ids = new ArrayList(sz);
        if(label == null)
            checkMissingAccountsMapFile();
        for(int i = 0; i < sz; i++)
        {
            String name = (String)accountNames.get(i);
            String acctId = null;
            if(label == null)
            {
                acctId = (String)mLatestAcctNameId.get(name);
            } else
            {
                BackupSet bak = getBackupSet(label);
                acctId = bak.getAccountId(name);
            }
            if(acctId == null && bail)
                throw AccountServiceException.NO_SUCH_ACCOUNT((new StringBuilder()).append("Account ID for ").append(name).append(" not found in backup ").append(label == null ? "" : label).toString());
            if(acctId != null)
                ids.add(acctId);
        }

        return (String[])(String[])ids.toArray(new String[0]);
    }

    private List getRedoLogDirs()
    {
        File incrBackups[] = mSessionsDir.listFiles(new LabelFilenameFilter(LabelType.ALL));
        if(incrBackups == null || incrBackups.length == 0)
            return new ArrayList(0);
        Arrays.sort(incrBackups, new LabelComparator(LabelSortOrder.OLD_TO_NEW));
        List dirs = new ArrayList(incrBackups.length);
        File arr$[] = incrBackups;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            File backup = arr$[i$];
            File redologsDir = new File(backup, "redologs");
            if(redologsDir.exists() && redologsDir.isDirectory())
                dirs.add(redologsDir);
        }

        return dirs;
    }

    private List getSharedBlobsDirs()
    {
        File fullBackups[] = mSessionsDir.listFiles(new LabelFilenameFilter(LabelType.FULL));
        if(fullBackups == null || fullBackups.length == 0)
            return new ArrayList(0);
        Arrays.sort(fullBackups, new LabelComparator(LabelSortOrder.NEW_TO_OLD));
        List dirs = new ArrayList(fullBackups.length);
        File arr$[] = fullBackups;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            File backup = arr$[i$];
            File sharedBlobsDir = new File(backup, "shared_blobs");
            if(sharedBlobsDir.exists() && sharedBlobsDir.isDirectory())
                dirs.add(sharedBlobsDir);
        }

        return dirs;
    }

    public File[] getRedoLogFiles(long from, long to)
        throws IOException
    {
        List toRet = new ArrayList();
        List redologDirs = getRedoLogDirs();
        Iterator i$ = redologDirs.iterator();
        do
        {
            if(!i$.hasNext())
                break;
            File dir = (File)i$.next();
            File logs[] = RolloverManager.getArchiveLogs(dir, from, to);
            if(logs != null)
            {
                File arr$[] = logs;
                int len$ = arr$.length;
                int i$ = 0;
                while(i$ < len$) 
                {
                    File f = arr$[i$];
                    toRet.add(f);
                    i$++;
                }
            }
        } while(true);
        if(toRet.size() < 1)
        {
            return new File[0];
        } else
        {
            File result[] = new File[toRet.size()];
            result = (File[])toRet.toArray(result);
            return result;
        }
    }

    public long getMostRecentRedoSequence()
        throws IOException
    {
        List dirs = getRedoLogDirs();
        int len = dirs.size();
        for(int i = len - 1; i >= 0; i--)
        {
            File latestDir = (File)dirs.get(i);
            File logs[] = RolloverManager.getArchiveLogs(latestDir);
            if(logs != null && logs.length > 0)
                return RolloverManager.getSeqForFile(logs[logs.length - 1]);
        }

        return -1L;
    }

    private void reloadLatestAccountMaps()
        throws IOException
    {
        try
        {
            File mapFile = new File(mBaseDir, "accounts.xml");
            if(mapFile.exists())
            {
                Pair maps = XmlMeta.readAccountMaps(mapFile);
                mLatestAcctNameId = Collections.synchronizedMap((Map)maps.getFirst());
                mLatestSessionForAccount = Collections.synchronizedMap((Map)maps.getSecond());
            } else
            {
                mLatestAcctNameId = Collections.synchronizedMap(new HashMap());
                mLatestSessionForAccount = Collections.synchronizedMap(new HashMap());
            }
        }
        catch(ServiceException e)
        {
            throw Utils.IOException("unable to retrieve latest session-account, account name-id maps", e);
        }
    }

    private BackupSet lookupLastBackupSession()
        throws ServiceException
    {
        File sessions[] = mSessionsDir.listFiles(new LabelFilenameFilter(LabelType.ALL));
        if(sessions == null || sessions.length == 0)
            return null;
        Arrays.sort(sessions, new LabelComparator(LabelSortOrder.OLD_TO_NEW));
        if(sessions.length > 0)
            return getBackupSet(sessions[sessions.length - 1].getName());
        else
            return null;
    }

    private String lookupLastBackupSession(String accountId)
    {
        return (String)mLatestSessionForAccount.get(accountId);
    }

    public BackupSet createIncrementalBackupSet(String label, String desc, Account accounts[], BackupParams params)
        throws IOException, ServiceException
    {
        FileBackupSet bak = new FileBackupSet(label, desc, accounts, 2, params);
        return bak;
    }

    public BackupAccountSession createIncrementalBackup(BackupSet set, Account account)
        throws IOException, ServiceException
    {
        if(!$assertionsDisabled && account == null)
        {
            throw new AssertionError();
        } else
        {
            BackupAccountSession incAcctBak = new BackupAcctSession(set, account);
            return incAcctBak;
        }
    }

    public void deleteBackups(long cutoffTime)
        throws IOException, ServiceException
    {
        final List toDelete;
        Log.backup.info((new StringBuilder()).append("deleting backups on or older than ").append(new Date(cutoffTime)).toString());
        toDelete = new ArrayList();
        List baks = getBackupSets(0L, cutoffTime);
        Iterator it = baks.iterator();
        do
        {
            if(!it.hasNext())
                break;
            BackupSet bak = (BackupSet)it.next();
            Log.backup.info((new StringBuilder()).append("deleting backup ").append(bak.getLabel()).toString());
            if(bak.getType() == 1)
            {
                String acctNames[] = bak.getAccountNames();
                if(acctNames != null)
                {
                    for(int i = 0; i < acctNames.length; i++)
                    {
                        String acctId = bak.getAccountId(acctNames[i]);
                        String label = lookupLastBackupSession(acctId);
                        if(bak.getLabel().equals(label))
                        {
                            Log.backup.info((new StringBuilder()).append("removed account ").append(acctNames[i]).append(" from backup").toString());
                            mLatestSessionForAccount.remove(acctId);
                            mLatestAcctNameId.remove(acctNames[i]);
                        }
                    }

                }
            }
            File bakDir = null;
            if(bak instanceof FileBackupSet)
                bakDir = ((FileBackupSet)bak).mDir;
            else
            if(bak instanceof InvalidBackupSet)
                bakDir = ((InvalidBackupSet)bak).mDir;
            if(bakDir != null && bakDir.exists())
            {
                File renamedBakDir = new File(bakDir.getParentFile(), (new StringBuilder()).append("TO_DELETE-").append(bakDir.getName()).toString());
                boolean renamed = bakDir.renameTo(renamedBakDir);
                if(!renamed)
                    Log.backup.warn((new StringBuilder()).append("Unable to mark backup label for deletion: ").append(bakDir.getAbsolutePath()).toString());
            }
            File ldapBak = new File(mBaseDir, (new StringBuilder()).append("ldap/").append(bak.getLabel()).toString());
            if(ldapBak.exists())
            {
                File renamedLdapBakDir = new File(ldapBak.getParentFile(), (new StringBuilder()).append("TO_DELETE-").append(ldapBak.getName()).toString());
                boolean renamed = ldapBak.renameTo(renamedLdapBakDir);
                if(!renamed)
                    Log.backup.warn((new StringBuilder()).append("Unable to mark backup label for deletion: ").append(ldapBak.getAbsolutePath()).toString());
            }
        } while(true);
        FilenameFilter deletedDirsFilter = new FilenameFilter() {

            public boolean accept(File dir, String name)
            {
                return name.startsWith("TO_DELETE-");
            }

            final FileBackupTarget this$0;

            
            {
                this$0 = FileBackupTarget.this;
                super();
            }
        }
;
        File deletedBackupDirs[] = mSessionsDir.listFiles(deletedDirsFilter);
        if(deletedBackupDirs != null)
        {
            File arr$[] = deletedBackupDirs;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                File dir = arr$[i$];
                toDelete.add(dir);
            }

        }
        File ldapBase = new File(mBaseDir, "ldap");
        File deletedLdapBackupDirs[] = ldapBase.listFiles(deletedDirsFilter);
        if(deletedLdapBackupDirs != null)
        {
            File arr$[] = deletedLdapBackupDirs;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                File dir = arr$[i$];
                toDelete.add(dir);
            }

        }
        File newAccountsXml = new File(mBaseDir, "accounts.xml.new");
        XmlMeta.writeAccountMaps(mLatestAcctNameId, mLatestSessionForAccount, newAccountsXml);
        File accountsXml = new File(mBaseDir, "accounts.xml");
        File renamedOld = new File(mBaseDir, "accounts.xml.old");
        if(accountsXml.exists() && !accountsXml.renameTo(renamedOld))
            throw new IOException((new StringBuilder()).append("Unable to rename ").append(accountsXml.getAbsolutePath()).append(" to ").append(renamedOld.getAbsolutePath()).toString());
        boolean renamed = newAccountsXml.renameTo(accountsXml);
        if(!renamed)
            throw new IOException((new StringBuilder()).append("Unable to rename ").append(newAccountsXml.getAbsolutePath()).append(" to ").append(accountsXml.getAbsolutePath()).toString());
        if(!toDelete.isEmpty())
        {
            Runnable r = new Runnable() {

                public void run()
                {
                    try
                    {
                        Log.backup.info((new StringBuilder()).append("Starting deletion of ").append(toDelete.size()).append(" old backups").toString());
                        for(Iterator i$ = toDelete.iterator(); i$.hasNext();)
                        {
                            File dir = (File)i$.next();
                            try
                            {
                                Log.backup.info((new StringBuilder()).append("Deleting old backup directory ").append(dir.getAbsolutePath()).toString());
                                FileUtil.deleteDir(dir);
                            }
                            catch(IOException e)
                            {
                                Log.backup.warn("Ignoring error while deleting old backups", e);
                            }
                        }

                        Log.backup.info((new StringBuilder()).append("Finished deletion of ").append(toDelete.size()).append(" old backups").toString());
                    }
                    catch(OutOfMemoryError e)
                    {
                        Zimbra.halt("OutOfMemoryError while deleting backup sets", e);
                    }
                }

                final List val$toDelete;
                final FileBackupTarget this$0;

            
            {
                this$0 = FileBackupTarget.this;
                toDelete = list;
                super();
            }
            }
;
            Thread deletionThread = new Thread(r, "BackupDeletion");
            deletionThread.setDaemon(true);
            deletionThread.start();
        }
        break MISSING_BLOCK_LABEL_1091;
        Exception exception;
        exception;
        File newAccountsXml = new File(mBaseDir, "accounts.xml.new");
        XmlMeta.writeAccountMaps(mLatestAcctNameId, mLatestSessionForAccount, newAccountsXml);
        File accountsXml = new File(mBaseDir, "accounts.xml");
        File renamedOld = new File(mBaseDir, "accounts.xml.old");
        if(accountsXml.exists() && !accountsXml.renameTo(renamedOld))
            throw new IOException((new StringBuilder()).append("Unable to rename ").append(accountsXml.getAbsolutePath()).append(" to ").append(renamedOld.getAbsolutePath()).toString());
        boolean renamed = newAccountsXml.renameTo(accountsXml);
        if(!renamed)
            throw new IOException((new StringBuilder()).append("Unable to rename ").append(newAccountsXml.getAbsolutePath()).append(" to ").append(accountsXml.getAbsolutePath()).toString());
        if(!toDelete.isEmpty())
        {
            Runnable r = new _cls2();
            Thread deletionThread = new Thread(r, "BackupDeletion");
            deletionThread.setDaemon(true);
            deletionThread.start();
        }
        throw exception;
    }

    private static LabelType getLabelType(String label)
    {
        if(label == null)
            break MISSING_BLOCK_LABEL_39;
        BackupManager.getLabelDate(label);
        if(label.startsWith("full-"))
            return LabelType.FULL;
        try
        {
            if(label.startsWith("incr-"))
                return LabelType.INCREMENTAL;
        }
        catch(ServiceException e) { }
        return LabelType.INVALID;
    }

    private static FileCopier createFileCopierFromLC()
    {
        FileCopierOptions fcOpts = null;
        try
        {
            com.zimbra.common.io.FileCopierOptions.Method method = com.zimbra.common.io.FileCopierOptions.Method.parseMethod(BackupLC.backup_file_copier_method.value());
            com.zimbra.common.io.FileCopierOptions.IOType ioType = com.zimbra.common.io.FileCopierOptions.IOType.parseIOType(BackupLC.backup_file_copier_iotype.value());
            int oioCopyBufferSize = BackupLC.backup_file_copier_oio_copy_buffer_size.intValue();
            int asyncQueueSize = BackupLC.backup_file_copier_async_queue_capacity.intValue();
            int parallelWorkers = BackupLC.backup_file_copier_parallel_workers.intValue();
            int pipes = BackupLC.backup_file_copier_pipes.intValue();
            int pipeBufferSize = BackupLC.backup_file_copier_pipe_buffer_size.intValue();
            int readersPerPipe = BackupLC.backup_file_copier_readers_per_pipe.intValue();
            int writersPerPipe = BackupLC.backup_file_copier_writers_per_pipe.intValue();
            fcOpts = new FileCopierOptions(method, ioType, oioCopyBufferSize, asyncQueueSize, parallelWorkers, pipes, pipeBufferSize, readersPerPipe, writersPerPipe);
        }
        catch(ServiceException e)
        {
            Log.backup.warn("Invalid FileCopier options in localconfig; using default settings", e);
            fcOpts = new FileCopierOptions();
        }
        return FileCopierFactory.createCopier(fcOpts);
    }

    private static final String SUBDIR_SESSIONS = "sessions";
    private static final String SUBDIR_SESSIONS_TMP = "tmp";
    private static final String SUBDIR_DB = "db";
    private static final String SUBDIR_INDEX = "index";
    private static final String FILE_BLOBS_ZIP = "blobs.zip";
    private static final String FILE_BLOBS_DIR = "blobs";
    private static final String DELETED_SESSION_PREFIX = "TO_DELETE-";
    private File mBaseDir;
    private File mSessionsDir;
    private File mSessionsTmpDir;
    private Map mLatestSessionForAccount;
    private Map mLatestAcctNameId;
    private RestorePathCache mRestorePathCache;
    private boolean mCustomDest;
    private Map mBackupSetCache;
    private static Set excludedSystemTables;
    static final boolean $assertionsDisabled = !com/zimbra/cs/backup/FileBackupTarget.desiredAssertionStatus();

    static 
    {
        excludedSystemTables = new HashSet();
        excludedSystemTables.add("mailbox");
        excludedSystemTables.add("mailbox_metadata");
        excludedSystemTables.add("scheduled_task");
        excludedSystemTables.add("out_of_office");
        excludedSystemTables.add("mobile_devices");
        excludedSystemTables.add("service_status");
    }










}
