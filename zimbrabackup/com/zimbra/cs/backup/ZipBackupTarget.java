// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ZipBackupTarget.java

package com.zimbra.cs.backup;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.zip.ZipEntry;
import com.zimbra.common.util.zip.ZipOutputStream;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.backup.util.Utils;
import com.zimbra.cs.backup.util.ZipUtil;
import com.zimbra.cs.db.DbBackup;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.file.Volume;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipInputStream;
import org.dom4j.io.XMLWriter;

// Referenced classes of package com.zimbra.cs.backup:
//            RestoreAccountSession, BackupTarget, BackupLC, BackupServiceException, 
//            RestoreParams, BackupSet, BackupParams, BackupAccountSession, 
//            AccountSession, DirBlobRepository, XmlMeta, BlobRepository

public class ZipBackupTarget
    implements BackupTarget
{
    class RestoreAcctSession extends RestoreAccountSession
    {

        private void unzipToTempFiles()
            throws IOException
        {
            Log.mboxmove.debug("RestoreAcctSession.unzipToTempFiles() started");
            for(java.util.zip.ZipEntry ze = null; (ze = mZipIn.getNextEntry()) != null;)
            {
                String zn = ze.getName();
                Log.mboxmove.debug((new StringBuilder()).append("Unzipping ").append(zn).toString());
                zn = zn.replace('/', File.separatorChar);
                File file = new File(mTempDir, zn);
                File dir = file.getParentFile();
                if(!dir.exists())
                    dir.mkdirs();
                FileUtil.copy(mZipIn, false, file);
                mZipIn.closeEntry();
            }

            Log.mboxmove.debug("RestoreAcctSession.unzipToTempFiles() finished");
        }

        protected void loadAccount(boolean attrsOnly, boolean full)
            throws IOException, ServiceException
        {
            String accountId = getAccountId();
            Account account = Provisioning.getInstance().get(com.zimbra.cs.account.Provisioning.AccountBy.id, accountId);
            if(account == null)
            {
                throw AccountServiceException.NO_SUCH_ACCOUNT(accountId);
            } else
            {
                setAccount(account);
                setTargetAccount(account);
                return;
            }
        }

        public void loadTables()
            throws IOException, ServiceException
        {
            File dbDir = new File(mTempDir, "db");
            loadTablesFromLocalFile(dbDir, true);
        }

        public void loadIndex()
            throws ServiceException, IOException
        {
            int mboxId = getTargetMailboxId();
            short volId = getIndexVolumeId();
            Volume vol = Volume.getById(volId);
            File srcDir = new File(mTempDir, "index");
            File destDir = new File(vol.getMailboxDir(mboxId, 10));
            Log.mboxmove.info((new StringBuilder()).append("Deleting directory ").append(destDir.getAbsolutePath()).toString());
            FileUtil.deleteDir(destDir);
            int count = FileUtil.copyDirectory(srcDir, destDir);
            Log.mboxmove.info((new StringBuilder()).append("loaded ").append(count).append(" index files during restore").toString());
        }

        public void loadBlobs()
            throws ServiceException, IOException
        {
            File base = new File(mTempDir, "blobs");
            mBlobRepo = new DirBlobRepository(base, false);
            int count = super.loadBlobsImpl(mBlobRepo, false);
            Log.mboxmove.info((new StringBuilder()).append("loaded ").append(count).append(" blobs during restore").toString());
            if(mBlobRepo != null)
            {
                try
                {
                    mBlobRepo.close();
                }
                catch(Exception e)
                {
                    Log.mboxmove.info("Non-critical error while closing blob repository", e);
                }
                mBlobRepo = null;
            }
            break MISSING_BLOCK_LABEL_139;
            Exception exception;
            exception;
            if(mBlobRepo != null)
            {
                try
                {
                    mBlobRepo.close();
                }
                catch(Exception e)
                {
                    Log.mboxmove.info("Non-critical error while closing blob repository", e);
                }
                mBlobRepo = null;
            }
            throw exception;
        }

        public void loadBlob(String digest, String msgId, int linkCount, File targetDir, short volId)
            throws IOException
        {
            if(!targetDir.exists() && !targetDir.mkdirs())
            {
                throw new IOException((new StringBuilder()).append("cannot create directory for ").append(targetDir.getPath()).toString());
            } else
            {
                File msgFile = new File(targetDir, (new StringBuilder()).append(msgId).append(".msg").toString());
                Log.mboxmove.debug((new StringBuilder()).append("target file path=").append(msgFile.getPath()).toString());
                BlobRepository.BlobEntry entry = mBlobRepo.getCurrentEntry();
                java.io.InputStream is = entry.getInputStream();
                FileUtil.copy(is, entry.mustCloseInputStream(), msgFile);
                return;
            }
        }

        public void endRestore()
            throws IOException, ServiceException
        {
            com.zimbra.cs.db.DbPool.Connection conn;
            super.endRestore();
            conn = null;
            conn = DbPool.getConnection();
            DbBackup.updateMailboxBackupTime(conn, getTargetMailboxId(), -1);
            conn.commit();
            DbPool.quietClose(conn);
            break MISSING_BLOCK_LABEL_37;
            Exception exception;
            exception;
            DbPool.quietClose(conn);
            throw exception;
            FileUtil.deleteDir(mTempDir);
            break MISSING_BLOCK_LABEL_57;
            Exception exception1;
            exception1;
            FileUtil.deleteDir(mTempDir);
            throw exception1;
        }

        public Mailbox getMailbox()
        {
            throw new IllegalStateException();
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
                String msgId = name.substring(0, pos);
                return msgId;
            }
        }

        protected int decodeLinkCount(String name)
        {
            return 1;
        }

        protected String decodeDigest(String name)
        {
            return null;
        }

        private File mTempDir;
        private BlobRepository mBlobRepo;
        final ZipBackupTarget this$0;

        public RestoreAcctSession(BackupSet bak, String accountId, int mailboxId)
            throws ServiceException, IOException
        {
            this$0 = ZipBackupTarget.this;
            super(bak, accountId, Log.mboxmove);
            mTempDir = new File(getTempRoot(), accountId);
            if(!mTempDir.exists() && !mTempDir.mkdirs())
                throw new IOException((new StringBuilder()).append("cannot create temp dir ").append(mTempDir.getPath()).toString());
            unzipToTempFiles();
            File metaFile = new File(mTempDir, "meta.xml");
            try
            {
                com.zimbra.common.soap.Element acctBackupElem = XmlMeta.readAccountBackup(metaFile);
                decodeMetadata(acctBackupElem);
                setTargetMailboxId(mailboxId);
            }
            catch(Exception e)
            {
                throw Utils.IOException((new StringBuilder()).append("unable to read metadata for account ").append(accountId).toString(), e);
            }
        }
    }

    class BackupAcctSession extends BackupAccountSession
    {

        protected void beginLockedStage()
            throws ServiceException
        {
            if(mLockAccount)
            {
                Log.mboxmove.info((new StringBuilder()).append("Putting account ").append(getAccount().getName()).append(" under maintenance").toString());
                Provisioning.getInstance().modifyAccountStatus(getAccount(), "maintenance");
            }
        }

        protected void storeTables()
            throws IOException, ServiceException
        {
            File dbDir = new File(mTempDir, "db");
            dbDir.mkdirs();
            super.storeTablesToLocalFile(dbDir);
            File files[] = dbDir.listFiles();
            if(files != null && files.length > 0)
            {
                File arr$[] = files;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    File f = arr$[i$];
                    String fname = f.getName();
                    if(!f.isDirectory())
                    {
                        ZipEntry ze = new ZipEntry((new StringBuilder()).append("db/").append(fname).toString());
                        addZipEntry(ze, f);
                    }
                }

            }
        }

        protected void storeIndex()
            throws IOException, ServiceException
        {
            storeIndexImpl();
        }

        protected void storeIndexFile(File src, String relPathDest)
            throws IOException
        {
            if(File.separatorChar == '\\')
                relPathDest = relPathDest.replaceAll("\\\\", "/");
            ZipEntry ze = new ZipEntry((new StringBuilder()).append("index/").append(relPathDest).toString());
            addZipEntry(ze, src);
        }

        protected void storeBlobsBeforeMaintenanceMode()
            throws ServiceException, IOException
        {
            if(getParams().blobsSyncToken > 0)
            {
                return;
            } else
            {
                Mailbox mbox = getMailbox();
                mMboxChangeIdAtStart = mbox.getLastChangeID();
                Log.mboxmove.debug((new StringBuilder()).append("Mailbox ").append(mbox.getId()).append(" at changeId ").append(mMboxChangeIdAtStart).append(" at mailbox move start").toString());
                storeBlobsImpl();
                return;
            }
        }

        protected void storeBlobs()
            throws ServiceException, IOException
        {
            int changeId;
            if(getParams().blobsSyncToken > 0)
                changeId = getParams().blobsSyncToken;
            else
                changeId = mMboxChangeIdAtStart;
            storeBlobsModifiedSince(changeId);
        }

        protected void storeBlob(String digest, Volume vol, File src)
            throws IOException
        {
            File parent = src.getParentFile();
            String entryName = (new StringBuilder()).append(vol.getId()).append("/").append(parent.getName()).append("/").append(src.getName()).toString();
            ZipEntry ze = new ZipEntry((new StringBuilder()).append("blobs/").append(entryName).toString());
            addZipEntry(ze, src);
        }

        public void incrementalBackup()
        {
            throw new UnsupportedOperationException();
        }

        protected void waitForFullBackupCompletion()
        {
        }

        public void endFullBackup()
            throws IOException, ServiceException
        {
            XMLWriter xmlWriter = null;
            super.endFullBackup();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xmlWriter = XmlMeta.createWriter(baos);
            XmlMeta.writeAccountBackup(this, xmlWriter);
            xmlWriter.close();
            ZipEntry ze = new ZipEntry("meta.xml");
            addZipEntry(ze, baos.toByteArray());
            if(xmlWriter != null)
                try
                {
                    xmlWriter.close();
                }
                catch(IOException e)
                {
                    Log.mboxmove.warn("Exception while closing XMLWriter", e);
                }
            mZipOut.close();
            mZipOut = null;
            FileUtil.deleteDir(mTempDir);
            break MISSING_BLOCK_LABEL_182;
            Exception exception;
            exception;
            FileUtil.deleteDir(mTempDir);
            throw exception;
            Exception exception1;
            exception1;
            if(xmlWriter != null)
                try
                {
                    xmlWriter.close();
                }
                catch(IOException e)
                {
                    Log.mboxmove.warn("Exception while closing XMLWriter", e);
                }
            mZipOut.close();
            mZipOut = null;
            FileUtil.deleteDir(mTempDir);
            break MISSING_BLOCK_LABEL_179;
            Exception exception2;
            exception2;
            FileUtil.deleteDir(mTempDir);
            throw exception2;
            throw exception1;
        }

        private File mTempDir;
        private int mMboxChangeIdAtStart;
        final ZipBackupTarget this$0;

        public BackupAcctSession(BackupSet bak, Account account)
            throws ServiceException, IOException
        {
            this$0 = ZipBackupTarget.this;
            super(bak, account, bak.getType(), Log.mboxmove);
            mMboxChangeIdAtStart = -1;
            String accountId = account.getId();
            mTempDir = new File(getTempRoot(), accountId);
            if(!mTempDir.exists() && !mTempDir.mkdirs())
                throw new IOException((new StringBuilder()).append("cannot create temp dir ").append(mTempDir.getPath()).toString());
            else
                return;
        }
    }

    class DummyBackupSet extends BackupSet
    {

        public BackupTarget getBackupTarget()
        {
            return ZipBackupTarget.this;
        }

        public void storeSystemData()
        {
        }

        public void loadSystemData()
        {
        }

        public void endFullBackup()
            throws IOException, ServiceException
        {
            super.endFullBackup();
            if(hasErrorOccurred())
                super.raiseError();
        }

        protected void backupRedoLogs()
            throws IOException, ServiceException
        {
        }

        public List getStats()
        {
            return null;
        }

        final ZipBackupTarget this$0;

        DummyBackupSet(String label, String desc, Account accounts[], BackupParams params)
            throws ServiceException
        {
            this$0 = ZipBackupTarget.this;
            super(label, desc, accounts, 1, params, Log.mboxmove);
        }

        DummyBackupSet(String label)
        {
            this$0 = ZipBackupTarget.this;
            super(label, Log.mboxmove);
        }
    }


    public ZipBackupTarget(ZipOutputStream zipOut, File tempDir, boolean lockAccount)
    {
        mCompressionMethod = BackupLC.mboxmove_enable_compression.booleanValue() ? 8 : 0;
        mZipOut = zipOut;
        mTempDir = tempDir;
        mZipOut.setMethod(mCompressionMethod);
        mLockAccount = lockAccount;
    }

    public ZipBackupTarget(ZipInputStream in, int mboxId)
        throws ServiceException, IOException
    {
        mCompressionMethod = BackupLC.mboxmove_enable_compression.booleanValue() ? 8 : 0;
        mZipIn = in;
        mRestoreMailboxId = mboxId;
        String path = Provisioning.getInstance().getLocalServer().getMailboxMoveTempDir();
        mTempDir = new File(path);
        FileUtil.ensureDirExists(mTempDir);
    }

    public boolean isCustom()
    {
        return true;
    }

    public boolean hasEnoughFreeSpace(String threshold)
    {
        return !Utils.freeSpaceLessThan(getTempRoot(), threshold);
    }

    public boolean outOfSpace()
    {
        String threshold = BackupLC.backup_out_of_disk_threshold.value();
        return Utils.freeSpaceLessThan(getTempRoot(), threshold);
    }

    public BackupServiceException makeOutOfSpaceException(IOException cause)
    {
        return BackupServiceException.OUT_OF_DISK(getTempRoot(), cause);
    }

    public String getURI()
    {
        return "mailbox-move";
    }

    public BackupSet createFullBackupSet(String label, String desc, Account accounts[], BackupParams params)
        throws ServiceException
    {
        if(mZipOut == null)
            throw new IllegalStateException("no zip output stream");
        else
            return new DummyBackupSet(label, desc, accounts, params);
    }

    public BackupSet getBackupSet()
    {
        throw new UnsupportedOperationException();
    }

    public BackupSet getBackupSet(String label)
    {
        throw new UnsupportedOperationException();
    }

    public List getBackupSets(long from, long to)
    {
        throw new UnsupportedOperationException();
    }

    public BackupAccountSession createFullBackup(BackupSet bak, Account account)
        throws IOException, ServiceException
    {
        if(mZipOut == null)
            throw new IllegalArgumentException("no zip output stream");
        else
            return new BackupAcctSession(bak, account);
    }

    public AccountSession getAccountSession(String accountId)
        throws IOException, ServiceException
    {
        return new RestoreAcctSession(new DummyBackupSet("mailbox-move"), accountId, mRestoreMailboxId);
    }

    public boolean hasAccountSession(String accountId)
    {
        return true;
    }

    public AccountSession getAccountSession(String accountId, String label)
    {
        throw new UnsupportedOperationException();
    }

    public List getBackupSets(String accountName, long from, long to)
    {
        throw new UnsupportedOperationException();
    }

    public void restore(String accountIds[], String label, RestoreParams params)
        throws IOException, ServiceException
    {
        int i;
        Log.mboxmove.debug("ZipBackupTarget.restore() started");
        i = 0;
_L3:
        if(i >= accountIds.length) goto _L2; else goto _L1
_L1:
        RestoreAccountSession acctBakSource;
        acctBakSource = (RestoreAccountSession)getAccountSession(accountIds[i]);
        if(acctBakSource == null)
            throw new IOException((new StringBuilder()).append("Full backup session not found for account ").append(accountIds[i]).toString());
        params.includeIncrementals = false;
        params.systemData = false;
        acctBakSource.startRestore(params);
        acctBakSource.endRestore();
        break MISSING_BLOCK_LABEL_100;
        Exception exception;
        exception;
        acctBakSource.endRestore();
        throw exception;
        Throwable err = acctBakSource.getError();
        if(err != null)
        {
            Log.mboxmove.warn((new StringBuilder()).append("Error occurred during restore account ").append(accountIds[i]).toString(), err);
            BackupSet.raiseError(err);
        }
        continue; /* Loop/switch isn't completed */
        Exception exception1;
        exception1;
        Throwable err = acctBakSource.getError();
        if(err != null)
        {
            Log.mboxmove.warn((new StringBuilder()).append("Error occurred during restore account ").append(accountIds[i]).toString(), err);
            BackupSet.raiseError(err);
        }
        throw exception1;
        i++;
          goto _L3
_L2:
    }

    public String[] getAccountIds(List accountNames, String label, boolean bail)
    {
        throw new UnsupportedOperationException();
    }

    public String[] getAccountIds()
    {
        throw new UnsupportedOperationException();
    }

    public File[] getRedoLogFiles(long startSequence)
    {
        throw new UnsupportedOperationException();
    }

    public File[] getRedoLogFiles(long startSequence, long endSequence)
    {
        throw new UnsupportedOperationException();
    }

    public long getMostRecentRedoSequence()
    {
        throw new UnsupportedOperationException();
    }

    public BackupAccountSession getFullBackupSession(String accountId)
    {
        throw new UnsupportedOperationException();
    }

    public BackupSet createIncrementalBackupSet(String label, String string, Account accounts[], BackupParams params)
    {
        throw new UnsupportedOperationException();
    }

    public BackupAccountSession createIncrementalBackup(BackupSet set, Account acct)
    {
        throw new UnsupportedOperationException();
    }

    public void deleteBackups(long l)
    {
    }

    private File getTempRoot()
    {
        return mTempDir;
    }

    private void addZipEntry(ZipEntry zentry, File file)
        throws IOException
    {
        zentry.setMethod(mCompressionMethod);
        if(mCompressionMethod == 0)
            if(file != null)
            {
                long fileLen = file.length();
                if(fileLen == 0L && !file.exists())
                {
                    Log.mboxmove.debug((new StringBuilder()).append("Skipping concurrently deleted blob ").append(file.getAbsolutePath()).toString());
                    return;
                }
                zentry.setSize(fileLen);
                zentry.setCompressedSize(fileLen);
                try
                {
                    zentry.setCrc(ZipUtil.computeCRC32(file));
                }
                catch(FileNotFoundException e)
                {
                    Log.mboxmove.debug((new StringBuilder()).append("Skipping concurrently deleted blob ").append(file.getAbsolutePath()).toString());
                    return;
                }
            } else
            {
                zentry.setSize(0L);
                zentry.setCompressedSize(0L);
                zentry.setCrc(0L);
            }
        mZipOut.putNextEntry(zentry);
        FileInputStream fis = new FileInputStream(file);
        ByteUtil.copy(fis, true, mZipOut, false);
        mZipOut.closeEntry();
    }

    private void addZipEntry(ZipEntry zentry, byte buf[])
        throws IOException
    {
        zentry.setMethod(mCompressionMethod);
        if(mCompressionMethod == 0)
            if(buf != null && buf.length > 0)
            {
                zentry.setSize(buf.length);
                zentry.setCompressedSize(buf.length);
                CRC32 crc = new CRC32();
                crc.reset();
                crc.update(buf);
                zentry.setCrc(crc.getValue());
            } else
            {
                zentry.setSize(0L);
                zentry.setCompressedSize(0L);
                zentry.setCrc(0L);
            }
        mZipOut.putNextEntry(zentry);
        if(buf != null && buf.length > 0)
            mZipOut.write(buf);
        mZipOut.closeEntry();
    }

    static final String NAME = "mailbox-move";
    ZipOutputStream mZipOut;
    private int mCompressionMethod;
    private File mTempDir;
    ZipInputStream mZipIn;
    private int mRestoreMailboxId;
    private boolean mLockAccount;
    private static final String BLOBS = "blobs";
    private static final String INDEX = "index";




}
