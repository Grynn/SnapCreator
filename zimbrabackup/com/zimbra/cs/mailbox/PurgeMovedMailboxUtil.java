// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PurgeMovedMailboxUtil.java

package com.zimbra.cs.mailbox;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import java.io.PrintStream;
import org.apache.commons.cli.*;

public class PurgeMovedMailboxUtil extends SoapCLI
{

    private PurgeMovedMailboxUtil()
        throws ServiceException
    {
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        Option acct = new Option("a", "account", true, "Email address of account to purge");
        acct.setRequired(true);
        options.addOption(acct);
    }

    protected String getCommandUsage()
    {
        return String.format("zmpurgeoldmbox -%s <account email>", new Object[] {
            "a"
        });
    }

    protected String getTrailer()
    {
        return "Purge the old mailbox for the account from this server.";
    }

    public static void main(String args[])
        throws Exception
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmpurgeoldmbox", BuildInfo.VERSION);
        PurgeMovedMailboxUtil cli = null;
        try
        {
            cli = new PurgeMovedMailboxUtil();
            cli.setupCommandLineOptions();
            CommandLine cl = cli.getCommandLine(args);
            if(cl != null)
            {
                ZAuthToken zat = getZAuthToken(cl);
                String email = cl.getOptionValue("a");
                cli.purge(zat, email);
                System.exit(0);
            }
        }
        catch(ParseException e)
        {
            cli.usage(e);
        }
        catch(Exception e)
        {
            System.err.println((new StringBuilder()).append("Error occurred: ").append(e.getMessage()).toString());
        }
        System.exit(1);
    }

    private void purge(ZAuthToken zat, String emailAddress)
        throws Exception
    {
        auth(zat);
        Element request = new com.zimbra.common.soap.Element.XMLElement(BackupService.PURGE_MOVED_MAILBOX_REQUEST);
        request.addUniqueElement("mbox").addAttribute("name", emailAddress);
        Element response = getTransport().invokeWithoutSession(request);
        Element mboxElem = response.getElement("mbox");
        String serverHost = mboxElem.getAttribute("server");
        int mboxId = (int)mboxElem.getAttributeLong("mbxid");
        System.out.println((new StringBuilder()).append("Purged mailbox id ").append(mboxId).append(" for account ").append(emailAddress).append(" on server ").append(serverHost).toString());
    }

    protected static final String O_A = "a";
}
