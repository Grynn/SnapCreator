// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   RestoreOfflineUtil.java

package com.zimbra.cs.backup;

import com.zimbra.common.io.FileCopierOptions;
import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.util.Zimbra;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Enumeration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;

// Referenced classes of package com.zimbra.cs.backup:
//            RestoreUtil, RestoreParams, BackupManager, BackupTarget, 
//            BackupRestoreFileCopierOptionsCLI

public class RestoreOfflineUtil extends RestoreUtil
{

    public static void main(String args[])
    {
        int exitCode;
        RestoreOfflineUtil util;
        exitCode = 1;
        util = null;
        setup();
        util = new RestoreOfflineUtil();
        CommandLine cl = util.getCommandLine(args);
        if(cl != null)
        {
            util.restore(cl.getOptionValue("lb"), cl.getOptionValues("a"), cl.getOptionValue("pre"), cl.hasOption("rf"), cl.hasOption("ra"), cl.hasOption("ca"), cl.hasOption("sys"), cl.hasOption("br"), cl.hasOption("c"), cl.hasOption("skipDeletes"), getFileCopierOptions(cl), getSkipComponentsOptions(cl));
            exitCode = 0;
        }
        ParseException e;
        try
        {
            teardown();
        }
        // Misplaced declaration of an exception variable
        catch(ParseException e)
        {
            Log.backup.warn("Error during shutdown", e);
        }
        break MISSING_BLOCK_LABEL_200;
        e;
        util.usage(e);
        try
        {
            teardown();
        }
        // Misplaced declaration of an exception variable
        catch(ParseException e)
        {
            Log.backup.warn("Error during shutdown", e);
        }
        break MISSING_BLOCK_LABEL_200;
        e;
        error(util, e);
        Log.backup.warn("Error during restore", e);
        try
        {
            teardown();
        }
        // Misplaced declaration of an exception variable
        catch(ParseException e)
        {
            Log.backup.warn("Error during shutdown", e);
        }
        break MISSING_BLOCK_LABEL_200;
        Exception exception;
        exception;
        try
        {
            teardown();
        }
        catch(ServiceException e)
        {
            Log.backup.warn("Error during shutdown", e);
        }
        throw exception;
        System.exit(exitCode);
        return;
    }

    protected RestoreOfflineUtil()
        throws ServiceException
    {
    }

    protected CommandLine getCommandLine(String args[])
        throws ParseException
    {
        CommandLine cl = super.getCommandLine(args);
        if(cl != null)
        {
            String server = cl.getOptionValue("s");
            if(server != null && !"localhost".equals(server))
                throw new ParseException("-s must be localhost for offline restore");
        }
        return cl;
    }

    private static void setup()
        throws ServiceException
    {
        ZimbraLog.toolSetupLog4j("INFO", LC.zimbra_log4j_properties.value());
        Logger rootLogger = Logger.getRootLogger();
        Appender consoleAppender = null;
        Enumeration appenders = rootLogger.getAllAppenders();
        do
        {
            if(!appenders.hasMoreElements())
                break;
            Appender appender = (Appender)appenders.nextElement();
            if(appender instanceof ConsoleAppender)
                consoleAppender = appender;
        } while(true);
        if(consoleAppender != null)
            rootLogger.removeAppender(consoleAppender);
        DbPool.startup();
        Zimbra.startupCLI();
    }

    private static void teardown()
        throws ServiceException
    {
        Zimbra.shutdown();
    }

    protected void restore(String label, String addrs[], String prefix, boolean fullbackupOnly, boolean restoreAccount, boolean createAccount, boolean sys, 
            boolean backedupRedologsOnly, boolean contOnErr, boolean skipDeleteOps, FileCopierOptions fcOpts, BackupRestoreFileCopierOptionsCLI.SkipComponentsOptions skipOpts)
        throws IOException, ServiceException
    {
        BackupManager mgr = BackupManager.getInstance();
        BackupTarget bkupTarget = mgr.getBackupTarget(mTarget, false);
        RestoreParams params = new RestoreParams();
        params.offline = true;
        params.includeIncrementals = !fullbackupOnly;
        params.prefix = prefix;
        params.systemData = sys;
        params.replayCurrentRedologs = !backedupRedologsOnly;
        params.continueOnError = contOnErr;
        params.method = 0;
        if(restoreAccount)
            params.method = 1;
        else
        if(createAccount)
            params.method = 2;
        params.skipDeleteOps = skipDeleteOps;
        params.fcOpts = fcOpts;
        if(skipOpts.hasSkipSearchIndexOpt())
            params.skipSearchIndex = skipOpts.getSkipSearchIndex();
        if(skipOpts.hasSkipBlobsOpt())
            params.skipBlobs = skipOpts.getSkipBlobs();
        if(skipOpts.hasSkipHsmBlobsOpt())
            params.skipSecondaryBlobs = skipOpts.getSkipHsmBlobs();
        if(addrs == null)
            addrs = new String[0];
        if(addrs.length == 1 && "all".equals(addrs[0]))
        {
            params.skipDeletedAccounts = true;
            mgr.restore(bkupTarget, label, params);
        } else
        {
            java.util.List emailList = Arrays.asList(addrs);
            String acctIds[] = bkupTarget.getAccountIds(emailList, label, true);
            mgr.restore(acctIds, bkupTarget, label, params);
        }
        RestoreParams.Result result = params.getResult();
        if(result.isResetRedoSequence())
            System.out.println("Redo log sequence has been reset from the backup\n");
        displayFeedback(sys, result.isRebuiltSchema(), "ok", null);
    }

    protected String getCommandUsage()
    {
        return "zmrestoreoffline <options>";
    }

    protected String getTrailer()
    {
        return "The mail server must not be running before this command is run.\n-s, if specified, must be localhost.";
    }
}
