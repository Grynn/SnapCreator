// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupMetadataDump.java

package com.zimbra.cs.backup.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.mailbox.Metadata;
import java.io.*;

public class BackupMetadataDump
{

    public BackupMetadataDump()
    {
    }

    private static String getUsage()
    {
        return "Usage: BackupMetadataDump <backup metadata file>";
    }

    public static Metadata loadMetadataFromFile(File file)
        throws IOException, ServiceException
    {
        RandomAccessFile raf = null;
        Metadata metadata;
        raf = new RandomAccessFile(file, "r");
        String magic = raf.readUTF();
        if(!"ZM_BACKUP".equals(magic))
            throw new IOException("invalid backup file");
        int major = raf.readInt();
        int minor = raf.readInt();
        System.out.println((new StringBuilder()).append("Version: ").append(major).append(".").append(minor).toString());
        String encoded = ByteUtil.readUTF8(raf);
        metadata = new Metadata(encoded);
        if(raf != null)
            raf.close();
        return metadata;
        Exception exception;
        exception;
        if(raf != null)
            raf.close();
        throw exception;
    }

    public static void main(String args[])
        throws Exception
    {
        CliUtil.toolSetup();
        if(args.length < 1)
        {
            System.err.println(getUsage());
            System.exit(1);
        }
        File file = new File(args[0]);
        Metadata meta = loadMetadataFromFile(file);
        String pretty = meta.prettyPrint();
        System.out.println(pretty);
    }
}
