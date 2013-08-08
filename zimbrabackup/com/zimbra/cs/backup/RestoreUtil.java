// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   RestoreUtil.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.*;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.service.backup.ToXML;
import com.zimbra.cs.util.BuildInfo;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.cli.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupRestoreFileCopierOptionsCLI

public class RestoreUtil extends BackupRestoreFileCopierOptionsCLI
{

    public static void main(String args[])
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmrestore", BuildInfo.VERSION);
        RestoreUtil util = null;
        try
        {
            util = new RestoreUtil();
            CommandLine cl = util.getCommandLine(args);
            if(cl != null)
            {
                util.auth();
                util.restore(cl);
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

    protected RestoreUtil()
        throws ServiceException
    {
        setupCommandLineOptions();
    }

    protected CommandLine getCommandLine(String args[])
        throws ParseException
    {
        CommandLine cl = super.getCommandLine(args);
        if(cl != null)
        {
            boolean createAcct = cl.hasOption("ca");
            if(createAcct && cl.hasOption("ra"))
                throw new ParseException("Either -ca or -ra, not both, can be specified");
            String prefix = cl.getOptionValue("pre");
            if(createAcct && prefix == null)
                throw new ParseException("-pre option must be specified when using -ca option");
            if(!createAcct && prefix != null)
                throw new ParseException("-pre option must not be specified unless using -ca option");
            if(!cl.hasOption("sys") && !cl.hasOption("a"))
                throw new ParseException("-a and/or -sys must be specified");
        }
        return cl;
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        options.addOption("rf", "restoreFullBackupOnly", false, "Restores to last full backup only, which excludes incremental backups.");
        options.addOption("ra", "restoreAccount", false, "Restores the account in directory service.");
        options.addOption("lb", "label", true, "The label of the full backup to restore to.");
        options.addOption("pre", "prefix", true, "Prefix to prepend to original account names.");
        options.addOption("ca", "createAccount", false, "Restores accounts to new target accounts whose names are prepended with prefix.");
        options.addOption("c", "continueOnError", false, "Continue to restore other accounts when an error occurs.");
        options.addOption("sys", "systemData", false, "Restores global tables and local config.");
        options.addOption("br", "backedupRedologsOnly", false, "Replays the redo logs in backup only, which excludes archived and current redo logs of the system");
        options.addOption("skipDeletedAccounts", "skipDeletedAccounts", false, "Do not restore if named accounts were deleted or did not exist at backup time (this option is always enabled with \"-a all\")");
        Option accountOption = new Option("a", "account", true, "Account email addresses seperated by white space or \"all\" for restoring all accounts. Required.");
        accountOption.setArgs(-2);
        options.addOption(accountOption);
        options.addOption("restoreToTime", true, "Replay redo logs until this date/time");
        options.addOption("restoreToRedoSeq", true, "Replay up to and including this redo log sequence");
        options.addOption("restoreToIncrLabel", true, "Replay redo logs up to and including this incremental backup");
        options.addOption(null, "ignoreRedoErrors", false, "If true, ignore all errors during redo log replay");
        options.addOption(null, "skipDeletes", false, "If true, do not execute delete ops during redo log replay");
        options.addOption(null, "excludeSearchIndex", false, "Do not restore search index");
        options.addOption(null, "excludeBlobs", false, "Do not restore blobs (HSM or not)");
        options.addOption(null, "excludeHsmBlobs", false, "Do not restore HSM blobs");
    }

    private void restore(CommandLine args)
        throws SoapFaultException, IOException, ParseException, ServiceException
    {
        boolean sys = args.hasOption("sys");
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.RESTORE_REQUEST);
        Element body = req.addElement("restore");
        body.addAttribute("includeIncrementals", !args.hasOption("rf"));
        body.addAttribute("sysData", sys);
        body.addAttribute("replayRedo", !args.hasOption("br"));
        body.addAttribute("continue", args.hasOption("c"));
        body.addAttribute("skipDeletedAccounts", args.hasOption("skipDeletedAccounts"));
        body.addAttribute("ignoreRedoErrors", args.hasOption("ignoreRedoErrors"));
        body.addAttribute("skipDeleteOps", args.hasOption("skipDeletes"));
        String method = "mb";
        if(args.hasOption("ca"))
            method = "ca";
        else
        if(args.hasOption("ra"))
            method = "ra";
        body.addAttribute("method", method);
        if(args.hasOption("pre"))
            body.addAttribute("prefix", args.getOptionValue("pre"));
        if(args.hasOption("lb"))
            body.addAttribute("label", args.getOptionValue("lb"));
        if(mTarget != null)
            body.addAttribute("target", mTarget);
        if(args.hasOption("restoreToTime"))
        {
            String timeStr = args.getOptionValue("restoreToTime");
            Date restoreToTime = parseDatetime(timeStr);
            if(restoreToTime != null)
            {
                SimpleDateFormat f = new SimpleDateFormat(CANONICAL_DATETIME_FORMAT);
                String tstamp = f.format(restoreToTime);
                System.out.printf("using restore to time of %s\n", new Object[] {
                    tstamp
                });
                body.addAttribute("restoreToTime", restoreToTime.getTime());
            } else
            {
                System.err.printf("Invalid timestamp \"%s\" specified for -%s option\n", new Object[] {
                    timeStr, "restoreToTime"
                });
                System.err.println();
                System.err.print(getAllowedDatetimeFormatsHelp());
                System.exit(1);
            }
        }
        if(args.hasOption("restoreToRedoSeq"))
        {
            long seq = Long.parseLong(args.getOptionValue("restoreToRedoSeq"));
            body.addAttribute("restoreToRedoSeq", seq);
        }
        if(args.hasOption("restoreToIncrLabel"))
            body.addAttribute("restoreToIncrLabel", args.getOptionValue("restoreToIncrLabel"));
        setAccountElem(body, args.getOptionValues("a"));
        FileCopierOptions fcOpts = getFileCopierOptions(args);
        if(fcOpts != null)
            ToXML.encodeFileCopierOptions(body, fcOpts);
        BackupRestoreFileCopierOptionsCLI.SkipComponentsOptions skipOpts = getSkipComponentsOptions(args);
        if(skipOpts.hasSkipSearchIndexOpt())
        {
            String val = skipOpts.getSkipSearchIndex() ? "exclude" : "include";
            body.addAttribute("searchIndex", val);
        }
        if(skipOpts.hasSkipBlobsOpt())
        {
            String val = skipOpts.getSkipBlobs() ? "exclude" : "include";
            body.addAttribute("blobs", val);
        }
        if(skipOpts.hasSkipHsmBlobsOpt())
        {
            String val = skipOpts.getSkipHsmBlobs() ? "exclude" : "include";
            body.addAttribute("secondaryBlobs", val);
        }
        Element resp = getTransport().invokeWithoutSession(req);
        boolean rebuiltSchema = resp.getAttributeBool("rebuiltSchema");
        String status = resp.getAttribute("status");
        List failed = resp.listElements("account");
        displayFeedback(sys, rebuiltSchema, status, failed);
    }

    protected void displayFeedback(boolean sys, boolean rebuiltSchema, String status, List failed)
        throws ServiceException
    {
        if(rebuiltSchema || sys)
        {
            if(rebuiltSchema)
                System.out.println("Database zimbra schema has been recreated.");
            System.out.println("System tables and local configuration are restored.");
            System.out.println("Local config is restored to <zimbra_home>/conf/localconfig.xml.restored.");
            System.out.println("Make sure current volumes are correct using zmvolume command.\n");
        }
        if(!"ok".equals(status))
        {
            if("err".equals(status))
                System.out.println("Error occurred during restore. Check logs for more details.");
            else
            if("interrupted".equals(status))
                System.out.println("Restore was interrupted.");
            if(failed != null && !failed.isEmpty())
            {
                System.out.println("The following accounts have not been restored:");
                Element a;
                for(Iterator it = failed.listIterator(); it.hasNext(); System.out.println((new StringBuilder()).append("  ").append(a.getAttribute("name")).toString()))
                    a = (Element)it.next();

            }
        }
    }

    protected String getCommandUsage()
    {
        return "zmrestore <options>";
    }

    protected String getTrailer()
    {
        return getAllowedDatetimeFormatsHelp();
    }

    protected static final String O_CA = "ca";
    protected static final String O_RA = "ra";
    protected static final String O_RF = "rf";
    protected static final String O_PRE = "pre";
    protected static final String O_BR = "br";
    protected static final String O_SYS = "sys";
    protected static final String O_C = "c";
    protected static final String O_SKIP_DELETED_ACCOUNTS = "skipDeletedAccounts";
    protected static final String O_RESTORE_TO_TIME = "restoreToTime";
    protected static final String O_RESTORE_TO_REDO_SEQ = "restoreToRedoSeq";
    protected static final String O_RESTORE_TO_INCR_LABEL = "restoreToIncrLabel";
    protected static final String O_IGNORE_REDO_ERRORS = "ignoreRedoErrors";
    protected static final String O_SKIP_DELETE_OPS = "skipDeletes";
}
