// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   QueryUtil.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.*;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.util.BuildInfo;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.cli.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupRestoreCLI, Counter

public class QueryUtil extends BackupRestoreCLI
{

    public static void main(String args[])
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmbackupquery", BuildInfo.VERSION);
        QueryUtil util = null;
        try
        {
            util = new QueryUtil();
            CommandLine cl = util.getCommandLine(args);
            if(cl != null)
            {
                util.auth();
                String type = cl.getOptionValue("type");
                String fromTstamp = cl.getOptionValue("from");
                String toTstamp = cl.getOptionValue("to");
                int backupListOffset = -1;
                int backupListCount = -1;
                if(cl.hasOption("listOffset"))
                    backupListOffset = Integer.parseInt(cl.getOptionValue("listOffset"));
                if(cl.hasOption("listCount"))
                    backupListCount = Integer.parseInt(cl.getOptionValue("listCount"));
                if(cl.hasOption("a"))
                {
                    util.queryAccount(cl.getOptionValues("a"), type, fromTstamp, toTstamp, backupListOffset, backupListCount);
                } else
                {
                    int accountListOffset = -1;
                    int accountListCount = -1;
                    if(cl.hasOption("accountListOffset"))
                        accountListOffset = Integer.parseInt(cl.getOptionValue("accountListOffset"));
                    if(cl.hasOption("accountListCount"))
                        accountListCount = Integer.parseInt(cl.getOptionValue("accountListCount"));
                    String accountListStatus = com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.NONE.toString();
                    if(cl.hasOption("accountListStatus"))
                        accountListStatus = cl.getOptionValue("accountListStatus");
                    else
                    if(cl.hasOption("v"))
                        accountListStatus = com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.ALL.toString();
                    util.query(type, cl.getOptionValue("lb"), fromTstamp, toTstamp, backupListOffset, backupListCount, accountListStatus, accountListOffset, accountListCount, cl.hasOption("stats"));
                }
                System.exit(0);
            }
        }
        catch(ParseException e)
        {
            util.usage(e);
        }
        catch(Exception e)
        {
            error(util, e);
        }
        System.exit(1);
    }

    protected QueryUtil()
        throws ServiceException
    {
        setupCommandLineOptions();
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        options.addOption("lb", "label", true, "The label of full backup to query.");
        options.addOption(null, "from", true, "List backups whose start date/time is at or after this date/time.");
        options.addOption(null, "to", true, "List backups whose start date/time is at or before this date/time.");
        options.addOption("v", "verbose", false, "Show account list in each backup.");
        options.addOption(null, "type", true, "Backup set type to query.  \"full\" or \"incremental\"; both if unspecified.");
        Option accountOption = new Option("a", "account", true, "Account email addresses seperated by white space or \"all\" for all accounts.");
        accountOption.setArgs(-2);
        options.addOption(accountOption);
        Options hiddenOptions = getHiddenOptions();
        hiddenOptions.addOption(null, "listOffset", true, "backup list offset; default 0");
        hiddenOptions.addOption(null, "listCount", true, "backup list count; default unlimited");
        hiddenOptions.addOption(null, "accountListStatus", true, "account list status filter; possible values are NONE (default), ALL, COMPLETED, ERROR, NOTSTARTED, and INPROGRESS");
        hiddenOptions.addOption(null, "accountListOffset", true, "account list offset; default 0");
        hiddenOptions.addOption(null, "accountListCount", true, "account list count; default unlimited");
        hiddenOptions.addOption(null, "stats", false, "Returns timing stats.  (not applicable to account query)");
    }

    protected String getCommandUsage()
    {
        return "zmbackupquery <options>";
    }

    protected String getTrailer()
    {
        return getAllowedDatetimeFormatsHelp();
    }

    private void query(String type, String label, String fromDate, String toDate, int backupListOffset, int backupListCount, String accountListStatus, 
            int accountListOffset, int accountListCount, boolean showStats)
        throws SoapFaultException, IOException, ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.BACKUP_QUERY_REQUEST);
        Element body = req.addElement("query");
        if(label != null)
            body.addAttribute("label", label);
        if(fromDate != null)
        {
            Date t = parseDatetime(fromDate);
            if(t == null)
            {
                System.err.printf("Invalid timestamp \"%s\" specified for --%s option\n", new Object[] {
                    fromDate, "from"
                });
                System.err.println();
                System.err.print(getAllowedDatetimeFormatsHelp());
                System.exit(1);
            }
            body.addAttribute("from", t.getTime());
        }
        if(toDate != null)
        {
            Date t = parseDatetime(toDate);
            if(t == null)
            {
                System.err.printf("Invalid timestamp \"%s\" specified for --%s option\n", new Object[] {
                    toDate, "to"
                });
                System.err.println();
                System.err.print(getAllowedDatetimeFormatsHelp());
                System.exit(1);
            }
            body.addAttribute("to", t.getTime());
        }
        body.addAttribute("stats", showStats);
        if(mTarget != null)
            body.addAttribute("target", mTarget);
        if("full".equalsIgnoreCase(type))
            body.addAttribute("type", "full");
        else
        if("incremental".equalsIgnoreCase(type))
            body.addAttribute("type", "incremental");
        if(backupListOffset >= 0)
            body.addAttribute("backupListOffset", backupListOffset);
        if(backupListCount > 0)
            body.addAttribute("backupListCount", backupListCount);
        if(accountListStatus != null)
            body.addAttribute("accountListStatus", accountListStatus);
        if(accountListOffset >= 0)
            body.addAttribute("accountListOffset", accountListOffset);
        if(accountListCount > 0)
            body.addAttribute("accountListCount", accountListCount);
        boolean empty = true;
        Element resp = getTransport().invokeWithoutSession(req);
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
        for(Iterator bit = resp.elementIterator("backup"); bit.hasNext(); System.out.println())
        {
            empty = false;
            Element backupElem = (Element)bit.next();
            System.out.println((new StringBuilder()).append("Label:   ").append(backupElem.getAttribute("label")).toString());
            System.out.println((new StringBuilder()).append("Type:    ").append(backupElem.getAttribute("type")).toString());
            boolean live = backupElem.getAttributeBool("live");
            long startTime = backupElem.getAttributeLong("start");
            long endTime = backupElem.getAttributeLong("end", 0L);
            boolean aborted = backupElem.getAttributeBool("aborted", false);
            long errorCount = 0L;
            Element accountsElem = backupElem.getOptionalElement("accounts");
            if(accountsElem != null)
                errorCount = accountsElem.getAttributeLong("errorCount", 0L);
            System.out.print("Status:  ");
            if(live)
                System.out.println("in progress");
            else
            if(endTime > 0L)
            {
                System.out.print("completed");
                if(aborted)
                    System.out.println(" (aborted by command)");
                else
                if(errorCount > 0L)
                    System.out.println(" (with errors)");
                else
                    System.out.println();
            } else
            {
                System.out.println("missing, incomplete, or invalid");
            }
            System.out.println((new StringBuilder()).append("Started: ").append(fmt.format(new Date(startTime))).toString());
            if(endTime > 0L)
                System.out.println((new StringBuilder()).append("Ended:   ").append(fmt.format(new Date(endTime))).toString());
            long minSeq = backupElem.getAttributeLong("minRedoSeq", -1L);
            long maxSeq = backupElem.getAttributeLong("maxRedoSeq", -1L);
            if(minSeq != -1L)
            {
                System.out.print((new StringBuilder()).append("Redo log sequence range: ").append(minSeq).append(" .. ").toString());
                if(maxSeq != -1L)
                    System.out.println(maxSeq);
                else
                    System.out.println("?");
            }
            if(live)
            {
                Element currentAccountsElem = backupElem.getOptionalElement("currentAccounts");
                if(currentAccountsElem != null)
                {
                    int num = 0;
                    StringBuilder sb = new StringBuilder();
                    for(Iterator ait = currentAccountsElem.elementIterator("account"); ait.hasNext();)
                    {
                        Element accountElem = (Element)ait.next();
                        if(num > 0)
                            sb.append(", ");
                        sb.append(accountElem.getAttribute("name"));
                        num++;
                    }

                    if(num > 0)
                        System.out.println((new StringBuilder()).append("Currently backing up: ").append(sb.toString()).toString());
                }
            }
            if(accountsElem != null)
            {
                long total = accountsElem.getAttributeLong("total", 0L);
                long completed = accountsElem.getAttributeLong("completionCount", 0L);
                System.out.println((new StringBuilder()).append("Number of accounts: ").append(completed).append(" out of ").append(total).append(" completed").toString());
                if(errorCount > 0L)
                    System.out.println((new StringBuilder()).append("Number of errors: ").append(errorCount).toString());
                boolean firstAccount = true;
                Iterator ait = accountsElem.elementIterator("account");
                do
                {
                    if(!ait.hasNext())
                        break;
                    Element accountElem = (Element)ait.next();
                    if(firstAccount)
                    {
                        System.out.println("Accounts:");
                        firstAccount = false;
                    }
                    String name = accountElem.getAttribute("name");
                    String statusStr = accountElem.getAttribute("status", null);
                    com.zimbra.cs.service.backup.BackupService.AccountBackupStatus status = com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.COMPLETED;
                    if(statusStr != null)
                        status = BackupService.lookupAccountBackupStatus(statusStr);
                    String statusLabel = getAccountBackupStatusLabel(status);
                    if(!com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.ERROR.equals(status))
                    {
                        System.out.println((new StringBuilder()).append("  ").append(name).append(": ").append(statusLabel).toString());
                    } else
                    {
                        String errorMsg = accountElem.getAttribute("errorMessage", null);
                        System.out.println((new StringBuilder()).append("  ").append(name).append(": ").append(statusLabel).append(" (").append(errorMsg).append(")").toString());
                        String stackTrace = accountElem.getText();
                        if(stackTrace != null && stackTrace.length() > 0)
                            System.out.println(stackTrace);
                    }
                } while(true);
                boolean more = accountsElem.getAttributeBool("more", false);
                if(more)
                    System.out.println("  (more accounts available)");
            }
            Iterator iter = backupElem.elementIterator("error");
            do
            {
                if(!iter.hasNext())
                    break;
                Element errorElem = (Element)iter.next();
                if(errorElem != null)
                {
                    String msg = errorElem.getAttribute("errorMessage", null);
                    System.out.println((new StringBuilder()).append("Error: ").append(msg).toString());
                    String stackTrace = errorElem.getText();
                    if(stackTrace != null && stackTrace.length() > 0)
                        System.out.println(stackTrace);
                }
            } while(true);
            if(!showStats)
                continue;
            Element stats = backupElem.getOptionalElement("stats");
            if(stats == null)
                continue;
            System.out.println("Stats:");
            Counter c;
            for(Iterator it = stats.elementIterator("counter"); it.hasNext(); System.out.println((new StringBuilder()).append("    ").append(c).toString()))
            {
                Element counter = (Element)it.next();
                String name = counter.getAttribute("name");
                String unit = counter.getAttribute("unit", null);
                long sum = counter.getAttributeLong("sum");
                long numSamples = counter.getAttributeLong("numSamples");
                c = new Counter(name, unit, sum, numSamples);
            }

        }

        boolean more = resp.getAttributeBool("more", false);
        if(more)
        {
            System.out.println("(more backups available)");
            System.out.println();
        }
        if(empty)
        {
            System.out.println("No backups found");
            System.out.println();
        }
        long totalSpace = resp.getAttributeLong("totalSpace", -1L);
        long freeSpace = resp.getAttributeLong("freeSpace", -1L);
        if(totalSpace > 0L)
        {
            System.out.println((new StringBuilder()).append("Total space: ").append(totalSpace / 1024L / 1024L).append("MB").toString());
            System.out.println((new StringBuilder()).append(" Free space: ").append(freeSpace / 1024L / 1024L).append("MB").toString());
        }
    }

    private void queryAccount(String accts[], String type, String fromDate, String toDate, int backupListOffset, int backupListCount)
        throws SoapFaultException, IOException, ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.BACKUP_ACCOUNT_QUERY_REQUEST);
        Element body = req.addElement("query");
        setAccountElem(body, accts);
        if(mTarget != null)
            body.addAttribute("target", mTarget);
        if("full".equalsIgnoreCase(type))
            body.addAttribute("type", "full");
        else
        if("incremental".equalsIgnoreCase(type))
            body.addAttribute("type", "incremental");
        if(fromDate != null)
        {
            Date t = parseDatetime(fromDate);
            if(t == null)
            {
                System.err.printf("Invalid timestamp \"%s\" specified for --%s option\n", new Object[] {
                    fromDate, "from"
                });
                System.err.println();
                System.err.print(getAllowedDatetimeFormatsHelp());
                System.exit(1);
            }
            body.addAttribute("from", t.getTime());
        }
        if(toDate != null)
        {
            Date t = parseDatetime(toDate);
            if(t == null)
            {
                System.err.printf("Invalid timestamp \"%s\" specified for --%s option\n", new Object[] {
                    toDate, "to"
                });
                System.err.println();
                System.err.print(getAllowedDatetimeFormatsHelp());
                System.exit(1);
            }
            body.addAttribute("to", t.getTime());
        }
        if(backupListOffset >= 0)
            body.addAttribute("backupListOffset", backupListOffset);
        if(backupListCount > 0)
            body.addAttribute("backupListCount", backupListCount);
        Element resp = getTransport().invokeWithoutSession(req);
        Iterator ait = resp.elementIterator("account");
        do
        {
            if(!ait.hasNext())
                break;
            Element acct = (Element)ait.next();
            String acctId = acct.getAttribute("name");
            System.out.println((new StringBuilder()).append("Account: ").append(acctId).toString());
            System.out.println();
            SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
            for(Iterator bit = acct.elementIterator("backup"); bit.hasNext(); System.out.println())
            {
                Element set = (Element)bit.next();
                System.out.println((new StringBuilder()).append("    Label:   ").append(set.getAttribute("label")).toString());
                System.out.println((new StringBuilder()).append("    Type:    ").append(set.getAttribute("type")).toString());
                long tm = set.getAttributeLong("start");
                System.out.println((new StringBuilder()).append("    Started: ").append(fmt.format(new Date(tm))).toString());
                tm = set.getAttributeLong("end");
                System.out.println((new StringBuilder()).append("    Ended:   ").append(fmt.format(new Date(tm))).toString());
                System.out.println((new StringBuilder()).append("    Acct ID: ").append(set.getAttribute("accountId")).toString());
            }

            boolean more = acct.getAttributeBool("more", false);
            if(more)
            {
                System.out.println("(more backups available)");
                System.out.println();
            }
        } while(true);
    }

    private static String getAccountBackupStatusLabel(com.zimbra.cs.service.backup.BackupService.AccountBackupStatus status)
    {
        static class _cls1
        {

            static final int $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus[];

            static 
            {
                $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus = new int[com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.values().length];
                try
                {
                    $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus[com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.COMPLETED.ordinal()] = 1;
                }
                catch(NoSuchFieldError ex) { }
                try
                {
                    $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus[com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.ERROR.ordinal()] = 2;
                }
                catch(NoSuchFieldError ex) { }
                try
                {
                    $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus[com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.INPROGRESS.ordinal()] = 3;
                }
                catch(NoSuchFieldError ex) { }
                try
                {
                    $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus[com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.NOTSTARTED.ordinal()] = 4;
                }
                catch(NoSuchFieldError ex) { }
                try
                {
                    $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus[com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.NONE.ordinal()] = 5;
                }
                catch(NoSuchFieldError ex) { }
                try
                {
                    $SwitchMap$com$zimbra$cs$service$backup$BackupService$AccountBackupStatus[com.zimbra.cs.service.backup.BackupService.AccountBackupStatus.ALL.ordinal()] = 6;
                }
                catch(NoSuchFieldError ex) { }
            }
        }

        switch(_cls1..SwitchMap.com.zimbra.cs.service.backup.BackupService.AccountBackupStatus[status.ordinal()])
        {
        case 1: // '\001'
            return "completed";

        case 2: // '\002'
            return "error";

        case 3: // '\003'
            return "in progress";

        case 4: // '\004'
            return "not started";

        case 5: // '\005'
            return "filter_none";

        case 6: // '\006'
            return "filter_all";
        }
        return "unknown";
    }

    protected static final String O_V = "v";
    protected static final String O_TYPE = "type";
    protected static final String O_FROM = "from";
    protected static final String O_TO = "to";
    protected static final String O_STATS = "stats";
    protected static final String O_BACKUP_LIST_OFFSET = "listOffset";
    protected static final String O_BACKUP_LIST_COUNT = "listCount";
    protected static final String O_ACCOUNT_LIST_STATUS = "accountListStatus";
    protected static final String O_ACCOUNT_LIST_OFFSET = "accountListOffset";
    protected static final String O_ACCOUNT_LIST_COUNT = "accountListCount";
    private static String DATE_FORMAT = "EEE, yyyy/MM/dd HH:mm:ss.SSS z";

}
