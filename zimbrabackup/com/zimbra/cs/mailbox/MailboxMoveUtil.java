// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MailboxMoveUtil.java

package com.zimbra.cs.mailbox;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.util.*;
import java.io.PrintStream;
import java.util.ArrayList;
import org.apache.commons.cli.*;

public class MailboxMoveUtil extends SoapCLI
{

    public static void main(String args[])
        throws Exception
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmmailboxmove", BuildInfo.VERSION);
        MailboxMoveUtil util = null;
        try
        {
            util = new MailboxMoveUtil();
            util.setupCommandLineOptions();
            CommandLine cl = util.getCommandLine(args);
            if(cl != null)
            {
                if(!cl.hasOption("force"))
                {
                    System.err.println("This command is deprecated.  Use zmmboxmove instead.  You can still use zmmailboxmove by adding --force option.");
                    System.exit(1);
                }
                ZAuthToken zat = getZAuthToken(cl);
                if(!cl.hasOption("a"))
                    throw new ParseException("Missing required option: a");
                if(!cl.hasOption("t") && !cl.hasOption("po"))
                {
                    String msg = String.format("-%s or -%s option must be specified", new Object[] {
                        "t", "po"
                    });
                    throw new ParseException(msg);
                }
                String email = cl.getOptionValue("a");
                if(cl.hasOption("t"))
                {
                    if(cl.hasOption("po"))
                    {
                        String msg = String.format("-%s option cannot be used when -%s option is used", new Object[] {
                            "po", "t"
                        });
                        throw new ParseException(msg);
                    }
                    boolean overwrite = cl.hasOption("ow");
                    String targetHost = cl.getOptionValue("t");
                    String tempDir = cl.getOptionValue("tempDir");
                    util.move(zat, email, targetHost, overwrite, tempDir);
                } else
                {
                    if(cl.hasOption("t") || cl.hasOption("ow"))
                    {
                        String msg = String.format("-%s or -%s option cannot be used when -%s option is used", new Object[] {
                            "t", "ow", "po"
                        });
                        throw new ParseException(msg);
                    }
                    util.purge(zat, email);
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
            System.err.println((new StringBuilder()).append("Error occurred: ").append(e.getMessage()).toString());
        }
        System.exit(1);
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        options.addOption(null, "force", false, "Required to use this deprecated command");
        options.addOption("a", "account", true, "Email address of account to move.");
        Option tHost = new Option("t", "targetServer", true, "Server where mailbox is moved to");
        options.addOption(tHost);
        options.addOption("ow", "overwriteMailbox", false, "Overwrite target mailbox if it exists");
        options.addOption("po", "purgeOld", false, "Purge old mailbox on previous server (for successfully moved mailboxes)");
        options.addOption(null, "tempDir", true, "temp directory on source server");
        options.addOption(SoapCLI.OPT_AUTHTOKEN);
        options.addOption(SoapCLI.OPT_AUTHTOKENFILE);
    }

    private void move(ZAuthToken zat, String emailAddress, String targetHost, boolean overwrite, String tempDir)
        throws Exception
    {
        auth(zat);
        Element reqDoc = createExportRequest(emailAddress, targetHost, overwrite, tempDir);
        getTransport().invokeWithoutSession(reqDoc);
        ArrayList accounts = new ArrayList();
        accounts.add(emailAddress);
        ProxyPurgeUtil.purgeAccounts(null, accounts, true, null);
    }

    private Element createExportRequest(String emailAddress, String targetHost, boolean overwrite, String tempDir)
    {
        Element doc = new com.zimbra.common.soap.Element.XMLElement(BackupService.EXPORTMAILBOX_REQUEST);
        Element body = doc.addUniqueElement("account");
        body.addAttribute("name", emailAddress);
        body.addAttribute("dest", targetHost);
        body.addAttribute("overwrite", overwrite);
        body.addAttribute("tempDir", tempDir);
        return doc;
    }

    private void purge(ZAuthToken zat, String emailAddress)
        throws Exception
    {
        auth(zat);
        Element reqDoc = createPurgeRequest(emailAddress);
        getTransport().invokeWithoutSession(reqDoc);
    }

    private Element createPurgeRequest(String emailAddress)
    {
        Element doc = new com.zimbra.common.soap.Element.XMLElement(BackupService.PURGE_MOVED_MAILBOX_REQUEST);
        Element body = doc.addUniqueElement("mbox");
        body.addAttribute("name", emailAddress);
        return doc;
    }

    private MailboxMoveUtil()
        throws ServiceException
    {
    }

    protected String getCommandUsage()
    {
        return String.format("zmmailboxmove -%s <email address> [ -%s <target server> OR -%s ]", new Object[] {
            "a", "t", "po"
        });
    }

    protected String getTrailer()
    {
        String msg = String.format("-%s and -%s options are mutually exclusive.\nUse -%s option to move a mailbox from the current server to the new server\nspecified by the option.  If -%s is specified, any existing mailbox for\nthe account on the target server is overwritten.\nAfter successfully moving the mailbox, use -%s option to purge the old\nmailbox on the original server.\n", new Object[] {
            "t", "po", "t", "ow", "po"
        });
        return msg;
    }

    protected static final String O_OW = "ow";
    protected static final String O_A = "a";
    protected static final String O_T = "t";
    protected static final String O_PO = "po";
    protected static final String O_TEMP_DIR = "tempDir";
    protected static final String O_FORCE = "force";
}
