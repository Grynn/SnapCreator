// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   QueryMailboxMove.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.backup.MailboxMoveTracker;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class QueryMailboxMove extends NetworkDocumentHandler
{
    private static class RemoteMoves
    {

        public String getHostname()
        {
            return hostname;
        }

        public void addMoveOut(String accountName)
        {
            moveOuts.add(accountName.toLowerCase());
        }

        public boolean isMovingOut(String accountName)
        {
            return moveOuts.contains(accountName.toLowerCase());
        }

        public void addMoveIn(String accountName)
        {
            moveIns.add(accountName.toLowerCase());
        }

        public boolean isMovingIn(String accountName)
        {
            return moveIns.contains(accountName.toLowerCase());
        }

        public void markUnavailable()
        {
            unavailable = true;
        }

        public boolean isUnavailable()
        {
            return unavailable;
        }

        private String hostname;
        private boolean unavailable;
        private Set moveIns;
        private Set moveOuts;

        public RemoteMoves(String hostname)
        {
            this.hostname = hostname;
            moveOuts = new HashSet();
            moveIns = new HashSet();
        }
    }

    private static class PeersData
    {

        private RemoteMoves lookupPeer(String peerHost)
        {
            RemoteMoves peerData = (RemoteMoves)peerServers.get(peerHost);
            if(peerData == null)
            {
                peerData = QueryMailboxMove.queryRemoteServer(zsc, peerHost, localHost);
                peerServers.put(peerHost, peerData);
            }
            return peerData;
        }

        public boolean isMovingIn(String accountName, String peerHost)
        {
            RemoteMoves rm = lookupPeer(peerHost);
            return rm.isMovingIn(accountName);
        }

        public boolean isMovingOut(String accountName, String peerHost)
        {
            RemoteMoves rm = lookupPeer(peerHost);
            return rm.isMovingOut(accountName);
        }

        public boolean isUnavailable(String peerHost)
        {
            RemoteMoves rm = lookupPeer(peerHost);
            return rm.isUnavailable();
        }

        private Map peerServers;
        private ZimbraSoapContext zsc;
        private String localHost;

        public PeersData(ZimbraSoapContext zsc, String localHost)
        {
            peerServers = new HashMap();
            this.zsc = zsc;
            this.localHost = localHost;
        }
    }

    public static final class MoveType extends Enum
    {

        public static MoveType[] values()
        {
            return (MoveType[])$VALUES.clone();
        }

        public static MoveType valueOf(String name)
        {
            return (MoveType)Enum.valueOf(com/zimbra/cs/service/backup/QueryMailboxMove$MoveType, name);
        }

        public static MoveType lookup(String val)
        {
            if(val != null)
                try
                {
                    return valueOf(val);
                }
                catch(IllegalArgumentException e) { }
            return null;
        }

        public static final MoveType out;
        public static final MoveType in;
        private static final MoveType $VALUES[];

        static 
        {
            out = new MoveType("out", 0);
            in = new MoveType("in", 1);
            $VALUES = (new MoveType[] {
                out, in
            });
        }

        private MoveType(String s, int i)
        {
            super(s, i);
        }
    }


    public QueryMailboxMove()
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
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String localHost = localServer.getServiceHostname();
        checkRight(zsc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
        boolean checkPeer = request.getAttributeBool("checkPeer", false);
        PeersData peersData = new PeersData(zsc, localHost);
        MailboxMoveTracker tracker = MailboxMoveTracker.getInstance();
        Element response = zsc.createElement(BackupService.QUERY_MAILBOX_MOVE_RESPONSE);
        List accounts = getRequestedAccounts(zsc, request);
        if(accounts.isEmpty())
        {
            List moveOuts = tracker.getAllMoveOuts();
            if(!moveOuts.isEmpty())
            {
                com.zimbra.cs.backup.MailboxMoveTracker.MoveDetail md;
                for(Iterator i$ = moveOuts.iterator(); i$.hasNext(); addMoveDetail(response, md, MoveType.out, localHost, checkPeer, peersData))
                    md = (com.zimbra.cs.backup.MailboxMoveTracker.MoveDetail)i$.next();

            }
            List moveIns = tracker.getAllMoveIns();
            if(!moveIns.isEmpty())
            {
                com.zimbra.cs.backup.MailboxMoveTracker.MoveDetail md;
                for(Iterator i$ = moveIns.iterator(); i$.hasNext(); addMoveDetail(response, md, MoveType.in, localHost, checkPeer, peersData))
                    md = (com.zimbra.cs.backup.MailboxMoveTracker.MoveDetail)i$.next();

            }
        } else
        {
            Iterator i$ = accounts.iterator();
            do
            {
                if(!i$.hasNext())
                    break;
                Account account = (Account)i$.next();
                com.zimbra.cs.backup.MailboxMoveTracker.MoveDetail mdOut = tracker.getMoveOut(account);
                if(mdOut != null)
                    addMoveDetail(response, mdOut, MoveType.out, localHost, checkPeer, peersData);
                com.zimbra.cs.backup.MailboxMoveTracker.MoveDetail mdIn = tracker.getMoveIn(account);
                if(mdIn != null)
                    addMoveDetail(response, mdIn, MoveType.in, localHost, checkPeer, peersData);
            } while(true);
        }
        return response;
    }

    private void addMoveDetail(Element parent, com.zimbra.cs.backup.MailboxMoveTracker.MoveDetail md, MoveType moveType, String localHost, boolean checkPeer, PeersData peersData)
        throws ServiceException
    {
        Element accountElem = parent.addElement("account");
        accountElem.addAttribute("name", md.getAccountName());
        accountElem.addAttribute("start", md.getStartedAt());
        accountElem.addAttribute("type", moveType.toString());
        if(MoveType.out.equals(moveType))
        {
            accountElem.addAttribute("src", localHost);
            accountElem.addAttribute("dest", md.getPeerHost());
        } else
        {
            accountElem.addAttribute("src", md.getPeerHost());
            accountElem.addAttribute("dest", localHost);
        }
        if(checkPeer)
        {
            String peerHost = md.getPeerHost();
            if(!peersData.isUnavailable(peerHost))
            {
                boolean live;
                if(MoveType.out.equals(moveType))
                    live = peersData.isMovingIn(md.getAccountName(), peerHost);
                else
                    live = peersData.isMovingOut(md.getAccountName(), peerHost);
                if(!live)
                    accountElem.addAttribute("noPeer", true);
            }
        }
    }

    protected void checkAccountRights(ZimbraSoapContext zsc, Provisioning prov, Account account)
        throws ServiceException
    {
        if(account.isCalendarResource())
        {
            com.zimbra.cs.account.CalendarResource cr = prov.get(com.zimbra.cs.account.Provisioning.CalendarResourceBy.id, account.getId());
            checkCalendarResourceRight(zsc, cr, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox);
        } else
        {
            checkAccountRight(zsc, account, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox);
        }
    }

    private List getRequestedAccounts(ZimbraSoapContext zsc, Element request)
        throws ServiceException
    {
        List list = new ArrayList();
        Provisioning prov = Provisioning.getInstance();
        Account account;
        for(Iterator accountIter = request.elementIterator("account"); accountIter.hasNext(); list.add(account))
        {
            String email = ((Element)accountIter.next()).getAttribute("name");
            account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, email);
            if(account == null)
                throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(email).append(" not found").toString(), null);
            checkAccountRights(zsc, prov, account);
        }

        return list;
    }

    private static RemoteMoves queryRemoteServer(ZimbraSoapContext zsc, String remoteHost, String localHost)
    {
        RemoteMoves remoteMoves = new RemoteMoves(remoteHost);
        Server remoteServer;
        try
        {
            remoteServer = Provisioning.getInstance().get(com.zimbra.cs.account.Provisioning.ServerBy.name, remoteHost);
        }
        catch(ServiceException e)
        {
            Log.mboxmove.warn((new StringBuilder()).append("Peer server ").append(remoteHost).append(" not found").toString());
            remoteMoves.markUnavailable();
            return remoteMoves;
        }
        SoapHttpTransport transport;
        try
        {
            String url = URLUtil.getAdminURL(remoteServer, "/service/admin/soap/", true);
            transport = new SoapHttpTransport(url);
            transport.setAuthToken(zsc.getRawAuthToken());
            transport.setTimeout(30000);
        }
        catch(ServiceException e)
        {
            Log.mboxmove.warn((new StringBuilder()).append("Error querying mailbox moves from remote server ").append(remoteHost).toString(), e);
            remoteMoves.markUnavailable();
            return remoteMoves;
        }
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.QUERY_MAILBOX_MOVE_REQUEST);
        req.addAttribute("checkPeer", false);
        Element response;
        try
        {
            response = transport.invokeWithoutSession(req);
        }
        catch(IOException e)
        {
            Log.mboxmove.warn((new StringBuilder()).append("Error querying mailbox moves from remote server ").append(remoteHost).toString(), e);
            remoteMoves.markUnavailable();
            return remoteMoves;
        }
        catch(ServiceException e)
        {
            Log.mboxmove.warn((new StringBuilder()).append("Error querying mailbox moves from remote server ").append(remoteHost).toString(), e);
            remoteMoves.markUnavailable();
            return remoteMoves;
        }
        Iterator iter = response.elementIterator("account");
        do
        {
            if(!iter.hasNext())
                break;
            Element accountElem = (Element)iter.next();
            String name = accountElem.getAttribute("name", null);
            MoveType moveType = MoveType.lookup(accountElem.getAttribute("type", null));
            if(name != null && moveType != null)
                if(MoveType.out.equals(moveType))
                {
                    String dest = accountElem.getAttribute("dest", null);
                    if(localHost.equalsIgnoreCase(dest))
                        remoteMoves.addMoveOut(name);
                } else
                {
                    String src = accountElem.getAttribute("src", null);
                    if(localHost.equalsIgnoreCase(src))
                        remoteMoves.addMoveIn(name);
                }
        } while(true);
        return remoteMoves;
    }

}
