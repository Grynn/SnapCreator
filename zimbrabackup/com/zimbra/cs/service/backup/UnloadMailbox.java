// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   UnloadMailbox.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class UnloadMailbox extends NetworkDocumentHandler
{

    public UnloadMailbox()
    {
    }

    public boolean domainAuthSufficient(Map context)
    {
        return true;
    }

    protected void checkRights(ZimbraSoapContext lc, Map context, Account account)
        throws ServiceException
    {
        Provisioning prov = Provisioning.getInstance();
        if(account.isCalendarResource())
        {
            com.zimbra.cs.account.CalendarResource cr = prov.get(com.zimbra.cs.account.Provisioning.CalendarResourceBy.id, account.getId());
            checkCalendarResourceRight(lc, cr, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox);
        } else
        {
            checkAccountRight(lc, account, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox);
        }
        com.zimbra.cs.account.Server localServer = prov.getLocalServer();
        checkRight(lc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
    }

    public Element handleNetworkRequest(Element request, Map context)
        throws ServiceException, SoapFaultException
    {
        ZimbraSoapContext lc;
        Account account;
        Mailbox mbox;
        com.zimbra.cs.mailbox.MailboxManager.MailboxLock maintenance;
        lc = NetworkDocumentHandler.getZimbraSoapContext(context);
        String email = request.getElement("account").getAttribute("name");
        Provisioning prov = Provisioning.getInstance();
        account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, email);
        if(account == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(email).append(" not found").toString(), null);
        prov.reload(account);
        checkRights(lc, context, account);
        mbox = MailboxManager.getInstance().getMailboxByAccountId(account.getId(), com.zimbra.cs.mailbox.MailboxManager.FetchMode.ONLY_IF_CACHED, true);
        if(mbox == null)
            break MISSING_BLOCK_LABEL_166;
        maintenance = null;
        maintenance = MailboxManager.getInstance().beginMaintenance(account.getId(), mbox.getId());
        if(maintenance != null)
            MailboxManager.getInstance().endMaintenance(maintenance, true, true);
        break MISSING_BLOCK_LABEL_166;
        Exception exception;
        exception;
        if(maintenance != null)
            MailboxManager.getInstance().endMaintenance(maintenance, true, true);
        throw exception;
        return lc.createElement(BackupService.UNLOAD_MAILBOX_RESPONSE);
    }
}
