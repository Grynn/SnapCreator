// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   MailboxMoveServiceException.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MailboxMoveServiceException extends ServiceException
{

    private MailboxMoveServiceException(String message, String code, boolean isReceiversFault, Throwable cause)
    {
        super(message, code, isReceiversFault, cause, new com.zimbra.common.service.ServiceException.Argument[0]);
    }

    public static MailboxMoveServiceException ALREADY_BEING_MOVED_OUT(String accountName, String destHostName, long startedAt)
    {
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
        String dateStr = fmt.format(new Date(startedAt));
        return new MailboxMoveServiceException((new StringBuilder()).append("Account ").append(accountName).append(" is already being moved to server ").append(destHostName).append("; move started at ").append(dateStr).toString(), "mboxmove.ALREADY_BEING_MOVED_OUT", false, null);
    }

    public static MailboxMoveServiceException ALREADY_BEING_MOVED_IN(String accountName, String srcHostName, long startedAt)
    {
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT);
        String dateStr = fmt.format(new Date(startedAt));
        return new MailboxMoveServiceException((new StringBuilder()).append("Account ").append(accountName).append(" is already being moved from server ").append(srcHostName).append("; move started at ").append(dateStr).toString(), "mboxmove.ALREADY_BEING_MOVED_IN", false, null);
    }

    public static MailboxMoveServiceException NO_SUCH_MOVE_OUT(String accountName, String destHostName)
    {
        return new MailboxMoveServiceException((new StringBuilder()).append("Account ").append(accountName).append(" is not currently being moved to server ").append(destHostName).toString(), "mboxmove.NO_SUCH_MOVE_OUT", false, null);
    }

    public static MailboxMoveServiceException NO_SUCH_MOVE_IN(String accountName, String srcHostName)
    {
        return new MailboxMoveServiceException((new StringBuilder()).append("Account ").append(accountName).append(" is not currently being moved from server ").append(srcHostName).toString(), "mboxmove.NO_SUCH_MOVE_IN", false, null);
    }

    public static MailboxMoveServiceException CANNOT_MOVE_TO_OLDER_SERVER(String accountName, String msg)
    {
        return new MailboxMoveServiceException((new StringBuilder()).append("Cannot move ").append(accountName).append(" to a server with an older version: ").append(msg).toString(), "mboxmove.CANNOT_MOVE_TO_OLDER_SERVER", false, null);
    }

    public static final String ALREADY_BEING_MOVED_OUT = "mboxmove.ALREADY_BEING_MOVED_OUT";
    public static final String ALREADY_BEING_MOVED_IN = "mboxmove.ALREADY_BEING_MOVED_IN";
    public static final String NO_SUCH_MOVE_OUT = "mboxmove.NO_SUCH_MOVE_OUT";
    public static final String NO_SUCH_MOVE_IN = "mboxmove.NO_SUCH_MOVE_IN";
    public static final String CANNOT_MOVE_TO_OLDER_SERVER = "mboxmove.CANNOT_MOVE_TO_OLDER_SERVER";
    private static String DATE_FORMAT = "EEE, yyyy/MM/dd HH:mm:ss.SSS z";

}
