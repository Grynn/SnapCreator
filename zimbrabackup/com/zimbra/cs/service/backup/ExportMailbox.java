// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ExportMailbox.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.zip.ZipOutputStream;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.backup.BackupManager;
import com.zimbra.cs.backup.BackupParams;
import com.zimbra.cs.backup.BackupSet;
import com.zimbra.cs.backup.MailboxMoveTracker;
import com.zimbra.cs.backup.ZipBackupTarget;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class ExportMailbox extends NetworkDocumentHandler
{

    public ExportMailbox()
    {
        mHost = LC.zimbra_server_hostname.value();
    }

    public Element handleNetworkRequest(Element document, Map context)
        throws ServiceException
    {
        ZimbraSoapContext lc;
        AuthToken authToken;
        Provisioning prov;
        Element exp;
        String accountEmail;
        Account account;
        Server targetServer;
        String targetUri;
        long t0;
        boolean moveStarted;
        String originalAccountStatus;
        File mboxExportFile;
        Exception err;
        lc = getZimbraSoapContext(context);
        authToken = lc.getAuthToken();
        com.zimbra.cs.backup.util.Log.mboxmove.debug((new StringBuilder()).append("auth token=").append(authToken).toString());
        prov = Provisioning.getInstance();
        exp = document.getElement("account");
        accountEmail = exp.getAttribute("name").toLowerCase();
        account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, accountEmail, authToken);
        if(account == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(accountEmail).append(" not found").toString(), null);
        prov.reload(account);
        String sourceHost = account.getAttr("zimbraMailHost", null);
        String targetHost = exp.getAttribute("dest");
        if(!Provisioning.onLocalServer(account))
            throw ServiceException.WRONG_HOST(sourceHost, null);
        if(sourceHost.equalsIgnoreCase(targetHost))
            throw ServiceException.FAILURE((new StringBuilder()).append("Target server is the same as the source mailbox server ").append(sourceHost).append(" for account ").append(accountEmail).toString(), null);
        targetServer = prov.get(com.zimbra.cs.account.Provisioning.ServerBy.name, targetHost);
        if(targetServer == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Target server ").append(targetHost).append(" not found").toString(), null);
        checkRights(lc, context, account, targetServer);
        int port = (int)exp.getAttributeLong("destPort", -1L);
        if(port == -1)
            port = targetServer.getIntAttr("zimbraAdminPort", -1);
        if(port == -1)
            throw ServiceException.FAILURE("unable to determine admin port", null);
        targetUri = null;
        try
        {
            targetUri = (new URL("https", targetHost, port, "/service/extension/backup/mboximport")).toExternalForm();
            com.zimbra.cs.backup.util.Log.mboxmove.debug((new StringBuilder()).append("moving mailbox to target: ").append(targetUri).toString());
        }
        catch(MalformedURLException e)
        {
            throw ServiceException.FAILURE("invalid URL", e);
        }
        t0 = System.currentTimeMillis();
        moveStarted = false;
        originalAccountStatus = null;
        mboxExportFile = null;
        err = null;
        Mailbox mailbox;
        HttpClient httpClient;
        PostMethod httpPost;
        FileInputStream fis;
        MailboxMoveTracker.getInstance().registerMoveOut(account, targetServer.getServiceHostname());
        moveStarted = true;
        mailbox = MailboxManager.getInstance().getMailboxByAccount(account, true);
        if(mailbox == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to create mailbox for account ").append(accountEmail).append(" on server ").append(mHost).append(" while preparing for mailbox move").toString(), null);
        originalAccountStatus = account.getAccountStatus(prov);
        if(originalAccountStatus == null)
            originalAccountStatus = "active";
        String tempDirPath = exp.getAttribute("tempDir", null);
        File tempDir = getTempDir(tempDirPath);
        mboxExportFile = File.createTempFile("zmmailboxmove-", ".zip", tempDir);
        export(mailbox, mboxExportFile, tempDir, true);
        com.zimbra.cs.backup.util.Log.mboxmove.debug((new StringBuilder()).append("exporting mailbox took ").append(System.currentTimeMillis() - t0).append(" millisec").toString());
        prov.reload(account);
        String newAccountStatus = account.getAccountStatus(prov);
        if(!"maintenance".equals(newAccountStatus))
            throw ServiceException.FAILURE("Account is not in maintenance mode after export", null);
        String urlEncodedAccountEmail = URLEncoder.encode(accountEmail, "UTF-8");
        URI u;
        try
        {
            u = new URI(targetUri);
        }
        catch(URISyntaxException e)
        {
            err = e;
            throw ServiceException.FAILURE((new StringBuilder()).append("Invalid target URL ").append(targetUri).toString(), e);
        }
        StringBuilder uri = new StringBuilder(targetUri);
        boolean overwrite = exp.getAttributeBool("overwrite", false);
        uri.append((new StringBuilder()).append("?account-name=").append(urlEncodedAccountEmail).append("&").append("account-status").append("=").append(originalAccountStatus).append("&").append("ow").append("=").append(overwrite).toString());
        httpClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        httpPost = new PostMethod(uri.toString());
        httpPost.getParams().setSoTimeout(0);
        authToken.encode(httpClient, httpPost, false, u.getHost());
        fis = null;
        fis = new FileInputStream(mboxExportFile);
        InputStreamRequestEntity isre = new InputStreamRequestEntity(fis, mboxExportFile.length(), "application/octet-stream");
        httpPost.setRequestEntity(isre);
        httpClient.executeMethod(httpPost);
        com.zimbra.cs.backup.util.Log.mboxmove.debug((new StringBuilder()).append("response status: (").append(httpPost.getStatusLine()).append(")").toString());
        if(httpPost.getStatusCode() != 200)
        {
            err = ServiceException.FAILURE((new StringBuilder()).append("Failed to send mailbox from source server ").append(mHost).append(": ").append(httpPost.getStatusText()).toString(), null);
            throw (ServiceException)err;
        }
        httpPost.releaseConnection();
        if(fis != null)
            fis.close();
        break MISSING_BLOCK_LABEL_938;
        Exception exception;
        exception;
        httpPost.releaseConnection();
        if(fis != null)
            fis.close();
        throw exception;
        String previousLocation = (new StringBuilder()).append(prov.getLocalServer().getId()).append(":").append(mailbox.getId()).toString();
        try
        {
            Map attrs = new HashMap(1);
            attrs.put("zimbraMailboxLocationBeforeMove", previousLocation);
            prov.modifyAttrs(account, attrs);
        }
        catch(ServiceException e)
        {
            com.zimbra.cs.backup.util.Log.mboxmove.warn((new StringBuilder()).append("Error while setting zimbraMailboxLocationBeforeMove attribute to ").append(previousLocation).append(" in account ").append(account.getName()).toString(), e);
        }
        com.zimbra.cs.mailbox.MailboxManager.MailboxLock mboxLock = MailboxManager.getInstance().beginMaintenance(account.getId(), mailbox.getId());
        if(mboxLock != null)
            MailboxManager.getInstance().endMaintenance(mboxLock, true, true);
        else
            com.zimbra.cs.backup.util.Log.mboxmove.warn((new StringBuilder()).append("Unable to put mailbox ").append(mailbox.getId()).append(" in maintenance mode after move").toString());
        IOException e;
        if(originalAccountStatus != null)
            try
            {
                com.zimbra.cs.backup.util.Log.mboxmove.debug((new StringBuilder()).append("setting account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).toString());
                prov.modifyAccountStatus(account, originalAccountStatus);
            }
            // Misplaced declaration of an exception variable
            catch(IOException e)
            {
                com.zimbra.cs.backup.util.Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox move").toString(), e);
            }
        if(moveStarted)
            MailboxMoveTracker.getInstance().unregisterMoveOut(account, targetServer.getServiceHostname());
        if(mboxExportFile != null)
            mboxExportFile.delete();
        break MISSING_BLOCK_LABEL_1293;
        Exception exception1;
        exception1;
        if(moveStarted)
            MailboxMoveTracker.getInstance().unregisterMoveOut(account, targetServer.getServiceHostname());
        if(mboxExportFile != null)
            mboxExportFile.delete();
        throw exception1;
        com.zimbra.cs.backup.util.Log.mboxmove.info((new StringBuilder()).append("Mailbox move: ").append(System.currentTimeMillis() - t0).append(" millisec").append(err != null ? " with error" : "").toString());
        break MISSING_BLOCK_LABEL_1607;
        e;
        err = e;
        throw ServiceException.FAILURE((new StringBuilder()).append("Failed to export mailbox from source server ").append(mHost).toString(), e);
        Exception exception2;
        exception2;
        if(originalAccountStatus != null)
            try
            {
                com.zimbra.cs.backup.util.Log.mboxmove.debug((new StringBuilder()).append("setting account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).toString());
                prov.modifyAccountStatus(account, originalAccountStatus);
            }
            catch(ServiceException e)
            {
                com.zimbra.cs.backup.util.Log.mboxmove.warn((new StringBuilder()).append("Unable to set account status back to \"").append(originalAccountStatus).append("\" for account ").append(account.getName()).append(" at the end of mailbox move").toString(), e);
            }
        if(moveStarted)
            MailboxMoveTracker.getInstance().unregisterMoveOut(account, targetServer.getServiceHostname());
        if(mboxExportFile != null)
            mboxExportFile.delete();
        break MISSING_BLOCK_LABEL_1554;
        Exception exception3;
        exception3;
        if(moveStarted)
            MailboxMoveTracker.getInstance().unregisterMoveOut(account, targetServer.getServiceHostname());
        if(mboxExportFile != null)
            mboxExportFile.delete();
        throw exception3;
        com.zimbra.cs.backup.util.Log.mboxmove.info((new StringBuilder()).append("Mailbox move: ").append(System.currentTimeMillis() - t0).append(" millisec").append(err != null ? " with error" : "").toString());
        throw exception2;
        return lc.createElement(BackupService.EXPORTMAILBOX_RESPONSE);
    }

    static void export(Mailbox mailbox, File exportFile, File tempDir, boolean lockAccount)
        throws IOException, ServiceException
    {
        BackupParams params = new BackupParams();
        Server server = Provisioning.getInstance().getLocalServer();
        params.skipDb = false;
        params.skipSearchIndex = server.isMailboxMoveSkipSearchIndex();
        params.skipBlobs = server.isMailboxMoveSkipBlobs();
        params.skipSecondaryBlobs = server.isMailboxMoveSkipHsmBlobs();
        export(mailbox, exportFile, tempDir, params, lockAccount);
    }

    static void export(Mailbox mailbox, File exportFile, File tempDir, BackupParams params, boolean lockAccount)
        throws IOException, ServiceException
    {
        FileOutputStream tempOut;
        com.zimbra.cs.backup.util.Log.mboxmove.debug((new StringBuilder()).append("exporting mailbox ").append(mailbox.getId()).append(" to temp file ").append(exportFile.getPath()).toString());
        tempOut = null;
        tempOut = new FileOutputStream(exportFile);
        export(mailbox, ((OutputStream) (tempOut)), tempDir, params, lockAccount);
        try
        {
            if(tempOut != null)
                tempOut.close();
        }
        catch(IOException e) { }
        break MISSING_BLOCK_LABEL_101;
        Exception exception;
        exception;
        try
        {
            if(tempOut != null)
                tempOut.close();
        }
        catch(IOException e) { }
        throw exception;
    }

    static void export(Mailbox mailbox, OutputStream os, File tempDir, BackupParams params, boolean lockAccount)
        throws IOException, ServiceException
    {
        ZipOutputStream zipOut = null;
        ZipBackupTarget target;
        zipOut = new ZipOutputStream(os, tempDir);
        target = new ZipBackupTarget(zipOut, tempDir, lockAccount);
        BackupSet bakSet = null;
        params.redologs = false;
        params.sync = true;
        params.zip = true;
        params.zipStore = true;
        bakSet = target.createFullBackupSet(BackupManager.getInstance().getLabel(3), "Mailbox export", new Account[] {
            mailbox.getAccount()
        }, params);
        bakSet.startFullBackup();
        if(bakSet != null)
            bakSet.endFullBackup();
        break MISSING_BLOCK_LABEL_144;
        Exception exception;
        exception;
        if(bakSet != null)
            bakSet.endFullBackup();
        throw exception;
        IOException e;
        e;
        if(FileUtil.isOutOfDiskError(e) || target.outOfSpace())
            throw target.makeOutOfSpaceException(e);
        else
            throw e;
        try
        {
            if(zipOut != null)
                zipOut.close();
        }
        catch(IOException e) { }
        break MISSING_BLOCK_LABEL_182;
        Exception exception1;
        exception1;
        try
        {
            if(zipOut != null)
                zipOut.close();
        }
        catch(IOException e) { }
        throw exception1;
    }

    static File getTempDir(String path)
        throws ServiceException, IOException
    {
        if(path == null)
            path = Provisioning.getInstance().getLocalServer().getMailboxMoveTempDir();
        File tempDir = new File(path);
        FileUtil.ensureDirExists(tempDir);
        return tempDir;
    }

    protected void checkRights(ZimbraSoapContext lc, Map context, Account account, Server targetServer)
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
        checkRight(lc, context, targetServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxToServer);
    }

    public void docRights(List relatedRights, List notes)
    {
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox);
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox);
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxToServer);
        notes.add((new StringBuilder()).append("If the account is a calendar resource, need  ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox.getName()).append(" right on the calendar resource.").toString());
        notes.add((new StringBuilder()).append("If the account is a regular account, need  ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox.getName()).append(" right on the account.").toString());
        notes.add((new StringBuilder()).append("Need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer.getName()).append(" right on the source server").toString());
        notes.add((new StringBuilder()).append("Need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxToServer.getName()).append(" right on the target server").toString());
    }

    private static final String MBOXIMPORT_URI = "/service/extension/backup/mboximport";
    private String mHost;
}
