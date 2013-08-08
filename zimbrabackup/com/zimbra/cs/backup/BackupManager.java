// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupManager.java

package com.zimbra.cs.backup;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.db.DbBackup;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoConfig;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.RolloverManager;
import com.zimbra.cs.redolog.logger.FileHeader;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.cs.redolog.logger.LogWriter;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.Zimbra;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupSet, RestoreParams, FileBackupTarget, BackupTarget, 
//            BackupParams, AccountSession, BackupServiceException, BackupLC

public class BackupManager
{
    private static class InProgress
    {

        public OpType opType;
        public long ticket;

        private InProgress()
        {
            opType = OpType.NO_OP;
            ticket = 0L;
        }

    }

    public static class OpType extends Enum
    {

        public static OpType[] values()
        {
            return (OpType[])$VALUES.clone();
        }

        public static OpType valueOf(String name)
        {
            return (OpType)Enum.valueOf(com/zimbra/cs/backup/BackupManager$OpType, name);
        }

        public static final OpType NO_OP;
        public static final OpType FULL_BACKUP;
        public static final OpType INCR_BACKUP;
        public static final OpType DEL_BACKUP;
        public static final OpType RESTORE;
        private static final OpType $VALUES[];

        static 
        {
            NO_OP = new OpType("NO_OP", 0);
            FULL_BACKUP = new OpType("FULL_BACKUP", 1) {

                public String toString()
                {
                    return "full backup";
                }

            }
;
            INCR_BACKUP = new OpType("INCR_BACKUP", 2) {

                public String toString()
                {
                    return "incremental backup";
                }

            }
;
            DEL_BACKUP = new OpType("DEL_BACKUP", 3) {

                public String toString()
                {
                    return "backup deletion";
                }

            }
;
            RESTORE = new OpType("RESTORE", 4) {

                public String toString()
                {
                    return "restore";
                }

            }
;
            $VALUES = (new OpType[] {
                NO_OP, FULL_BACKUP, INCR_BACKUP, DEL_BACKUP, RESTORE
            });
        }

        private OpType(String s, int i)
        {
            super(s, i);
        }

    }

    private static class AutoGroupedBackupHelper
    {
        private static class Interval
        {

            public int getMonths()
            {
                return mMonths;
            }

            public int getWeeks()
            {
                return mWeeks;
            }

            public int getDays()
            {
                return mDays;
            }

            public long getMillis()
            {
                return mMillis;
            }

            public static Interval parse(String str)
                throws ServiceException
            {
                int len;
                if(str == null || (len = str.length()) < 1)
                    throw ServiceException.FAILURE("Missing auto-grouped backup interval", null);
                long total = 0L;
                long num = 0L;
                for(int i = 0; i < len; i++)
                {
                    char ch = str.charAt(i);
                    if(ch >= '0' && ch <= '9')
                    {
                        num = num * 10L + (long)(ch - 48);
                        continue;
                    }
                    switch(ch)
                    {
                    case 77: // 'M'
                        if(num > 0L)
                            return new Interval((int)num, 0, 0, 0L);
                        break;

                    case 119: // 'w'
                        if(num > 0L)
                            return new Interval(0, (int)num, 0, 0L);
                        break;

                    case 100: // 'd'
                        if(num > 0L)
                            return new Interval(0, 0, (int)num, 0L);
                        break;

                    case 104: // 'h'
                        num *= 0x36ee80L;
                        break;

                    case 109: // 'm'
                        num *= 60000L;
                        break;

                    case 115: // 's'
                        num *= 1000L;
                        break;

                    default:
                        throw ServiceException.FAILURE((new StringBuilder()).append("Invalid auto-grouped backup interval \"").append(str).append("\".  Defaulting to 1d.").toString(), null);
                    }
                    total += num;
                    num = 0L;
                }

                total += num;
                if(total < 1000L)
                    throw ServiceException.FAILURE((new StringBuilder()).append("Invalid auto-grouped backup interval \"").append(str).append("\".  Defaulting to 1d.").toString(), null);
                else
                    return new Interval(0, 0, 0, total);
            }

            private final int mMonths;
            private final int mWeeks;
            private final int mDays;
            private final long mMillis;

            public Interval(int months, int weeks, int days, long millis)
            {
                mMonths = months;
                mWeeks = weeks;
                mDays = days;
                mMillis = millis;
            }
        }


        public static Pair getBackupBoundaries(long now, int groups, String intervalStr)
            throws ServiceException
        {
            Interval interval = Interval.parse(intervalStr);
            long mustBackupIfOlderThan;
            long neverBackupIfAsRecentAs;
            if(interval.getMillis() == 0L)
            {
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(now);
                cal.set(11, 0);
                cal.set(12, 0);
                cal.set(13, 0);
                cal.set(14, 0);
                neverBackupIfAsRecentAs = cal.getTimeInMillis();
                int intervalLength;
                int calField;
                if(interval.getDays() > 0)
                {
                    calField = 6;
                    intervalLength = interval.getDays();
                } else
                if(interval.getWeeks() > 0)
                {
                    calField = 3;
                    intervalLength = interval.getWeeks();
                } else
                if(interval.getMonths() > 0)
                {
                    calField = 2;
                    intervalLength = interval.getMonths();
                } else
                {
                    throw ServiceException.FAILURE("Missing auto-grouped backup interval", null);
                }
                cal.add(calField, -1 * groups * intervalLength);
                cal.add(6, 1);
                mustBackupIfOlderThan = cal.getTimeInMillis();
            } else
            {
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(now);
                cal.set(13, 0);
                cal.set(14, 0);
                neverBackupIfAsRecentAs = cal.getTimeInMillis();
                int seconds = (int)(interval.getMillis() / 1000L);
                cal.add(13, -1 * groups * seconds);
                cal.add(12, 1);
                mustBackupIfOlderThan = cal.getTimeInMillis();
            }
            return new Pair(Long.valueOf(mustBackupIfOlderThan), Long.valueOf(neverBackupIfAsRecentAs));
        }

