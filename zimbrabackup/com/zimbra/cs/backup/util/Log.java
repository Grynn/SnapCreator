// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Log.java

package com.zimbra.cs.backup.util;

import com.zimbra.common.util.LogFactory;

public class Log
{

    public Log()
    {
    }

    public static final com.zimbra.common.util.Log mboxmove = LogFactory.getLog("zimbra.mboxmove");
    public static final com.zimbra.common.util.Log backup = LogFactory.getLog("zimbra.backup");

}
