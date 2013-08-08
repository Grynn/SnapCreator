// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   ZipUtil.java

package com.zimbra.cs.backup.util;

import java.io.*;
import java.util.zip.CRC32;

public class ZipUtil
{

    public ZipUtil()
    {
    }

    public static long computeCRC32(File file)
        throws IOException
    {
        byte buf[];
        CRC32 crc;
        FileInputStream fis;
        buf = new byte[32768];
        crc = new CRC32();
        crc.reset();
        fis = null;
        long l;
        fis = new FileInputStream(file);
        int bytesRead;
        while((bytesRead = fis.read(buf)) != -1) 
            crc.update(buf, 0, bytesRead);
        l = crc.getValue();
        if(fis != null)
            try
            {
                fis.close();
            }
            catch(IOException e) { }
        return l;
        Exception exception;
        exception;
        if(fis != null)
            try
            {
                fis.close();
            }
            catch(IOException e) { }
        throw exception;
    }
}
