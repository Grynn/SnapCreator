// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupAccountSession.java

package com.zimbra.cs.backup;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.*;
import com.zimbra.cs.account.*;
import com.zimbra.cs.db.*;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.znative.IO;
import java.io.File;
import java.io.IOException;
import java.util.*;

// Referenced classes of package com.zimbra.cs.backup:
//            AccountSession, BackupSet, BackupParams, BackupTarget, 
//            BackupLC, XmlMeta, BackupServiceException

public abstract class BackupAccountSession extends AccountSession
{
    private class BlobDigests
    {

        public boolean isEnabled()
        {
            return enabled;
        }

        public void preload()
            throws ServiceException
        {
            int mboxId;
            com.zimbra.cs.db.DbPool.Connection conn;
            enabled = true;
            if(!mPreloadEnabled)
                break MISSING_BLOCK_LABEL_95;
            mboxId = getMailboxId();
            conn = null;
            conn = DbPool.getConnection();
            mMap = DbBackup.getBlobDigestMap(conn, mboxId);
            DbPool.quietClose(conn);
            break MISSING_BLOCK_LABEL_49;
            Exception exception;
            exception;
            DbPool.quietClose(conn);
            throw exception;
            logger.info((new StringBuilder()).append("Number of blobs to backup for mailbox ").append(mboxId).append(": ").append(mMap.size()).toString());
        }

        public String get(Pair idRev)
            throws ServiceException
        {
            com.zimbra.cs.db.DbPool.Connection conn;
            if(!enabled)
                break MISSING_BLOCK_LABEL_98;
            if(mPreloadEnabled)
            {
                String key = (new StringBuilder()).append((String)idRev.getFirst()).append("-").append((String)idRev.getSecond()).toString();
                return (String)mMap.get(key);
            }
            conn = null;
            String s;
            conn = DbPool.getConnection();
            s = DbBackup.getBlobDigest(conn, getMailboxId(), idRev);
            DbPool.quietClose(conn);
            return s;
            Exception exception;
            exception;
            DbPool.quietClose(conn);
            throw exception;
            return null;
        }

        public void clear()
        {
            if(mMap != null)
            {
                mMap.clear();
                mMap = null;
            }
            enabled = false;
        }

        private boolean enabled;
        private boolean mPreloadEnabled;
        private Map mMap;
        final BackupAccountSession this$0;

        private BlobDigests()
        {
            this$0 = BackupAccountSession.this;
            super();
            enabled = false;
            mPreloadEnabled = !BackupLC.backup_disable_blob_digest_preloading.booleanValue();
        }

    }


    protected BackupAccountSession(BackupSet bak, Account account, int type, Log logger)
        throws ServiceException, IOException
    {
        super(bak, account.getId(), logger);
        mBlobDigests = new BlobDigests();
        if(!$assertionsDisabled && type != bak.getType())
            throw new AssertionError();
        String accountId = getAccountId();
        if(type == 2)
            mFullSession = bak.getBackupTarget().getAccountSession(accountId);
        setAccount(account);
        String acctName = account.getName();
        setAccountName(acctName);
        Provisioning prov = Provisioning.getInstance();
        setServer(prov.getLocalServer().getServiceHostname());
        if(type == 1)
        {
            long seq = RedoLogProvider.getInstance().getRedoLogManager().getCurrentLogSequence();
            setRedoLogFileSequence(seq);
            logger.info((new StringBuilder()).append("redo log file sequence is ").append(seq).append(" at full backup for ").append(acctName).toString());
        } else
        {
            setRedoLogFileSequence(mFullSession.getRedoLogFileSequence());
        }
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(accountId, false);
        setMailbox(mbox);
        if(mbox != null)
        {
            setMailboxId(mbox.getId());
        } else
        {
            setMailboxId(-1);
            logger.info((new StringBuilder()).append("Account ").append(acctName).append(" does not have a mailbox").toString());
        }
    }

