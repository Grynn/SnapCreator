// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   UnregisterMailboxMoveOut.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.backup.MailboxMoveTracker;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class UnregisterMailboxMoveOut extends NetworkDocumentHandler
{

    public UnregisterMailboxMoveOut()
    {
    }

    public boolean domainAuthSufficient(Map context)
    {
        return true;
    }

    protected void checkRights(ZimbraSoapContext zsc, Map context, Account account)
        throws ServiceException
    {
        Provisioning prov = Provisioning.getInstance();
        if(account.isCalendarResource())
        {
            com.zimbra.cs.account.CalendarResource cr = prov.get(com.zimbra.cs.account.Provisioning.CalendarResourceBy.id, account.getId());
            checkCalendarResourceRight(zsc, cr, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox);
        } else
        {
            checkAccountRight(zsc, account, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox);
        }
        com.zimbra.cs.account.Server localServer = prov.getLocalServer();
        checkRight(zsc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
    }

    public Element handleNetworkRequest(Element request, Map context)
        throws ServiceException, SoapFaultException
    {
        ZimbraSoapContext zsc = NetworkDocumentHandler.getZimbraSoapContext(context);
        Element accountElem = request.getElement("account");
        String email = accountElem.getAttribute("name");
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, email);
        if(account == null)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(email).append(" not found").toString(), null);
        } else
        {
            checkRights(zsc, context, account);
            String destHost = accountElem.getAttribute("dest");
            MailboxMoveTracker.getInstance().unregisterMoveOut(account, destHost);
            return zsc.createElement(BackupService.UNREGISTER_MAILBOX_MOVE_OUT_RESPONSE);
        }
    }
}
