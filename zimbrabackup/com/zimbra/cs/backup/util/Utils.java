// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Utils.java

package com.zimbra.cs.backup.util;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.backup.*;
import com.zimbra.cs.redolog.logger.FileHeader;
import com.zimbra.cs.redolog.logger.FileLogReader;
import com.zimbra.znative.IO;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.naming.NameNotFoundException;

// Referenced classes of package com.zimbra.cs.backup.util:
//            Log

public class Utils
{
    public static interface Replacement
    {

        public abstract String getValue(String s);
    }


    public Utils()
    {
    }

    public static boolean refreshAccount(Account acct)
        throws ServiceException
    {
        try
        {
            Provisioning.getInstance().reload(acct);
            return true;
        }
        catch(ServiceException e)
        {
            Throwable t = e.getCause();
            if(t instanceof NameNotFoundException)
                return false;
            else
                throw e;
        }
    }

    public static IOException IOException(String msg, Throwable e)
    {
        IOException ioe = new IOException(msg);
        ioe.initCause(e);
        return ioe;
    }

    public static File fixDbDumpFile(File file, Map replMap)
        throws IOException, ServiceException
    {
        File newFile;
        BufferedReader in;
        Writer out;
        File tempDir = BackupManager.getInstance().getRestoreCacheDir();
        File dir = new File(tempDir, "temp");
        if(!dir.exists())
            dir.mkdirs();
        try
        {
            newFile = File.createTempFile("dbfix", null, dir);
        }
        catch(IOException e)
        {
            String msg = (new StringBuilder()).append("Unable to create temp file under ").append(tempDir.getAbsolutePath()).append("; make sure the directory exists and is writable, or set localconfig key ").append(BackupLC.backup_restore_cache_dir.key()).append(" to a writable location").toString();
            IOException ioe = new IOException(msg);
            ioe.initCause(e);
            throw ioe;
        }
        in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        out = new OutputStreamWriter(new FileOutputStream(newFile), "UTF-8");
        File file1;
        for(String line = null; (line = fixLine(in, replMap)) != null;)
        {
            out.write(line);
            out.write(10);
        }

        out.flush();
        file1 = newFile;
        try
        {
            in.close();
        }
        catch(IOException e) { }
        out.close();
        return file1;
        Exception exception;
        exception;
        try
        {
            in.close();
        }
        catch(IOException e) { }
        out.close();
        throw exception;
    }

    private static String fixLine(BufferedReader in, Map replMap)
        throws IOException
    {
        StringBuilder buf = new StringBuilder();
        StringBuilder seg = new StringBuilder();
        int colIndex = 0;
        do
        {
            int c;
            if((c = in.read()) == -1)
                break;
            if(c == 34)
            {
                seg.append((char)c);
                String qt = consumeQuotedText(in);
                seg.append(qt);
                continue;
            }
            if(c == 44 || c == 10)
            {
                String val = seg.toString();
                String replVal = val;
                Replacement repl = (Replacement)replMap.get(Integer.valueOf(colIndex));
                if(repl != null)
                    replVal = repl.getValue(val);
                buf.append(replVal);
                if(c == 10)
                    break;
                buf.append((char)c);
                colIndex++;
                seg.setLength(0);
            } else
            {
                seg.append((char)c);
            }
        } while(true);
        if(buf.length() > 0)
            return buf.toString();
        else
            return null;
    }

    private static String consumeQuotedText(BufferedReader in)
        throws IOException
    {
        StringBuilder buf = new StringBuilder();
        int c;
        for(boolean esc = false; (c = in.read()) != -1 && (esc || c != 34); buf.append((char)c))
        {
            if(esc)
            {
                esc = false;
                continue;
            }
            if(c == 92)
                esc = true;
        }

        if(c == -1)
        {
            throw new IOException("unterminated quoted string");
        } else
        {
            buf.append((char)c);
            return buf.toString();
        }
    }

    public static void link(File newFile, File existingFile)
        throws IOException
    {
        Log.backup.debug((new StringBuilder()).append("linking ").append(newFile.getAbsolutePath()).append(" (new) to ").append(existingFile.getAbsolutePath()).append(" (existing)").toString());
        if(newFile.exists())
            newFile.delete();
        IO.link(existingFile.getAbsolutePath(), newFile.getAbsolutePath());
    }

    public static long parseDate(String val)
        throws ParseException
    {
        Date d = null;
        if(val.indexOf(':') != -1)
            d = sDateFormat.parse(val);
        else
            d = sDateFormat2.parse(val);
        return d.getTime();
    }

