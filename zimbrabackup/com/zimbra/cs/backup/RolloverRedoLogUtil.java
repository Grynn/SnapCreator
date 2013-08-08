// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   RolloverRedoLogUtil.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapTransport;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.service.backup.BackupService;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.util.SoapCLI;
import java.io.IOException;
import org.apache.commons.cli.ParseException;

public class RolloverRedoLogUtil extends SoapCLI
{

    public static void main(String args[])
    {
        CliUtil.toolSetup();
        SoapTransport.setDefaultUserAgent("zmrolloverredolog", BuildInfo.VERSION);
        RolloverRedoLogUtil util = null;
        try
        {
            util = new RolloverRedoLogUtil();
            org.apache.commons.cli.CommandLine cl = util.getCommandLine(args);
            if(cl != null)
            {
                util.auth();
                util.rolloverRedoLog();
                System.exit(0);
            }
        }
        catch(ParseException e)
        {
            util.usage(e);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.exit(1);
    }

    private RolloverRedoLogUtil()
        throws ServiceException
    {
        setupCommandLineOptions();
    }

    private void rolloverRedoLog()
        throws ServiceException, IOException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.ROLLOVER_REDOLOG_REQUEST);
        getTransport().invokeWithoutSession(req);
    }

    protected String getCommandUsage()
    {
        return "zmrolloverredolog";
    }
}