        private static final long MILLIS_PER_SECOND = 1000L;
        private static final long MILLIS_PER_MINUTE = 60000L;
        private static final long MILLIS_PER_HOUR = 0x36ee80L;

        private AutoGroupedBackupHelper()
        {
        }
    }

    private static class NoOrphanAutoGroupFilter
        implements com.zimbra.cs.db.DbBackup.AutoGroupSelectionFilter
    {

        public boolean accept(String accountId, String email, long lastBackupAt)
            throws ServiceException
        {
            Account acct = mProv.get(com.zimbra.cs.account.Provisioning.AccountBy.id, accountId);
            if(acct != null)
            {
                if(Provisioning.onLocalServer(acct))
                {
                    if(lastBackupAt <= 0L)
                        mNumNew++;
                    return true;
                }
                Log.backup.info((new StringBuilder()).append("Skipping orphaned mailbox ").append(email).append(" (account moved to another host)").toString());
            } else
            {
                Log.backup.info((new StringBuilder()).append("Skipping orphaned mailbox ").append(email).append(" (account was deleted)").toString());
            }
            mNumOrphans++;
            return false;
        }

        public int getNumNew()
        {
            return mNumNew;
        }

        public int getNumOrphans()
        {
            return mNumOrphans;
        }

        private final Provisioning mProv = Provisioning.getInstance();
        private int mNumNew;
        private int mNumOrphans;

        public NoOrphanAutoGroupFilter()
        {
        }
    }

    public static final class BackupMode extends Enum
    {

        public static BackupMode[] values()
        {
            return (BackupMode[])$VALUES.clone();
        }

        public static BackupMode valueOf(String name)
        {
            return (BackupMode)Enum.valueOf(com/zimbra/cs/backup/BackupManager$BackupMode, name);
        }

        public static final BackupMode STANDARD;
        public static final BackupMode AUTO_GROUPED;
        private static final BackupMode $VALUES[];

        static 
        {
            STANDARD = new BackupMode("STANDARD", 0);
            AUTO_GROUPED = new BackupMode("AUTO_GROUPED", 1);
            $VALUES = (new BackupMode[] {
                STANDARD, AUTO_GROUPED
            });
        }

        private BackupMode(String s, int i)
        {
            super(s, i);
        }
    }

    private class FullBackupThread extends Thread
    {

        public void run()
        {
            boolean sentReportEmail = false;
            doBackupFull(mBackupSet, mAccounts, mTarget);
            sendReportEmail(new BackupSet[] {
                mBackupSet
            });
            sentReportEmail = true;
            break MISSING_BLOCK_LABEL_67;
            ServiceException e;
            e;
            sendReportEmail(new BackupSet[] {
                mBackupSet
            });
            sentReportEmail = true;
            throw e;
            try
            {
                clearCurrentOp(mTicket);
            }
            // Misplaced declaration of an exception variable
            catch(ServiceException e)
            {
                Log.backup.error("Error while marking backup operation finished", e);
            }
            break MISSING_BLOCK_LABEL_206;
            e;
            Zimbra.halt("out of memory", e);
            try
            {
                clearCurrentOp(mTicket);
            }
            // Misplaced declaration of an exception variable
            catch(ServiceException e)
            {
                Log.backup.error("Error while marking backup operation finished", e);
            }
            break MISSING_BLOCK_LABEL_206;
            Throwable t;
            t;
            Log.backup.error("Error occurred during full backup", t);
            if(!sentReportEmail)
                sendErrorReportEmail(t);
            try
            {
                clearCurrentOp(mTicket);
            }
            // Misplaced declaration of an exception variable
            catch(Throwable t)
            {
                Log.backup.error("Error while marking backup operation finished", t);
            }
            break MISSING_BLOCK_LABEL_206;
            Exception exception;
            exception;
            try
            {
                clearCurrentOp(mTicket);
            }
            catch(ServiceException e)
            {
                Log.backup.error("Error while marking backup operation finished", e);
            }
            throw exception;
        }

        private final BackupSet mBackupSet;
        private final Account mAccounts[];
        private final BackupTarget mTarget;
        private final long mTicket;
        final BackupManager this$0;

        public FullBackupThread(BackupSet backupSet, Account accounts[], BackupTarget target, long ticket)
        {
            this$0 = BackupManager.this;
            super("FullBackupThread");
            mBackupSet = backupSet;
            mAccounts = accounts;
            mTarget = target;
            mTicket = ticket;
        }
    }


    public BackupManager()
    {
        mRunningRestore = false;
    }

    public static synchronized BackupManager getInstance()
    {
        if(mManager == null)
            mManager = new BackupManager();
        return mManager;
    }

    public BackupSet backupFull(BackupTarget target, BackupParams params, List syncBaks)
        throws IOException, ServiceException
    {
        Account accounts[] = lookupAccounts(target);
        return backupFull(accounts, target, params, syncBaks);
    }

