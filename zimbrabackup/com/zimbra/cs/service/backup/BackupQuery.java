// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupQuery.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.backup.*;
import com.zimbra.soap.ZimbraSoapContext;
import java.io.File;
import java.io.IOException;
import java.util.*;

// Referenced classes of package com.zimbra.cs.service.backup:
//            Backup, BackupService

public class BackupQuery extends Backup
{

    public BackupQuery()
    {
    }

    public Element handle(Element document, Map context)
        throws ServiceException
    {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        checkRights(lc, context);
        Element query = document.getElement("query");
        String bkupTarget = query.getAttribute("target", null);
        String label = query.getAttribute("label", null);
        int type;
        if(label != null)
            type = 0;
        else
            type = BackupSet.parseTypeLabel(query.getAttribute("type", null));
        long fromTime = query.getAttributeLong("from", 0L);
        long toTime = query.getAttributeLong("to", 0xffffffffL);
        boolean showStats = query.getAttributeBool("stats", false);
        int backupListOffset = (int)query.getAttributeLong("backupListOffset", 0L);
        int backupListCount = (int)query.getAttributeLong("backupListCount", -1L);
        String accountListStatusVal = query.getAttribute("accountListStatus", null);
        BackupService.AccountBackupStatus accountListStatus = BackupService.lookupAccountBackupStatus(accountListStatusVal);
        int accountListOffset = (int)query.getAttributeLong("accountListOffset", 0L);
        int accountListCount = (int)query.getAttributeLong("accountListCount", -1L);
        BackupTarget target = null;
        try
        {
            target = BackupManager.getInstance().getBackupTarget(bkupTarget, false);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Error accessing backup target: ").append(e.getMessage()).toString(), e);
        }
        BackupSet baks[] = null;
        if(label != null)
        {
            BackupSet bak = target.getBackupSet(label);
            baks = (new BackupSet[] {
                bak
            });
        } else
        {
            List bakList = target.getBackupSets(fromTime, toTime);
            baks = (BackupSet[])bakList.toArray(new BackupSet[0]);
        }
        Element response = lc.createElement(BackupService.BACKUP_QUERY_RESPONSE);
        File rootPath = new File(target.getURI());
        response.addAttribute("totalSpace", rootPath.getTotalSpace());
        response.addAttribute("freeSpace", rootPath.getUsableSpace());
        boolean allTypes = type == 0;
        if(backupListCount == -1)
            backupListCount = baks.length;
        int numResults = 0;
        for(int j = 0; j < baks.length;)
        {
            BackupSet bak = null;
            int k = j;
            do
            {
                if(k >= baks.length)
                    break;
                BackupSet b = baks[k];
                if(b != null && (allTypes || b.getType() == type))
                {
                    bak = b;
                    break;
                }
                k++;
                j++;
            } while(true);
            if(bak == null)
                break;
            if(numResults >= backupListOffset)
            {
                if(numResults >= backupListOffset + backupListCount)
                {
                    response.addAttribute("more", true);
                    break;
                }
                Element body = response.addElement("backup");
                body.addAttribute("label", bak.getLabel());
                body.addAttribute("type", BackupSet.getTypeLabel(bak.getType()));
                body.addAttribute("aborted", bak.isAborted());
                body.addAttribute("start", bak.getStartTime());
                if(bak.getEndTime() > 0L)
                    body.addAttribute("end", bak.getEndTime());
                body.addAttribute("minRedoSeq", bak.getMinRedoSequence());
                body.addAttribute("maxRedoSeq", bak.getMaxRedoSequence());
                boolean live = bak.isLive();
                body.addAttribute("live", live);
                int numCompleted = 0;
                if(live)
                {
                    com.zimbra.cs.backup.BackupSet.LiveBackupStatus liveBackupStatus = bak.getLiveBackupStatus();
                    if(liveBackupStatus != null)
                    {
                        SortedSet inprogress;
                        synchronized(liveBackupStatus)
                        {
                            numCompleted = liveBackupStatus.getNumCompletedAccts();
                            inprogress = liveBackupStatus.getInProgressAccts();
                        }
                        if(!inprogress.isEmpty())
                        {
                            Element currAccounts = body.addElement("currentAccounts");
                            currAccounts.addAttribute("total", inprogress.size());
                            String acctName;
                            Element acctElem;
                            for(Iterator i$ = inprogress.iterator(); i$.hasNext(); acctElem.addAttribute("name", acctName))
                            {
                                acctName = (String)i$.next();
                                acctElem = currAccounts.addElement("account");
                            }

                        }
                    }
                } else
                {
                    SortedMap completedAcctsMap = bak.getAccountStatusMap(BackupService.AccountBackupStatus.COMPLETED);
                    if(completedAcctsMap != null)
                        numCompleted = completedAcctsMap.size();
                }
                Element accountsElem = body.addElement("accounts");
                accountsElem.addAttribute("total", bak.getNumAccounts());
                if(numCompleted > 0)
                    accountsElem.addAttribute("completionCount", numCompleted);
                int numErrors = bak.getNumErrors();
                if(numErrors > 0)
                {
                    if(bak.isAborted())
                        numErrors--;
                    accountsElem.addAttribute("errorCount", numErrors);
                    List sysErrs = bak.getSystemErrors();
                    com.zimbra.cs.backup.BackupSet.ErrorInfo sysErr;
                    Element elem;
                    for(Iterator i$ = sysErrs.iterator(); i$.hasNext(); elem.addText(sysErr.getStacktrace()))
                    {
                        sysErr = (com.zimbra.cs.backup.BackupSet.ErrorInfo)i$.next();
                        elem = body.addElement("error");
                        elem.addAttribute("errorMessage", sysErr.getMessage());
                    }

                }
                if(!BackupService.AccountBackupStatus.NONE.equals(accountListStatus))
                {
                    SortedMap acctStatusMap = bak.getAccountStatusMap(accountListStatus);
                    if(acctStatusMap != null)
                    {
                        Map acctErrs = bak.getAccountErrors();
                        addAccountNames(accountsElem, live, acctStatusMap, acctErrs, accountListStatus, accountListOffset, accountListCount);
                    }
                }
                if(showStats)
                {
                    List stats = bak.getStats();
                    if(stats != null && !stats.isEmpty())
                    {
                        boolean hasStat = false;
                        Iterator i$ = stats.iterator();
                        do
                        {
                            if(!i$.hasNext())
                                break;
                            Counter counter = (Counter)i$.next();
                            long numSamples = counter.getNumSamples();
                            if(numSamples == 0L)
                                continue;
                            hasStat = true;
                            break;
                        } while(true);
                        if(hasStat)
                        {
                            Element statsElem = body.addElement("stats");
                            Iterator i$ = stats.iterator();
                            do
                            {
                                if(!i$.hasNext())
                                    break;
                                Counter counter = (Counter)i$.next();
                                long numSamples = counter.getNumSamples();
                                if(numSamples != 0L)
                                {
                                    Element counterElem = statsElem.addElement("counter");
                                    counterElem.addAttribute("name", counter.getName());
                                    String unit = counter.getUnit();
                                    if(unit != null)
                                        counterElem.addAttribute("unit", unit);
                                    counterElem.addAttribute("sum", counter.getSum());
                                    counterElem.addAttribute("numSamples", numSamples);
                                }
                            } while(true);
                        }
                    }
                }
            }
            j++;
            numResults++;
        }

        return response;
    }

