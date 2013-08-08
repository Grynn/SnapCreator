// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   XmlMeta.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.*;
import com.zimbra.cs.db.DbBackup;
import com.zimbra.cs.service.backup.BackupService;
import java.io.*;
import java.util.*;
import org.dom4j.DocumentException;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

// Referenced classes of package com.zimbra.cs.backup:
//            Counter, AccountSession, Version, BackupSet, 
//            FileBackupTarget

public class XmlMeta
{
    public static class SignatureData
    {

        public String getName()
        {
            return mName;
        }

        public Map getAttrs()
        {
            return mAttrs;
        }

        private String mName;
        private Map mAttrs;

        public SignatureData(String n, Map a)
        {
            mName = n;
            mAttrs = a;
        }
    }

    public static class DataSourceData
    {

        public String getName()
        {
            return mName;
        }

        public com.zimbra.cs.account.DataSource.Type getType()
        {
            return mType;
        }

        public Map getAttrs()
        {
            return mAttrs;
        }

        private String mName;
        private com.zimbra.cs.account.DataSource.Type mType;
        private Map mAttrs;

        public DataSourceData(String n, com.zimbra.cs.account.DataSource.Type t, Map a)
        {
            mName = n;
            mType = t;
            mAttrs = a;
        }
    }

    public static class IdentityData
    {

        public String getName()
        {
            return mName;
        }

        public Map getAttrs()
        {
            return mAttrs;
        }

        private String mName;
        private Map mAttrs;

        public IdentityData(String n, Map a)
        {
            mName = n;
            mAttrs = a;
        }
    }

    public static class AccountLdapMeta
    {

        public String getId()
        {
            return mZimbraId;
        }

        public String getEmail()
        {
            return mEmail;
        }

        public Map getAttrs()
        {
            return mAttrs;
        }

        public String[] getAliases()
        {
            return mAliases;
        }

        public List getIdentities()
        {
            return mIdentities;
        }

        public List getDataSources()
        {
            return mDataSources;
        }

        public List getSignatures()
        {
            return mSignatures;
        }

        public String[] getDistributionLists()
        {
            return mDistributionLists;
        }

        private String mZimbraId;
        private String mEmail;
        private Map mAttrs;
        private String mAliases[];
        private List mIdentities;
        private List mDataSources;
        private List mSignatures;
        private String mDistributionLists[];

        public AccountLdapMeta(String zimbraId, String email, Map attrs, String aliases[], List identities, List dataSources, List signatures, 
                String distributionLists[])
        {
            mZimbraId = zimbraId;
            mEmail = email;
            mAttrs = attrs;
            mAliases = aliases;
            mIdentities = identities;
            mDataSources = dataSources;
            mSignatures = signatures;
            mDistributionLists = distributionLists;
        }
    }


    public XmlMeta()
    {
    }

    private static void checkVersion(String version)
        throws ServiceException
    {
        Pair ver = Version.parseVersion(version);
        int major = ((Integer)ver.getFirst()).intValue();
        int minor = ((Integer)ver.getSecond()).intValue();
        if(7 != major || 1 < minor)
            throw ServiceException.FAILURE((new StringBuilder()).append("Version ").append(version).append(" is not compatible with current version ").append(Version.current()).toString(), null);
        else
            return;
    }

    public static XMLWriter createWriter(OutputStream os)
        throws UnsupportedEncodingException
    {
        return new XMLWriter(os, new OutputFormat("  ", true, "utf-8"));
    }

