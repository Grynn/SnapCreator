// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupService.java

package com.zimbra.cs.service.backup;

import com.zimbra.soap.DocumentDispatcher;
import com.zimbra.soap.DocumentService;
import org.dom4j.Namespace;
import org.dom4j.QName;

// Referenced classes of package com.zimbra.cs.service.backup:
//            ExportMailbox, Backup, Restore, BackupQuery, 
//            BackupAccountQuery, PurgeMovedMailbox, RolloverRedoLog, ScheduleBackups, 
//            MoveMailbox, GetMailboxVersion, GetMailboxVolumes, UnloadMailbox, 
//            ReloadAccount, RegisterMailboxMoveOut, UnregisterMailboxMoveOut, QueryMailboxMove

public class BackupService
    implements DocumentService
{
    public static final class AccountBackupStatus extends Enum
    {

        public static AccountBackupStatus[] values()
        {
            return (AccountBackupStatus[])$VALUES.clone();
        }

        public static AccountBackupStatus valueOf(String name)
        {
            return (AccountBackupStatus)Enum.valueOf(com/zimbra/cs/service/backup/BackupService$AccountBackupStatus, name);
        }

        public static final AccountBackupStatus UNKNOWN;
        public static final AccountBackupStatus NOTSTARTED;
        public static final AccountBackupStatus INPROGRESS;
        public static final AccountBackupStatus COMPLETED;
        public static final AccountBackupStatus ERROR;
        public static final AccountBackupStatus NONE;
        public static final AccountBackupStatus ALL;
        private static final AccountBackupStatus $VALUES[];

        static 
        {
            UNKNOWN = new AccountBackupStatus("UNKNOWN", 0);
            NOTSTARTED = new AccountBackupStatus("NOTSTARTED", 1);
            INPROGRESS = new AccountBackupStatus("INPROGRESS", 2);
            COMPLETED = new AccountBackupStatus("COMPLETED", 3);
            ERROR = new AccountBackupStatus("ERROR", 4);
            NONE = new AccountBackupStatus("NONE", 5);
            ALL = new AccountBackupStatus("ALL", 6);
            $VALUES = (new AccountBackupStatus[] {
                UNKNOWN, NOTSTARTED, INPROGRESS, COMPLETED, ERROR, NONE, ALL
            });
        }

        private AccountBackupStatus(String s, int i)
        {
            super(s, i);
        }
    }


    public BackupService()
    {
    }

    public void registerHandlers(DocumentDispatcher dispatcher)
    {
        dispatcher.registerHandler(EXPORTMAILBOX_REQUEST, new ExportMailbox());
        dispatcher.registerHandler(BACKUP_REQUEST, new Backup());
        dispatcher.registerHandler(RESTORE_REQUEST, new Restore());
        dispatcher.registerHandler(BACKUP_QUERY_REQUEST, new BackupQuery());
        dispatcher.registerHandler(BACKUP_ACCOUNT_QUERY_REQUEST, new BackupAccountQuery());
        dispatcher.registerHandler(PURGE_MOVED_MAILBOX_REQUEST, new PurgeMovedMailbox());
        dispatcher.registerHandler(ROLLOVER_REDOLOG_REQUEST, new RolloverRedoLog());
        dispatcher.registerHandler(SCHEDULE_BACKUPS_REQUEST, new ScheduleBackups());
        dispatcher.registerHandler(MOVE_MAILBOX_REQUEST, new MoveMailbox());
        dispatcher.registerHandler(GET_MAILBOX_VERSION_REQUEST, new GetMailboxVersion());
        dispatcher.registerHandler(GET_MAILBOX_VOLUMES_REQUEST, new GetMailboxVolumes());
        dispatcher.registerHandler(UNLOAD_MAILBOX_REQUEST, new UnloadMailbox());
        dispatcher.registerHandler(RELOAD_ACCOUNT_REQUEST, new ReloadAccount());
        dispatcher.registerHandler(REGISTER_MAILBOX_MOVE_OUT_REQUEST, new RegisterMailboxMoveOut());
        dispatcher.registerHandler(UNREGISTER_MAILBOX_MOVE_OUT_REQUEST, new UnregisterMailboxMoveOut());
        dispatcher.registerHandler(QUERY_MAILBOX_MOVE_REQUEST, new QueryMailboxMove());
    }

    public static AccountBackupStatus lookupAccountBackupStatus(String status)
    {
        AccountBackupStatus st = AccountBackupStatus.UNKNOWN;
        try
        {
            if(status != null)
                st = AccountBackupStatus.valueOf(status.toUpperCase());
        }
        catch(IllegalArgumentException e) { }
        return st;
    }

    public static final String NAMESPACE_STR = "urn:zimbraAdmin";
    public static final Namespace NAMESPACE;
    public static final QName EXPORTMAILBOX_REQUEST;
    public static final QName EXPORTMAILBOX_RESPONSE;
    public static final QName BACKUP_REQUEST;
    public static final QName BACKUP_RESPONSE;
    public static final QName BACKUP_QUERY_REQUEST;
    public static final QName BACKUP_QUERY_RESPONSE;
    public static final QName BACKUP_ACCOUNT_QUERY_REQUEST;
    public static final QName BACKUP_ACCOUNT_QUERY_RESPONSE;
    public static final QName RESTORE_REQUEST;
    public static final QName RESTORE_RESPONSE;
    public static final QName ROLLOVER_REDOLOG_REQUEST;
    public static final QName ROLLOVER_REDOLOG_RESPONSE;
    public static final QName PURGE_MOVED_MAILBOX_REQUEST;
    public static final QName PURGE_MOVED_MAILBOX_RESPONSE;
    public static final QName SCHEDULE_BACKUPS_REQUEST;
    public static final QName SCHEDULE_BACKUPS_RESPONSE;
    public static final QName MOVE_MAILBOX_REQUEST;
    public static final QName MOVE_MAILBOX_RESPONSE;
    public static final QName GET_MAILBOX_VERSION_REQUEST;
    public static final QName GET_MAILBOX_VERSION_RESPONSE;
    public static final QName GET_MAILBOX_VOLUMES_REQUEST;
    public static final QName GET_MAILBOX_VOLUMES_RESPONSE;
    public static final QName UNLOAD_MAILBOX_REQUEST;
    public static final QName UNLOAD_MAILBOX_RESPONSE;
    public static final QName RELOAD_ACCOUNT_REQUEST;
    public static final QName RELOAD_ACCOUNT_RESPONSE;
    public static final QName REGISTER_MAILBOX_MOVE_OUT_REQUEST;
    public static final QName REGISTER_MAILBOX_MOVE_OUT_RESPONSE;
    public static final QName UNREGISTER_MAILBOX_MOVE_OUT_REQUEST;
    public static final QName UNREGISTER_MAILBOX_MOVE_OUT_RESPONSE;
    public static final QName QUERY_MAILBOX_MOVE_REQUEST;
    public static final QName QUERY_MAILBOX_MOVE_RESPONSE;
    public static final String ZM_SCHEDULE_BACKUP = "zmschedulebackup";
    public static final String E_ACCOUNT = "account";
    public static final String E_ACCOUNTS = "accounts";
    public static final String E_CURRENT_ACCOUNTS = "currentAccounts";
    public static final String E_MAILBOX = "mbox";
    public static final String E_QUERY = "query";
    public static final String E_BACKUP = "backup";
    public static final String E_RESTORE = "restore";
    public static final String E_ERROR = "error";
    public static final String E_STATS = "stats";
    public static final String E_COUNTER = "counter";
    public static final String A_MAILBOXID = "mbxid";
    public static final String A_NAME = "name";
    public static final String A_METHOD = "method";
    public static final String A_ACCOUNT_ID = "accountId";
    public static final String A_PREFIX = "prefix";
    public static final String A_INCLUDEINCREMENTALS = "includeIncrementals";
    public static final String A_SYSDATA = "sysData";
    public static final String A_BACKUP_TARGET = "target";
    public static final String A_LABEL = "label";
    public static final String A_TYPE = "type";
    public static final String A_FROM = "from";
    public static final String A_TO = "to";
    public static final String A_ABORTED = "aborted";
    public static final String A_BACKUP_LIST_OFFSET = "backupListOffset";
    public static final String A_BACKUP_LIST_COUNT = "backupListCount";
    public static final String A_ACCOUNT_LIST_STATUS = "accountListStatus";
    public static final String A_ACCOUNT_LIST_OFFSET = "accountListOffset";
    public static final String A_ACCOUNT_LIST_COUNT = "accountListCount";
    public static final String A_VERBOSE = "verbose";
    public static final String A_STATS = "stats";
    public static final String A_COMPLETION_COUNT = "completionCount";
    public static final String A_ERROR_COUNT = "errorCount";
    public static final String A_TOTAL_COUNT = "total";
    public static final String A_MORE = "more";
    public static final String A_LIVE = "live";
    public static final String A_ERROR_MESSAGE = "errorMessage";
    public static final String A_START = "start";
    public static final String A_END = "end";
    public static final String A_MIN_REDO_SEQ = "minRedoSeq";
    public static final String A_MAX_REDO_SEQ = "maxRedoSeq";
    public static final String A_SYNC = "sync";
    public static final String A_ZIP = "zip";
    public static final String A_ZIP_STORE = "zipStore";
    public static final String A_SERVER = "server";
    public static final String A_STATUS = "status";
    public static final String A_REPLAY_CURRENT_REDOLOGS = "replayRedo";
    public static final String A_REBUILTSCHEMA = "rebuiltSchema";
    public static final String A_CONTINUE = "continue";
    public static final String A_BEFORE = "before";
    public static final String A_INCR_LABEL = "incr-label";
    public static final String A_SKIP_DELETED_ACCT = "skipDeletedAccounts";
    public static final String A_RESTORE_TO_TIME = "restoreToTime";
    public static final String A_RESTORE_TO_REDO_SEQ = "restoreToRedoSeq";
    public static final String A_RESTORE_TO_INCR_LABEL = "restoreToIncrLabel";
    public static final String A_IGNORE_REDO_ERRORS = "ignoreRedoErrors";
    public static final String A_SKIP_DELETE_OPS = "skipDeleteOps";
    public static final String A_COUNTER_UNIT = "unit";
    public static final String A_COUNTER_SUM = "sum";
    public static final String A_COUNTER_NUM_SAMPLES = "numSamples";
    public static final String A_TOTAL_SPACE = "totalSpace";
    public static final String A_FREE_SPACE = "freeSpace";
    public static final String A_SOURCE = "src";
    public static final String A_TARGET = "dest";
    public static final String A_PORT = "destPort";
    public static final String A_OVERWRITE = "overwrite";
    public static final String A_MAX_SYNCS = "maxSyncs";
    public static final String A_SYNC_FINISH_THRESHOLD = "syncFinishThreshold";
    public static final String A_TEMP_DIR = "tempDir";
    public static final String A_CHECK_PEER = "checkPeer";
    public static final String A_NO_PEER = "noPeer";
    public static final String A_MAJOR_VERSION = "majorVer";
    public static final String A_MINOR_VERSION = "minorVer";
    public static final String A_DB_VERSION = "dbVer";
    public static final String A_INDEX_VERSION = "indexVer";
    public static final String E_FILE_COPIER = "fileCopier";
    public static final String A_FC_METHOD = "fcMethod";
    public static final String A_FC_IOTYPE = "fcIOType";
    public static final String A_FC_OIO_COPY_BUFSIZE = "fcOIOCopyBufferSize";
    public static final String A_FC_ASYNC_QUEUE_CAPACITY = "fcAsyncQueueCapacity";
    public static final String A_FC_PARALLEL_WORKERS = "fcParallelWorkers";
    public static final String A_FC_PIPES = "fcPipes";
    public static final String A_FC_PIPE_BUFFER_SIZE = "fcPipeBufferSize";
    public static final String A_FC_PIPE_READERS = "fcPipeReadersPerPipe";
    public static final String A_FC_PIPE_WRITERS = "fcPipeWritersPerPipe";
    public static final String A_SEARCH_INDEX = "searchIndex";
    public static final String A_BLOBS = "blobs";
    public static final String A_SECONDARY_BLOBS = "secondaryBlobs";
    public static final String V_INCLUDE = "include";
    public static final String V_EXCLUDE = "exclude";
    public static final String V_CONFIG = "config";

    static 
    {
        NAMESPACE = Namespace.get("urn:zimbraAdmin");
        EXPORTMAILBOX_REQUEST = QName.get("ExportMailboxRequest", NAMESPACE);
        EXPORTMAILBOX_RESPONSE = QName.get("ExportMailboxResponse", NAMESPACE);
        BACKUP_REQUEST = QName.get("BackupRequest", NAMESPACE);
        BACKUP_RESPONSE = QName.get("BackupResponse", NAMESPACE);
        BACKUP_QUERY_REQUEST = QName.get("BackupQueryRequest", NAMESPACE);
        BACKUP_QUERY_RESPONSE = QName.get("BackupQueryResponse", NAMESPACE);
        BACKUP_ACCOUNT_QUERY_REQUEST = QName.get("BackupAccountQueryRequest", NAMESPACE);
        BACKUP_ACCOUNT_QUERY_RESPONSE = QName.get("BackupAccountQueryResponse", NAMESPACE);
        RESTORE_REQUEST = QName.get("RestoreRequest", NAMESPACE);
        RESTORE_RESPONSE = QName.get("RestoreResponse", NAMESPACE);
        ROLLOVER_REDOLOG_REQUEST = QName.get("RolloverRedoLogRequest", NAMESPACE);
        ROLLOVER_REDOLOG_RESPONSE = QName.get("RolloverRedoLogResponse", NAMESPACE);
        PURGE_MOVED_MAILBOX_REQUEST = QName.get("PurgeMovedMailboxRequest", NAMESPACE);
        PURGE_MOVED_MAILBOX_RESPONSE = QName.get("PurgeMovedMailboxResponse", NAMESPACE);
        SCHEDULE_BACKUPS_REQUEST = QName.get("ScheduleBackupsRequest", NAMESPACE);
        SCHEDULE_BACKUPS_RESPONSE = QName.get("ScheduleBackupsResponse", NAMESPACE);
        MOVE_MAILBOX_REQUEST = QName.get("MoveMailboxRequest", NAMESPACE);
        MOVE_MAILBOX_RESPONSE = QName.get("MoveMailboxResponse", NAMESPACE);
        GET_MAILBOX_VERSION_REQUEST = QName.get("GetMailboxVersionRequest", NAMESPACE);
        GET_MAILBOX_VERSION_RESPONSE = QName.get("GetMailboxVersionResponse", NAMESPACE);
        GET_MAILBOX_VOLUMES_REQUEST = QName.get("GetMailboxVolumesRequest", NAMESPACE);
        GET_MAILBOX_VOLUMES_RESPONSE = QName.get("GetMailboxVolumesResponse", NAMESPACE);
        UNLOAD_MAILBOX_REQUEST = QName.get("UnloadMailboxRequest", NAMESPACE);
        UNLOAD_MAILBOX_RESPONSE = QName.get("UnloadMailboxResponse", NAMESPACE);
        RELOAD_ACCOUNT_REQUEST = QName.get("ReloadAccountRequest", NAMESPACE);
        RELOAD_ACCOUNT_RESPONSE = QName.get("ReloadAccountResponse", NAMESPACE);
        REGISTER_MAILBOX_MOVE_OUT_REQUEST = QName.get("RegisterMailboxMoveOutRequest", NAMESPACE);
        REGISTER_MAILBOX_MOVE_OUT_RESPONSE = QName.get("RegisterMailboxMoveOutResponse", NAMESPACE);
        UNREGISTER_MAILBOX_MOVE_OUT_REQUEST = QName.get("UnregisterMailboxMoveOutRequest", NAMESPACE);
        UNREGISTER_MAILBOX_MOVE_OUT_RESPONSE = QName.get("UnregisterMailboxMoveOutResponse", NAMESPACE);
        QUERY_MAILBOX_MOVE_REQUEST = QName.get("QueryMailboxMoveRequest", NAMESPACE);
        QUERY_MAILBOX_MOVE_RESPONSE = QName.get("QueryMailboxMoveResponse", NAMESPACE);
    }
}
