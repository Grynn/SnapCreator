// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MailboxExportServlet.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.backup.BackupParams;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.extension.ExtensionHttpHandler;
import com.zimbra.cs.extension.ZimbraExtension;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.servlet.ZimbraServlet;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// Referenced classes of package com.zimbra.cs.service.backup:
//            ExportMailbox

public class MailboxExportServlet extends ExtensionHttpHandler
{

    public MailboxExportServlet()
    {
    }

    public String getPath()
    {
        return (new StringBuilder()).append(super.getPath()).append("/").append("mboxexport").toString();
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

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        AuthToken authToken;
        String host;
        Map qparams;
        String accountEmail;
        Account account;
        authToken = ZimbraServlet.getAuthTokenFromCookie(req, resp);
        if(authToken == null || !authToken.isAdmin())
        {
            Log.mboxmove.warn("Auth failed");
            sendError(resp, 403, "Auth failed");
            return;
        }
        host = req.getServerName();
        qparams = HttpUtil.getURIParams(req);
        String urlEncodedAccountEmail = (String)qparams.get("account-name");
        accountEmail = URLDecoder.decode(urlEncodedAccountEmail, "UTF-8").toLowerCase();
        if(accountEmail == null)
        {
            sendError(resp, 400, (new StringBuilder()).append("Missing required parameter account-name, at target server ").append(host).toString());
            return;
        }
        Log.mboxmove.info((new StringBuilder()).append("Exporting mailbox for account ").append(accountEmail).toString());
        account = null;
        account = mProvisioning.get(com.zimbra.cs.account.Provisioning.AccountBy.name, accountEmail, authToken);
        if(account == null)
        {
            sendError(resp, 400, (new StringBuilder()).append("Account ").append(accountEmail).append(" not found on server ").append(host).toString());
            return;
        }
        try
        {
            mProvisioning.reload(account);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            long t0 = System.currentTimeMillis();
            String tempDirPath = (String)qparams.get("tempdir");
            File tempDir = ExportMailbox.getTempDir(tempDirPath);
            BackupParams bparams = new BackupParams();
            bparams.skipDb = parseBoolean((String)qparams.get("skip-db"), false);
            bparams.skipBlobs = parseBoolean((String)qparams.get("skip-blobs"), false);
            bparams.skipSecondaryBlobs = parseBoolean((String)qparams.get("skip-hsm-blobs"), false);
            bparams.skipSearchIndex = parseBoolean((String)qparams.get("skip-search-index"), false);
            bparams.blobsSyncToken = parseInt((String)qparams.get("blobs-sync-token"), 0);
            if(bparams.blobsSyncToken > 0)
            {
                int curr = mbox.getLastChangeID();
                int diff = curr - bparams.blobsSyncToken;
                if(diff > 10000)
                    throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Sync token is too old; requested=").append(bparams.blobsSyncToken).append(", current=").append(curr).append("; do full blobs export instead").toString(), null);
            }
            resp.setStatus(200);
            resp.setContentType("application/zip");
            ExportMailbox.export(mbox, resp.getOutputStream(), tempDir, bparams, false);
            Log.mboxmove.info((new StringBuilder()).append("Completed mailbox export for account ").append(accountEmail).append(" in ").append(System.currentTimeMillis() - t0).append(" millisec").toString());
        }
        catch(IOException e)
        {
            Log.mboxmove.warn("IO error", e);
            sendError(resp, 500, accountEmail, host, e.getMessage());
        }
        catch(ServiceException e)
        {
            Log.mboxmove.warn("Service error", e);
            sendError(resp, 500, accountEmail, host, e.getMessage());
        }
        catch(Exception e)
        {
            Log.mboxmove.warn("Unexpected error", e);
            sendError(resp, 500, accountEmail, host, e.getMessage());
        }
        return;
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

    private int parseInt(String val, int defaultValue)
        throws ServiceException
    {
        int num = defaultValue;
        if(!StringUtil.isNullOrEmpty(val))
            try
            {
                num = Integer.parseInt(val);
            }
            catch(NumberFormatException e)
            {
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Invalid number ").append(val).toString(), e);
            }
        return num;
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
        sendError(resp, sc, (new StringBuilder()).append("Error exporting mailbox for account ").append(accountId).append(" on server ").append(host).append(": ").append(msg).toString());
    }

    public static final String HANDLER_NAME_MBOXEXPORT = "mboxexport";
    public static final String PARAM_ACCT_NAME = "account-name";
    public static final String PARAM_TEMPDIR = "tempdir";
    public static final String PARAM_SKIP_DB = "skip-db";
    public static final String PARAM_SKIP_BLOBS = "skip-blobs";
    public static final String PARAM_SKIP_HSM_BLOBS = "skip-hsm-blobs";
    public static final String PARAM_SKIP_SEARCH_INDEX = "skip-search-index";
    public static final String PARAM_BLOBS_SYNC_TOKEN = "blobs-sync-token";
    private static final int MAX_SYNC_TOKEN_DIFF = 10000;
    private Provisioning mProvisioning;
}
