// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   DbSharedBlobDigest.java

package com.zimbra.cs.db;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.util.Config;
import java.io.*;
import java.sql.*;

// Referenced classes of package com.zimbra.cs.db:
//            DbPool, DbUtil

public class DbSharedBlobDigest
{

    public DbSharedBlobDigest()
    {
    }

    public static void initialize()
        throws ServiceException
    {
        File file;
        DbPool.Connection conn;
        file = Config.getPathRelativeToZimbraHome("db/backup_schema.sql");
        conn = null;
        try
        {
            conn = DbPool.getConnection();
            String script = new String(ByteUtil.getContent(file));
            DbUtil.executeScript(conn, new StringReader(script));
        }
        catch(IOException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("unable to read SQL statements from ").append(file.getPath()).toString(), e);
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE("unable to create backup/restore schema", e);
        }
        DbPool.quietClose(conn);
        break MISSING_BLOCK_LABEL_86;
        Exception exception;
        exception;
        DbPool.quietClose(conn);
        throw exception;
    }

    public static void add(String digest)
        throws ServiceException
    {
        DbPool.Connection conn;
        PreparedStatement stmt;
        conn = null;
        stmt = null;
        try
        {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("INSERT INTO backup.shared_blob_digest (blob_digest) VALUES (?) ON DUPLICATE KEY UPDATE blob_digest = ?");
            stmt.setString(1, digest);
            stmt.setString(2, digest);
            stmt.executeUpdate();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("adding digest ").append(digest).append(" to ").append("backup.shared_blob_digest").append(" table").toString(), e);
        }
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        break MISSING_BLOCK_LABEL_102;
        Exception exception;
        exception;
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        throw exception;
    }

    public static boolean contains(String digest)
        throws ServiceException
    {
        DbPool.Connection conn;
        PreparedStatement stmt;
        ResultSet rs;
        conn = null;
        stmt = null;
        rs = null;
        boolean flag;
        try
        {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("SELECT blob_digest FROM backup.shared_blob_digest WHERE blob_digest = ?");
            stmt.setString(1, digest);
            rs = stmt.executeQuery();
            flag = rs.next();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE("querying backup.shared_blob_digest table", e);
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        return flag;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        throw exception;
    }

    public static void cleanup()
        throws ServiceException
    {
        DbPool.Connection conn;
        PreparedStatement stmt;
        conn = null;
        stmt = null;
        try
        {
            conn = DbPool.getConnection();
            stmt = conn.prepareStatement("DROP TABLE IF EXISTS backup.shared_blob_digest");
            stmt.executeUpdate();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE("dropping backup.shared_blob_digest table", e);
        }
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        break MISSING_BLOCK_LABEL_52;
        Exception exception;
        exception;
        DbPool.closeStatement(stmt);
        DbPool.quietClose(conn);
        throw exception;
    }

    private static final String TABLE_NAME = "backup.shared_blob_digest";
    private static final String DROP_QUERY = "DROP TABLE IF EXISTS backup.shared_blob_digest";
    private static final String PUT_QUERY = "INSERT INTO backup.shared_blob_digest (blob_digest) VALUES (?) ON DUPLICATE KEY UPDATE blob_digest = ?";
    private static final String CONTAINS_QUERY = "SELECT blob_digest FROM backup.shared_blob_digest WHERE blob_digest = ?";
}