    private Account[] getAccountsOnServer()
        throws ServiceException
    {
        Provisioning prov = Provisioning.getInstance();
        String serverName = prov.getLocalServer().getAttr("zimbraServiceHostname");
        List accts = prov.searchAccounts((new StringBuilder()).append("(zimbraMailHost=").append(serverName).append(")").toString(), new String[] {
            "zimbraId"
        }, null, false, 521);
        Account a[] = (Account[])(Account[])accts.toArray(new Account[0]);
        Log.backup.info((new StringBuilder()).append("Found ").append(accts.size()).append(" accounts on server ").append(serverName).toString());
        return a;
    }

    public BackupSet startBackupFull(BackupTarget target, BackupParams params)
        throws IOException, ServiceException
    {
        Account accounts[] = lookupAccounts(target);
        return startBackupFull(accounts, target, params);
    }

    public BackupSet backupFull(Account accounts[], BackupTarget target, BackupParams params, List syncBaks)
        throws IOException, ServiceException
    {
        long ticket = setCurrentOp(OpType.FULL_BACKUP);
        BackupSet backupset;
        BackupSet bak = target.createFullBackupSet(getLabel(1), "Full backup", accounts, params);
        syncBaks.add(bak);
        doBackupFull(bak, accounts, target);
        backupset = bak;
        try
        {
            clearCurrentOp(ticket);
        }
        catch(ServiceException e)
        {
            Log.backup.error("Error while marking backup operation finished", e);
        }
        return backupset;
        Exception exception;
        exception;
        try
        {
            clearCurrentOp(ticket);
        }
        catch(ServiceException e)
        {
            Log.backup.error("Error while marking backup operation finished", e);
        }
        throw exception;
    }

    public void deleteBackups(BackupTarget target, long cutoffTime)
        throws IOException, ServiceException
    {
        long ticket = setCurrentOp(OpType.DEL_BACKUP);
        target.deleteBackups(cutoffTime);
        try
        {
            clearCurrentOp(ticket);
        }
        catch(ServiceException e)
        {
            Log.backup.error("Error while marking backup operation finished", e);
        }
        break MISSING_BLOCK_LABEL_66;
        Exception exception;
        exception;
        try
        {
            clearCurrentOp(ticket);
        }
        catch(ServiceException e)
        {
            Log.backup.error("Error while marking backup operation finished", e);
        }
        throw exception;
    }

    public BackupSet startBackupFull(Account accounts[], BackupTarget target, BackupParams params)
        throws IOException, ServiceException
    {
        long ticket;
        BackupSet bak;
        boolean backupSetCreated;
        ticket = setCurrentOp(OpType.FULL_BACKUP);
        bak = null;
        backupSetCreated = false;
        bak = target.createFullBackupSet(getLabel(1), "Full backup", accounts, params);
        backupSetCreated = true;
        if(!backupSetCreated)
            try
            {
                clearCurrentOp(ticket);
            }
            catch(ServiceException e)
            {
                Log.backup.error("Error while marking backup operation finished", e);
            }
        break MISSING_BLOCK_LABEL_95;
        Exception exception;
        exception;
        if(!backupSetCreated)
            try
            {
                clearCurrentOp(ticket);
            }
            catch(ServiceException e)
            {
                Log.backup.error("Error while marking backup operation finished", e);
            }
        throw exception;
        FullBackupThread thread = new FullBackupThread(bak, accounts, target, ticket);
        thread.start();
        return bak;
    }

    private Account[] lookupAccounts(BackupTarget target)
        throws ServiceException
    {
        Account accts[] = getAccountsOnServer();
        return accts;
    }

    public BackupMode getBackupMode()
        throws ServiceException
    {
        String mode = Provisioning.getInstance().getLocalServer().getAttr("zimbraBackupMode", "Standard");
        if("Auto-Grouped".equalsIgnoreCase(mode))
            return BackupMode.AUTO_GROUPED;
        else
            return BackupMode.STANDARD;
    }

