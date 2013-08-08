// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PurgeMovedMailbox.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.im.IMPersona;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class PurgeMovedMailbox extends NetworkDocumentHandler
{

    public PurgeMovedMailbox()
    {
    }

    public boolean domainAuthSufficient(Map context)
    {
        return true;
    }

    public Element handleNetworkRequest(Element request, Map context)
        throws ServiceException, SoapFaultException
    {
        ZimbraSoapContext lc = NetworkDocumentHandler.getZimbraSoapContext(context);
        String email = request.getElement("mbox").getAttribute("name");
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String localServerHostname = localServer.getServiceHostname();
        Account account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, email);
        if(account == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(email).append(" not found").toString(), null);
        prov.reload(account);
        int mboxId = MailboxManager.getInstance().lookupMailboxId(account.getId());
        if(mboxId == -1)
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(account.getName()).append(" does not have an old mailbox to purge on server ").append(localServerHostname).toString(), null);
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId, true);
        if(Provisioning.onLocalServer(account))
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Mailbox ").append(mboxId).append(" on server ").append(localServerHostname).append(" is the live mailbox for account ").append(account.getName()).append(" and will not be purged; Use zmprov deleteAccount command instead").toString(), null);
        } else
        {
            checkRights(lc, context, account);
            Log.mboxmove.info((new StringBuilder()).append("Purging old mailbox ").append(mboxId).append(" of account ").append(account.getName()).toString());
            IMPersona.deleteIMPersona(account.getName());
            mbox.deleteMailbox();
            clearPrevLocation(account);
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(new String[] {
                "cmd", "PurgeMovedMailbox", "id", Integer.toString(mboxId)
            }));
            Element response = lc.createElement(BackupService.PURGE_MOVED_MAILBOX_RESPONSE);
            Element mboxElem = response.addElement("mbox");
            mboxElem.addAttribute("server", localServerHostname);
            mboxElem.addAttribute("mbxid", mboxId);
            return response;
        }
    }

    private void clearPrevLocation(Account account)
        throws ServiceException
    {
        String prevLoc = account.getMailboxLocationBeforeMove();
        if(prevLoc == null)
            return;
        boolean needToClear = false;
        Provisioning prov = Provisioning.getInstance();
        String parts[] = prevLoc.split(":", 2);
        if(parts != null && parts.length >= 2 && parts[0] != null && parts[0].length() > 0)
        {
            Server server = prov.get(com.zimbra.cs.account.Provisioning.ServerBy.id, parts[0]);
            if(server != null)
            {
                Server localServer = prov.getLocalServer();
                needToClear = server.getId().equalsIgnoreCase(localServer.getId());
            } else
            {
                Log.mboxmove.warn((new StringBuilder()).append("Server ").append(parts[0]).append(" not found").toString());
                needToClear = true;
            }
        } else
        {
            Log.mboxmove.warn((new StringBuilder()).append("Invalid previous location value: ").append(prevLoc).toString());
            needToClear = true;
        }
        if(needToClear)
            try
            {
                Map map = new HashMap(1);
                map.put("zimbraMailboxLocationBeforeMove", null);
                Provisioning.getInstance().modifyAttrs(account, map);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Error while clearing zimbraMailboxLocationBeforeMove attribute from account ").append(account.getName()).toString(), e);
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
