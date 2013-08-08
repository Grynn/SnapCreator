// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   AbstractBackupTarget.java

package com.zimbra.cs.backup;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.util.Config;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Referenced classes of package com.zimbra.cs.backup:
//            BackupSet, BackupTarget

public abstract class AbstractBackupTarget
    implements BackupTarget
{

    public AbstractBackupTarget()
    {
        mZimbraTables = null;
    }

    public BackupSet getBackupSet(String label)
        throws ServiceException
    {
        return (BackupSet)mRunningBackups.get(getKey(label));
    }

    public List getBackupSets(long from, long to)
        throws ServiceException
    {
        List result = new ArrayList();
        result.addAll(mRunningBackups.values());
        Iterator it = result.iterator();
        do
        {
            if(!it.hasNext())
                break;
            BackupSet bak = (BackupSet)it.next();
            if(bak.getStartTime() < from || bak.getStartTime() > to)
                it.remove();
        } while(true);
        return result;
    }

    public File[] getRedoLogFiles(long startSequence)
        throws IOException
    {
        return getRedoLogFiles(startSequence, 0xffffffffL);
    }

    protected String getKey(String label)
    {
        return (new StringBuilder()).append(getURI()).append("-").append(label).toString();
    }

    protected boolean initDb()
        throws ServiceException, IOException
    {
        PreparedStatement stmt;
        ResultSet rs;
        com.zimbra.cs.db.DbPool.Connection conn;
        stmt = null;
        rs = null;
        conn = DbPool.getMaintenanceConnection();
        boolean flag;
        stmt = conn.prepareStatement("USE zimbra");
        stmt.executeUpdate();
        stmt = conn.prepareStatement("SHOW TABLES");
        rs = stmt.executeQuery();
        int count = 0;
        Set zimbraTables = getZimbraTables();
        do
        {
            if(!rs.next())
                break;
            String table = rs.getString(1);
            if(zimbraTables.contains(table))
                count++;
        } while(true);
        if(count != zimbraTables.size())
            throw new SQLException((new StringBuilder()).append("one or more zimbra.* tables not found ").append(zimbraTables).toString());
        flag = false;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        return flag;
        SQLException e;
        e;
        DbPool.closeResults(rs);
        rs = null;
        DbPool.closeStatement(stmt);
        Log.backup.info("Restoring database zimbra schema");
        try
        {
            conn.disableForeignKeyConstraints();
            stmt = conn.prepareStatement("DROP DATABASE IF EXISTS zimbra");
            stmt.executeUpdate();
            File file = Config.getPathRelativeToZimbraHome("db/db.sql");
            DbUtil.executeScript(conn, new FileReader(file));
            stmt = conn.prepareStatement("USE zimbra");
            stmt.executeUpdate();
            flag = true;
        }
        catch(SQLException e1)
        {
            throw ServiceException.FAILURE("error restoring schema for database zimbra", e1);
        }
        conn.enableForeignKeyConstraints();
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        return flag;
        Exception exception;
        exception;
        conn.enableForeignKeyConstraints();
        throw exception;
        Exception exception1;
        exception1;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        throw exception1;
    }

    protected Set getZimbraTables()
        throws IOException
    {
        BufferedReader in;
        if(mZimbraTables != null)
            return mZimbraTables;
        File scriptFile = Config.getPathRelativeToZimbraHome("db/db.sql");
        in = new BufferedReader(new FileReader(scriptFile));
        Set set;
        StringBuilder buf = new StringBuilder();
        String line;
        while((line = in.readLine()) != null) 
        {
            int hash = line.indexOf('#');
            if(hash != -1)
                line = line.substring(0, hash);
            buf.append(line).append(' ');
        }
        Matcher matcher = PAT_CREATE_TABLE.matcher(buf.toString());
        Set tables = new HashSet();
        for(; matcher.find(); tables.add(matcher.group(1).trim()));
        mZimbraTables = tables;
        set = tables;
        in.close();
        return set;
        Exception exception;
        exception;
        in.close();
        throw exception;
    }

    protected static Map mRunningBackups = Collections.synchronizedMap(new HashMap());
    private Set mZimbraTables;
    private static Pattern PAT_CREATE_TABLE = Pattern.compile("create table (\\w+)", 2);

}
