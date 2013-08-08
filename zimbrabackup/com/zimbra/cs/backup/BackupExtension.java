// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupExtension.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.extension.ExtensionDispatcherServlet;
import com.zimbra.cs.license.ZimbraNetworkExtension;
import com.zimbra.cs.service.backup.*;
import com.zimbra.qa.unittest.TestCreateMessage;
import com.zimbra.qa.unittest.ZimbraSuite;
import com.zimbra.soap.SoapServlet;

public class BackupExtension extends ZimbraNetworkExtension
{

    public BackupExtension()
    {
    }

    public void initNetworkExtension()
        throws ServiceException
    {
        SoapServlet.addService("AdminServlet", new BackupService());
        ExtensionDispatcherServlet.register(this, new MailboxExportServlet());
        ExtensionDispatcherServlet.register(this, new MailboxImportServlet());
        try
        {
            ZimbraSuite.addTest(com/zimbra/qa/unittest/TestCreateMessage);
        }
        catch(NoClassDefFoundError e)
        {
            ZimbraLog.test.debug("Unable to load ZimbraBackup unit tests.", e);
        }
    }

    public void destroy()
    {
        ExtensionDispatcherServlet.unregister(this);
    }

    public String getName()
    {
        return "backup";
    }

    public static final String EXTENSION_NAME_BACKUP = "backup";
}
