// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MoveMailbox.java

package com.zimbra.cs.service.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights;
import com.zimbra.cs.backup.MailboxMoveServiceException;
import com.zimbra.cs.backup.MailboxMoveTracker;
import com.zimbra.cs.backup.RestoreParams;
import com.zimbra.cs.backup.ZipBackupTarget;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.Versions;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxVersion;
import com.zimbra.cs.rmgmt.RemoteManager;
import com.zimbra.cs.rmgmt.RemoteResult;
import com.zimbra.cs.service.NetworkDocumentHandler;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.util.ProxyPurgeUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.ZimbraSoapContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

// Referenced classes of package com.zimbra.cs.service.backup:
//            BackupService

public class MoveMailbox extends NetworkDocumentHandler
{
    private static class RsyncStats
    {

        public long getNumberOfFiles()
        {
            return numberOfFiles;
        }

        public long getNumberOfFilesTransferred()
        {
            return numberOfFilesTransferred;
        }

        public long getTotalFileSize()
        {
            return totalFileSize;
        }

        public long getTotalTransferredFileSize()
        {
            return totalTransferredFileSize;
        }

        public long getLiteralData()
        {
            return literalData;
        }

        public long getMatchedData()
        {
            return matchedData;
        }

        public long getFileListSize()
        {
            return fileListSize;
        }

        public long getFileListGenerationTime()
        {
            return fileListGenerationTime;
        }

        public long getFileListTransferTime()
        {
            return fileListTransferTime;
        }

        public long getTotalBytesSent()
        {
            return totalBytesSent;
        }

        public long getTotalBytesReceived()
        {
            return totalBytesReceived;
        }

        public long getTotalTime()
        {
            return totalTime;
        }

        public void add(RsyncStats other)
        {
            numberOfFiles += other.numberOfFiles;
            numberOfFilesTransferred += other.numberOfFilesTransferred;
            totalFileSize += other.totalFileSize;
            totalTransferredFileSize += other.totalTransferredFileSize;
            literalData += other.literalData;
            matchedData += other.matchedData;
            fileListSize += other.fileListSize;
            fileListGenerationTime += other.fileListGenerationTime;
            fileListTransferTime += other.fileListTransferTime;
            totalBytesSent += other.totalBytesSent;
            totalBytesReceived += other.totalBytesReceived;
            totalTime += other.totalTime;
        }

        private long parseNumber(String val)
        {
            String fields[] = val.split("\\s+", 2);
            if(fields != null)
                try
                {
                    return Long.parseLong(fields[0]);
                }
                catch(NumberFormatException e) { }
            return 0L;
        }

        private long parseSecondsAsMillis(String val)
        {
            String fields[] = val.split("\\s+", 2);
            if(fields != null)
            {
                double seconds;
                try
                {
                    seconds = Double.parseDouble(fields[0]);
                }
                catch(NumberFormatException e)
                {
                    seconds = 0.0D;
                }
                long millis = (long)(seconds * 1000D);
                return millis;
            } else
            {
                return 0L;
            }
        }

        private String toSecondsString(long millis)
        {
            double seconds = (double)millis / 1000D;
            return String.format("%.3f", new Object[] {
                Double.valueOf(seconds)
            });
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Number of files: ").append(numberOfFiles).append("\n");
            sb.append("Number of files transferred: ").append(numberOfFilesTransferred).append("\n");
            sb.append("Total file size: ").append(totalFileSize).append(" bytes\n");
            sb.append("Total transferred file size: ").append(totalTransferredFileSize).append(" bytes\n");
            sb.append("Literal data: ").append(literalData).append(" bytes\n");
            sb.append("Matched data: ").append(literalData).append(" bytes\n");
            sb.append("File list size: ").append(fileListSize).append("\n");
            sb.append("File list generation time: ").append(toSecondsString(fileListGenerationTime)).append(" seconds\n");
            sb.append("File list transfer time: ").append(toSecondsString(fileListTransferTime)).append(" seconds\n");
            sb.append("Total bytes sent: ").append(totalBytesSent).append("\n");
            sb.append("Total bytes received: ").append(totalBytesReceived).append("\n");
            sb.append("Total time: ").append(toSecondsString(totalTime)).append(" seconds\n");
            double speed = (double)(totalBytesSent + totalBytesReceived) / ((double)totalTime / 1000D);
            sb.append("Send/Receive speed: ").append(String.format("%.2f", new Object[] {
                Double.valueOf(speed)
            })).append(" bytes/sec\n");
            return sb.toString();
        }

        private long numberOfFiles;
        private long numberOfFilesTransferred;
        private long totalFileSize;
        private long totalTransferredFileSize;
        private long literalData;
        private long matchedData;
        private long fileListSize;
        private long fileListGenerationTime;
        private long fileListTransferTime;
        private long totalBytesSent;
        private long totalBytesReceived;
        private long totalTime;

        public RsyncStats()
        {
        }

        public RsyncStats(String stdout, long totalTime)
        {
            this.totalTime = totalTime;
            String lines[] = stdout.split("[\\r\\n]+");
            if(lines != null)
            {
                String arr$[] = lines;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    String line = arr$[i$];
                    if(!line.contains(": "))
                        continue;
                    String fields[] = line.split(": ", 2);
                    if(fields == null)
                        continue;
                    if(fields[0].equalsIgnoreCase("Number of files"))
                    {
                        numberOfFiles = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("Number of files transferred"))
                    {
                        numberOfFilesTransferred = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("Total file size"))
                    {
                        totalFileSize = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("Total transferred file size"))
                    {
                        totalTransferredFileSize = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("Literal data"))
                    {
                        literalData = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("Matched data"))
                    {
                        matchedData = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("File list size"))
                    {
                        fileListSize = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("File list generation time"))
                    {
                        fileListGenerationTime = parseSecondsAsMillis(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("File list transfer time"))
                    {
                        fileListTransferTime = parseSecondsAsMillis(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("Total bytes sent"))
                    {
                        totalBytesSent = parseNumber(fields[1]);
                        continue;
                    }
                    if(fields[0].equalsIgnoreCase("Total bytes received"))
                        totalBytesReceived = parseNumber(fields[1]);
                }

            }
        }
    }

