// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupServiceException.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import java.io.File;

public class BackupServiceException extends ServiceException
{

    private BackupServiceException(String message, String code, boolean isReceiversFault, Throwable cause)
    {
        super(message, code, isReceiversFault, cause, new com.zimbra.common.service.ServiceException.Argument[0]);
    }

    public static BackupServiceException INVALID_BACKUP_TARGET(String msg)
    {
        return new BackupServiceException((new StringBuilder()).append("Invalid backup target: ").append(msg).toString(), "backup.INVALID_BACKUP_TARGET", false, null);
    }

    public static BackupServiceException NO_SUCH_ACCOUNT_BACKUP(String name)
    {
        return new BackupServiceException((new StringBuilder()).append("no such backup for account: ").append(name).toString(), "backup.NO_SUCH_ACCOUNT_BACKUP", false, null);
    }

    public static BackupServiceException ACCOUNT_BACKUP_EXISTS(String name)
    {
        return new BackupServiceException((new StringBuilder()).append("backup for account already exists: ").append(name).toString(), "backup.ACCOUNT_BACKUP_EXISTS", false, null);
    }

    public static BackupServiceException FULL_BACKUP_SESSION_REQUIRED(String name)
    {
        return new BackupServiceException((new StringBuilder()).append("full backup required: ").append(name).toString(), "backup.FULL_BACKUP_SESSION_REQUIRED", false, null);
    }

    public static BackupServiceException CANNOT_ABORT_COMPLETED_BACKUP()
    {
        return new BackupServiceException("cannot abort an already completed backup", "backup.CANNOT_ABORT_COMPLETED_BACKUP", false, null);
    }

    public static BackupServiceException INVALID_BACKUP_LABEL(String label)
    {
        return new BackupServiceException((new StringBuilder()).append("invalid backup label ").append(label).toString(), "backup.INVALID_BACKUP_LABEL", false, null);
    }

    public static BackupServiceException INVALID_BACKUP_LABEL(String label, Throwable cause)
    {
        return new BackupServiceException((new StringBuilder()).append("invalid backup label ").append(label).toString(), "backup.INVALID_BACKUP_LABEL", false, cause);
    }

    public static BackupServiceException NO_SUCH_BACKUP_LABEL(String label, String msg)
    {
        String errorMsg = (new StringBuilder()).append("backup ").append(label).append(" not found").toString();
        if(msg == null)
            errorMsg = (new StringBuilder()).append(errorMsg).append(": ").append(msg).toString();
        return new BackupServiceException(errorMsg, "backup.NO_SUCH_BACKUP_LABEL", false, null);
    }

    public static BackupServiceException INCOMPLETE_BACKUP(String label, String msg)
    {
        return new BackupServiceException((new StringBuilder()).append("incomplete backup ").append(label).append(": ").append(msg).toString(), "backup.INCOMPLETE_BACKUP", true, null);
    }

    public static BackupServiceException REDOLOG_OUT_OF_SEQUENCE(long minMissing, long maxMissing)
    {
        String msg;
        if(minMissing == maxMissing)
            msg = String.format("Found gap in redo log sequence; missing %d; %s", new Object[] {
                Long.valueOf(minMissing), "To avoid future restore problems, discard all existing backups and take a full backup of all accounts; If this error occurred during restore, try the --ignoreRedoErrors option"
            });
        else
            msg = String.format("Found gap in redo log sequence; missing %d through %d; %s", new Object[] {
                Long.valueOf(minMissing), Long.valueOf(maxMissing), "To avoid future restore problems, discard all existing backups and take a full backup of all accounts; If this error occurred during restore, try the --ignoreRedoErrors option"
            });
        return new BackupServiceException(msg, "backup.REDOLOG_OUT_OF_SEQUENCE", true, null);
    }

    public static BackupServiceException REDOLOG_RESET_DETECTED(long expected, long minFound, long maxFound)
    {
        String msg;
        if(minFound == maxFound)
            msg = String.format("Detected possible redo log sequence reset; expected sequence %d, but found %d; %s", new Object[] {
                Long.valueOf(expected), Long.valueOf(minFound), "To avoid future restore problems, discard all existing backups and take a full backup of all accounts; If this error occurred during restore, try the --ignoreRedoErrors option"
            });
        else
            msg = String.format("Detected possible redo log sequence reset; expected sequence %d, but found %d through %d; %s", new Object[] {
                Long.valueOf(expected), Long.valueOf(minFound), Long.valueOf(maxFound), "To avoid future restore problems, discard all existing backups and take a full backup of all accounts; If this error occurred during restore, try the --ignoreRedoErrors option"
            });
        return new BackupServiceException(msg, "backup.REDOLOG_RESET_DETECTED", true, null);
    }

    public static BackupServiceException ABORTED_BY_COMMAND()
    {
        return new BackupServiceException("Backup aborted by command", "backup.ABORTED_BY_COMMAND", false, null);
    }

    public static BackupServiceException OUT_OF_DISK(File path, Throwable cause)
    {
        long total = path.getTotalSpace() / 1024L / 1024L;
        long usable = path.getUsableSpace() / 1024L / 1024L;
        return new BackupServiceException((new StringBuilder()).append("Not enough free space; path=").append(path.getAbsolutePath()).append(", total=").append(total).append("MB, free=").append(usable).append("MB").toString(), "backup.OUT_OF_DISK", true, cause);
    }

    public static BackupServiceException AUTO_GROUPED_BACKUP_TOO_SOON()
    {
        return new BackupServiceException("Auto-grouped backup requested too soon; no account needs to be backed up at this time", "backup.AUTO_GROUPED_BACKUP_TOO_SOON", false, null);
    }

    public static final String INVALID_BACKUP_TARGET = "backup.INVALID_BACKUP_TARGET";
    public static final String NO_SUCH_ACCOUNT_BACKUP = "backup.NO_SUCH_ACCOUNT_BACKUP";
    public static final String ACCOUNT_BACKUP_EXISTS = "backup.ACCOUNT_BACKUP_EXISTS";
    public static final String FULL_BACKUP_SESSION_REQUIRED = "backup.FULL_BACKUP_SESSION_REQUIRED";
    public static final String CANNOT_ABORT_COMPLETED_BACKUP = "backup.CANNOT_ABORT_COMPLETED_BACKUP";
    public static final String INVALID_BACKUP_LABEL = "backup.INVALID_BACKUP_LABEL";
    public static final String NO_SUCH_BACKUP_LABEL = "backup.NO_SUCH_BACKUP_LABEL";
    public static final String INCOMPLETE_BACKUP = "backup.INCOMPLETE_BACKUP";
    public static final String REDOLOG_OUT_OF_SEQUENCE = "backup.REDOLOG_OUT_OF_SEQUENCE";
    public static final String REDOLOG_RESET_DETECTED = "backup.REDOLOG_RESET_DETECTED";
    public static final String ABORTED_BY_COMMAND = "backup.ABORTED_BY_COMMAND";
    public static final String OUT_OF_DISK = "backup.OUT_OF_DISK";
    public static final String AUTO_GROUPED_BACKUP_TOO_SOON = "backup.AUTO_GROUPED_BACKUP_TOO_SOON";
    private static final String MSG_NEW_BACKUP_NEEDED = "To avoid future restore problems, discard all existing backups and take a full backup of all accounts; If this error occurred during restore, try the --ignoreRedoErrors option";
}
