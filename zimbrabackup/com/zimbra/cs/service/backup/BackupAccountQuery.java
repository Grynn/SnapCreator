// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupAccountQuery.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.backup.*;
import com.zimbra.soap.ZimbraSoapContext;
import java.io.IOException;
import java.util.*;

// Referenced classes of package com.zimbra.cs.service.backup:
//            Backup, BackupService

public class BackupAccountQuery extends Backup
{

    public BackupAccountQuery()
    {
    }

    public Element handle(Element document, Map context)
        throws ServiceException
    {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        checkRights(lc, context);
        Element query = document.getElement("query");
        String bkupTarget = query.getAttribute("target", null);
        int type = BackupSet.parseTypeLabel(query.getAttribute("type", null));
        long fromTime = query.getAttributeLong("from", 0L);
        long toTime = query.getAttributeLong("to", 0xffffffffL);
        int backupListOffset = (int)query.getAttributeLong("backupListOffset", 0L);
        int backupListCount = (int)query.getAttributeLong("backupListCount", -1L);
        BackupTarget target = null;
        try
        {
            target = BackupManager.getInstance().getBackupTarget(bkupTarget, false);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Error accessing backup target: ").append(e.getMessage()).toString(), e);
        }
        List acctElems = query.listElements("account");
        if(acctElems.isEmpty())
            throw ServiceException.INVALID_REQUEST("Account names are required", null);
        List acctNames = super.parseAccountNames(acctElems);
        if(acctNames.size() == 1 && "all".equals(acctNames.get(0)))
            throw ServiceException.INVALID_REQUEST("Account names are required", null);
        Element response = lc.createElement(BackupService.BACKUP_ACCOUNT_QUERY_RESPONSE);
        Iterator i$ = acctNames.iterator();
label0:
        do
        {
            if(i$.hasNext())
            {
                String accountName = (String)i$.next();
                Element acct = response.addElement("account");
                acct.addAttribute("name", accountName);
                List sets = target.getBackupSets(accountName, fromTime, toTime);
                boolean allTypes = type == 0;
                if(backupListCount == -1)
                    backupListCount = sets.size();
                int numResults = 0;
                Iterator i$ = sets.iterator();
                do
                {
                    BackupSet set;
                    do
                    {
                        if(!i$.hasNext())
                            continue label0;
                        set = (BackupSet)i$.next();
                    } while(!allTypes && set.getType() != type);
                    if(numResults < backupListOffset)
                    {
                        numResults++;
                    } else
                    {
                        if(numResults >= backupListOffset + backupListCount)
                        {
                            acct.addAttribute("more", true);
                            continue label0;
                        }
                        numResults++;
                        Element b = acct.addElement("backup");
                        b.addAttribute("label", set.getLabel());
                        b.addAttribute("type", BackupSet.getTypeLabel(set.getType()));
                        b.addAttribute("start", set.getStartTime());
                        if(set.getEndTime() > 0L)
                            b.addAttribute("end", set.getEndTime());
                        String accountUid = set.getAccountId(accountName);
                        b.addAttribute("accountId", accountUid);
                    }
                } while(true);
            }
            return response;
        } while(true);
    }

    public void docRights(List relatedRights, List notes)
    {
        super.docRights(relatedRights, notes);
    }
}
