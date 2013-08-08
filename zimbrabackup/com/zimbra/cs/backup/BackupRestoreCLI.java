// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupRestoreCLI.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.util.SoapCLI;
import java.io.PrintStream;
import org.apache.commons.cli.*;

public abstract class BackupRestoreCLI extends SoapCLI
{

    protected BackupRestoreCLI()
        throws ServiceException
    {
    }

    protected CommandLine getCommandLine(String args[])
        throws ParseException
    {
        CommandLine cl = super.getCommandLine(args);
        if(cl != null)
        {
            if(cl.hasOption("t"))
                mTarget = cl.getOptionValue("t");
            mDebug = cl.hasOption("d");
        }
        return cl;
    }

    protected static void setAccountElem(Element elem, String addrs[])
    {
        if(addrs != null && addrs.length > 0)
        {
            for(int i = 0; i < addrs.length; i++)
            {
                Element acctElem = elem.addElement("account");
                acctElem.addAttribute("name", addrs[i]);
            }

        }
    }

    protected void setupCommandLineOptions()
    {
        super.setupCommandLineOptions();
        Options options = getOptions();
        options.addOption("t", "target", true, "Backup target location (default <zimbra_home>/backup).");
        options.addOption("d", "debug", false, "Display diagnostics for debugging purposes.");
    }

    protected static void error(BackupRestoreCLI util, Exception e)
    {
        System.err.println((new StringBuilder()).append("Error occurred: ").append(e.getMessage()).toString());
        if(util != null && util.mDebug)
            if(e instanceof SoapFaultException)
                System.err.println(((SoapFaultException)e).dump());
            else
                e.printStackTrace();
    }

    protected static final String O_A = "a";
    protected static final String O_T = "t";
    protected static final String O_LB = "lb";
    protected static final String O_D = "d";
    protected String mTarget;
    protected boolean mDebug;
}
