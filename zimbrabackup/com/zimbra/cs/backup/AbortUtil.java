// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   AbortUtil.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.util.BuildInfo;
import java.io.IOException;
import org.apache.commons.cli.*;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupRestoreCLI

public class AbortUtil extends BackupRestoreCLI
{

    public static void main(String args[])
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmbackupabort", BuildInfo.VERSION);
        AbortUtil util = null;
        try
        {
            util = new AbortUtil();
            CommandLine cl = util.getCommandLine(args);
            if(cl != null)
            {
                boolean abortRestore = cl.hasOption(O_R);
                if(!abortRestore && !cl.hasOption("lb"))
                    throw new ParseException("backup set label is required");
                util.auth();
                util.abort(abortRestore, cl.getOptionValue("lb"));
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

    protected AbortUtil()
        throws ServiceException
    {
        setupCommandLineOptions();
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        options.addOption("lb", "label", true, "Label of full backup set to abort.");
        options.addOption(O_R, "restore", false, "Abort the restore in progress.");
    }

    private void abort(boolean abortRestore, String label)
        throws IOException, ServiceException
    {
        Element req = null;
        if(abortRestore)
        {
            req = new com.zimbra.common.soap.Element.XMLElement(BackupService.RESTORE_REQUEST);
            Element body = req.addElement("restore");
            body.addAttribute("method", "abort");
        } else
        {
            req = new com.zimbra.common.soap.Element.XMLElement(BackupService.BACKUP_REQUEST);
            Element body = req.addElement("backup");
            body.addAttribute("label", label);
            body.addAttribute("method", "abort");
            if(mTarget != null)
                body.addAttribute("target", mTarget);
        }
        getTransport().invokeWithoutSession(req);
    }

    protected String getCommandUsage()
    {
        return "zmbackupabort <options>";
    }

    protected static String O_R = "r";

}