    public static Pair splitRedoLogsAtSeq(File logs[], long seqSplit)
        throws IOException, ServiceException
    {
        List errorList = new ArrayList();
        int indexLastOld = -1;
        long seqLast = -1L;
        long seqMin = 0xffffffffL;
        long seqMax = 0x0L;
        for(int i = 0; i < logs.length; i++)
        {
            FileLogReader reader = new FileLogReader(logs[i]);
            long seq = reader.getHeader().getSequence();
            if(i == 0)
                if(seq > seqSplit && seqSplit > 0L)
                    seqLast = seqSplit;
                else
                    seqLast = seq - 1L;
            seqMin = Math.min(seqMin, seq);
            seqMax = Math.max(seqMax, seq);
            if(seq <= seqSplit)
                indexLastOld = i;
            if(seq != seqLast + 1L)
            {
                long minMissing = seqLast + 1L;
                long maxMissing = seq - 1L;
                if(maxMissing > seqSplit)
                {
                    BackupServiceException e = BackupServiceException.REDOLOG_OUT_OF_SEQUENCE(minMissing, maxMissing);
                    Log.backup.error(e.getMessage());
                    errorList.add(e);
                }
            }
            seqLast = seq;
        }

        File oldLogs[] = null;
        File newLogs[] = null;
        int numOld = indexLastOld + 1;
        int numNew = logs.length - numOld;
        if(numOld > 0)
        {
            oldLogs = new File[numOld];
            System.arraycopy(logs, 0, oldLogs, 0, numOld);
        }
        if(numNew > 0)
        {
            newLogs = new File[numNew];
            System.arraycopy(logs, indexLastOld + 1, newLogs, 0, numNew);
        }
        if(newLogs == null && seqMax < seqSplit && logs.length > 0)
        {
            BackupServiceException e = BackupServiceException.REDOLOG_RESET_DETECTED(seqSplit + 1L, seqMin, seqMax);
            Log.backup.error(e.getMessage());
            errorList.add(e);
            return new Pair(new Pair(null, oldLogs), errorList);
        } else
        {
            return new Pair(new Pair(oldLogs, newLogs), errorList);
        }
    }

    public static int getDefaultAccountDirDepth()
    {
        return sAccountBucketDepth;
    }

    public static String accountIdToBucketizedPath(String accountId, int depth)
    {
        int len = accountId.length();
        if(len < depth * 3)
            depth = len / 3;
        if(depth > 2)
            accountId = accountId.replace('-', '_');
        StringBuilder sb = new StringBuilder(len + depth * 4);
        for(int i = 0; i < depth; i++)
        {
            int begin = i * 3;
            sb.append(accountId.substring(begin, begin + 3));
            sb.append(File.separatorChar);
        }

        sb.append(accountId);
        return sb.toString();
    }

    public static boolean freeSpaceLessThan(File path, String threshold)
    {
        long usable;
        usable = path.getUsableSpace();
        if(usable < 0L)
            return false;
        String val;
        double minPct;
        long total;
        val = threshold.toUpperCase();
        if(!val.endsWith("%"))
            break MISSING_BLOCK_LABEL_119;
        val = val.substring(0, val.length() - 1);
        minPct = Double.parseDouble(val);
        if(minPct < 0.0D)
            minPct = 0.0D;
        else
        if(minPct > 100D)
            minPct = 100D;
        total = path.getTotalSpace();
        if(total <= 0L)
            return false;
        long multiplier;
        double min;
        try
        {
            double min = ((double)total / 100D) * minPct;
            return (double)usable < min;
        }
        catch(NumberFormatException e)
        {
            Log.backup.warn((new StringBuilder()).append("Invalid minimum free space specified: \"").append(threshold).append("\"").toString());
        }
        break MISSING_BLOCK_LABEL_333;
        multiplier = 1L;
        if(val.endsWith("B"))
            val = val.substring(0, val.length() - 1);
        if(val.endsWith("T"))
        {
            val = val.substring(0, val.length() - 1);
            multiplier = 0x0L;
        } else
        if(val.endsWith("G"))
        {
            val = val.substring(0, val.length() - 1);
            multiplier = 0x40000000L;
        } else
        if(val.endsWith("M"))
        {
            val = val.substring(0, val.length() - 1);
            multiplier = 0x100000L;
        } else
        if(val.endsWith("K"))
        {
            val = val.substring(0, val.length() - 1);
            multiplier = 1024L;
        }
        min = Double.parseDouble(val) * (double)multiplier;
        return (double)usable < min;
        return false;
    }

    private static SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    private static SimpleDateFormat sDateFormat2 = new SimpleDateFormat("yyyy/MM/dd");
    private static final int LETTERS_PER_BUCKET = 3;
    private static final int sAccountBucketDepth;

    static 
    {
        int depth = 2;
        try
        {
            depth = BackupLC.backup_accounts_dir_depth.intValue();
            if(depth < 0)
                depth = 0;
            else
            if(depth > 12)
                depth = 12;
        }
        catch(NumberFormatException e) { }
        sAccountBucketDepth = depth;
    }
}