    public BackupSet[] backupIncremental(BackupTarget target, BackupParams params, List syncBaks)
        throws IOException, ServiceException
    {
        List fullBackupNeeded;
        BackupSet result[];
        Account accts[];
        long ticket;
        ServiceException e;
        Account accounts[] = lookupAccounts(target);
        int sz = accounts.length;
        List incrAccounts = new ArrayList(sz);
        fullBackupNeeded = new ArrayList(sz);
        for(int i = 0; i < accounts.length; i++)
        {
            Account account = accounts[i];
            String acctId = account.getId();
            if(!target.hasAccountSession(acctId))
            {
                fullBackupNeeded.add(account);
                continue;
            }
            AccountSession sess = null;
            try
            {
                sess = target.getAccountSession(acctId);
                if(sess.isAccountOnly())
                    fullBackupNeeded.add(account);
                else
                    incrAccounts.add(account);
                continue;
            }
            // Misplaced declaration of an exception variable
            catch(ServiceException e)
            {
                Log.backup.warn((new StringBuilder()).append("Error looking up last backup for ").append(acctId).toString(), e);
            }
            fullBackupNeeded.add(account);
        }

        result = new BackupSet[2];
        if(incrAccounts.isEmpty())
            break MISSING_BLOCK_LABEL_401;
        if(!RedoConfig.redoLogEnabled())
            throw ServiceException.FAILURE("Cannot do incremental backup: redo logging is disabled", null);
        accts = (Account[])incrAccounts.toArray(new Account[0]);
        ticket = setCurrentOp(OpType.INCR_BACKUP);
        BackupSet incrBak;
        incrBak = target.createIncrementalBackupSet(getLabel(2), "Incremental backup", accts, params);
        syncBaks.add(incrBak);
        incrBak.startIncrementalBackup();
        incrBak.endIncrementalBackup();
        result[0] = incrBak;
        break MISSING_BLOCK_LABEL_320;
        Exception exception;
        exception;
        result[0] = incrBak;
        throw exception;
        Exception exception1;
        exception1;
        incrBak.endIncrementalBackup();
        result[0] = incrBak;
        break MISSING_BLOCK_LABEL_317;
        Exception exception2;
        exception2;
        result[0] = incrBak;
        throw exception2;
        throw exception1;
        try
        {
            clearCurrentOp(ticket);
        }
        // Misplaced declaration of an exception variable
        catch(BackupSet incrBak)
        {
            Log.backup.error("Error while marking backup operation finished", incrBak);
        }
        break MISSING_BLOCK_LABEL_401;
        incrBak;
        if(FileUtil.isOutOfDiskError(incrBak) || target.outOfSpace())
            throw target.makeOutOfSpaceException(incrBak);
        else
            throw incrBak;
        Exception exception3;
        exception3;
        try
        {
            clearCurrentOp(ticket);
        }
        catch(ServiceException e)
        {
            Log.backup.error("Error while marking backup operation finished", e);
        }
        throw exception3;
        BackupMode backupMode = getBackupMode();
        if(!fullBackupNeeded.isEmpty() && !BackupMode.AUTO_GROUPED.equals(backupMode))
        {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for(Iterator i$ = fullBackupNeeded.iterator(); i$.hasNext();)
            {
                Account acct = (Account)i$.next();
                try
                {
                    MailboxManager.getInstance().getMailboxByAccount(acct, true);
                }
                catch(AccountServiceException e)
                {
                    if(!e.getCode().equals("account.NO_SUCH_ACCOUNT"))
                        throw e;
                }
                if(i > 0)
                    sb.append(", ");
                else
                    sb.append('[');
                sb.append(acct.getName());
                i++;
            }

            sb.append(']');
            String accountString = sb.length() >= 200 ? (new StringBuilder()).append(sb.substring(0, 150)).append(" ... ").append(sb.substring(sb.length() - 50)).toString() : sb.toString();
            Log.backup.info((new StringBuilder()).append("No prior full backup found for accounts ").append(accountString).append("; performing full backup instead of incremental backup").toString());
            Account accts[] = (Account[])fullBackupNeeded.toArray(new Account[0]);
            if(params.sync)
                result[1] = backupFull(accts, target, params, syncBaks);
            else
                result[1] = startBackupFull(accts, target, params);
        }
        return result;
    }

    public Account[] lookupAccounts(List acctNames, com.zimbra.cs.account.Provisioning.AccountBy acctBy, BackupTarget target)
        throws ServiceException
    {
        List accts = new ArrayList(acctNames.size());
        Iterator i$ = acctNames.iterator();
        do
        {
            if(!i$.hasNext())
                break;
            String name = (String)i$.next();
            Account acct = Provisioning.getInstance().get(acctBy, name);
            if(acct != null)
                accts.add(acct);
        } while(true);
        if(accts.isEmpty())
            throw ServiceException.INVALID_REQUEST("No account to backup", null);
        else
            return (Account[])accts.toArray(new Account[0]);
    }

    public Account[] lookupAccountsByOldestBackup(BackupTarget target)
        throws ServiceException
    {
        int groups;
        boolean throttled;
        long mustBackupIfOlderThan;
        long neverBackupIfAsRecentAs;
        int totalMailboxes;
        int minCount;
        int maxCount;
        NoOrphanAutoGroupFilter noOrphanFilter;
        com.zimbra.cs.db.DbPool.Connection conn;
        Server server = Provisioning.getInstance().getLocalServer();
        String intervalStr = server.getAttr("zimbraBackupAutoGroupedInterval", "1d");
        groups = server.getIntAttr("zimbraBackupAutoGroupedNumGroups", 7);
        throttled = server.getBooleanAttr("zimbraBackupAutoGroupedThrottled", false);
        if(groups < 1)
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Invalid backup group count: ").append(groups).toString(), null);
        long now = System.currentTimeMillis();
        Pair thresholds = AutoGroupedBackupHelper.getBackupBoundaries(now, groups, intervalStr);
        mustBackupIfOlderThan = ((Long)thresholds.getFirst()).longValue();
        neverBackupIfAsRecentAs = ((Long)thresholds.getSecond()).longValue();
        totalMailboxes = MailboxManager.getInstance().getMailboxCount();
        minCount = ((totalMailboxes + groups) - 1) / groups;
        maxCount = throttled ? minCount : 0x7fffffff;
        DateFormat dateFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
        String msg = String.format("Auto-Grouped backup: groups=%d, interval=%s, total mbox=%d, min mbox=%d, max mbox=%s, older than %s, not since %s, now = %s", new Object[] {
            Integer.valueOf(groups), intervalStr, Integer.valueOf(totalMailboxes), Integer.valueOf(minCount), throttled ? (new StringBuilder()).append(maxCount).append(" (throttled)").toString() : "unlimited", dateFmt.format(new Date(mustBackupIfOlderThan)), dateFmt.format(new Date(neverBackupIfAsRecentAs)), dateFmt.format(Long.valueOf(now))
        });
        Log.backup.info(msg);
        noOrphanFilter = new NoOrphanAutoGroupFilter();
        conn = null;
        Account aaccount[];
        conn = DbPool.getConnection();
        List acctIds = DbBackup.getAccountsByOldestBackup(conn, mustBackupIfOlderThan, neverBackupIfAsRecentAs, minCount, maxCount, noOrphanFilter);
        List adjustedAcctIds = acctIds;
        int numOrphans = noOrphanFilter.getNumOrphans();
        if(numOrphans > 0)
        {
            totalMailboxes = Math.max(totalMailboxes - numOrphans, 0);
            minCount = ((totalMailboxes + groups) - 1) / groups;
            maxCount = throttled ? minCount : 0x7fffffff;
            int newCount = Math.max(minCount, noOrphanFilter.getNumNew());
            newCount = Math.min(newCount, maxCount);
            newCount = Math.min(newCount, acctIds.size());
            if(newCount < acctIds.size() && newCount >= 0)
            {
                adjustedAcctIds = acctIds.subList(0, newCount);
                String msgAdjusted = String.format("Auto-Grouped backup: adjusting group selection after finding %d orphaned mailboxes: groups=%d, total mbox=%d, min mbox=%d, max mbox=%s, new mbox=%d", new Object[] {
                    Integer.valueOf(numOrphans), Integer.valueOf(groups), Integer.valueOf(totalMailboxes), Integer.valueOf(minCount), throttled ? (new StringBuilder()).append(maxCount).append(" (throttled)").toString() : "unlimited", Integer.valueOf(noOrphanFilter.getNumNew())
                });
                Log.backup.info(msgAdjusted);
            }
        }
        Log.backup.info((new StringBuilder()).append("Backup group will contain ").append(adjustedAcctIds.size()).append(" mailboxes").toString());
        if(adjustedAcctIds.isEmpty())
            break MISSING_BLOCK_LABEL_595;
        aaccount = lookupAccounts(adjustedAcctIds, com.zimbra.cs.account.Provisioning.AccountBy.id, target);
        DbPool.quietClose(conn);
        return aaccount;
        aaccount = new Account[0];
        DbPool.quietClose(conn);
        return aaccount;
        Exception exception;
        exception;
        DbPool.quietClose(conn);
        throw exception;
    }

