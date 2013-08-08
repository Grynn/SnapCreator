// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MailboxMoveTracker.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import java.util.*;

// Referenced classes of package com.zimbra.cs.backup:
//            MailboxMoveServiceException

public class MailboxMoveTracker
{
    public static class MoveDetail
    {

        public String getAccountId()
        {
            return accountId;
        }

        public String getAccountName()
        {
            return accountName;
        }

        public String getPeerHost()
        {
            return peerHost;
        }

        public long getStartedAt()
        {
            return startedAt;
        }

        private String accountId;
        private String accountName;
        private String peerHost;
        private long startedAt;

        public MoveDetail(String accountId, String accountName, String peerHost, long startedAt)
        {
            this.accountId = accountId;
            this.accountName = accountName;
            this.peerHost = peerHost;
            this.startedAt = startedAt;
        }
    }


    public MailboxMoveTracker()
    {
        moveOuts = new HashMap();
        moveIns = new HashMap();
    }

    public static MailboxMoveTracker getInstance()
    {
        return sInstance;
    }

    public void registerMoveOut(Account account, String destHost)
        throws ServiceException
    {
        synchronized(moveOuts)
        {
            MoveDetail md = (MoveDetail)moveOuts.get(account.getId());
            if(md != null)
                throw MailboxMoveServiceException.ALREADY_BEING_MOVED_OUT(account.getName(), md.getPeerHost(), md.getStartedAt());
            moveOuts.put(account.getId(), new MoveDetail(account.getId(), account.getName(), destHost, System.currentTimeMillis()));
        }
    }

    public void registerMoveIn(Account account, String srcHost)
        throws ServiceException
    {
        synchronized(moveIns)
        {
            MoveDetail md = (MoveDetail)moveIns.get(account.getId());
            if(md != null)
                throw MailboxMoveServiceException.ALREADY_BEING_MOVED_IN(account.getName(), md.getPeerHost(), md.getStartedAt());
            moveIns.put(account.getId(), new MoveDetail(account.getId(), account.getName(), srcHost, System.currentTimeMillis()));
        }
    }

    public MoveDetail unregisterMoveOut(Account account, String destHost)
        throws ServiceException
    {
        Map map = moveOuts;
        JVM INSTR monitorenter ;
        MoveDetail md = (MoveDetail)moveOuts.get(account.getId());
        if(md != null && md.getPeerHost().equalsIgnoreCase(destHost))
            return (MoveDetail)moveOuts.remove(account.getId());
        map;
        JVM INSTR monitorexit ;
          goto _L1
        Exception exception;
        exception;
        throw exception;
_L1:
        throw MailboxMoveServiceException.NO_SUCH_MOVE_OUT(account.getName(), destHost);
    }

    public MoveDetail unregisterMoveIn(Account account, String srcHost)
        throws ServiceException
    {
        Map map = moveIns;
        JVM INSTR monitorenter ;
        MoveDetail md = (MoveDetail)moveIns.get(account.getId());
        if(md != null && md.getPeerHost().equalsIgnoreCase(srcHost))
            return (MoveDetail)moveIns.remove(account.getId());
        map;
        JVM INSTR monitorexit ;
          goto _L1
        Exception exception;
        exception;
        throw exception;
_L1:
        throw MailboxMoveServiceException.NO_SUCH_MOVE_IN(account.getName(), srcHost);
    }

    public MoveDetail getMoveOut(Account account)
    {
        Map map = moveOuts;
        JVM INSTR monitorenter ;
        return (MoveDetail)moveOuts.get(account.getId());
        Exception exception;
        exception;
        throw exception;
    }

    public MoveDetail getMoveIn(Account account)
    {
        Map map = moveIns;
        JVM INSTR monitorenter ;
        return (MoveDetail)moveIns.get(account.getId());
        Exception exception;
        exception;
        throw exception;
    }

    public List getAllMoveOuts()
    {
        Map map = moveOuts;
        JVM INSTR monitorenter ;
        return new ArrayList(moveOuts.values());
        Exception exception;
        exception;
        throw exception;
    }

    public List getAllMoveIns()
    {
        Map map = moveIns;
        JVM INSTR monitorenter ;
        return new ArrayList(moveIns.values());
        Exception exception;
        exception;
        throw exception;
    }

    private static MailboxMoveTracker sInstance = new MailboxMoveTracker();
    private Map moveOuts;
    private Map moveIns;

}