    public static void writeAccountMaps(Map acctNameId, Map acctLastSess, File file)
        throws ServiceException
    {
        FileOutputStream fos;
        XMLWriter writer;
        fos = null;
        writer = null;
        try
        {
            fos = new FileOutputStream(file);
            writer = createWriter(fos);
            writer.startDocument();
            AttributesImpl topAttrs = new AttributesImpl();
            topAttrs.addAttribute("", "", "xmlns", null, "urn:zimbraBackupMeta");
            topAttrs.addAttribute("", "", "version", null, Version.current());
            writer.startElement("", "", "backupMetadata", topAttrs);
            writer.startElement("", "", "allAccounts", new AttributesImpl());
            for(Iterator iter = acctNameId.entrySet().iterator(); iter.hasNext(); writer.endElement("", "", "account"))
            {
                java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
                String email = (String)entry.getKey();
                String zimbraId = (String)entry.getValue();
                String fullBackup = (String)acctLastSess.get(zimbraId);
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "", "zimbraId", null, zimbraId);
                attrs.addAttribute("", "", "email", null, email);
                if(fullBackup != null)
                    attrs.addAttribute("", "", "latestFullBackupLabel", null, fullBackup);
                writer.startElement("", "", "account", attrs);
            }

            writer.endElement("", "", "allAccounts");
            writer.endElement("", "", "backupMetadata");
            writer.endDocument();
            writer.println();
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(SAXException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        break MISSING_BLOCK_LABEL_411;
        Exception exception;
        exception;
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        throw exception;
    }

    public static Pair readAccountMaps(File file)
        throws ServiceException
    {
        try
        {
            Map nameIdMap = new HashMap();
            Map idLabelMap = new HashMap();
            String docStr = new String(ByteUtil.getContent(file), "utf-8");
            Element root = Element.parseXML(docStr);
            String version = root.getAttribute("version");
            checkVersion(version);
            Element allAccounts = root.getElement("allAccounts");
            String zimbraId;
            String latestFullBackupLabel;
            for(Iterator iter = allAccounts.elementIterator("account"); iter.hasNext(); idLabelMap.put(zimbraId, latestFullBackupLabel))
            {
                Element account = (Element)iter.next();
                zimbraId = account.getAttribute("zimbraId");
                String email = account.getAttribute("email");
                latestFullBackupLabel = account.getAttribute("latestFullBackupLabel", null);
                nameIdMap.put(email, zimbraId);
            }

            return new Pair(nameIdMap, idLabelMap);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(DocumentException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
    }

    public static void writeBackupSet(FileBackupTarget.FileBackupSet backupSet, File file)
        throws ServiceException
    {
        FileOutputStream fos;
        XMLWriter writer;
        fos = null;
        writer = null;
        try
        {
            fos = new FileOutputStream(file);
            writer = createWriter(fos);
            writer.startDocument();
            AttributesImpl topAttrs = new AttributesImpl();
            topAttrs.addAttribute("", "", "xmlns", null, "urn:zimbraBackupMeta");
            topAttrs.addAttribute("", "", "version", null, Version.current());
            writer.startElement("", "", "backupMetadata", topAttrs);
            AttributesImpl backupSetAttrs = new AttributesImpl();
            backupSetAttrs.addAttribute("", "", "label", null, backupSet.getLabel());
            backupSetAttrs.addAttribute("", "", "zcsRelease", null, backupSet.getZCSRelease());
            backupSetAttrs.addAttribute("", "", "startTime", null, Long.toString(backupSet.getStartTime()));
            backupSetAttrs.addAttribute("", "", "endTime", null, Long.toString(backupSet.getEndTime()));
            backupSetAttrs.addAttribute("", "", "minRedoSeq", null, Long.toString(backupSet.getMinRedoSequence()));
            backupSetAttrs.addAttribute("", "", "maxRedoSeq", null, Long.toString(backupSet.getMaxRedoSequence()));
            backupSetAttrs.addAttribute("", "", "sharedBlobsZipped", null, Boolean.toString(backupSet.sharedBlobsZipped()));
            backupSetAttrs.addAttribute("", "", "sharedBlobsZipNameDigestChars", null, Integer.toString(backupSet.sharedBlobsZipNameDigestChars()));
            backupSetAttrs.addAttribute("", "", "sharedBlobsDirectoryDepth", null, Integer.toString(backupSet.sharedBlobsDirectoryDepth()));
            backupSetAttrs.addAttribute("", "", "sharedBlobsCharsPerDirectory", null, Integer.toString(backupSet.sharedBlobsCharsPerDirectory()));
            backupSetAttrs.addAttribute("", "", "type", null, BackupSet.getTypeLabel(backupSet.getType()));
            if(backupSet.isAborted())
                backupSetAttrs.addAttribute("", "", "aborted", null, Integer.toString(1));
            backupSetAttrs.addAttribute("", "", "accountsDirectoryDepth", null, Integer.toString(backupSet.getAccountDirDepth()));
            writer.startElement("", "", "backupSet", backupSetAttrs);
            String desc = backupSet.getDescription();
            if(desc != null && desc.length() > 0)
            {
                writer.startElement("", "", "desc", new AttributesImpl());
                writer.write(desc);
                writer.endElement("", "", "desc");
            }
            writer.startElement("", "", "accounts", new AttributesImpl());
            Iterator iter = backupSet.getAccountNameIdMap().entrySet().iterator();
            do
            {
                if(!iter.hasNext())
                    break;
                java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();
                String email = (String)entry.getKey();
                String zimbraId = (String)entry.getValue();
                com.zimbra.cs.service.backup.BackupService.AccountBackupStatus status = backupSet.getAccountStatus(email);
                if(status != null)
                {
                    AttributesImpl attrs = new AttributesImpl();
                    attrs.addAttribute("", "", "zimbraId", null, zimbraId);
                    attrs.addAttribute("", "", "email", null, email);
                    attrs.addAttribute("", "", "status", null, status.toString());
                    writer.startElement("", "", "account", attrs);
                    writer.endElement("", "", "account");
                }
            } while(true);
            writer.endElement("", "", "accounts");
            writer.startElement("", "", "stats", new AttributesImpl());
            List stats = backupSet.getStats();
            for(Iterator i$ = stats.iterator(); i$.hasNext(); writer.endElement("", "", "counter"))
            {
                Counter counter = (Counter)i$.next();
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "", "name", null, counter.getName());
                attrs.addAttribute("", "", "numSamples", null, Long.toString(counter.getNumSamples()));
                attrs.addAttribute("", "", "sum", null, Long.toString(counter.getSum()));
                attrs.addAttribute("", "", "unit", null, counter.getUnit());
                writer.startElement("", "", "counter", attrs);
            }

            writer.endElement("", "", "stats");
            writer.startElement("", "", "errors", new AttributesImpl());
            List errors = backupSet.getAllErrors();
            for(Iterator i$ = errors.iterator(); i$.hasNext(); writer.endElement("", "", "error"))
            {
                BackupSet.ErrorInfo error = (BackupSet.ErrorInfo)i$.next();
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "", "email", null, error.getAccountName());
                writer.startElement("", "", "error", attrs);
                writer.startElement("", "", "message", new AttributesImpl());
                writer.write(error.getMessage());
                writer.endElement("", "", "message");
                String stackTrace = error.getStacktrace();
                if(stackTrace != null && stackTrace.length() > 0)
                {
                    writer.startElement("", "", "stackTrace", new AttributesImpl());
                    writer.write(stackTrace);
                    writer.endElement("", "", "stackTrace");
                }
            }

            writer.endElement("", "", "errors");
            writer.endElement("", "", "backupSet");
            writer.endElement("", "", "backupMetadata");
            writer.endDocument();
            writer.println();
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(SAXException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        break MISSING_BLOCK_LABEL_1101;
        Exception exception;
        exception;
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        throw exception;
    }

    public static Element readBackupSet(File file)
        throws ServiceException
    {
        try
        {
            String docStr = new String(ByteUtil.getContent(file), "utf-8");
            Element root = Element.parseXML(docStr);
            String version = root.getAttribute("version");
            checkVersion(version);
            Element backupSet = root.getElement("backupSet");
            return backupSet;
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(DocumentException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
    }

    public static void writeAccountBackup(AccountSession acctSession, File file)
        throws ServiceException
    {
        FileOutputStream fos;
        XMLWriter writer;
        fos = null;
        writer = null;
        try
        {
            fos = new FileOutputStream(file);
            writer = createWriter(fos);
            writeAccountBackup(acctSession, writer);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        break MISSING_BLOCK_LABEL_119;
        Exception exception;
        exception;
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        throw exception;
    }

    public static void writeAccountBackup(AccountSession acctSession, XMLWriter writer)
        throws ServiceException
    {
        try
        {
            writer.startDocument();
            AttributesImpl topAttrs = new AttributesImpl();
            topAttrs.addAttribute("", "", "xmlns", null, "urn:zimbraBackupMeta");
            topAttrs.addAttribute("", "", "version", null, Version.current());
            writer.startElement("", "", "backupMetadata", topAttrs);
            AttributesImpl backupSetAttrs = new AttributesImpl();
            backupSetAttrs.addAttribute("", "", "zimbraId", null, acctSession.getAccountId());
            backupSetAttrs.addAttribute("", "", "email", null, acctSession.getAccountName());
            backupSetAttrs.addAttribute("", "", "mailboxId", null, Integer.toString(acctSession.getMailboxId()));
            backupSetAttrs.addAttribute("", "", "startTime", null, Long.toString(acctSession.getStartTime()));
            backupSetAttrs.addAttribute("", "", "endTime", null, Long.toString(acctSession.getEndTime()));
            backupSetAttrs.addAttribute("", "", "redoSeq", null, Long.toString(acctSession.getRedoLogFileSequence()));
            backupSetAttrs.addAttribute("", "", "server", null, acctSession.getServer());
            if(acctSession.blobsZipped())
                backupSetAttrs.addAttribute("", "", "blobsZipped", null, Boolean.toString(acctSession.blobsZipped()));
            if(acctSession.isBlobCompressedDeprecated())
                backupSetAttrs.addAttribute("", "", "blobsCompressed", null, Boolean.toString(acctSession.isBlobCompressedDeprecated()));
            if(acctSession.isAccountOnly())
                backupSetAttrs.addAttribute("", "", "accountOnly", null, Boolean.toString(acctSession.isAccountOnly()));
            writer.startElement("", "", "accountBackup", backupSetAttrs);
            writer.startElement("", "", "volumes", new AttributesImpl());
            for(Iterator iter = acctSession.getVolumeInfoIterator(); iter.hasNext(); writer.endElement("", "", "volume"))
            {
                AccountSession.VolumeInfo vol = (AccountSession.VolumeInfo)iter.next();
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "", "id", null, Short.toString(vol.getId()));
                attrs.addAttribute("", "", "path", null, vol.getMailboxPath());
                if(vol.isSecondary())
                    attrs.addAttribute("", "", "isSecondary", null, Boolean.toString(vol.isSecondary()));
                writer.startElement("", "", "volume", attrs);
            }

            writer.endElement("", "", "volumes");
            writer.endElement("", "", "accountBackup");
            writer.endElement("", "", "backupMetadata");
            writer.endDocument();
            writer.println();
            writer.flush();
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE("Unable to save XML file", e);
        }
        catch(SAXException e)
        {
            throw ServiceException.FAILURE("Unable to save XML file", e);
        }
    }

    public static Element readAccountBackup(File file)
        throws ServiceException
    {
        try
        {
            String docStr = new String(ByteUtil.getContent(file), "utf-8");
            Element root = Element.parseXML(docStr);
            String version = root.getAttribute("version");
            checkVersion(version);
            Element accountBackup = root.getElement("accountBackup");
            return accountBackup;
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(DocumentException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
    }

    public static void writeAccountLdap(Account account, String aliases[], List identities, List dataSources, List signatures, String distributionLists[], File file)
        throws ServiceException
    {
        FileOutputStream fos;
        XMLWriter writer;
        fos = null;
        writer = null;
        try
        {
            fos = new FileOutputStream(file);
            writer = createWriter(fos);
            writer.startDocument();
            AttributesImpl topAttrs = new AttributesImpl();
            topAttrs.addAttribute("", "", "xmlns", null, "urn:zimbraBackupMeta");
            topAttrs.addAttribute("", "", "version", null, Version.current());
            writer.startElement("", "", "backupMetadata", topAttrs);
            AttributesImpl acctLdapAttrs = new AttributesImpl();
            acctLdapAttrs.addAttribute("", "", "zimbraId", null, account.getId());
            acctLdapAttrs.addAttribute("", "", "email", null, account.getName());
            writer.startElement("", "", "accountLdap", acctLdapAttrs);
            writeLdapEntryAttributes(account.getAttrs(false), writer);
            if(aliases != null && aliases.length > 0)
            {
                writer.startElement("", "", "aliases", new AttributesImpl());
                String arr$[] = aliases;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    String alias = arr$[i$];
                    AttributesImpl aliasAttrs = new AttributesImpl();
                    aliasAttrs.addAttribute("", "", "email", null, alias);
                    writer.startElement("", "", "alias", aliasAttrs);
                    writer.endElement("", "", "alias");
                }

                writer.endElement("", "", "aliases");
            }
            if(identities != null && identities.size() > 0)
            {
                writer.startElement("", "", "identities", new AttributesImpl());
                for(Iterator i$ = identities.iterator(); i$.hasNext(); writer.endElement("", "", "identity"))
                {
                    Identity identity = (Identity)i$.next();
                    AttributesImpl identityAttrs = new AttributesImpl();
                    identityAttrs.addAttribute("", "", "name", null, identity.getName());
                    writer.startElement("", "", "identity", identityAttrs);
                    writeLdapEntryAttributes(identity.getAttrs(false), writer);
                }

                writer.endElement("", "", "identities");
            }
            if(dataSources != null && dataSources.size() > 0)
            {
                writer.startElement("", "", "dataSources", new AttributesImpl());
                for(Iterator i$ = dataSources.iterator(); i$.hasNext(); writer.endElement("", "", "dataSource"))
                {
                    DataSource ds = (DataSource)i$.next();
                    AttributesImpl dsAttrs = new AttributesImpl();
                    dsAttrs.addAttribute("", "", "name", null, ds.getName());
                    dsAttrs.addAttribute("", "", "type", null, ds.getType().toString());
                    writer.startElement("", "", "dataSource", dsAttrs);
                    writeLdapEntryAttributes(ds.getAttrs(false), writer);
                }

                writer.endElement("", "", "dataSources");
            }
            if(signatures != null && signatures.size() > 0)
            {
                writer.startElement("", "", "signatures", new AttributesImpl());
                for(Iterator i$ = signatures.iterator(); i$.hasNext(); writer.endElement("", "", "signature"))
                {
                    Signature sig = (Signature)i$.next();
                    AttributesImpl sigAttrs = new AttributesImpl();
                    sigAttrs.addAttribute("", "", "name", null, sig.getName());
                    writer.startElement("", "", "signature", sigAttrs);
                    writeLdapEntryAttributes(sig.getAttrs(false), writer);
                }

                writer.endElement("", "", "signatures");
            }
            if(distributionLists != null && distributionLists.length > 0)
            {
                writer.startElement("", "", "distributionLists", new AttributesImpl());
                String arr$[] = distributionLists;
                int len$ = arr$.length;
                for(int i$ = 0; i$ < len$; i$++)
                {
                    String dlEmail = arr$[i$];
                    AttributesImpl dsAttrs = new AttributesImpl();
                    dsAttrs.addAttribute("", "", "email", null, dlEmail);
                    writer.startElement("", "", "distributionList", dsAttrs);
                    writer.endElement("", "", "distributionList");
                }

                writer.endElement("", "", "distributionLists");
            }
            writer.endElement("", "", "accountLdap");
            writer.endElement("", "", "backupMetadata");
            writer.endDocument();
            writer.println();
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(SAXException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        break MISSING_BLOCK_LABEL_978;
        Exception exception;
        exception;
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        throw exception;
    }

    public static AccountLdapMeta readAccountLdap(File file)
        throws ServiceException
    {
        try
        {
            String docStr = new String(ByteUtil.getContent(file), "utf-8");
            Element root = Element.parseXML(docStr);
            String version = root.getAttribute("version");
            checkVersion(version);
            Element acctLdapElem = root.getElement("accountLdap");
            String zimbraId = acctLdapElem.getAttribute("zimbraId");
            String email = acctLdapElem.getAttribute("email");
            Element acctAttrsElem = acctLdapElem.getElement("attributes");
            Map acctAttrs = readLdapEntryAttributes(acctAttrsElem);
            Element aliasesElem = acctLdapElem.getOptionalElement("aliases");
            String aliases[] = null;
            if(aliasesElem != null)
            {
                List aliasList = new ArrayList();
                Element aliasElem;
                for(Iterator iter = aliasesElem.elementIterator("alias"); iter.hasNext(); aliasList.add(aliasElem.getAttribute("email")))
                    aliasElem = (Element)iter.next();

                aliases = new String[aliasList.size()];
                aliases = (String[])aliasList.toArray(aliases);
            }
            Element identitiesElem = acctLdapElem.getOptionalElement("identities");
            List identities = null;
            if(identitiesElem != null)
            {
                identities = new ArrayList();
                IdentityData iden;
                for(Iterator iter = identitiesElem.elementIterator("identity"); iter.hasNext(); identities.add(iden))
                {
                    Element identityElem = (Element)iter.next();
                    String name = identityElem.getAttribute("name");
                    Map idenAttrs = null;
                    Element idenAttrsElem = identityElem.getOptionalElement("attributes");
                    if(idenAttrsElem != null)
                        idenAttrs = readLdapEntryAttributes(idenAttrsElem);
                    iden = new IdentityData(name, idenAttrs);
                }

            }
            Element dataSourcesElem = acctLdapElem.getOptionalElement("dataSources");
            List dataSources = null;
            if(dataSourcesElem != null)
            {
                dataSources = new ArrayList();
                DataSourceData iden;
                for(Iterator iter = dataSourcesElem.elementIterator("dataSource"); iter.hasNext(); dataSources.add(iden))
                {
                    Element dataSourceElem = (Element)iter.next();
                    String name = dataSourceElem.getAttribute("name");
                    com.zimbra.cs.account.DataSource.Type dsType = com.zimbra.cs.account.DataSource.Type.fromString(dataSourceElem.getAttribute("type"));
                    Map dsAttrs = null;
                    Element dsAttrsElem = dataSourceElem.getOptionalElement("attributes");
                    if(dsAttrsElem != null)
                        dsAttrs = readLdapEntryAttributes(dsAttrsElem);
                    iden = new DataSourceData(name, dsType, dsAttrs);
                }

            }
            Element signaturesElem = acctLdapElem.getOptionalElement("signatures");
            List signatures = null;
            if(signaturesElem != null)
            {
                signatures = new ArrayList();
                SignatureData sig;
                for(Iterator iter = signaturesElem.elementIterator("signature"); iter.hasNext(); signatures.add(sig))
                {
                    Element signatureElem = (Element)iter.next();
                    String name = signatureElem.getAttribute("name");
                    Map sigAttrs = null;
                    Element sigAttrsElem = signatureElem.getOptionalElement("attributes");
                    if(sigAttrsElem != null)
                        sigAttrs = readLdapEntryAttributes(sigAttrsElem);
                    sig = new SignatureData(name, sigAttrs);
                }

            }
            Element distributionListsElem = acctLdapElem.getOptionalElement("distributionLists");
            String distributionLists[] = null;
            if(distributionListsElem != null)
            {
                List dlList = new ArrayList();
                Element dlElem;
                for(Iterator iter = distributionListsElem.elementIterator("distributionList"); iter.hasNext(); dlList.add(dlElem.getAttribute("email")))
                    dlElem = (Element)iter.next();

                distributionLists = new String[dlList.size()];
                distributionLists = (String[])dlList.toArray(distributionLists);
            }
            return new AccountLdapMeta(zimbraId, email, acctAttrs, aliases, identities, dataSources, signatures, distributionLists);
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(DocumentException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
    }

    private static Map readLdapEntryAttributes(Element attrsElem)
        throws ServiceException
    {
        Map attrs = new HashMap();
        Iterator iter = attrsElem.elementIterator("attribute");
        do
        {
            if(!iter.hasNext())
                break;
            Element attrElem = (Element)iter.next();
            String name = attrElem.getAttribute("name");
            List vals = new ArrayList();
            String lastVal = null;
            int num = 0;
            for(Iterator valIter = attrElem.elementIterator("value"); valIter.hasNext();)
            {
                Element valElem = (Element)valIter.next();
                String value = valElem.getText();
                vals.add(value);
                lastVal = value;
                num++;
            }

            if(num == 1)
                attrs.put(name, lastVal);
            else
            if(num > 1)
            {
                String valArray[] = new String[num];
                valArray = (String[])vals.toArray(valArray);
                attrs.put(name, valArray);
            }
        } while(true);
        return attrs;
    }

    private static void writeLdapEntryAttributes(Map attrs, XMLWriter writer)
        throws SAXException, IOException
    {
        writer.startElement("", "", "attributes", new AttributesImpl());
        for(Iterator iter = attrs.entrySet().iterator(); iter.hasNext(); writer.endElement("", "", "attribute"))
        {
            java.util.Map.Entry mapEntry = (java.util.Map.Entry)iter.next();
            String name = (String)mapEntry.getKey();
            AttributesImpl nameAttrs = new AttributesImpl();
            nameAttrs.addAttribute("", "", "name", null, name);
            writer.startElement("", "", "attribute", nameAttrs);
            Object obj = mapEntry.getValue();
            if(obj instanceof String)
            {
                String value = (String)obj;
                writer.startElement("", "", "value", new AttributesImpl());
                writer.write(value);
                writer.endElement("", "", "value");
                continue;
            }
            String vals[] = (String[])(String[])obj;
            for(int i = 0; i < vals.length; i++)
            {
                writer.startElement("", "", "value", new AttributesImpl());
                writer.write(vals[i]);
                writer.endElement("", "", "value");
            }

        }

        writer.endElement("", "", "attributes");
    }

    public static void writeTablesSchema(List tableInfos, File file)
        throws ServiceException
    {
        FileOutputStream fos;
        XMLWriter writer;
        fos = null;
        writer = null;
        try
        {
            fos = new FileOutputStream(file);
            writer = createWriter(fos);
            writer.startDocument();
            AttributesImpl topAttrs = new AttributesImpl();
            topAttrs.addAttribute("", "", "xmlns", null, "urn:zimbraBackupMeta");
            topAttrs.addAttribute("", "", "version", null, Version.current());
            writer.startElement("", "", "backupMetadata", topAttrs);
            writer.startElement("", "", "tables", new AttributesImpl());
            for(Iterator i$ = tableInfos.iterator(); i$.hasNext(); writer.endElement("", "", "table"))
            {
                com.zimbra.cs.db.DbBackup.TableInfo tinfo = (com.zimbra.cs.db.DbBackup.TableInfo)i$.next();
                AttributesImpl tattrs = new AttributesImpl();
                tattrs.addAttribute("", "", "name", null, tinfo.getName());
                writer.startElement("", "", "table", tattrs);
                List columnInfos = tinfo.getColumns();
                for(Iterator i$ = columnInfos.iterator(); i$.hasNext(); writer.endElement("", "", "column"))
                {
                    com.zimbra.cs.db.DbBackup.ColumnInfo cinfo = (com.zimbra.cs.db.DbBackup.ColumnInfo)i$.next();
                    AttributesImpl cattrs = new AttributesImpl();
                    cattrs.addAttribute("", "", "position", null, Integer.toString(cinfo.getPosition()));
                    cattrs.addAttribute("", "", "name", null, cinfo.getName());
                    cattrs.addAttribute("", "", "type", null, cinfo.getType());
                    cattrs.addAttribute("", "", "nullable", null, Boolean.toString(cinfo.isNullable()));
                    String defVal = cinfo.getDefaultValue();
                    if(defVal != null)
                        cattrs.addAttribute("", "", "default", null, defVal);
                    writer.startElement("", "", "column", cattrs);
                }

            }

            writer.endElement("", "", "tables");
            writer.endElement("", "", "backupMetadata");
            writer.endDocument();
            writer.println();
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(SAXException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        break MISSING_BLOCK_LABEL_496;
        Exception exception;
        exception;
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        throw exception;
    }

    public static Map readTableSchema(File file)
        throws ServiceException
    {
        Map tableInfos = new HashMap();
        try
        {
            String docStr = new String(ByteUtil.getContent(file), "utf-8");
            Element root = Element.parseXML(docStr);
            String version = root.getAttribute("version");
            checkVersion(version);
            Element tablesElem = root.getElement("tables");
            com.zimbra.cs.db.DbBackup.TableInfo tableInfo;
            String tableNameWithoutDbPrefix;
            for(Iterator tablesIter = tablesElem.elementIterator("table"); tablesIter.hasNext(); tableInfos.put(tableNameWithoutDbPrefix, tableInfo))
            {
                Element tableElem = (Element)tablesIter.next();
                String tableName = tableElem.getAttribute("name");
                List colInfos = new ArrayList();
                com.zimbra.cs.db.DbBackup.ColumnInfo colInfo;
                for(Iterator colsIter = tableElem.elementIterator("column"); colsIter.hasNext(); colInfos.add(colInfo))
                {
                    Element colElem = (Element)colsIter.next();
                    int pos = (int)colElem.getAttributeLong("position");
                    String name = colElem.getAttribute("name");
                    String type = colElem.getAttribute("type");
                    boolean nullable = colElem.getAttributeBool("nullable", true);
                    String defaultVal = colElem.getAttribute("default", null);
                    colInfo = new com.zimbra.cs.db.DbBackup.ColumnInfo(name, pos, type, nullable, defaultVal);
                }

                tableInfo = new com.zimbra.cs.db.DbBackup.TableInfo(tableName, colInfos);
                tableNameWithoutDbPrefix = DbBackup.removeDatabasePrefix(tableName);
            }

            return tableInfos;
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(DocumentException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
    }

    public static void writeRestorePathCache(short volumeId, Map digestPathMap, File file)
        throws ServiceException
    {
        FileOutputStream fos;
        XMLWriter writer;
        fos = null;
        writer = null;
        try
        {
            fos = new FileOutputStream(file);
            writer = createWriter(fos);
            writer.startDocument();
            AttributesImpl topAttrs = new AttributesImpl();
            topAttrs.addAttribute("", "", "xmlns", null, "urn:zimbraBackupMeta");
            topAttrs.addAttribute("", "", "version", null, Version.current());
            writer.startElement("", "", "backupMetadata", topAttrs);
            AttributesImpl sharedBlobsAttrs = new AttributesImpl();
            sharedBlobsAttrs.addAttribute("", "", "volumeId", null, Short.toString(volumeId));
            writer.startElement("", "", "sharedBlobs", sharedBlobsAttrs);
            for(Iterator i$ = digestPathMap.entrySet().iterator(); i$.hasNext(); writer.endElement("", "", "blob"))
            {
                java.util.Map.Entry blob = (java.util.Map.Entry)i$.next();
                String digest = (String)blob.getKey();
                String path = (String)blob.getValue();
                AttributesImpl blobAttrs = new AttributesImpl();
                blobAttrs.addAttribute("", "", "digest", null, digest);
                blobAttrs.addAttribute("", "", "path", null, path);
                writer.startElement("", "", "blob", blobAttrs);
            }

            writer.endElement("", "", "sharedBlobs");
            writer.endElement("", "", "backupMetadata");
            writer.endDocument();
            writer.println();
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(SAXException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to save XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        break MISSING_BLOCK_LABEL_399;
        Exception exception;
        exception;
        if(writer != null)
            try
            {
                writer.close();
            }
            catch(IOException e) { }
        else
        if(fos != null)
            try
            {
                fos.close();
            }
            catch(IOException e) { }
        throw exception;
    }

    public static Map readRestorePathCache(File file)
        throws ServiceException
    {
        Map digestPathMap = new HashMap();
        try
        {
            String docStr = new String(ByteUtil.getContent(file), "utf-8");
            Element root = Element.parseXML(docStr);
            String version = root.getAttribute("version");
            checkVersion(version);
            Element sharedBlobsElem = root.getElement("sharedBlobs");
            String digest;
            String path;
            for(Iterator iter = sharedBlobsElem.elementIterator("blob"); iter.hasNext(); digestPathMap.put(digest, path))
            {
                Element blobElem = (Element)iter.next();
                digest = blobElem.getAttribute("digest");
                path = blobElem.getAttribute("path");
            }

            return digestPathMap;
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
        catch(DocumentException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("Unable to parse XML file ").append(file.getAbsolutePath()).toString(), e);
        }
    }

    private static final String NAMESPACE = "urn:zimbraBackupMeta";
}
