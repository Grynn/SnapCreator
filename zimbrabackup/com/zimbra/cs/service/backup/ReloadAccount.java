// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ReloadAccount.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class ReloadAccount extends NetworkDocumentHandler
{

    public ReloadAccount()
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
        String email = request.getElement("account").getAttribute("name");
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, email);
        if(account == null)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(email).append(" not found").toString(), null);
        } else
        {
            prov.reload(account);
            return lc.createElement(BackupService.RELOAD_ACCOUNT_RESPONSE);
        }
    }
}
