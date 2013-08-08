// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   GetVersion.java

package com.zimbra.cs.backup.util;

import com.zimbra.cs.backup.Version;
import java.io.PrintStream;

public class GetVersion
{

    public GetVersion()
    {
    }

    public static void main(String args[])
    {
        System.out.println(Version.current());
    }
}
