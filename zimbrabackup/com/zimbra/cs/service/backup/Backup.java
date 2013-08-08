// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Backup.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.backup.BackupManager;
import com.zimbra.cs.backup.BackupParams;
import com.zimbra.cs.backup.BackupServiceException;
import com.zimbra.cs.backup.BackupSet;
import com.zimbra.cs.backup.BackupTarget;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.backup.util.Utils;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService, ParseXML

public class Backup extends NetworkDocumentHandler
{

    public Backup()
    {
    }

    public Element handleNetworkRequest(Element document, Map context)
        throws ServiceException
    {
        Element backup;
        String method;
        String bkupTarget;
        String label;
        Element response;
        Log.backup.info("Backup request started");
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        checkRights(lc, context);
        backup = document.getElement("backup");
        method = backup.getAttribute("method");
        bkupTarget = backup.getAttribute("target", null);
        label = backup.getAttribute("label", null);
        response = lc.createElement(BackupService.BACKUP_RESPONSE);
        BackupManager mgr;
        BackupTarget backupTarget;
        boolean sync;
        boolean sentReportEmail;
        ServiceException error;
        mgr = BackupManager.getInstance();
        if(bkupTarget != null && "incremental".equals(method))
            throw ServiceException.FAILURE("Custom backup target is not allowed for incremental backup", null);
        backupTarget = mgr.getBackupTarget(bkupTarget, true);
        if("abort".equals(method))
        {
            BackupSet bak = backupTarget.getBackupSet(label);
            bak.abortFullBackup();
            break MISSING_BLOCK_LABEL_852;
        }
        if("delete".equals(method))
        {
            String val = backup.getAttribute("before");
            long cutoffTime = getCutoffTime(val, backupTarget);
            mgr.deleteBackups(backupTarget, cutoffTime);
            break MISSING_BLOCK_LABEL_852;
        }
        sync = backup.getAttributeBool("sync", false);
        sentReportEmail = false;
        error = null;
        BackupParams params;
        List syncBackups;
        BackupSet fullBak;
        BackupSet incrBak;
        params = new BackupParams();
        params.sync = sync;
        parseComponentIncludeExcludeAttrs(backup, params);
        Element fcOptsElem = backup.getOptionalElement("fileCopier");
        if(fcOptsElem != null)
            params.fcOpts = ParseXML.parseFileCopierOptions(fcOptsElem);
        syncBackups = new ArrayList();
        fullBak = null;
        incrBak = null;
        int size;
        try
        {
            com.zimbra.cs.backup.BackupManager.BackupMode backupMode = mgr.getBackupMode();
            boolean autoGroupedMode = com.zimbra.cs.backup.BackupManager.BackupMode.AUTO_GROUPED.equals(backupMode);
            if("full".equals(method))
            {
                params.zip = backup.getAttributeBool("zip", true);
                params.zipStore = backup.getAttributeBool("zipStore", true);
                List acctElems = backup.listElements("account");
                if(acctElems.size() > 0)
                {
                    params.redologs = false;
                    List acctNames = parseAccountNames(acctElems);
                    boolean all = acctNames.size() == 1 && "all".equals(acctNames.get(0));
                    if(all)
                    {
                        if(!sync)
                            fullBak = mgr.startBackupFull(backupTarget, params);
                        else
                            fullBak = mgr.backupFull(backupTarget, params, syncBackups);
                    } else
                    {
                        com.zimbra.cs.account.Account accounts[] = mgr.lookupAccounts(acctNames, com.zimbra.cs.account.Provisioning.AccountBy.name, backupTarget);
                        if(!sync)
                            fullBak = mgr.startBackupFull(accounts, backupTarget, params);
                        else
                            fullBak = mgr.backupFull(accounts, backupTarget, params, syncBackups);
                    }
                } else
                if(autoGroupedMode)
                {
                    if(backupTarget.isCustom())
                        throw ServiceException.FAILURE("Custom backup target is not allowed for auto-grouped backup", null);
                    params.redologs = true;
                    com.zimbra.cs.account.Account accounts[] = mgr.lookupAccountsByOldestBackup(backupTarget);
                    if(accounts == null || accounts.length == 0)
                        throw BackupServiceException.AUTO_GROUPED_BACKUP_TOO_SOON();
                    if(!sync)
                        fullBak = mgr.startBackupFull(accounts, backupTarget, params);
                    else
                        fullBak = mgr.backupFull(accounts, backupTarget, params, syncBackups);
                } else
                {
                    throw ServiceException.INVALID_REQUEST("Missing account list", null);
                }
            } else
            if("incremental".equals(method))
            {
                params.zip = backup.getAttributeBool("zip", true);
                params.zipStore = backup.getAttributeBool("zipStore", true);
                BackupSet baks[] = mgr.backupIncremental(backupTarget, params, syncBackups);
                incrBak = baks[0];
                fullBak = baks[1];
            } else
            {
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Invalid backup method: ").append(method).toString(), null);
            }
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
        size = syncBackups.size();
        if(size > 0)
        {
            BackupSet baks[] = new BackupSet[size];
            syncBackups.toArray(baks);
            mgr.sendReportEmail(baks);
            sentReportEmail = true;
        }
        break MISSING_BLOCK_LABEL_756;
        Exception exception;
        exception;
        int size = syncBackups.size();
        if(size > 0)
        {
            BackupSet baks[] = new BackupSet[size];
            syncBackups.toArray(baks);
            mgr.sendReportEmail(baks);
            sentReportEmail = true;
        }
        throw exception;
        Element body = response.addElement("backup");
        if(fullBak != null)
            body.addAttribute("label", fullBak.getLabel());
        if(incrBak != null)
            body.addAttribute("incr-label", incrBak.getLabel());
        if(error != null && !sentReportEmail)
            mgr.sendErrorReportEmail(error);
        break MISSING_BLOCK_LABEL_852;
        ServiceException e;
        e;
        error = e;
        throw e;
        Exception exception1;
        exception1;
        if(error != null && !sentReportEmail)
            mgr.sendErrorReportEmail(error);
        throw exception1;
        Log.backup.info("Backup request finished");
        return response;
        IOException e;
        e;
        throw ServiceException.FAILURE(e.getMessage(), e);
    }

    private long getCutoffTime(String val, BackupTarget target)
        throws ServiceException, IOException
    {
        if(val.indexOf('/') != -1)
            try
            {
                return Utils.parseDate(val);
            }
            catch(ParseException e)
            {
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("invalid date: ").append(val).toString(), e);
            }
        String d = val.toLowerCase().trim();
        if(d.endsWith("d") || d.endsWith("m") || d.endsWith("y"))
        {
            int len = d.length();
            char unit = d.charAt(len - 1);
            int num = Integer.parseInt(d.substring(0, len - 1));
            if(num < 0)
                throw ServiceException.INVALID_REQUEST("invalid cutoff period: negative value means cutoff date in the future", null);
            Calendar today = Calendar.getInstance();
            switch(unit)
            {
            case 100: // 'd'
                today.add(5, -num);
                break;

            case 109: // 'm'
                today.add(2, -num);
                break;

            case 121: // 'y'
                today.add(1, -num);
                break;
            }
            return today.getTimeInMillis();
        } else
        {
            return BackupManager.getLabelDate(val);
        }
    }