    private static void addAccountNames(Element parent, boolean backupInProgress, SortedMap statusMap, Map errors, BackupService.AccountBackupStatus statusFilter, int offset, int count)
    {
        if(count == -1)
            count = statusMap.size();
        int numResults = 0;
        boolean wantAll = BackupService.AccountBackupStatus.ALL.equals(statusFilter);
        Iterator iter = statusMap.entrySet().iterator();
        do
        {
            if(!iter.hasNext())
                break;
            java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
            String email = (String)entry.getKey();
            BackupService.AccountBackupStatus status = (BackupService.AccountBackupStatus)entry.getValue();
            if(!wantAll && !statusFilter.equals(status))
                continue;
            if(numResults >= offset + count)
            {
                parent.addAttribute("more", true);
                break;
            }
            if(numResults >= offset)
            {
                Element elem = parent.addElement("account");
                elem.addAttribute("name", email);
                elem.addAttribute("status", status.toString());
                com.zimbra.cs.backup.BackupSet.ErrorInfo err = (com.zimbra.cs.backup.BackupSet.ErrorInfo)errors.get(email);
                if(err != null)
                {
                    elem.addAttribute("errorMessage", err.getMessage());
                    String trace = err.getStacktrace();
                    if(trace != null)
                        elem.addText(trace);
                }
            }
            numResults++;
        } while(true);
    }

    public void docRights(List relatedRights, List notes)
    {
        super.docRights(relatedRights, notes);
    }
}