    public void startFullBackup(BackupParams params)
        throws ServiceException
    {
        Mailbox mbox;
        com.zimbra.cs.mailbox.MailboxManager.MailboxLock lock;
        if(!$assertionsDisabled && getBackupSet().getType() != 1)
            throw new AssertionError();
        int mboxid = getMailboxId();
        logger.info((new StringBuilder()).append("Full backup started for account ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(mboxid == -1 ? "" : (new StringBuilder()).append(" mailbox ").append(mboxid).toString()).toString());
        mParams = params;
        if(params.zip)
        {
            if(!BackupLC.backup_debug_use_old_zip_format.booleanValue())
            {
                setBlobsZipped(true);
                setBlobCompressedDeprecated(false);
            } else
            {
                setBlobsZipped(false);
                setBlobCompressedDeprecated(true);
            }
        } else
        {
            setBlobsZipped(false);
            setBlobCompressedDeprecated(false);
        }
        mbox = getMailbox();
        lock = null;
        beginUnlockedStage();
        storeVolumesInfo();
        if(mbox != null && !mParams.skipBlobs)
            storeBlobsBeforeMaintenanceMode();
        storeAccount();
        if(mbox != null)
            break MISSING_BLOCK_LABEL_272;
        setStartTime(System.currentTimeMillis());
        setAccountOnly(true);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, false);
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        break MISSING_BLOCK_LABEL_271;
        OutOfMemoryError e;
        e;
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        throw e;
        return;
        mbox.setSharedDeliveryAllowed(false);
        mbox.waitUntilSharedDeliveryCompletes();
        lock = MailboxManager.getInstance().beginMaintenance(getAccountId(), mbox.getId());
        Thread workers[] = getWorkerThreads();
        if(workers != null)
        {
            Thread arr$[] = workers;
            int len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                Thread worker = arr$[i$];
                lock.registerAllowedThread(worker);
            }

        }
        setStartTime(System.currentTimeMillis());
        beginLockedStage();
        if(!mParams.skipDb)
            storeTables();
        if(!mParams.skipBlobs)
            storeBlobs();
        if(!mParams.skipSearchIndex)
            storeIndex();
        waitForFullBackupCompletion();
        storeVolumesInfo();
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, false);
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        break MISSING_BLOCK_LABEL_647;
        Exception exception;
        exception;
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        throw exception;
        workers;
        Zimbra.halt("out of memory", workers);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, false);
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        break MISSING_BLOCK_LABEL_647;
        Exception exception1;
        exception1;
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        throw exception1;
        workers;
        initError(workers);
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, false);
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        break MISSING_BLOCK_LABEL_647;
        Exception exception2;
        exception2;
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        throw exception2;
        Exception exception3;
        exception3;
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, getError() == null, false);
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        break MISSING_BLOCK_LABEL_644;
        Exception exception4;
        exception4;
        if(mbox != null)
            mbox.setSharedDeliveryAllowed(true);
        throw exception4;
        throw exception3;
    }

    public void endFullBackup()
        throws IOException, ServiceException
    {
        if(!$assertionsDisabled && getBackupSet().getType() != 1)
            throw new AssertionError();
        mBlobDigests.clear();
        setEndTime(System.currentTimeMillis());
        int mboxid = getMailboxId();
        logger.info((new StringBuilder()).append("Full backup finished for account ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(mboxid == -1 ? "" : (new StringBuilder()).append(" mailbox ").append(mboxid).toString()).toString());
        break MISSING_BLOCK_LABEL_211;
        Exception exception;
        exception;
        int mboxid = getMailboxId();
        logger.info((new StringBuilder()).append("Full backup finished for account ").append(getAccountName()).append(" (").append(getAccountId()).append(")").append(mboxid == -1 ? "" : (new StringBuilder()).append(" mailbox ").append(mboxid).toString()).toString());
        throw exception;
    }

    public void incrementalBackup()
        throws IOException, ServiceException
    {
        logger.info((new StringBuilder()).append("Incremental backup is started for account ").append(getAccountName()).toString());
        if(!$assertionsDisabled && getBackupSet().getType() != 2)
        {
            throw new AssertionError();
        } else
        {
            logger.info("Saving account information from LDAP");
            storeAccount();
            logger.info("Incremental backup has ended");
            return;
        }
    }

    public BackupParams getParams()
    {
        return mParams;
    }

    protected void beginUnlockedStage()
        throws ServiceException
    {
    }

    protected void beginLockedStage()
        throws ServiceException
    {
    }

    protected void storeAccount()
        throws IOException, ServiceException
    {
    }

    protected abstract void storeTables()
        throws ServiceException, IOException;

    protected abstract void storeIndex()
        throws ServiceException, IOException;

    protected abstract void waitForFullBackupCompletion()
        throws ServiceException;

    protected long storeTablesToLocalFile(File targetDir)
        throws ServiceException
    {
        com.zimbra.cs.db.DbPool.Connection conn = null;
        long l;
        conn = DbPool.getConnection();
        long bytes = 0L;
        int mboxId = getMailboxId();
        String tableNames[] = getTables();
        File schemaFile = new File(targetDir, "db_schema.xml");
        List tableInfos = new ArrayList(tableNames.length);
        String arr$[] = tableNames;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            String tname = arr$[i$];
            com.zimbra.cs.db.DbBackup.TableInfo tinfo = DbBackup.getTableInfo(conn, tname);
            tableInfos.add(tinfo);
        }

        XmlMeta.writeTablesSchema(tableInfos, schemaFile);
        for(Iterator i$ = tableInfos.iterator(); i$.hasNext();)
        {
            com.zimbra.cs.db.DbBackup.TableInfo tinfo = (com.zimbra.cs.db.DbBackup.TableInfo)i$.next();
            String tableName = DbBackup.removeDatabasePrefix(tinfo.getName());
            String whereCol = "mailbox".equals(tableName) ? "id" : "mailbox_id";
            bytes += DbBackup.saveTable(conn, tinfo, targetDir, (new StringBuilder()).append("WHERE ").append(whereCol).append(" = ").append(mboxId).toString());
        }

        l = bytes;
        DbPool.quietClose(conn);
        return l;
        Exception exception;
        exception;
        DbPool.quietClose(conn);
        throw exception;
    }

    protected String[] getTables()
        throws ServiceException
    {
        if(mTables == null)
        {
            int id = getMailboxId();
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(id);
            mTables = (new String[] {
                "mailbox", "mailbox_metadata", "scheduled_task", "out_of_office", "mobile_devices", DbMailItem.getMailItemTableName(mbox, false), DbMailItem.getMailItemTableName(mbox, true), DbMailItem.getConversationTableName(mbox), DbMailItem.getCalendarItemTableName(mbox, false), DbMailItem.getCalendarItemTableName(mbox, true), 
                DbMailItem.getRevisionTableName(mbox, false), DbMailItem.getRevisionTableName(mbox, true), DbMailItem.getTombstoneTableName(mbox), DbDataSource.getTableName(mbox), DbImapFolder.getTableName(mbox), DbImapMessage.getTableName(mbox), DbPop3Message.getTableName(mbox)
            });
        }
        return mTables;
    }

    protected abstract void storeBlobsBeforeMaintenanceMode()
        throws ServiceException, IOException;

    protected abstract void storeBlobs()
        throws ServiceException, IOException;

    protected abstract void storeBlob(String s, Volume volume, File file)
        throws ServiceException, IOException;

    protected void storeBlobsImpl()
        throws ServiceException, IOException
    {
        int total = 0;
        int mboxId = getMailboxId();
        List vols = Volume.getAll();
        Iterator it = vols.iterator();
        do
        {
            if(!it.hasNext())
                break;
            Volume vol = (Volume)it.next();
            short volType = vol.getType();
            if(volType == 1 || volType == 2 && !mParams.skipSecondaryBlobs)
            {
                String path = vol.getMailboxDir(mboxId, 1);
                File mboxPath = new File(path);
                if(mboxPath.exists())
                {
                    int count = storeBlobsRecursive(vol, mboxPath);
                    logger.info((new StringBuilder()).append("Stored ").append(count).append(" blob files from volume ").append(vol.getName()).toString());
                    total += count;
                }
            }
        } while(true);
        logger.info((new StringBuilder()).append("Stored ").append(total).append(" blob files").toString());
    }

    private void storeVolumesInfo()
    {
        int mboxId = getMailboxId();
        List vols = Volume.getAll();
        Iterator it = vols.iterator();
        do
        {
            if(!it.hasNext())
                break;
            Volume vol = (Volume)it.next();
            String id = String.valueOf(vol.getId());
            if(getVolumeInfo(id) == null)
            {
                String path = vol.getMailboxDir(mboxId, vol.getType());
                addVolumeInfo(id, new AccountSession.VolumeInfo(vol, path));
            }
        } while(true);
    }

    private int storeBlobsRecursive(Volume vol, File src)
        throws ServiceException, IOException
    {
        File files[] = src.listFiles();
        if(files == null)
            return 0;
        int count = 0;
        for(int i = 0; i < files.length; i++)
        {
            if(getBackupSet().isAborted())
                throw BackupServiceException.ABORTED_BY_COMMAND();
            if(!files[i].exists())
                continue;
            if(!fileHasMsgExt(files[i]))
            {
                if(files[i].isDirectory())
                    count += storeBlobsRecursive(vol, files[i]);
                continue;
            }
            Pair idRev = parseMessageIdRev(files[i]);
            if(idRev == null)
                continue;
            if(mBlobDigests.isEnabled())
            {
                String digest = mBlobDigests.get(idRev);
                if(digest != null)
                {
                    storeBlob(digest, vol, files[i]);
                    count++;
                } else
                {
                    logger.debug((new StringBuilder()).append("Skipping file with unknown digest: ").append(files[i].getAbsolutePath()).toString());
                }
            } else
            {
                storeBlob(null, vol, files[i]);
                count++;
            }
        }

        return count;
    }

    protected void storeBlobsModifiedSince(int sinceChangeId)
        throws ServiceException, IOException
    {
        Mailbox mbox = getMailbox();
        int changeId = mbox.getLastChangeID();
        if(changeId > sinceChangeId)
        {
            logger.debug((new StringBuilder()).append("Mailbox ").append(mbox.getId()).append(" at changeId ").append(changeId).append(" after blob stage").toString());
            Pair modItemsResult = mbox.getModifiedItems(null, sinceChangeId);
            if(modItemsResult != null)
            {
                int numModifiedBlobs = 0;
                List visibleItemIds = (List)modItemsResult.getFirst();
                if(visibleItemIds != null)
                {
                    Iterator i$ = visibleItemIds.iterator();
                    do
                    {
                        if(!i$.hasNext())
                            break;
                        int itemId = ((Integer)i$.next()).intValue();
                        MailItem mi = mbox.getItemById(null, itemId, (byte)-1);
                        MailboxBlob mblob = mi.getBlob();
                        if(mblob != null)
                        {
                            numModifiedBlobs++;
                            Volume vol = Volume.getById(mblob.getLocator());
                            Blob blob = mblob.getLocalBlob();
                            storeBlob(blob.getDigest(), vol, blob.getFile());
                        }
                    } while(true);
                }
                TypedIdList tilist = (TypedIdList)modItemsResult.getSecond();
                if(tilist != null)
                {
                    for(Iterator entryIter = tilist.iterator(); entryIter.hasNext();)
                    {
                        List itemIds = (List)((java.util.Map.Entry)entryIter.next()).getValue();
                        if(itemIds != null)
                        {
                            Iterator i$ = itemIds.iterator();
                            while(i$.hasNext()) 
                            {
                                int itemId = ((Integer)i$.next()).intValue();
                                MailItem mi = mbox.getItemById(null, itemId, (byte)-1);
                                MailboxBlob mblob = mi.getBlob();
                                if(mblob != null)
                                {
                                    numModifiedBlobs++;
                                    Volume vol = Volume.getById(mblob.getLocator());
                                    Blob blob = mblob.getLocalBlob();
                                    storeBlob(blob.getDigest(), vol, blob.getFile());
                                }
                            }
                        }
                    }

                }
                if(numModifiedBlobs > 0)
                    logger.info((new StringBuilder()).append("Found ").append(numModifiedBlobs).append(" blobs that were created/updated after backup began for mailbox ").append(mbox.getId()).toString());
                else
                    logger.info((new StringBuilder()).append("No additional blobs to backup for mailbox ").append(mbox.getId()).toString());
            }
        } else
        {
            logger.debug((new StringBuilder()).append("Mailbox ").append(mbox.getId()).append(" at same changeId; there was no change since backup start").toString());
        }
    }

    protected abstract void storeIndexFile(File file, String s)
        throws IOException;

    protected void storeIndexImpl()
        throws IOException, ServiceException
    {
        int total = 0;
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        short volId = mbox.getIndexVolume();
        Volume vol = Volume.getById(volId);
        if(vol.getType() == 10)
        {
            String path = vol.getMailboxDir(mboxId, 10);
            File mboxPath = new File(path);
            if(mboxPath.exists())
                total += storeIndexFilesRecursive("", mboxPath);
        }
        logger.info((new StringBuilder()).append("Stored ").append(total).append(" index files").toString());
    }

    private int storeIndexFilesRecursive(String relPath, File src)
        throws IOException, ServiceException
    {
        File files[] = src.listFiles();
        if(files == null)
            return 0;
        int count = 0;
        for(int i = 0; i < files.length; i++)
        {
            if(getBackupSet().isAborted())
                throw BackupServiceException.ABORTED_BY_COMMAND();
            String nextRelPath;
            if(!StringUtil.isNullOrEmpty(relPath))
                nextRelPath = (new StringBuilder()).append(relPath).append(File.separator).append(files[i].getName()).toString();
            else
                nextRelPath = files[i].getName();
            if(files[i].isDirectory())
            {
                count += storeIndexFilesRecursive(nextRelPath, files[i]);
            } else
            {
                storeIndexFile(files[i], nextRelPath);
                count++;
            }
        }

        return count;
    }

    private static boolean fileHasMsgExt(File file)
    {
        return file.getName().endsWith(".msg");
    }

    private Pair parseMessageIdRev(File file)
    {
        if(!fileHasMsgExt(file))
            return null;
        String filename = file.getName();
        int len = filename.length();
        if(len < 5)
        {
            logger.warn((new StringBuilder()).append("Invalid blob filename ").append(file.getAbsolutePath()).toString());
            return null;
        }
        String basename = filename.substring(0, len - 4);
        String pieces[] = basename.split("-");
        if(pieces != null)
        {
            if(pieces.length >= 2)
                return new Pair(pieces[0], pieces[1]);
            if(pieces.length == 1)
                return new Pair(pieces[0], null);
        }
        return null;
    }

    protected void loadBlobDigestMap()
        throws ServiceException
    {
        mBlobDigests.preload();
    }

    protected int getLinkCount(File file)
        throws IOException
    {
        return IO.linkCount(file.getPath());
    }

    protected void verifyStateForBackup()
    {
        if(getMailbox() == null)
            throw new IllegalStateException("mailbox is null during backup");
        else
            return;
    }

    protected Thread[] getWorkerThreads()
    {
        return null;
    }

    private String mTables[];
    public static final String TABLE_MAILBOX = "mailbox";
    public static final String TABLE_MAILBOX_METADATA = "mailbox_metadata";
    public static final String TABLE_SCHEDULED_TASK = "scheduled_task";
    public static final String TABLE_OUT_OF_OFFICE = "out_of_office";
    public static final String TABLE_MOBILE_DEVICES = "mobile_devices";
    private BlobDigests mBlobDigests;
    protected AccountSession mFullSession;
    private BackupParams mParams;
    static final boolean $assertionsDisabled = !com/zimbra/cs/backup/BackupAccountSession.desiredAssertionStatus();

}
