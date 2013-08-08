// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Restore.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.backup.*;
import com.zimbra.soap.ZimbraSoapContext;
import java.io.IOException;
import java.util.*;

// Referenced classes of package com.zimbra.cs.service.backup:
//            Backup, BackupService, ParseXML

public class Restore extends Backup
{

    public Restore()
    {
    }

    public Element handle(Element request, Map context)
        throws ServiceException
    {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(lc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_restoreAccount);
        Element restore = request.getElement("restore");
        Element response = lc.createElement(BackupService.RESTORE_RESPONSE);
        String method = restore.getAttribute("method");
        if("abort".equals(method))
        {
            BackupManager.getInstance().markCurrentRestoreAsInterrupted();
            response.addAttribute("status", "aborting");
            return response;
        }
        RestoreParams params = new RestoreParams();
        parseComponentIncludeExcludeAttrs(restore, params);
        Element fcOptsElem = restore.getOptionalElement("fileCopier");
        if(fcOptsElem != null)
            params.fcOpts = ParseXML.parseFileCopierOptions(fcOptsElem);
        String bkupTarget = restore.getAttribute("target", null);
        String label = restore.getAttribute("label", null);
        List acctElems = restore.listElements("account");
        params.systemData = restore.getAttributeBool("sysData", false);
        if(acctElems.isEmpty() && !params.systemData)
            throw ServiceException.INVALID_REQUEST("Neither accounts or system data are specified for restore", null);
        if(!acctElems.isEmpty())
        {
            params.includeIncrementals = restore.getAttributeBool("includeIncrementals", true);
            params.replayCurrentRedologs = restore.getAttributeBool("replayRedo", true);
            params.continueOnError = restore.getAttributeBool("continue", false);
            String prefix = restore.getAttribute("prefix", null);
            if(prefix != null)
                prefix = prefix.trim();
            params.prefix = prefix;
            if("mb".equals(method))
                params.method = 0;
            else
            if("ra".equals(method))
                params.method = 1;
            else
            if("ca".equals(method))
            {
                if(params.prefix == null)
                    throw ServiceException.INVALID_REQUEST("Prefix is required for restoring to new target account", null);
                params.method = 2;
            } else
            {
                throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("invalid method in restore: ").append(method).toString(), null);
            }
            if(params.prefix != null && !"ca".equals(method))
                throw ServiceException.INVALID_REQUEST("Prefix must not be specified unless restoring to new target account", null);
        }
        List acctNameList = parseAccountNames(acctElems);
        boolean all = acctNameList.size() == 1 && "all".equals(acctNameList.get(0));
        params.restoreToTime = restore.getAttributeLong("restoreToTime", 0xffffffffL);
        if(params.restoreToTime < 0L)
            throw ServiceException.INVALID_REQUEST("restoreToTime cannot be negative", null);
        params.restoreToSequence = restore.getAttributeLong("restoreToRedoSeq", 0xffffffffL);
        if(params.restoreToSequence < 0L)
            throw ServiceException.INVALID_REQUEST("restoreToRedoSeq cannot be negative", null);
        params.restoreToIncrementalLabel = restore.getAttribute("restoreToIncrLabel", null);
        params.ignoreRedoErrors = restore.getAttributeBool("ignoreRedoErrors", false);
        params.skipDeleteOps = restore.getAttributeBool("skipDeleteOps", false);
        BackupManager mgr = BackupManager.getInstance();
        try
        {
            BackupTarget backupSrc = mgr.getBackupTarget(bkupTarget, false);
            if(all)
            {
                boolean skipDefault = true;
                params.skipDeletedAccounts = restore.getAttributeBool("skipDeletedAccounts", skipDefault);
                mgr.restore(backupSrc, label, params);
            } else
            {
                String acctIds[] = backupSrc.getAccountIds(acctNameList, label, true);
                params.skipDeletedAccounts = restore.getAttributeBool("skipDeletedAccounts", false);
                mgr.restore(acctIds, backupSrc, label, params);
            }
            com.zimbra.cs.backup.RestoreParams.Result result = params.getResult();
            response.addAttribute("rebuiltSchema", result.isRebuiltSchema());
            response.addAttribute("status", result.getStatus());
            SortedSet failed = result.getFailedAccounts();
            if(failed != null)
            {
                Element acctElem;
                for(Iterator it = failed.iterator(); it.hasNext(); acctElem.addAttribute("name", (String)it.next()))
                    acctElem = response.addElement("account");

            }
            return response;
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE(e.getMessage(), e);
        }
    }

    private static boolean parseIncludeExcludeAttr(String attrVal)
        throws ServiceException
    {
        boolean skip;
        if(attrVal.equalsIgnoreCase("exclude"))
            skip = true;
        else
        if(attrVal.equalsIgnoreCase("include"))
            skip = false;
        else
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Invalid include/exclude value \"").append(attrVal).append("\"").toString(), null);
        return skip;
    }

    private static void parseComponentIncludeExcludeAttrs(Element backupElem, RestoreParams params)
        throws ServiceException
    {
        String choice = backupElem.getAttribute("searchIndex", null);
        if(choice != null)
            params.skipSearchIndex = parseIncludeExcludeAttr(choice);
        choice = backupElem.getAttribute("blobs", null);
        if(choice != null)
            params.skipBlobs = parseIncludeExcludeAttr(choice);
        choice = backupElem.getAttribute("secondaryBlobs", null);
        if(choice != null)
            params.skipSecondaryBlobs = parseIncludeExcludeAttr(choice);
    }

    public void docRights(List relatedRights, List notes)
    {
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_restoreAccount);
    }

    public static final String RESTORE_CA = "ca";
    public static final String RESTORE_RA = "ra";
    public static final String RESTORE_MB = "mb";
    public static final String RESTORE_ABORT = "abort";
    public static final String STATUS_OK = "ok";
    public static final String STATUS_ERR = "err";
    public static final String STATUS_INTERRUPTED = "interrupted";
    public static final String STATUS_ABORTING = "aborting";
}
