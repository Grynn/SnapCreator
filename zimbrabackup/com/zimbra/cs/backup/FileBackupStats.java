// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   FileBackupStats.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.MetadataList;
import java.util.*;

// Referenced classes of package com.zimbra.cs.backup:
//            Counter

public class FileBackupStats
{

    private static String getUnit(String name)
    {
        if(name.endsWith("time"))
            return "ms";
        if(name.endsWith("bytes"))
            return "bytes";
        else
            return "unknown";
    }

    private Counter getCounter(String name)
    {
        int pos = 0;
        if(name.equals(sNames[pos++]))
            return mTotalTime;
        if(name.equals(sNames[pos++]))
            return mSysDbTime;
        if(name.equals(sNames[pos++]))
            return mSysDbBytes;
        if(name.equals(sNames[pos++]))
            return mRedologsTime;
        if(name.equals(sNames[pos++]))
            return mRedologsBytes;
        if(name.equals(sNames[pos++]))
            return mAccountsTime;
        if(name.equals(sNames[pos++]))
            return mLdapTime;
        if(name.equals(sNames[pos++]))
            return mLdapBytes;
        if(name.equals(sNames[pos++]))
            return mDbTime;
        if(name.equals(sNames[pos++]))
            return mDbBytes;
        if(name.equals(sNames[pos++]))
            return mDbDigestMapTime;
        if(name.equals(sNames[pos++]))
            return mMsgsTime;
        if(name.equals(sNames[pos++]))
            return mMsgCopyTime;
        if(name.equals(sNames[pos++]))
            return mMsgCopyBytes;
        if(name.equals(sNames[pos++]))
            return mMsgLinkTime;
        if(name.equals(sNames[pos++]))
            return mIndexTime;
        if(name.equals(sNames[pos++]))
            return mIndexBytes;
        for(Iterator i$ = mUnknownCounters.iterator(); i$.hasNext();)
        {
            Counter counter = (Counter)i$.next();
            if(name.equals(counter.getName()))
                return counter;
        }

        throw new IllegalArgumentException((new StringBuilder()).append("No such counter ").append(name).toString());
    }

    public void setCounter(String name, Counter counter)
    {
        int pos = 0;
        if(name.equals(sNames[pos++]))
            mTotalTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mSysDbTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mSysDbBytes = counter;
        else
        if(name.equals(sNames[pos++]))
            mRedologsTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mRedologsBytes = counter;
        else
        if(name.equals(sNames[pos++]))
            mAccountsTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mLdapTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mLdapBytes = counter;
        else
        if(name.equals(sNames[pos++]))
            mDbTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mDbBytes = counter;
        else
        if(name.equals(sNames[pos++]))
            mDbDigestMapTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mMsgsTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mMsgCopyTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mMsgCopyBytes = counter;
        else
        if(name.equals(sNames[pos++]))
            mMsgLinkTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mIndexTime = counter;
        else
        if(name.equals(sNames[pos++]))
            mIndexBytes = counter;
        else
            mUnknownCounters.add(counter);
    }

    public FileBackupStats()
    {
        mUnknownCounters = new ArrayList();
        String arr$[] = sNames;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            String name = arr$[i$];
            Counter counter = new Counter(name, getUnit(name), 0L, 0L);
            setCounter(name, counter);
        }

    }

    private void decodeCounterMetadata(Metadata meta)
        throws ServiceException
    {
        String name = meta.get("name");
        String unit = meta.get("unit");
        long sum = meta.getLong("sum");
        long numSamples = meta.getLong("smpl");
        Counter counter = new Counter(name, unit, sum, numSamples);
        setCounter(name, counter);
    }

    private static Metadata encodeCounterMetadata(Counter counter)
    {
        Metadata meta = new Metadata();
        meta.put("name", counter.getName());
        meta.put("unit", counter.getUnit());
        meta.put("sum", counter.getSum());
        meta.put("smpl", counter.getNumSamples());
        return meta;
    }

    public List toList()
    {
        List list = new ArrayList(sNames.length);
        String arr$[] = sNames;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            String name = arr$[i$];
            Counter counter = getCounter(name);
            if(counter != null)
                list.add(counter);
        }

        list.addAll(mUnknownCounters);
        return list;
    }

    public MetadataList encodeMetadataList()
    {
        MetadataList metaList = new MetadataList();
        String arr$[] = sNames;
        int len$ = arr$.length;
        for(int i$ = 0; i$ < len$; i$++)
        {
            String name = arr$[i$];
            Counter counter = getCounter(name);
            if(counter != null)
                metaList.add(encodeCounterMetadata(counter));
        }

        Counter counter;
        for(Iterator i$ = mUnknownCounters.iterator(); i$.hasNext(); metaList.add(encodeCounterMetadata(counter)))
            counter = (Counter)i$.next();

        return metaList;
    }

    public void decodeMetadataList(MetadataList metaList)
        throws ServiceException
    {
        int size = metaList.size();
        for(int i = 0; i < size; i++)
        {
            Metadata meta = metaList.getMap(i);
            decodeCounterMetadata(meta);
        }

    }

    Counter mTotalTime;
    Counter mSysDbTime;
    Counter mSysDbBytes;
    Counter mRedologsTime;
    Counter mRedologsBytes;
    Counter mAccountsTime;
    Counter mLdapTime;
    Counter mLdapBytes;
    Counter mDbTime;
    Counter mDbBytes;
    Counter mDbDigestMapTime;
    Counter mMsgsTime;
    Counter mMsgCopyTime;
    Counter mMsgCopyBytes;
    Counter mMsgLinkTime;
    Counter mIndexTime;
    Counter mIndexBytes;
    private List mUnknownCounters;
    private static String sNames[] = {
        "total_time", "sysdb_time", "sysdb_bytes", "redologs_time", "redologs_bytes", "accounts_time", "ldap_time", "ldap_bytes", "db_time", "db_bytes", 
        "db_digest_map_load_time", "msgs_time", "msg_copy_time", "msg_copy_bytes", "msg_link_time", "index_time", "index_bytes"
    };
    private static final String MF_NAME = "name";
    private static final String MF_UNIT = "unit";
    private static final String MF_SUM = "sum";
    private static final String MF_SAMPLES = "smpl";

}