    public com.zimbra.cs.db.DbMailbox.DeletedAccount getDeletedAccount(String email)
        throws ServiceException
    {
        com.zimbra.cs.db.DbPool.Connection conn = null;
        com.zimbra.cs.db.DbMailbox.DeletedAccount deletedaccount;
        conn = DbPool.getConnection();
        deletedaccount = DbMailbox.getDeletedAccount(conn, email);
        DbPool.quietClose(conn);
        return deletedaccount;
        Exception exception;
        exception;
        DbPool.quietClose(conn);
        throw exception;
    }

    public void restore(BackupTarget source)
        throws IOException, ServiceException
    {
        restore(source, null, new RestoreParams());
    }

    public void restore(BackupTarget source, String label, RestoreParams params)
        throws IOException, ServiceException
    {
        if(params.offline)
        {
            long seq = source.getMostRecentRedoSequence();
            resetRedoLogSequence(seq, params);
        }
        BackupSet bak = null;
        String accountIds[];
        if(label == null)
        {
            accountIds = source.getAccountIds();
        } else
        {
            bak = source.getBackupSet(label);
            if(bak == null)
                throw ServiceException.FAILURE((new StringBuilder()).append("Backup set not found (").append(label).append(")").toString(), null);
            if(bak.getType() == 2)
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Cannot restore from incremental backup set ").append(label).toString(), null);
            accountIds = bak.getAccountIds();
        }
        restore(accountIds, source, label, params);
    }

    private void resetRedoLogSequence(long latestSeqInBackup, RestoreParams params)
        throws IOException
    {
        RedoLogManager redoMgr;
        File loggerFile;
        LogWriter logger;
        if(!$assertionsDisabled && !params.offline)
            throw new AssertionError();
        redoMgr = RedoLogProvider.getInstance().getRedoLogManager();
        loggerFile = redoMgr.getLogFile();
        long currSeq = -1L;
        if(loggerFile.exists())
            currSeq = (new FileLogReader(loggerFile)).getHeader().getSequence();
        Log.backup.info((new StringBuilder()).append("current redo log sequence is ").append(currSeq).append(" sequence from last incr backup is ").append(latestSeqInBackup).toString());
        long expected = latestSeqInBackup + 1L;
        if(currSeq >= expected)
            break MISSING_BLOCK_LABEL_264;
        Log.backup.info((new StringBuilder()).append("removing out-of-sequence current redo log and set sequence to ").append(expected).append(" during restore").toString());
        if(loggerFile.exists() && !loggerFile.delete())
            throw new IOException((new StringBuilder()).append("unable to remove current redo log file ").append(loggerFile.getPath()).toString());
        redoMgr.getRolloverManager().initSequence(expected);
        logger = null;
        logger = redoMgr.createLogWriter(redoMgr, loggerFile, 0L);
        logger.open();
        if(logger != null)
            logger.close();
        break MISSING_BLOCK_LABEL_256;
        Exception exception;
        exception;
        if(logger != null)
            logger.close();
        throw exception;
        params.getResult().setResetRedoSequence(true);
    }

