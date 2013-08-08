// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   GetMailboxVersion.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.*;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.db.Versions;
import com.zimbra.cs.mailbox.*;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.List;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class GetMailboxVersion extends NetworkDocumentHandler
{

    public GetMailboxVersion()
    {
    }

    public boolean domainAuthSufficient(Map context)
    {
        return true;
    }

    public Element handleNetworkRequest(Element request, Map context)
        throws ServiceException, SoapFaultException
    {
        ZimbraSoapContext zsc = NetworkDocumentHandler.getZimbraSoapContext(context);
        String email = request.getElement("account").getAttribute("name");
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String localServerHostname = localServer.getServiceHostname();
        Account account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, email);
        if(account == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(email).append(" not found").toString(), null);
        int mboxId = MailboxManager.getInstance().lookupMailboxId(account.getId());
        if(mboxId == -1)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(account.getName()).append(" does not have a mailbox on server ").append(localServerHostname).toString(), null);
        } else
        {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId, true);
            checkRights(zsc, context, account);
            Element response = zsc.createElement(BackupService.GET_MAILBOX_VERSION_RESPONSE);
            Element mboxElem = response.addElement("account");
            mboxElem.addAttribute("mbxid", mboxId);
            MailboxVersion mboxVer = mbox.getVersion();
            mboxElem.addAttribute("majorVer", mboxVer.getMajor());
            mboxElem.addAttribute("minorVer", mboxVer.getMinor());
            mboxElem.addAttribute("dbVer", Versions.getDbVersion());
            mboxElem.addAttribute("indexVer", Versions.getIndexVersion());
            return response;
        }
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
        Server localServer = prov.getLocalServer();
        checkRight(lc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
    }

    public void docRights(List relatedRights, List notes)
    {
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox);
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox);
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
        notes.add((new StringBuilder()).append("If the account is a calendar resource, need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox.getName()).append(" right on the calendar resource.").toString());
        notes.add((new StringBuilder()).append("If the account is a regular account, need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox.getName()).append(" right on the account.").toString());
        notes.add((new StringBuilder()).append("Need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer.getName()).append(" right on the source server").toString());
    }
}
