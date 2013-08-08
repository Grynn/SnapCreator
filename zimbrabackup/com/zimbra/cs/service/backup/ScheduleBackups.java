// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ScheduleBackups.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.List;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class ScheduleBackups extends NetworkDocumentHandler
{

    public ScheduleBackups()
    {
    }

    public Element handleNetworkRequest(Element request, Map context)
        throws ServiceException
    {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Element serverElem = request.getElement("server");
        String serverName = serverElem.getAttribute("name");
        Server server = prov.get(com.zimbra.cs.account.Provisioning.ServerBy.name, serverName);
        if(server == null)
        {
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("server with name ").append(serverName).append(" could not be found").toString(), null);
        } else
        {
            checkRight(zsc, context, server, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_backupAccount);
            RemoteManager rmgr = RemoteManager.getRemoteManager(server);
            rmgr.execute("zmschedulebackup");
            Element response = zsc.createElement(BackupService.SCHEDULE_BACKUPS_RESPONSE);
            return response;
        }
    }

    public void docRights(List relatedRights, List notes)
    {
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_backupAccount);
    }
}