    public void restore(String accountIds[], BackupTarget source, String label, RestoreParams params)
        throws IOException, ServiceException
    {
        long ticket = setCurrentOp(OpType.RESTORE);
label0:
        {
            String incrLabel = params.restoreToIncrementalLabel;
            if(incrLabel != null)
            {
                BackupSet incr = source.getBackupSet(incrLabel);
                long last = incr.getMaxRedoSequence();
                if(last < params.restoreToSequence)
                {
                    params.restoreToSequence = last;
                    Log.backup.info((new StringBuilder()).append("Incremental restore will stop after sequence ").append(last).append(", the last sequence in incremental backup ").append(incrLabel).toString());
                }
                long incrEnd = incr.getEndTime();
                if(incrEnd < params.restoreToTime)
                    params.restoreToTime = incrEnd;
                break label0;
            }
            if(params.restoreToSequence == 0xffffffffL)
                break label0;
            List baks = source.getBackupSets(0x0L, 0xffffffffL);
            if(baks == null || baks.isEmpty())
                break label0;
            Iterator i$ = baks.iterator();
            BackupSet bak;
            long startSeq;
            long endSeq;
            do
            {
                if(!i$.hasNext())
                    break label0;
                bak = (BackupSet)i$.next();
                startSeq = bak.getMinRedoSequence();
                endSeq = bak.getMaxRedoSequence();
            } while(startSeq > params.restoreToSequence || params.restoreToSequence > endSeq);
            long bakEnd = bak.getEndTime();
            if(bakEnd < params.restoreToTime)
                params.restoreToTime = bakEnd;
        }
        source.restore(accountIds, label, params);
        try
        {
            clearCurrentOp(ticket);
        }
        catch(ServiceException e)
        {
            Log.backup.error("Error while marking backup operation finished", e);
        }
        break MISSING_BLOCK_LABEL_319;
        Exception exception;
        exception;
        try
        {
            clearCurrentOp(ticket);
        }
        catch(ServiceException e)
        {
            Log.backup.error("Error while marking backup operation finished", e);
        }
        throw exception;
    }

    void doBackupFull(BackupSet bak, Account accounts[], BackupTarget target)
        throws IOException, ServiceException
    {
        long t0;
        String threshold = Provisioning.getInstance().getLocalServer().getBackupMinFreeSpace();
        if(!target.hasEnoughFreeSpace(threshold))
        {
            BackupServiceException e = target.makeOutOfSpaceException(null);
            bak.addError(e);
            throw ServiceException.FAILURE((new StringBuilder()).append("Backup cannot start because free space is less than ").append(threshold).toString(), e);
        }
        t0 = System.currentTimeMillis();
        bak.startFullBackup();
        bak.endFullBackup();
        Log.backup.info((new StringBuilder()).append("full backup took ").append(System.currentTimeMillis() - t0).append(" millisec").toString());
        break MISSING_BLOCK_LABEL_278;
        IOException e;
        e;
        Log.backup.info((new StringBuilder()).append("full backup took ").append(System.currentTimeMillis() - t0).append(" millisec").toString());
        throw e;
        Exception exception;
        exception;
        bak.endFullBackup();
        Log.backup.info((new StringBuilder()).append("full backup took ").append(System.currentTimeMillis() - t0).append(" millisec").toString());
        break MISSING_BLOCK_LABEL_241;
        Exception exception1;
        exception1;
        Log.backup.info((new StringBuilder()).append("full backup took ").append(System.currentTimeMillis() - t0).append(" millisec").toString());
        throw exception1;
        throw exception;
        e;
        if(FileUtil.isOutOfDiskError(e) || target.outOfSpace())
            throw target.makeOutOfSpaceException(e);
        else
            throw e;
    }

    public synchronized String getLabel(int type)
    {
        String prefix = null;
        if(type == 1)
            prefix = "full-";
        else
        if(type == 2)
            prefix = "incr-";
        else
        if(type == 3)
            prefix = "move-";
        else
            throw new IllegalArgumentException((new StringBuilder()).append("invalid backup set type: ").append(type).toString());
        DateFormat fmt = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(fmt.format(new Date()));
        return sb.toString();
    }

    public static long getLabelDate(String label)
        throws BackupServiceException
    {
        String tstamp;
        if(label.startsWith("incr-"))
            tstamp = label.substring("incr-".length());
        else
        if(label.startsWith("full-"))
            tstamp = label.substring("full-".length());
        else
        if(label.startsWith("move-"))
            tstamp = label.substring("move-".length());
        else
            throw BackupServiceException.INVALID_BACKUP_LABEL(label);
        DateFormat fmt = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        try
        {
            return fmt.parse(tstamp).getTime();
        }
        catch(ParseException e)
        {
            throw BackupServiceException.INVALID_BACKUP_LABEL(label, e);
        }
    }