    private static class VersionInfo
    {

        public MailboxVersion getMboxVersion()
        {
            return mboxVer;
        }

        public int getDbVersion()
        {
            return dbVer;
        }

        public int getIndexVersion()
        {
            return indexVer;
        }

        private MailboxVersion mboxVer;
        private int dbVer;
        private int indexVer;

        public VersionInfo(MailboxVersion mboxVer, int dbVer, int indexVer)
        {
            this.mboxVer = mboxVer;
            this.dbVer = dbVer;
            this.indexVer = indexVer;
        }
    }

    private static class MailboxDataDirectory
    {

        public short getVolumeType()
        {
            return volumeType;
        }

        public String getPath()
        {
            return path;
        }

        private short volumeType;
        private String path;

        MailboxDataDirectory(short volumeType, String path)
        {
            this.volumeType = volumeType;
            this.path = path;
        }
    }

    private static class LocalVolumes
    {

        public Volume getPrimaryBlobs()
        {
            return primaryBlobs;
        }

        public Volume getSecondaryBlobs()
        {
            return secondaryBlobs;
        }

        public Volume getIndex()
        {
            return index;
        }

        public static LocalVolumes getSnapshot()
        {
            LocalVolumes lv = new LocalVolumes();
            lv.primaryBlobs = Volume.getCurrentMessageVolume();
            lv.secondaryBlobs = Volume.getCurrentSecondaryMessageVolume();
            if(lv.secondaryBlobs == null)
                lv.secondaryBlobs = lv.primaryBlobs;
            lv.index = Volume.getCurrentIndexVolume();
            return lv;
        }

        private Volume primaryBlobs;
        private Volume secondaryBlobs;
        private Volume index;

        private LocalVolumes()
        {
        }
    }

    private static class SkipParams
    {

        public boolean skipBlobs()
        {
            return skipBlobs;
        }

        public boolean skipHsmBlobs()
        {
            return skipHsmBlobs;
        }

        public boolean skipSearchIndex()
        {
            return skipSearchIndex;
        }

        public boolean skipAll()
        {
            return skipBlobs && skipHsmBlobs && skipSearchIndex;
        }

        public static SkipParams parse(Element elem, Server server)
            throws ServiceException
        {
            SkipParams params = new SkipParams();
            params.skipBlobs = parseIncludeExcludeAttr(elem.getAttribute("blobs", null), server.isMailboxMoveSkipBlobs());
            if(params.skipBlobs)
                params.skipHsmBlobs = true;
            else
                params.skipHsmBlobs = parseIncludeExcludeAttr(elem.getAttribute("secondaryBlobs", null), server.isMailboxMoveSkipHsmBlobs());
            params.skipSearchIndex = parseIncludeExcludeAttr(elem.getAttribute("searchIndex", null), server.isMailboxMoveSkipSearchIndex());
            return params;
        }

        private static boolean parseIncludeExcludeAttr(String attrVal, boolean defaultVal)
            throws ServiceException
        {
            boolean skip = defaultVal;
            if(attrVal != null)
                if(attrVal.equalsIgnoreCase("exclude"))
                    skip = true;
                else
                if(attrVal.equalsIgnoreCase("include"))
                    skip = false;
                else
                if(attrVal.equalsIgnoreCase("config"))
                    skip = defaultVal;
                else
                    throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Invalid include/exclude value \"").append(attrVal).append("\"").toString(), null);
            return skip;
        }

        private boolean skipBlobs;
        private boolean skipHsmBlobs;
        private boolean skipSearchIndex;

