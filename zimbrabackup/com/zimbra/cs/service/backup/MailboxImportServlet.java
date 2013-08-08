// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MailboxImportServlet.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.backup.MailboxMoveTracker;
import com.zimbra.cs.backup.RestoreAccountSession;
import com.zimbra.cs.backup.RestoreParams;
import com.zimbra.cs.backup.ZipBackupTarget;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.extension.ExtensionHttpHandler;
import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.servlet.ZimbraServlet;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MailboxImportServlet extends ExtensionHttpHandler
{

    public MailboxImportServlet()
    {
    }

    public String getPath()
    {
        return (new StringBuilder()).append(super.getPath()).append("/").append("mboximport").toString();
    }

    public void init(ZimbraExtension ext)
        throws ServiceException
    {
        super.init(ext);
        Log.mboxmove.info((new StringBuilder()).append("Handler at ").append(getPath()).append(" starting up").toString());
        mProvisioning = Provisioning.getInstance();
    }

    public void destroy()
    {
        Log.mboxmove.info((new StringBuilder()).append("Handler at ").append(getPath()).append(" shutting down").toString());
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        AuthToken authToken;
        String host;
        Map qparams;
        String accountEmail;
        String originalAccountStatus;
        boolean overwrite;
        boolean switchOnly;
        boolean noSwitch;
        boolean append;
        Account account;
        authToken = ZimbraServlet.getAuthTokenFromCookie(req, resp);
        if(authToken == null || !authToken.isAdmin())
        {
            Log.mboxmove.warn("Auth failed");
            sendError(resp, 403, "Auth failed");
        }
        host = req.getServerName();
        qparams = HttpUtil.getURIParams(req);
        String urlEncodedAccountEmail = (String)qparams.get("account-name");
        accountEmail = URLDecoder.decode(urlEncodedAccountEmail, "UTF-8").toLowerCase();
        originalAccountStatus = (String)qparams.get("account-status");
        if(originalAccountStatus == null)
            originalAccountStatus = "active";
        String owStr = (String)qparams.get("ow");
        if(accountEmail == null)
        {
            sendError(resp, 400, (new StringBuilder()).append("Missing required parameter account-name, at target server ").append(host).toString());
            return;
        }
        if(owStr == null)
        {
            sendError(resp, 400, (new StringBuilder()).append("Missing required parameter ow, at target server ").append(host).toString());
            return;
        }
        overwrite = parseBoolean(owStr, false);
        Log.mboxmove.info((new StringBuilder()).append("Importing mailbox for account ").append(accountEmail).append(" overwrite=").append(overwrite).toString());
        switchOnly = parseBoolean((String)qparams.get("switch-only"), false);
        noSwitch = parseBoolean((String)qparams.get("no-switch"), false);
        append = parseBoolean((String)qparams.get("append"), false);
        if(switchOnly && noSwitch)
            sendError(resp, 400, "Conflicting parameters switch-only and no-switch");
        account = null;
        account = mProvisioning.get(com.zimbra.cs.account.Provisioning.AccountBy.name, accountEmail, authToken);
        if(account != null)
            break MISSING_BLOCK_LABEL_436;
        sendError(resp, 400, (new StringBuilder()).append("Account ").append(accountEmail).append(" not found on target server ").append(host).toString());
        ServiceException e;
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            // Misplaced declaration of an exception variable
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), e);
            }
        return;
        mProvisioning.reload(account);
        String status = account.getAccountStatus(mProvisioning);
        if(status.equals("maintenance") || noSwitch)
            break MISSING_BLOCK_LABEL_582;
        sendError(resp, 400, (new StringBuilder()).append("Account not in maintenance state (").append(status).append(") on target server ").append(host).toString());
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), e);
            }
        return;
        String oldMailHost;
        String srcHost;
        boolean moveInRegistered;
        oldMailHost = account.getAttr("zimbraMailHost");
        srcHost = account.getServer().getServiceHostname();
        moveInRegistered = false;
        boolean success;
        if(!noSwitch)
        {
            MailboxMoveTracker.getInstance().registerMoveIn(account, srcHost);
            moveInRegistered = true;
            String newMailHost = mProvisioning.getLocalServer().getAttr("zimbraServiceHostname");
            updateZimbraMailHost(account, newMailHost);
        }
        if(switchOnly)
            break MISSING_BLOCK_LABEL_1184;
        success = false;
        Mailbox mbox;
        mbox = MailboxManager.getInstance().getMailboxByAccountId(account.getId(), com.zimbra.cs.mailbox.MailboxManager.FetchMode.DO_NOT_AUTOCREATE, true);
        if(mbox == null)
            break MISSING_BLOCK_LABEL_911;
        if(append || overwrite)
            break MISSING_BLOCK_LABEL_901;
        sendError(resp, 400, (new StringBuilder()).append("Mailbox ").append(mbox.getId()).append(" already exists on target server ").append(host).append(" for account ").append(account.getName()).append("; consider specifying -ow option to overwrite the existing mailbox").toString());
        if(!success)
            try
            {
                if(!noSwitch)
                    updateZimbraMailHost(account, oldMailHost);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to rollback zimbraMailHost on account ").append(account.getName()).append(" to ").append(oldMailHost).append(" after failed mailbox move").toString(), e);
            }
        if(moveInRegistered)
            MailboxMoveTracker.getInstance().unregisterMoveIn(account, srcHost);
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), e);
            }
        return;
        int mailboxId;
        mailboxId = mbox.getId();
        break MISSING_BLOCK_LABEL_937;
        com.zimbra.cs.db.DbMailbox.MailboxIdentifier newId = RestoreAccountSession.getNextMailboxId(-1);
        RestoreAccountSession.createMailboxDatabase(newId.id, newId.groupId);
        mailboxId = newId.id;
        Log.mboxmove.info("Importing data for %s into mailbox id %d.", new Object[] {
            accountEmail, Integer.valueOf(mailboxId)
        });
        long t0 = System.currentTimeMillis();
        javax.servlet.ServletInputStream in = req.getInputStream();
        importFrom(in, account.getId(), mailboxId, qparams);
        Log.mboxmove.info((new StringBuilder()).append("Completed mailbox import for account ").append(accountEmail).append(" in ").append(System.currentTimeMillis() - t0).append(" millisec").toString());
        success = true;
        if(!success)
            try
            {
                if(!noSwitch)
                    updateZimbraMailHost(account, oldMailHost);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to rollback zimbraMailHost on account ").append(account.getName()).append(" to ").append(oldMailHost).append(" after failed mailbox move").toString(), e);
            }
        break MISSING_BLOCK_LABEL_1184;
        Exception exception;
        exception;
        if(!success)
            try
            {
                if(!noSwitch)
                    updateZimbraMailHost(account, oldMailHost);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to rollback zimbraMailHost on account ").append(account.getName()).append(" to ").append(oldMailHost).append(" after failed mailbox move").toString(), e);
            }
        throw exception;
        if(moveInRegistered)
            MailboxMoveTracker.getInstance().unregisterMoveIn(account, srcHost);
        break MISSING_BLOCK_LABEL_1224;
        Exception exception1;
        exception1;
        if(moveInRegistered)
            MailboxMoveTracker.getInstance().unregisterMoveIn(account, srcHost);
        throw exception1;
        resp.setStatus(200);
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            // Misplaced declaration of an exception variable
            catch(String status)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), status);
            }
        break MISSING_BLOCK_LABEL_1697;
        status;
        Log.mboxmove.warn("IO error", status);
        sendError(resp, 400, accountEmail, host, status.getMessage());
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            // Misplaced declaration of an exception variable
            catch(String status)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), status);
            }
        break MISSING_BLOCK_LABEL_1697;
        status;
        Log.mboxmove.warn("Service error", status);
        sendError(resp, 400, accountEmail, host, status.getMessage());
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            // Misplaced declaration of an exception variable
            catch(String status)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), status);
            }
        break MISSING_BLOCK_LABEL_1697;
        status;
        Log.mboxmove.warn("Unexpected error", status);
        sendError(resp, 500, accountEmail, host, status.getMessage());
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            // Misplaced declaration of an exception variable
            catch(String status)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), status);
            }
        break MISSING_BLOCK_LABEL_1697;
        Exception exception2;
        exception2;
        if(account != null && !noSwitch)
            try
            {
                mProvisioning.modifyAccountStatus(account, originalAccountStatus);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox import").toString(), e);
            }
        throw exception2;
    }

    private void updateZimbraMailHost(Account account, String host)
        throws ServiceException
    {
        Map attrs = new HashMap(1);
        attrs.put("zimbraMailHost", host);
        Provisioning.getInstance().modifyAttrs(account, attrs);
        Log.mboxmove.debug((new StringBuilder()).append("Updated mail host in LDAP to ").append(host).toString());
    }

    private void importFrom(InputStream in, String accountId, int targetMailboxId, Map queryParams)
        throws IOException, ServiceException
    {
        Log.mboxmove.debug("MailboxImportServlet.importFrom() started");
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipBackupTarget source = new ZipBackupTarget(zipIn, targetMailboxId);
        RestoreParams params = new RestoreParams();
        Server server = Provisioning.getInstance().getLocalServer();
        params.skipDb = parseBoolean((String)queryParams.get("skip-db"), false);
        params.skipSearchIndex = parseBoolean((String)queryParams.get("skip-search-index"), server.isMailboxMoveSkipSearchIndex());
        params.skipBlobs = parseBoolean((String)queryParams.get("skip-blobs"), server.isMailboxMoveSkipBlobs());
        params.skipSecondaryBlobs = parseBoolean((String)queryParams.get("skip-hsm-blobs"), server.isMailboxMoveSkipHsmBlobs());
        params.append = parseBoolean((String)queryParams.get("append"), false);
        source.restore(new String[] {
            accountId
        }, null, params);
    }

    private boolean parseBoolean(String val, boolean defaultValue)
    {
        if(val != null)
        {
            if(val.equals("1") || val.equalsIgnoreCase("TRUE"))
                return true;
            if(val.equals("0") || val.equalsIgnoreCase("FALSE"))
                return false;
        }
        return defaultValue;
    }

    private void sendError(HttpServletResponse resp, int sc, String msg)
        throws IOException
    {
        Log.mboxmove.error(msg);
        resp.sendError(sc, msg);
    }

    private void sendError(HttpServletResponse resp, int sc, String accountId, String host, String msg)
        throws IOException
    {
        sendError(resp, sc, (new StringBuilder()).append("Error importing mailbox for account ").append(accountId).append(" on server ").append(host).append(": ").append(msg).toString());
    }

    public static final String HANDLER_NAME_MBOXIMPORT = "mboximport";
    public static final String PARAM_ACCT_STATUS = "account-status";
    public static final String PARAM_OVERWRITE = "ow";
    public static final String PARAM_APPEND = "append";
    public static final String PARAM_SWITCH_ONLY = "switch-only";
    public static final String PARAM_NO_SWITCH = "no-switch";
    private Provisioning mProvisioning;
}