    protected List parseAccountNames(List acctElems)
        throws ServiceException
    {
        List a = new ArrayList(acctElems.size());
        String name;
        for(Iterator i$ = acctElems.iterator(); i$.hasNext(); a.add(name.toLowerCase()))
        {
            Element elem = (Element)i$.next();
            name = elem.getAttribute("name");
            if("all".equals(name))
            {
                if(acctElems.size() != 1)
                    throw ServiceException.INVALID_REQUEST("\"all\" cannot be mixed with specific account names", null);
                continue;
            }
            String parts[] = name.split("@");
            if(parts.length != 2)
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("invalid account email address: ").append(name).toString(), null);
        }

        return a;
    }

    private static boolean parseIncludeExcludeAttr(String attrVal, boolean defaultVal)
        throws ServiceException
    {
        boolean skip;
        if(attrVal == null || attrVal.equalsIgnoreCase("config"))
            skip = defaultVal;
        else
        if(attrVal.equalsIgnoreCase("exclude"))
            skip = true;
        else
        if(attrVal.equalsIgnoreCase("include"))
            skip = false;
        else
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Invalid include/exclude value \"").append(attrVal).append("\"").toString(), null);
        return skip;
    }

    private static void parseComponentIncludeExcludeAttrs(Element backupElem, BackupParams params)
        throws ServiceException
    {
        Server server = Provisioning.getInstance().getLocalServer();
        boolean confSkipSearchIndex = server.isBackupSkipSearchIndex();
        boolean confSkipBlobs = server.isBackupSkipBlobs();
        boolean confSkipSecondaryBlobs = server.isBackupSkipHsmBlobs();
        String choice = backupElem.getAttribute("searchIndex", null);
        params.skipSearchIndex = parseIncludeExcludeAttr(choice, confSkipSearchIndex);
        choice = backupElem.getAttribute("blobs", null);
        params.skipBlobs = parseIncludeExcludeAttr(choice, confSkipBlobs);
        choice = backupElem.getAttribute("secondaryBlobs", null);
        params.skipSecondaryBlobs = parseIncludeExcludeAttr(choice, confSkipSecondaryBlobs);
    }

    protected void checkRights(ZimbraSoapContext lc, Map context)
        throws ServiceException
    {
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(lc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_backupAccount);
    }

    public void docRights(List relatedRights, List notes)
    {
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_backupAccount);
    }

    public static final String FULL_BACKUP = "full";
    public static final String INCREMENTAL_BACKUP = "incremental";
    public static final String ABORT_FULL_BACKUP = "abort";
    public static final String DELETE_BACKUP = "delete";
    public static final String ALL = "all";
}