        private SkipParams()
        {
            skipBlobs = false;
            skipHsmBlobs = false;
            skipSearchIndex = false;
        }
    }

    private class MailboxMoveThread extends Thread
    {

        public void run()
        {
            moveIt(authToken, soapTransport, account, params);
            MailboxMoveTracker.getInstance().unregisterMoveIn(account, srcHost);
            Provisioning prov = Provisioning.getInstance();
            String localHost = prov.getLocalServer().getServiceHostname();
            unregisterMailboxMoveOut(soapTransport, account, localHost, true);
            break MISSING_BLOCK_LABEL_226;
            Exception exception;
            exception;
            Provisioning prov = Provisioning.getInstance();
            String localHost = prov.getLocalServer().getServiceHostname();
            unregisterMailboxMoveOut(soapTransport, account, localHost, true);
            throw exception;
            Exception exception1;
            exception1;
            MailboxMoveTracker.getInstance().unregisterMoveIn(account, srcHost);
            Provisioning prov = Provisioning.getInstance();
            String localHost = prov.getLocalServer().getServiceHostname();
            unregisterMailboxMoveOut(soapTransport, account, localHost, true);
            break MISSING_BLOCK_LABEL_200;
            Exception exception2;
            exception2;
            Provisioning prov = Provisioning.getInstance();
            String localHost = prov.getLocalServer().getServiceHostname();
            unregisterMailboxMoveOut(soapTransport, account, localHost, true);
            throw exception2;
            throw exception1;
            OutOfMemoryError e;
            e;
            Zimbra.halt("out of memory", e);
            break MISSING_BLOCK_LABEL_226;
            e;
            Log.mboxmove.error("Error during background mailbox move", e);
        }

        private AuthToken authToken;
        private SoapHttpTransport soapTransport;
        private Account account;
        private String srcHost;
        private Params params;
        final MoveMailbox this$0;

        public MailboxMoveThread(AuthToken authToken, SoapHttpTransport soapTransport, Account account, String srcHost, Params params)
        {
            this$0 = MoveMailbox.this;
            super();
            this.authToken = authToken;
            this.soapTransport = soapTransport;
            this.account = account;
            this.srcHost = srcHost;
            this.params = params;
        }
    }

    private static class Params
    {

        SkipParams skipParams;
        int maxSyncs;
        long syncFinishThreshold;
        boolean append;

        private Params()
        {
            append = true;
        }

    }


    public MoveMailbox()
    {
    }

    public boolean domainAuthSufficient(Map context)
    {
        return true;
    }

    protected void checkRights(ZimbraSoapContext zsc, Map context, Account account)
        throws ServiceException
    {
        Provisioning prov = Provisioning.getInstance();
        if(account.isCalendarResource())
        {
            com.zimbra.cs.account.CalendarResource cr = prov.get(com.zimbra.cs.account.Provisioning.CalendarResourceBy.id, account.getId());
            checkCalendarResourceRight(zsc, cr, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox);
        } else
        {
            checkAccountRight(zsc, account, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox);
        }
        Server localServer = prov.getLocalServer();
        checkRight(zsc, context, localServer, com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
    }

    public void docRights(List relatedRights, List notes)
    {
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox);
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox);
        relatedRights.add(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer);
        notes.add((new StringBuilder()).append("If the account is a calendar resource, need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveCalendarResourceMailbox.getName()).append(" right on the calendar resource.").toString());
        notes.add((new StringBuilder()).append("If the account is a regular account, need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveAccountMailbox.getName()).append(" right on the account.").toString());
        notes.add((new StringBuilder()).append("Need ").append(com.zimbra.cs.account.accesscontrol.Rights.Admin.R_moveMailboxFromServer.getName()).append(" right on the source server").toString());
    }

    public Element handleNetworkRequest(Element request, Map context)
        throws ServiceException, SoapFaultException
    {
        ZimbraSoapContext zsc;
        Element accountElem;
        Account account;
        Server srcServer;
        Server localServer;
        String srcHost;
        AuthToken authToken;
        SoapHttpTransport soapTransport;
        Params params;
        boolean moveInRegistered;
        zsc = NetworkDocumentHandler.getZimbraSoapContext(context);
        accountElem = request.getElement("account");
        String email = accountElem.getAttribute("name");
        Provisioning prov = Provisioning.getInstance();
        account = prov.get(com.zimbra.cs.account.Provisioning.AccountBy.name, email);
        if(account == null)
            throw ServiceException.FAILURE((new StringBuilder()).append("Account ").append(email).append(" not found").toString(), null);
        prov.reload(account);
        checkRights(zsc, context, account);
        srcServer = account.getServer();
        localServer = prov.getLocalServer();
        String dhost = accountElem.getAttribute("dest");
        Server destServer = prov.get(com.zimbra.cs.account.Provisioning.ServerBy.name, dhost);
        if(destServer == null)
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Destination server ").append(dhost).append(" not found").toString(), null);
        if(!destServer.getId().equals(localServer.getId()))
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Request must be sent to ").append(destServer.getServiceHostname()).toString(), null);
        String shost = accountElem.getAttribute("src");
        Server shServer = prov.get(com.zimbra.cs.account.Provisioning.ServerBy.name, shost);
        if(shServer == null)
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Source server ").append(shost).append(" not found").toString(), null);
        srcHost = srcServer.getServiceHostname();
        if(!shServer.getId().equals(srcServer.getId()))
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Account ").append(email).append(" is not on server ").append(shost).append("; source server should be ").append(srcHost).toString(), null);
        if(localServer.getId().equals(srcServer.getId()))
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Account ").append(email).append(" is already on this server").toString(), null);
        authToken = zsc.getAuthToken();
        soapTransport = getSoapTransport(zsc, srcServer);
        params = new Params();
        params.skipParams = SkipParams.parse(accountElem, localServer);
        params.maxSyncs = Math.min((int)accountElem.getAttributeLong("maxSyncs", 10L), 100);
        params.syncFinishThreshold = Math.max(accountElem.getAttributeLong("syncFinishThreshold", 0x2bf20L), 0L);
        VersionInfo versionInfo = getRemoteVersionInfo(soapTransport, account);
        if(versionInfo.getMboxVersion().tooHigh() || versionInfo.getDbVersion() > Versions.getDbVersion() || versionInfo.getIndexVersion() > Versions.getIndexVersion())
        {
            String msg = String.format("source=[mbox=%s, db=%d, index=%d], dest=[mbox=%s, db=%d, index=%d]", new Object[] {
                versionInfo.getMboxVersion().toString(), Integer.valueOf(versionInfo.getDbVersion()), Integer.valueOf(versionInfo.getIndexVersion()), MailboxVersion.getCurrent(), Integer.valueOf(Versions.getDbVersion()), Integer.valueOf(Versions.getIndexVersion())
            });
            throw MailboxMoveServiceException.CANNOT_MOVE_TO_OLDER_SERVER(email, msg);
        }
        registerMailboxMoveOut(soapTransport, account, localServer.getServiceHostname());
        moveInRegistered = false;
        MailboxMoveTracker.getInstance().registerMoveIn(account, srcHost);
        moveInRegistered = true;
        if(!moveInRegistered)
            unregisterMailboxMoveOut(soapTransport, account, localServer.getServiceHostname(), true);
        break MISSING_BLOCK_LABEL_645;
        Exception exception;
        exception;
        if(!moveInRegistered)
            unregisterMailboxMoveOut(soapTransport, account, localServer.getServiceHostname(), true);
        throw exception;
        if(!accountElem.getAttributeBool("sync", false))
            break MISSING_BLOCK_LABEL_773;
        moveIt(authToken, soapTransport, account, params);
        MailboxMoveTracker.getInstance().unregisterMoveIn(account, srcHost);
        unregisterMailboxMoveOut(soapTransport, account, localServer.getServiceHostname(), true);
        break MISSING_BLOCK_LABEL_845;
        Exception exception1;
        exception1;
        unregisterMailboxMoveOut(soapTransport, account, localServer.getServiceHostname(), true);
        throw exception1;
        Exception exception2;
        exception2;
        MailboxMoveTracker.getInstance().unregisterMoveIn(account, srcHost);
        unregisterMailboxMoveOut(soapTransport, account, localServer.getServiceHostname(), true);
        break MISSING_BLOCK_LABEL_767;
        Exception exception3;
        exception3;
        unregisterMailboxMoveOut(soapTransport, account, localServer.getServiceHostname(), true);
        throw exception3;
        throw exception2;
        MailboxMoveThread thread = new MailboxMoveThread(authToken, soapTransport, account, srcHost, params);
        thread.setName((new StringBuilder()).append("MailboxMove-").append(account.getName()).append(":").append(srcServer.getServiceHostname()).toString());
        thread.setDaemon(true);
        thread.start();
        return zsc.createElement(BackupService.MOVE_MAILBOX_RESPONSE);
    }

    private Mailbox prepareTargetMailbox(SoapHttpTransport soapTransport, Account account, boolean append)
        throws ServiceException
    {
        MailboxManager mboxMgr;
        Mailbox mbox;
        com.zimbra.cs.mailbox.MailboxManager.MailboxLock lock;
        com.zimbra.cs.db.DbPool.Connection conn;
        boolean success;
        mboxMgr = MailboxManager.getInstance();
        mbox = mboxMgr.getMailboxByAccountId(account.getId(), com.zimbra.cs.mailbox.MailboxManager.FetchMode.DO_NOT_AUTOCREATE, true);
        if(mbox == null)
            break MISSING_BLOCK_LABEL_196;
        if(!append)
            throw ServiceException.FAILURE((new StringBuilder()).append("A mailbox already exists (id=").append(mbox.getId()).append(") on this server for account ").append(account.getName()).append("; Purge it with zmpurgeoldmbox first.").toString(), null);
        lock = MailboxManager.getInstance().beginMaintenance(account.getId(), mbox.getId());
        conn = null;
        success = false;
        conn = DbPool.getConnection();
        DbMailbox.clearMailboxContent(conn, mbox);
        success = true;
        if(conn != null)
            if(success)
                conn.commit();
            else
                DbPool.quietRollback(conn);
        DbPool.quietClose(conn);
        MailboxManager.getInstance().endMaintenance(lock, false, true);
        break MISSING_BLOCK_LABEL_196;
        Exception exception;
        exception;
        if(conn != null)
            if(success)
                conn.commit();
            else
                DbPool.quietRollback(conn);
        DbPool.quietClose(conn);
        MailboxManager.getInstance().endMaintenance(lock, false, true);
        throw exception;
        mbox = mboxMgr.getMailboxByAccountId(account.getId(), com.zimbra.cs.mailbox.MailboxManager.FetchMode.AUTOCREATE, true);
        return mbox;
    }

    private void moveIt(AuthToken authToken, SoapHttpTransport soapTransport, Account account, Params params)
        throws ServiceException
    {
        com.zimbra.cs.mailbox.MailboxManager.MailboxLock lock;
        boolean success;
        long startMaintenance;
        boolean accountInMaintenance;
        String originalAccountStatus;
        Server srcServer;
        lock = null;
        success = false;
        startMaintenance = 0L;
        accountInMaintenance = false;
        originalAccountStatus = null;
        srcServer = account.getServer();
        Mailbox mbox;
        LocalVolumes localVols;
        mbox = prepareTargetMailbox(soapTransport, account, params.append);
        lock = MailboxManager.getInstance().beginMaintenance(account.getId(), mbox.getId());
        localVols = LocalVolumes.getSnapshot();
        if(!params.skipParams.skipAll())
        {
            boolean caughtUp = false;
            int round;
            for(round = 1; !caughtUp && round <= params.maxSyncs; round++)
            {
                List remoteDirs = getRemoteDataDirectories(soapTransport, account);
                RsyncStats stats = rsyncMailboxVolumes(srcServer, remoteDirs, account, mbox.getId(), localVols, params.skipParams);
                long elapsed = stats.getTotalTime();
                Log.mboxmove.debug((new StringBuilder()).append("rsync round ").append(round).append(" for mailbox move of ").append(account.getName()).append(" took ").append(elapsed).append("ms").toString());
                caughtUp = elapsed <= params.syncFinishThreshold;
            }

            if(!caughtUp)
                Log.mboxmove.info((new StringBuilder()).append("Finished with unlocked delta syncs after maximum ").append(round - 1).append(" rounds").toString());
        }
        Log.mboxmove.info((new StringBuilder()).append("Putting account ").append(account.getName()).append(" under maintenance").toString());
        Provisioning prov = Provisioning.getInstance();
        prov.reload(account);
        originalAccountStatus = account.getAccountStatus(prov);
        if(originalAccountStatus == null)
            originalAccountStatus = "active";
        if("maintenance".equals(originalAccountStatus))
            throw ServiceException.FAILURE("Account is already in maintenance mode", null);
        startMaintenance = System.currentTimeMillis();
        accountInMaintenance = true;
        prov.modifyAccountStatus(account, "maintenance");
        prov.reload(account);
        reloadAccountOnRemoteServer(soapTransport, account, false);
        if(!srcServer.getId().equals(account.getServer().getId()))
            throw ServiceException.INVALID_REQUEST((new StringBuilder()).append("Account ").append(account.getName()).append(" was apparently moved by someone else; abandoning current move").toString(), null);
        List remoteDirs = getRemoteDataDirectories(soapTransport, account);
        unloadMailboxOnRemoteServer(soapTransport, account);
        if(!params.skipParams.skipAll())
        {
            RsyncStats stats = rsyncMailboxVolumes(srcServer, remoteDirs, account, mbox.getId(), localVols, params.skipParams);
            long elapsed = stats.getTotalTime();
            Log.mboxmove.debug((new StringBuilder()).append("final rsync round for mailbox move of ").append(account.getName()).append(" took ").append(elapsed).append("ms").toString());
        }
        MailboxManager.getInstance().endMaintenance(lock, false, true);
        lock = null;
        break MISSING_BLOCK_LABEL_520;
        Exception exception;
        exception;
        lock = null;
        throw exception;
        copyDatabase(authToken, srcServer, account, mbox.getId(), localVols);
        unloadMailboxOnRemoteServer(soapTransport, account);
        success = true;
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, false, true);
        if(accountInMaintenance && originalAccountStatus != null)
        {
            Provisioning prov = Provisioning.getInstance();
            if(success)
            {
                String localHost = prov.getLocalServer().getServiceHostname();
                Map attrs = new HashMap(2);
                attrs.put("zimbraMailHost", localHost);
                attrs.put("zimbraAccountStatus", originalAccountStatus);
                prov.modifyAttrs(account, attrs);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" moved to ").append(localHost).append("; account status = ").append(originalAccountStatus).toString());
                long start = System.currentTimeMillis();
                MailboxManager.getInstance().getMailboxByAccountId(account.getId());
                long elapsed = System.currentTimeMillis() - start;
                Log.mboxmove.debug((new StringBuilder()).append("final mailbox load/upgrade of ").append(account.getName()).append(" took ").append(elapsed).append("ms").toString());
            } else
            {
                prov.modifyAccountStatus(account, originalAccountStatus);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append("'s status reverted to ").append(originalAccountStatus).toString());
            }
            long durMaintSec = (System.currentTimeMillis() - startMaintenance) / 1000L;
            Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" was under maintenance for ").append(durMaintSec).append(" seconds").toString());
            prov.reload(account);
            reloadAccountOnRemoteServer(soapTransport, account, true);
        }
        break MISSING_BLOCK_LABEL_1814;
        Exception exception1;
        exception1;
        if(accountInMaintenance && originalAccountStatus != null)
        {
            Provisioning prov = Provisioning.getInstance();
            if(success)
            {
                String localHost = prov.getLocalServer().getServiceHostname();
                Map attrs = new HashMap(2);
                attrs.put("zimbraMailHost", localHost);
                attrs.put("zimbraAccountStatus", originalAccountStatus);
                prov.modifyAttrs(account, attrs);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" moved to ").append(localHost).append("; account status = ").append(originalAccountStatus).toString());
                long start = System.currentTimeMillis();
                MailboxManager.getInstance().getMailboxByAccountId(account.getId());
                long elapsed = System.currentTimeMillis() - start;
                Log.mboxmove.debug((new StringBuilder()).append("final mailbox load/upgrade of ").append(account.getName()).append(" took ").append(elapsed).append("ms").toString());
            } else
            {
                prov.modifyAccountStatus(account, originalAccountStatus);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append("'s status reverted to ").append(originalAccountStatus).toString());
            }
            long durMaintSec = (System.currentTimeMillis() - startMaintenance) / 1000L;
            Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" was under maintenance for ").append(durMaintSec).append(" seconds").toString());
            prov.reload(account);
            reloadAccountOnRemoteServer(soapTransport, account, true);
        }
        throw exception1;
        Exception exception2;
        exception2;
        if(lock != null)
            MailboxManager.getInstance().endMaintenance(lock, false, true);
        if(accountInMaintenance && originalAccountStatus != null)
        {
            Provisioning prov = Provisioning.getInstance();
            if(success)
            {
                String localHost = prov.getLocalServer().getServiceHostname();
                Map attrs = new HashMap(2);
                attrs.put("zimbraMailHost", localHost);
                attrs.put("zimbraAccountStatus", originalAccountStatus);
                prov.modifyAttrs(account, attrs);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" moved to ").append(localHost).append("; account status = ").append(originalAccountStatus).toString());
                long start = System.currentTimeMillis();
                MailboxManager.getInstance().getMailboxByAccountId(account.getId());
                long elapsed = System.currentTimeMillis() - start;
                Log.mboxmove.debug((new StringBuilder()).append("final mailbox load/upgrade of ").append(account.getName()).append(" took ").append(elapsed).append("ms").toString());
            } else
            {
                prov.modifyAccountStatus(account, originalAccountStatus);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append("'s status reverted to ").append(originalAccountStatus).toString());
            }
            long durMaintSec = (System.currentTimeMillis() - startMaintenance) / 1000L;
            Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" was under maintenance for ").append(durMaintSec).append(" seconds").toString());
            prov.reload(account);
            reloadAccountOnRemoteServer(soapTransport, account, true);
        }
        break MISSING_BLOCK_LABEL_1811;
        Exception exception3;
        exception3;
        if(accountInMaintenance && originalAccountStatus != null)
        {
            Provisioning prov = Provisioning.getInstance();
            if(success)
            {
                String localHost = prov.getLocalServer().getServiceHostname();
                Map attrs = new HashMap(2);
                attrs.put("zimbraMailHost", localHost);
                attrs.put("zimbraAccountStatus", originalAccountStatus);
                prov.modifyAttrs(account, attrs);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" moved to ").append(localHost).append("; account status = ").append(originalAccountStatus).toString());
                long start = System.currentTimeMillis();
                MailboxManager.getInstance().getMailboxByAccountId(account.getId());
                long elapsed = System.currentTimeMillis() - start;
                Log.mboxmove.debug((new StringBuilder()).append("final mailbox load/upgrade of ").append(account.getName()).append(" took ").append(elapsed).append("ms").toString());
            } else
            {
                prov.modifyAccountStatus(account, originalAccountStatus);
                Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append("'s status reverted to ").append(originalAccountStatus).toString());
            }
            long durMaintSec = (System.currentTimeMillis() - startMaintenance) / 1000L;
            Log.mboxmove.info((new StringBuilder()).append("Account ").append(account.getName()).append(" was under maintenance for ").append(durMaintSec).append(" seconds").toString());
            prov.reload(account);
            reloadAccountOnRemoteServer(soapTransport, account, true);
        }
        throw exception3;
        throw exception2;
        if(success)
            try
            {
                ArrayList accounts = new ArrayList();
                accounts.add(account.getName());
                ProxyPurgeUtil.purgeAccounts(null, accounts, true, null);
            }
            catch(ServiceException e)
            {
                Log.mboxmove.warn("Error while purging routes in memcached", e);
            }
        return;
    }

    private SoapHttpTransport getSoapTransport(ZimbraSoapContext zsc, Server srcServer)
        throws ServiceException
    {
        String url = URLUtil.getAdminURL(srcServer, "/service/admin/soap/", true);
        SoapHttpTransport transport = new SoapHttpTransport(url);
        transport.setAuthToken(zsc.getRawAuthToken());
        return transport;
    }

    private VersionInfo getRemoteVersionInfo(SoapHttpTransport soapTransport, Account account)
        throws ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.GET_MAILBOX_VERSION_REQUEST);
        req.addElement("account").addAttribute("name", account.getName());
        Element resp;
        try
        {
            resp = soapTransport.invokeWithoutSession(req);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE("Error getting mailbox version info from remote server", e);
        }
        Element accountElem = resp.getElement("account");
        short majorVer = (short)(int)accountElem.getAttributeLong("majorVer", 1L);
        short minorVer = (short)(int)accountElem.getAttributeLong("minorVer", 0L);
        int dbVer = (int)accountElem.getAttributeLong("dbVer", 1L);
        int indexVer = (int)accountElem.getAttributeLong("indexVer", 1L);
        return new VersionInfo(new MailboxVersion(majorVer, minorVer), dbVer, indexVer);
    }

    private List getRemoteDataDirectories(SoapHttpTransport soapTransport, Account account)
        throws ServiceException
    {
        List list = new ArrayList();
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.GET_MAILBOX_VOLUMES_REQUEST);
        req.addElement("account").addAttribute("name", account.getName());
        Element resp;
        try
        {
            resp = soapTransport.invokeWithoutSession(req);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE("Error getting mailbox data directory list from remote server", e);
        }
        Element accountElem = resp.getElement("account");
        for(Iterator iter = accountElem.elementIterator("volume"); iter.hasNext();)
        {
            Element volume = (Element)iter.next();
            short type = (short)(int)volume.getAttributeLong("type", 0L);
            String path = volume.getAttribute("rootpath", null);
            if(path != null)
                list.add(new MailboxDataDirectory(type, path));
            else
                Log.mboxmove.warn((new StringBuilder()).append("Ignoring remote mailbox data directory with no path; volume type=").append(type).toString());
        }

        return list;
    }

    private RsyncStats rsyncDirectory(String srcHost, String srcPath, String destPath)
        throws ServiceException
    {
        if(!srcPath.endsWith(File.separator))
            srcPath = (new StringBuilder()).append(srcPath).append(File.separator).toString();
        try
        {
            FileUtil.ensureDirExists(destPath);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Error creating directory ").append(destPath).toString(), e);
        }
        Server localServer = Provisioning.getInstance().getLocalServer();
        RemoteManager remote = RemoteManager.getRemoteManager(localServer);
        String rsyncCmd = String.format("rsync --stats -a -e 'ssh -i %s -o StrictHostKeyChecking=no' %s:%s %s", new Object[] {
            remote.getPrivateKeyPath(), srcHost, srcPath, destPath
        });
        Log.mboxmove.debug((new StringBuilder()).append("Running rsync: ").append(rsyncCmd).toString());
        long start = System.currentTimeMillis();
        RemoteResult result = remote.execute(rsyncCmd);
        long elapsed = System.currentTimeMillis() - start;
        if(result.getMExitStatus() == 0)
        {
            String stdout = null;
            try
            {
                stdout = new String(result.getMStdout(), "UTF-8");
            }
            catch(UnsupportedEncodingException e)
            {
                stdout = "";
            }
            Log.mboxmove.debug(stdout);
            return new RsyncStats(stdout, elapsed);
        }
        String stderr = null;
        try
        {
            stderr = new String(result.getMStderr(), "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            stderr = "";
        }
        String errmsg = String.format("Command \"%s\" failed; exit code=%d; stderr=\n%s", new Object[] {
            rsyncCmd, Integer.valueOf(result.getMExitStatus()), stderr
        });
        Log.mboxmove.error(errmsg);
        throw ServiceException.FAILURE(errmsg, null);
    }

    private RsyncStats rsyncMailboxVolumes(Server srcServer, List remoteDirs, Account account, int targetMailboxId, LocalVolumes localVols, SkipParams skipParams)
        throws ServiceException
    {
        RsyncStats statsSum = new RsyncStats();
        Iterator i$ = remoteDirs.iterator();
        do
        {
            if(!i$.hasNext())
                break;
            MailboxDataDirectory mdd = (MailboxDataDirectory)i$.next();
            short volType = mdd.getVolumeType();
            Volume localVol;
            if(volType == 1)
            {
                if(skipParams.skipBlobs())
                    continue;
                localVol = localVols.getPrimaryBlobs();
            } else
            if(volType == 2)
            {
                if(skipParams.skipHsmBlobs())
                    continue;
                localVol = localVols.getSecondaryBlobs();
                if(localVol == null)
                    localVol = localVols.getPrimaryBlobs();
            } else
            if(volType == 10)
            {
                if(skipParams.skipSearchIndex())
                    continue;
                localVol = localVols.getIndex();
            } else
            {
                Log.mboxmove.warn((new StringBuilder()).append("Invalid volume type ").append(volType).append(" for remote directory ").append(mdd.getPath()).append("; skipping").toString());
                continue;
            }
            if(localVol == null)
                throw ServiceException.FAILURE((new StringBuilder()).append("Local destination volume not available for volume type ").append(mdd.getVolumeType()).toString(), null);
            String destPath = localVol.getMailboxDir(targetMailboxId, mdd.getVolumeType());
            String srcHost = srcServer.getAttr("zimbraServiceHostname");
            RsyncStats stats = rsyncDirectory(srcHost, mdd.getPath(), destPath);
            statsSum.add(stats);
        } while(true);
        Log.mboxmove.debug((new StringBuilder()).append("Stats for rsync round:\n").append(statsSum.toString()).toString());
        return statsSum;
    }

    private void reloadAccountOnRemoteServer(SoapHttpTransport soapTransport, Account account, boolean ignoreError)
        throws ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.RELOAD_ACCOUNT_REQUEST);
        req.addElement("account").addAttribute("name", account.getName());
        try
        {
            soapTransport.invokeWithoutSession(req);
        }
        catch(IOException e)
        {
            String errmsg = (new StringBuilder()).append("Error sending account reload command for ").append(account.getName()).append(" to source server").toString();
            if(ignoreError)
                Log.mboxmove.warn((new StringBuilder()).append(errmsg).append("; ignored").toString(), e);
            else
                throw ServiceException.FAILURE(errmsg, e);
        }
        catch(ServiceException e)
        {
            String errmsg = (new StringBuilder()).append("Error sending account reload command for ").append(account.getName()).append(" to source server").toString();
            if(ignoreError)
                Log.mboxmove.warn((new StringBuilder()).append(errmsg).append("; ignored").toString(), e);
            else
                throw e;
        }
    }

    private void unloadMailboxOnRemoteServer(SoapHttpTransport soapTransport, Account account)
        throws ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.UNLOAD_MAILBOX_REQUEST);
        req.addElement("account").addAttribute("name", account.getName());
        try
        {
            soapTransport.invokeWithoutSession(req);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Error sending mailbox unload command for ").append(account.getName()).append(" to source server").toString(), e);
        }
    }

    private void registerMailboxMoveOut(SoapHttpTransport soapTransport, Account account, String destHost)
        throws ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.REGISTER_MAILBOX_MOVE_OUT_REQUEST);
        Element accountElem = req.addElement("account");
        accountElem.addAttribute("name", account.getName());
        accountElem.addAttribute("dest", destHost);
        try
        {
            soapTransport.invokeWithoutSession(req);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Error sending mailbox move register command for ").append(account.getName()).append(" to source server").toString(), e);
        }
    }

    private void unregisterMailboxMoveOut(SoapHttpTransport soapTransport, Account account, String destHost, boolean ignoreError)
        throws ServiceException
    {
        Element req = new com.zimbra.common.soap.Element.XMLElement(BackupService.UNREGISTER_MAILBOX_MOVE_OUT_REQUEST);
        Element accountElem = req.addElement("account");
        accountElem.addAttribute("name", account.getName());
        accountElem.addAttribute("dest", destHost);
        try
        {
            soapTransport.invokeWithoutSession(req);
        }
        catch(IOException e)
        {
            String errmsg = (new StringBuilder()).append("Error sending mailbox move unregister command for ").append(account.getName()).append(" to source server").toString();
            if(ignoreError)
                Log.mboxmove.warn((new StringBuilder()).append(errmsg).append("; ignored").toString(), e);
            else
                throw ServiceException.FAILURE(errmsg, e);
        }
        catch(ServiceException e)
        {
            String errmsg = (new StringBuilder()).append("Error sending mailbox move unregister command for ").append(account.getName()).append(" to source server").toString();
            if(ignoreError)
                Log.mboxmove.warn((new StringBuilder()).append(errmsg).append("; ignored").toString(), e);
            else
                throw e;
        }
    }

    private void downloadDatabaseExport(AuthToken authToken, Server srcServer, Account account, File saveTo)
        throws ServiceException
    {
        String srcHost;
        HttpClient httpClient;
        GetMethod httpGet;
        InputStream is;
        FileOutputStream fos;
        long start;
        srcHost = srcServer.getServiceHostname();
        int port = srcServer.getIntAttr("zimbraAdminPort", -1);
        if(port == -1)
            throw ServiceException.FAILURE("unable to determine admin port", null);
        String exportUri = null;
        try
        {
            exportUri = (new URL("https", srcHost, port, "/service/extension/backup/mboxexport")).toExternalForm();
            Log.mboxmove.debug((new StringBuilder()).append("Exporting mailbox data from remote server: ").append(exportUri).toString());
        }
        catch(MalformedURLException e)
        {
            throw ServiceException.FAILURE("invalid URL", e);
        }
        URI u;
        try
        {
            u = new URI(exportUri);
        }
        catch(URISyntaxException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Invalid export URL ").append(exportUri).toString(), e);
        }
        StringBuilder uri = new StringBuilder(exportUri);
        String urlEncodedAccountEmail;
        try
        {
            urlEncodedAccountEmail = URLEncoder.encode(account.getName(), "UTF-8");
        }
        catch(UnsupportedEncodingException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to encode account name: ").append(account.getName()).toString(), e);
        }
        uri.append((new StringBuilder()).append("?account-name=").append(urlEncodedAccountEmail).append("&").append("skip-blobs").append("=1").append("&").append("skip-hsm-blobs").append("=1").append("&").append("skip-search-index").append("=1").toString());
        httpClient = ZimbraHttpConnectionManager.getInternalHttpConnMgr().newHttpClient();
        httpGet = new GetMethod(uri.toString());
        httpGet.getParams().setSoTimeout(0);
        authToken.encode(httpClient, httpGet, false, u.getHost());
        is = null;
        fos = null;
        start = System.currentTimeMillis();
        long elapsed;
        try
        {
            httpClient.executeMethod(httpGet);
            Log.mboxmove.debug((new StringBuilder()).append("response status: (").append(httpGet.getStatusLine()).append(")").toString());
            if(httpGet.getStatusCode() != 200)
                throw ServiceException.FAILURE((new StringBuilder()).append("Failed to export mailbox from source server ").append(srcHost).append(": ").append(httpGet.getStatusText()).toString(), null);
            is = httpGet.getResponseBodyAsStream();
            fos = new FileOutputStream(saveTo);
            ByteUtil.copy(is, false, fos, false);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Failed to export mailbox from source server ").append(srcHost).toString(), e);
        }
        httpGet.releaseConnection();
        ByteUtil.closeStream(fos);
        ByteUtil.closeStream(is);
        elapsed = System.currentTimeMillis() - start;
        Log.mboxmove.debug((new StringBuilder()).append("Export took ").append(elapsed).append("ms").toString());
        break MISSING_BLOCK_LABEL_602;
        Exception exception;
        exception;
        httpGet.releaseConnection();
        ByteUtil.closeStream(fos);
        ByteUtil.closeStream(is);
        long elapsed = System.currentTimeMillis() - start;
        Log.mboxmove.debug((new StringBuilder()).append("Export took ").append(elapsed).append("ms").toString());
        throw exception;
    }

    private void copyDatabase(AuthToken authToken, Server srcServer, Account account, int localMailboxId, LocalVolumes localVols)
        throws ServiceException
    {
        File dbExportZip = null;
        FileInputStream fis;
        try
        {
            File tempDir = new File(Provisioning.getInstance().getLocalServer().getMailboxMoveTempDir());
            FileUtil.ensureDirExists(tempDir);
            dbExportZip = File.createTempFile("zmmailboxmove-db-", ".zip", tempDir);
            Log.mboxmove.debug((new StringBuilder()).append("Downloading database data to temp file ").append(dbExportZip.getAbsolutePath()).toString());
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE("Can't create temp file for downloaing database export", e);
        }
        downloadDatabaseExport(authToken, srcServer, account, dbExportZip);
        fis = null;
        try
        {
            Log.mboxmove.debug("Database data import started");
            fis = new FileInputStream(dbExportZip);
            ZipInputStream zipIn = new ZipInputStream(fis);
            ZipBackupTarget source = new ZipBackupTarget(zipIn, localMailboxId);
            RestoreParams params = new RestoreParams();
            params.skipDb = false;
            params.skipSearchIndex = params.skipBlobs = params.skipSecondaryBlobs = true;
            params.append = true;
            params.primaryBlobVolume = localVols.getPrimaryBlobs();
            params.secondaryBlobVolume = localVols.getSecondaryBlobs();
            params.indexVolume = localVols.getIndex();
            source.restore(new String[] {
                account.getId()
            }, null, params);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE("Unable to import database data", e);
        }
        ByteUtil.closeStream(fis);
        break MISSING_BLOCK_LABEL_256;
        Exception exception;
        exception;
        ByteUtil.closeStream(fis);
        throw exception;
        if(dbExportZip != null && dbExportZip.exists())
            dbExportZip.delete();
        break MISSING_BLOCK_LABEL_302;
        Exception exception1;
        exception1;
        if(dbExportZip != null && dbExportZip.exists())
            dbExportZip.delete();
        throw exception1;
    }

    private static final long DEFAULT_SYNC_FINISH_THRESHOLD = 0x2bf20L;
    private static final int DEFAULT_MAX_SYNCS = 10;
    private static final String MBOXEXPORT_URI = "/service/extension/backup/mboxexport";


}
