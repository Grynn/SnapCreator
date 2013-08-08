// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MboxMoveQueryUtil.java

package com.zimbra.cs.mailbox;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.service.backup.QueryMailboxMove;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.cli.*;

public class MboxMoveQueryUtil extends SoapCLI
{

    private MboxMoveQueryUtil()
        throws ServiceException
    {
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        options.addOption("a", "account", true, "Email address of account (all if omitted)");
    }

    protected String getCommandUsage()
    {
        return String.format("zmmboxmovequery [-%s <account email>] [ -%s <server to query>]", new Object[] {
            "a", "s"
        });
    }

    protected String getTrailer()
    {
        return "Show on-going mailbox moves on this server.";
    }

    public static void main(String args[])
        throws Exception
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmmboxmovequery", BuildInfo.VERSION);
        MboxMoveQueryUtil cli = null;
        try
        {
            cli = new MboxMoveQueryUtil();
            cli.setupCommandLineOptions();
            CommandLine cl = cli.getCommandLine(args);
            if(cl != null)
            {
                ZAuthToken zat = getZAuthToken(cl);
                String email = cl.getOptionValue("a");
                cli.query(zat, email);
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

    private void query(ZAuthToken zat, String emailAddress)
        throws Exception
    {
        auth(zat);
        Element request = new com.zimbra.common.soap.Element.XMLElement(BackupService.QUERY_MAILBOX_MOVE_REQUEST);
        request.addAttribute("checkPeer", true);
        if(emailAddress != null)
            request.addElement("account").addAttribute("name", emailAddress);
        Element response = getTransport().invokeWithoutSession(request);
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
        boolean first = true;
        for(Iterator iter = response.elementIterator("account"); iter.hasNext();)
        {
            if(!first)
                System.out.println();
            Element accountElem = (Element)iter.next();
            String name = accountElem.getAttribute("name", null);
            System.out.println((new StringBuilder()).append("Account: ").append(name).toString());
            boolean noPeer = accountElem.getAttributeBool("noPeer", false);
            com.zimbra.cs.service.backup.QueryMailboxMove.MoveType moveType = com.zimbra.cs.service.backup.QueryMailboxMove.MoveType.lookup(accountElem.getAttribute("type", null));
            if(com.zimbra.cs.service.backup.QueryMailboxMove.MoveType.out.equals(moveType))
            {
                System.out.println("Type: move-out");
                String dest = accountElem.getAttribute("dest", null);
                System.out.println((new StringBuilder()).append("To: ").append(dest).append(noPeer ? " (stranded)" : "").toString());
            } else
            {
                System.out.println("Type: move-in");
                String src = accountElem.getAttribute("src", null);
                System.out.println((new StringBuilder()).append("From: ").append(src).append(noPeer ? " (stranded)" : "").toString());
            }
            long startedAt = accountElem.getAttributeLong("start", 0L);
            if(startedAt > 0L)
            {
                String dateStr = fmt.format(new Date(startedAt));
                System.out.println((new StringBuilder()).append("Started: ").append(dateStr).toString());
            }
            first = false;
        }

        if(first)
            System.out.println("No mailbox move in progress");
    }

    protected static final String O_A = "a";
    private static String DATE_FORMAT = "EEE, yyyy/MM/dd HH:mm:ss.SSS z";

}
