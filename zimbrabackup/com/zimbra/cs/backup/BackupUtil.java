// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupUtil.java

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
import org.apache.commons.cli.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupRestoreFileCopierOptionsCLI

public class BackupUtil extends BackupRestoreFileCopierOptionsCLI
{

    public static void main(String args[])
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmbackup", BuildInfo.VERSION);
        BackupUtil util = null;
        try
        {
            util = new BackupUtil();
            CommandLine cl = util.getCommandLine(args);
            if(cl != null)
            {
                String methodOpts[] = {
                    "f", "i", "del"
                };
                int numMethodOpts = 0;
                for(int i = 0; i < methodOpts.length; i++)
                    if(cl.hasOption(methodOpts[i]))
                        numMethodOpts++;

                if(numMethodOpts != 1)
                    throw new ParseException("exactly one of -f, -i, and -del options should be specified");
                boolean hasZip = cl.hasOption("z");
                boolean hasNonZip = cl.hasOption("noZip");
                boolean hasZipStore = cl.hasOption("zipStore");
                if(hasNonZip && (hasZip || hasZipStore))
                    throw new ParseException("--noZip can't be used together with --zip or --zipStore");
                if(!hasNonZip && !hasZip && !hasZipStore)
                    hasZipStore = true;
                if(hasZipStore)
                    hasZip = true;
                boolean sync = cl.hasOption("sync");
                if(cl.hasOption("f"))
                {
                    util.auth();
                    if(sync)
                        util.setTransportTimeout(0);
                    util.fullBackup(cl.getOptionValues("a"), sync, hasZip, hasZipStore, getFileCopierOptions(cl), getSkipComponentsOptions(cl));
                } else
                if(cl.hasOption("i"))
                {
                    if(cl.hasOption("a"))
                        System.out.println("Ignoring -a option.  Incremental backups always include all accounts on the server.");
                    util.auth();
                    if(sync)
                        util.setTransportTimeout(0);
                    util.incBackup(getFileCopierOptions(cl), sync, hasZip, hasZipStore);
                } else
                if(cl.hasOption("del"))
                {
                    util.auth();
                    util.deleteBackups(cl.getOptionValue("del"));
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

    private BackupUtil()
        throws ServiceException
    {
        setupCommandLineOptions();
    }

    private void incBackup(FileCopierOptions fcOpts, boolean sync, boolean zip, boolean zipStore)
        throws SoapFaultException, IOException, ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.BACKUP_REQUEST);
        Element body = req.addElement("backup");
        body.addAttribute("method", "incremental");
        body.addAttribute("sync", sync);
        body.addAttribute("zip", zip);
        body.addAttribute("zipStore", zipStore);
        if(mTarget != null)
            body.addAttribute("target", mTarget);
        if(fcOpts != null)
            ToXML.encodeFileCopierOptions(body, fcOpts);
        Element resp = getTransport().invokeWithoutSession(req);
        Element respBody = resp.getElement("backup");
        String fullLabel = respBody.getAttribute("label", null);
        String incrLabel = respBody.getAttribute("incr-label", null);
        if(incrLabel != null)
            System.out.println(incrLabel);
        if(fullLabel != null)
            System.out.println(fullLabel);
    }

    private void fullBackup(String addrs[], boolean sync, boolean zip, boolean zipStore, FileCopierOptions fcOpts, BackupRestoreFileCopierOptionsCLI.SkipComponentsOptions skipOpts)
        throws SoapFaultException, IOException, ServiceException
    {
        String label = fullBackup(addrs, sync, zip, zipStore, fcOpts, skipOpts, mTarget, getTransport());
        if(label != null)
            System.out.println(label);
    }

    public static String fullBackup(String addrs[], boolean sync, boolean zip, boolean zipStore, FileCopierOptions fcOpts, BackupRestoreFileCopierOptionsCLI.SkipComponentsOptions skipOpts, String target, SoapTransport transport)
        throws SoapFaultException, IOException, ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.BACKUP_REQUEST);
        Element body = req.addElement("backup");
        body.addAttribute("method", "full");
        if(target != null)
            body.addAttribute("target", target);
        body.addAttribute("sync", sync);
        body.addAttribute("zip", zip);
        body.addAttribute("zipStore", zipStore);
        if(skipOpts != null)
        {
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
        }
        setAccountElem(body, addrs);
        if(fcOpts != null)
            ToXML.encodeFileCopierOptions(body, fcOpts);
        Element resp = transport.invokeWithoutSession(req);
        Element respBody = resp.getElement("backup");
        String label = respBody.getAttribute("label", null);
        return label;
    }

    private void deleteBackups(String val)
        throws IOException, ServiceException
    {
        deleteBackups(val, mTarget, getTransport());
    }

    public static void deleteBackups(String val, String target, SoapTransport transport)
        throws IOException, ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.BACKUP_REQUEST);
        Element body = req.addElement("backup");
        body.addAttribute("before", val);
        body.addAttribute("method", "delete");
        if(target != null)
            body.addAttribute("target", target);
        transport.invokeWithoutSession(req);
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        OptionGroup og = new OptionGroup();
        og.addOption(new Option("f", "fullBackup", false, "Starts full backup."));
        og.addOption(new Option("i", "incrementalBackup", false, "Starts incremental backup."));
        og.addOption(new Option("del", "delete", true, "Deletes backups including and prior to the specified label, date (YYYY/MM/DD[-hh:mm:ss]) or period (nn{d|m|y})."));
        options.addOptionGroup(og);
        options.addOption("sync", "sync", false, "Runs full backup synchronously.");
        options.addOption("z", "zip", false, "Backs up blobs in compressed zip files.  Ignored if --zipStore is specified.");
        options.addOption(null, "zipStore", false, "Backup up blobs in zip files without compression. (default)");
        options.addOption(null, "noZip", false, "Backs up blobs as individual files rather than in zip files.");
        Option accountOption = new Option("a", "account", true, "Account email addresses seperated by white space or \"all\" for backing up all accounts.  Required for full backup in standard mode only.  Do not specify for auto-grouped full backup or incremental backup.");
        accountOption.setArgs(-2);
        options.addOption(accountOption);
        options.addOption(null, "includeSearchIndex", false, "Include search index in full backup; if unspecified, use server config");
        options.addOption(null, "excludeSearchIndex", false, "Exclude search index from full backup; if unspecified, use server config");
        options.addOption(null, "includeBlobs", false, "Include blobs in full backup; if unspecified, use server config");
        options.addOption(null, "excludeBlobs", false, "Exclude blobs from full backup; if unspecified, use server config");
        options.addOption(null, "includeHsmBlobs", false, "Include blobs on HSM volumes in full backup; if unspecified, use server config");
        options.addOption(null, "excludeHsmBlobs", false, "Exclude blobs on HSM volumes from full backup; if unspecified, use server config");
    }

    protected String getCommandUsage()
    {
        return "zmbackup {-f | -i | -del} <options>";
    }

    protected String getTrailer()
    {
        return "Exactly one of -f, -i, or -del must be specified.";
    }

    protected static final String O_F = "f";
    protected static final String O_I = "i";
    protected static final String O_SYNC = "sync";
    protected static final String O_Z = "z";
    protected static final String O_ZIP = "zip";
    protected static final String O_ZIP_STORE = "zipStore";
    protected static final String O_NO_ZIP = "noZip";
    protected static final String O_DEL = "del";
}