    private long setCurrentOp(OpType op)
        throws ServiceException
    {
        InProgress inprogress = mOpInProgress;
        JVM INSTR monitorenter ;
        boolean busy = mOpInProgress.opType != OpType.NO_OP;
        if(busy)
            if(op == OpType.FULL_BACKUP || op == OpType.INCR_BACKUP)
            {
                int i = 0;
                do
                {
                    if(i >= 6)
                        break;
                    try
                    {
                        mOpInProgress.wait(5000L);
                    }
                    catch(InterruptedException e) { }
                    busy = mOpInProgress.opType != OpType.NO_OP;
                    if(!busy)
                        break;
                    i++;
                } while(true);
                if(busy)
                    throw ServiceException.INVALID_REQUEST((new StringBuilder()).append(mOpInProgress.opType).append(" is still in progress").toString(), null);
            } else
            {
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append(mOpInProgress.opType).append(" is still in progress").toString(), null);
            }
        if(mOpInProgress.opType != OpType.NO_OP)
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append(mOpInProgress.opType).append(" is still in progress").toString(), null);
        mOpInProgress.opType = op;
        mOpInProgress.ticket = System.currentTimeMillis();
        if(op == OpType.RESTORE)
            mRunningRestore = true;
        return mOpInProgress.ticket;
        Exception exception;
        exception;
        throw exception;
    }

    private void clearCurrentOp(long ticket)
        throws ServiceException
    {
        synchronized(mOpInProgress)
        {
            if(ticket != mOpInProgress.ticket)
                throw ServiceException.INVALID_REQUEST("Wrong ticket passed to opFinished()", null);
            if(mOpInProgress.opType == OpType.NO_OP)
                throw ServiceException.INVALID_REQUEST("opFinished() called when no backup/restore operation is currently in progress", null);
            if(mOpInProgress.opType == OpType.RESTORE)
                mRunningRestore = false;
            mOpInProgress.opType = OpType.NO_OP;
            mOpInProgress.ticket = 0L;
        }
    }

    boolean isRestoreRunning()
    {
        InProgress inprogress = mOpInProgress;
        JVM INSTR monitorenter ;
        return mRunningRestore;
        Exception exception;
        exception;
        throw exception;
    }

    public void markCurrentRestoreAsInterrupted()
        throws ServiceException
    {
        synchronized(mOpInProgress)
        {
            if(mOpInProgress.opType != OpType.RESTORE)
                throw ServiceException.INVALID_REQUEST("Restore is not running", null);
            mRunningRestore = false;
        }
    }

    public void sendReportEmail(BackupSet sets[])
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        int numSets = 0;
        boolean overallSuccess = true;
        BackupSet arr$[] = sets;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            BackupSet bak = arr$[i$];
            if(bak == null)
                continue;
            numSets++;
            if(bak.hasErrorOccurred())
                overallSuccess = false;
        }

        String subject = overallSuccess ? "SUCCESS" : "FAIL";
        writer.println((new StringBuilder()).append("Server: ").append(LC.zimbra_server_hostname.value()).toString());
        int errorsTotal = 0;
        BackupSet arr$[] = sets;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            BackupSet bak = arr$[i$];
            if(bak == null)
                continue;
            writer.println();
            boolean success = !bak.hasErrorOccurred();
            String label = bak.getLabel();
            long startTime = bak.getStartTime();
            long endTime = bak.getEndTime();
            boolean outOfDisk = bak.hadOutOfDiskError();
            writer.println((new StringBuilder()).append("Label:   ").append(label).toString());
            writer.println((new StringBuilder()).append("Type:    ").append(BackupSet.getTypeLabel(bak.getType())).toString());
            writer.print("Status:  ");
            if(outOfDisk)
                writer.println("failed (out of disk)");
            else
            if(endTime > 0L)
            {
                if(success)
                    writer.println("completed");
                else
                    writer.println("completed (with errors)");
            } else
            {
                writer.println("incomplete/invalid");
            }
            DateFormat fmt = new SimpleDateFormat("EEE, yyyy/MM/dd HH:mm:ss.SSS z");
            writer.println((new StringBuilder()).append("Started: ").append(fmt.format(Long.valueOf(startTime))).toString());
            if(endTime > 0L)
                writer.println((new StringBuilder()).append("Ended:   ").append(fmt.format(Long.valueOf(endTime))).toString());
            else
                writer.println("Ended:   (did not complete)");
            if(!outOfDisk)
            {
                long minSeq = bak.getMinRedoSequence();
                long maxSeq = bak.getMaxRedoSequence();
                writer.print((new StringBuilder()).append("Redo log sequence range: ").append(minSeq).append(" .. ").toString());
                if(maxSeq != -1L)
                    writer.println(maxSeq);
                else
                    writer.println("?");
                writer.println((new StringBuilder()).append("Number of accounts: ").append(bak.getNumAccounts()).toString());
            }
            List errors = bak.getAllErrors();
            int numErrors = errors.size();
            if(numErrors > 0)
                writer.println((new StringBuilder()).append("Number of errors: ").append(numErrors).toString());
            errorsTotal += numErrors;
        }

        if(errorsTotal > 0)
        {
            writer.println();
            writer.println();
            writer.println("ERRORS");
            arr$ = sets;
            len$ = arr$.length;
            for(int i$ = 0; i$ < len$; i$++)
            {
                BackupSet bak = arr$[i$];
                if(bak == null || !bak.hasErrorOccurred())
                    continue;
                List errors = bak.getAllErrors();
                int numErrors = errors.size();
                if(numSets > 1)
                {
                    writer.println();
                    writer.println();
                    writer.println((new StringBuilder()).append("Label: ").append(bak.getLabel()).toString());
                    writer.println((new StringBuilder()).append("Number of errors: ").append(numErrors).toString());
                }
                int errorsReported = 0;
                int errorsReportMax = 10;
                Iterator i$ = errors.iterator();
                do
                {
                    if(!i$.hasNext())
                        break;
                    BackupSet.ErrorInfo err = (BackupSet.ErrorInfo)i$.next();
                    writer.println();
                    writer.println((new StringBuilder()).append(err.getAccountName()).append(": ").append(err.getMessage()).toString());
                    writer.println(err.getStacktrace());
                } while(++errorsReported != errorsReportMax);
                if(errorsReported < numErrors)
                {
                    writer.println();
                    writer.println((new StringBuilder()).append("(").append(numErrors - errorsReported).append(" more errors)").toString());
                }
            }

        }
        writer.close();
        sendReportEmail(overallSuccess, subject, stringWriter.toString());
    }

    private void sendReportEmail(boolean success, String subject, String text)
    {
        String toStrs[];
        String fromStr;
        String subjectPrefix;
        Server server;
        try
        {
            Provisioning prov = Provisioning.getInstance();
            server = prov.getLocalServer();
            toStrs = server.getMultiAttr("zimbraBackupReportEmailRecipients");
            if(toStrs == null || toStrs.length == 0)
                return;
        }
        catch(ServiceException e)
        {
            Log.backup.warn("Unable to send backup report email", e);
            return;
        }
        fromStr = server.getAttr("zimbraBackupReportEmailSender");
        if(fromStr == null)
            fromStr = (new StringBuilder()).append("root@").append(server.getName()).toString();
        subjectPrefix = server.getAttr("zimbraBackupReportEmailSubjectPrefix", "ZCS Backup Report");
        Exception error;
        MimeMessage mm;
        InternetAddress to;
        error = null;
        mm = null;
        try
        {
            mm = new ZMimeMessage(JMSession.getSmtpSession());
        }
        catch(MessagingException e)
        {
            Log.backup.warn("Unable to send backup report email", e);
            return;
        }
        InternetAddress from = null;
        to = null;
        List toList;
        subject = (new StringBuilder()).append(subjectPrefix).append(": ").append(subject).toString();
        toList = new ArrayList(toStrs.length);
        String arr$[] = toStrs;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            String addrStr = arr$[i$];
            try
            {
                if(addrStr != null && addrStr.length() > 0)
                {
                    Address addr = new JavaMailInternetAddress(addrStr);
                    toList.add(addr);
                }
            }
            catch(MessagingException mex)
            {
                Log.backup.warn((new StringBuilder()).append("Ignoring invalid notification recipient address \"").append(addrStr).append("\"").toString());
            }
        }

        if(toList.size() == 0)
        {
            Log.backup.warn("Skipping email notification because no valid recipient address was found");
            return;
        }
        try
        {
            Address rcpts[] = new Address[toList.size()];
            toList.toArray(rcpts);
            mm.setRecipients(javax.mail.Message.RecipientType.TO, rcpts);
            Log.backup.debug("Sending backup report email");
            InternetAddress from = new JavaMailInternetAddress(fromStr);
            mm.setFrom(from);
            mm.setSubject(subject);
            mm.setText(text);
            mm.setSentDate(new Date());
            mm.saveChanges();
            Transport.send(mm);
        }
        catch(MessagingException mex)
        {
            error = new com.zimbra.cs.mailbox.MailSender.SafeMessagingException(mex);
        }
        if(error != null)
            Log.backup.warn((new StringBuilder()).append("Unable to send backup report email to ").append(to).append(", subject = ").append(subject).toString(), error);
        return;
    }

    public void sendErrorReportEmail(Throwable t)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        writer.println((new StringBuilder()).append("Server: ").append(LC.zimbra_server_hostname.value()).toString());
        writer.println();
        DateFormat fmt = new SimpleDateFormat("EEE, yyyy/MM/dd HH:mm:ss.SSS z");
        writer.println((new StringBuilder()).append("Time: ").append(fmt.format(new Date())).toString());
        writer.println("Reason:");
        writer.println();
        t.printStackTrace(writer);
        writer.println();
        writer.close();
        sendReportEmail(false, "FAIL", stringWriter.toString());
    }

    public BackupTarget getBackupTarget(String target, boolean create)
        throws IOException, ServiceException
    {
        boolean isCustom = false;
        File backupRoot = null;
        Server server = Provisioning.getInstance().getLocalServer();
        String defaultPath = server.getAttr("zimbraBackupTarget", null);
        if(defaultPath != null)
            backupRoot = new File(defaultPath);
        else
            backupRoot = new File(LC.zimbra_home.value(), "backup");
        if(target != null)
        {
            if(sBadTargetRegex.matcher(target).find())
                throw ServiceException.INVALID_REQUEST("invalid target: no white spaces allowed", null);
            File customRoot = new File(target);
            if(!customRoot.isAbsolute() && !target.startsWith("/"))
                customRoot = new File(LC.zimbra_home.value(), target);
            isCustom = !customRoot.equals(backupRoot);
            backupRoot = customRoot;
        }
        FileBackupTarget backupTarget = new FileBackupTarget(backupRoot, create, isCustom);
        return backupTarget;
    }

    public File getRestoreCacheDir()
        throws ServiceException
    {
        String lcVal = BackupLC.backup_restore_cache_dir.value();
        if(!StringUtil.isNullOrEmpty(lcVal))
            return new File(lcVal);
        Server server = Provisioning.getInstance().getLocalServer();
        String defaultPath = server.getAttr("zimbraBackupTarget", null);
        File backupRoot;
        if(defaultPath != null)
            backupRoot = new File(defaultPath);
        else
            backupRoot = new File(LC.zimbra_home.value(), "backup");
        return new File(backupRoot, (new StringBuilder()).append("tmp").append(File.separator).append("restore").append(File.separator).append("shared_blobs").toString());
    }

    private static BackupManager mManager;
    public static final String MAGIC = "ZM_BACKUP";
    public static final int VERSION_MAJOR = 7;
    public static final int VERSION_MINOR = 1;
    private static final String BACKUP_MODE_STANDARD = "Standard";
    private static final String BACKUP_MODE_AUTO_GROUPED = "Auto-Grouped";
    private static final String FRIENDLY_TIMESTAMP_FORMAT = "EEE, yyyy/MM/dd HH:mm:ss.SSS z";
    private static final String LABEL_TIMESTAMP_FORMAT = "yyyyMMdd.HHmmss.SSS";
    public static final String LABEL_PREFIX_FULL = "full-";
    public static final String LABEL_PREFIX_INCREMENTAL = "incr-";
    public static final String LABEL_PREFIX_MBOXMOVE = "move-";
    private final InProgress mOpInProgress = new InProgress();
    private boolean mRunningRestore;
    private static Pattern sBadTargetRegex = Pattern.compile("\\s");
    static final boolean $assertionsDisabled = !com/zimbra/cs/backup/BackupManager.desiredAssertionStatus();


}
