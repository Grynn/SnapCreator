// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MboxMoveUtil.java

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

public class MboxMoveUtil extends SoapCLI
{

    public static void main(String args[])
        throws Exception
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmmboxmove", BuildInfo.VERSION);
        MboxMoveUtil cli = null;
        try
        {
            cli = new MboxMoveUtil();
            cli.setupCommandLineOptions();
            CommandLine cl = cli.getCommandLine(args);
            if(cl != null)
            {
                ZAuthToken zat = getZAuthToken(cl);
                String email = cl.getOptionValue("a");
                String srcHost = cl.getOptionValue("f");
                String destHost = cl.getOptionValue("t");
                cli.setServer(destHost);
                cli.move(zat, email, srcHost, destHost, cl.hasOption("sync"));
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

    private MboxMoveUtil()
        throws ServiceException
    {
        super(true);
    }

    protected String getCommandUsage()
    {
        return "zmmboxmove -a <email> --from <src> --to <dest> [--sync]";
    }

    protected String getTrailer()
    {
        return "Move the account's mailbox from source server to destination server.";
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        Option acct = new Option("a", "account", true, "Email address of account to move");
        acct.setRequired(true);
        options.addOption(acct);
        Option from = new Option("f", "from", true, "Current home server of the account");
        from.setRequired(true);
        options.addOption(from);
        Option to = new Option("t", "to", true, "Destination server");
        to.setRequired(true);
        options.addOption(to);
        options.addOption("sync", "sync", false, "Run synchronously");
    }

    private void move(ZAuthToken zat, String emailAddress, String srcHost, String destHost, boolean sync)
        throws Exception
    {
        auth(zat);
        Element request = new com.zimbra.common.soap.Element.XMLElement(BackupService.MOVE_MAILBOX_REQUEST);
        Element account = request.addUniqueElement("account");
        account.addAttribute("name", emailAddress);
        account.addAttribute("src", srcHost);
        account.addAttribute("dest", destHost);
        account.addAttribute("sync", sync);
        getTransport().invokeWithoutSession(request);
        if(!sync)
            System.out.println("Mailbox move started");
        else
            System.out.println("Mailbox move completed successfully");
    }

    protected static final String O_A = "a";
    protected static final String O_F = "f";
    protected static final String O_T = "t";
    protected static final String O_SYNC = "sync";
}
