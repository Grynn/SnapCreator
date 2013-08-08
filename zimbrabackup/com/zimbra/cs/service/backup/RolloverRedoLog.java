// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   RolloverRedoLog.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.List;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            Backup, BackupService

public class RolloverRedoLog extends Backup
{

    public RolloverRedoLog()
    {
    }

    public Element handleNetworkRequest(Element request, Map context)
        throws ServiceException
    {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        com.zimbra.cs.account.Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(lc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_rolloverRedoLog);
        RedoLogManager redoMgr = RedoLogProvider.getInstance().getRedoLogManager();
        redoMgr.forceRollover();
        Element response = lc.createElement(BackupService.ROLLOVER_REDOLOG_RESPONSE);
        return response;
    }

    public void docRights(List relatedRights, List notes)
    {
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_rolloverRedoLog);
    }
}
