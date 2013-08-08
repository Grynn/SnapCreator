// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   DbBackup.java

package com.zimbra.cs.db;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.backup.util.Log;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.file.Volume;
import com.zimbra.cs.store.file.VolumeServiceException;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Referenced classes of package com.zimbra.cs.db:
//            DbPool, DbMailItem, DbMailbox

public class DbBackup
{
    public static class TableInfo
    {

        public String getName()
        {
            return mName;
        }

        public List getColumns()
        {
            return mColumnInfos;
        }

        public String getSelectList()
        {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            ColumnInfo cinfo;
            for(Iterator i$ = mColumnInfos.iterator(); i$.hasNext(); sb.append(cinfo.getName()))
            {
                cinfo = (ColumnInfo)i$.next();
                if(first)
                    first = false;
                else
                    sb.append(", ");
            }

            return sb.toString();
        }

        private String mName;
        private List mColumnInfos;

        public TableInfo(String name, List columns)
        {
            mName = name;
            mColumnInfos = columns;
        }
    }

    public static class ColumnInfo
    {

        public String getName()
        {
            return mName;
        }

        public int getPosition()
        {
            return mPosition;
        }

        public String getType()
        {
            return mType;
        }

        public boolean isNullable()
        {
            return mNullable;
        }

        public String getDefaultValue()
        {
            return mDefaultValue;
        }

        private String mName;
        private int mPosition;
        private String mType;
        private boolean mNullable;
        private String mDefaultValue;

        public ColumnInfo(String name, int position, String type, boolean nullable, String defaultValue)
        {
            mName = name;
            mPosition = position;
            mType = type;
            mNullable = nullable;
            mDefaultValue = defaultValue;
        }
    }

    public static interface AutoGroupSelectionFilter
    {

        public abstract boolean accept(String s, String s1, long l)
            throws ServiceException;
    }


    public DbBackup()
    {
    }

    public static void loadTable(DbPool.Connection conn, String tableName, TableInfo tableInfo, File source)
        throws ServiceException
    {
        loadTable(conn, tableName, tableInfo, source, false);
    }

    public static void loadTable(DbPool.Connection conn, String tableName, TableInfo tableInfo, File source, boolean deleteExistingRows)
        throws ServiceException
    {
        PreparedStatement stmt;
        Log.backup.debug((new StringBuilder()).append("Restoring table ").append(tableName).append(" with data from ").append(source.getAbsolutePath()).toString());
        stmt = null;
        try
        {
            if(deleteExistingRows)
            {
                StringBuffer sql = new StringBuffer((new StringBuilder()).append("DELETE FROM ").append(tableName).toString());
                stmt = conn.prepareStatement(sql.toString());
                stmt.executeUpdate();
                stmt.close();
            }
            String stmtStr = (new StringBuilder()).append("LOAD DATA LOCAL INFILE ? REPLACE INTO TABLE ").append(tableName).append(" FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' LINES TERMINATED BY '\\n'").toString();
            if(tableInfo != null)
                stmtStr = (new StringBuilder()).append(stmtStr).append(" (").append(tableInfo.getSelectList()).append(")").toString();
            stmt = conn.prepareStatement(stmtStr);
            stmt.setString(1, source.getPath());
            stmt.execute();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("restoring table: ").append(tableName).toString(), e);
        }
        DbPool.closeStatement(stmt);
        break MISSING_BLOCK_LABEL_236;
        Exception exception;
        exception;
        DbPool.closeStatement(stmt);
        throw exception;
    }

    public static long saveTable(DbPool.Connection conn, TableInfo tinfo, File targetDir)
        throws ServiceException
    {
        return saveTable(conn, tinfo, targetDir, null);
    }

    public static long saveTable(DbPool.Connection conn, TableInfo tinfo, File targetDir, String whereClause)
        throws ServiceException
    {
        String tableName;
        long bytes;
        File target;
        PreparedStatement stmt;
        tableName = tinfo.getName();
        bytes = -1L;
        target = getDbFile(targetDir, removeDatabasePrefix(tableName));
        stmt = null;
        try
        {
            StringBuffer sql = new StringBuffer((new StringBuilder()).append("SELECT ").append(tinfo.getSelectList()).toString());
            sql.append((new StringBuilder()).append(" INTO OUTFILE ? FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' LINES TERMINATED BY '\\n' FROM ").append(tableName).toString());
            if(whereClause != null)
            {
                sql.append(" ");
                sql.append(whereClause);
            }
            stmt = conn.prepareStatement(sql.toString());
            if(target.exists())
                target.delete();
            stmt.setString(1, target.getPath());
            stmt.execute();
            bytes = target.length();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("backing up table: ").append(tableName).toString(), e);
        }
        DbPool.closeStatement(stmt);
        if(bytes == 0L)
            target.delete();
        else
            target.setReadOnly();
        break MISSING_BLOCK_LABEL_244;
        Exception exception;
        exception;
        DbPool.closeStatement(stmt);
        if(bytes == 0L)
            target.delete();
        else
            target.setReadOnly();
        throw exception;
        return bytes;
    }

    public static File getDbFile(File dir, String tableName)
    {
        return new File(dir, (new StringBuilder()).append(tableName).append(".dat").toString());
    }

    public static String removeDatabasePrefix(String tableName)
    {
        int dot = tableName.indexOf('.');
        if(dot != -1 && dot < tableName.length() - 1)
            return tableName.substring(dot + 1);
        else
            return tableName;
    }

    public static void updateConstraints(DbPool.Connection conn)
        throws ServiceException
    {
        PreparedStatement stmt;
        ResultSet rs;
        stmt = null;
        rs = null;
        try
        {
            int maxMboxId = 0;
            stmt = conn.prepareStatement("SELECT MAX(id) FROM mailbox");
            rs = stmt.executeQuery();
            if(rs.next() && !rs.wasNull())
                maxMboxId = rs.getInt(1);
            rs.close();
            stmt.close();
            int nextMboxId = maxMboxId + 1;
            stmt = conn.prepareStatement("UPDATE current_volumes SET next_mailbox_id = ? WHERE next_mailbox_id < ?");
            stmt.setInt(1, nextMboxId);
            stmt.setInt(2, nextMboxId);
            stmt.executeUpdate();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE("updating next mailbox id during restore", e);
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        break MISSING_BLOCK_LABEL_127;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        throw exception;
    }

    public static Map getBlobDigestMap(DbPool.Connection conn, int mboxId)
        throws ServiceException
    {
        Map map;
        String arr$[];
        int len$;
        int i$;
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        map = new HashMap(1000);
        String queries[] = new String[4];
        queries[0] = (new StringBuilder()).append("SELECT id, mod_content, blob_digest FROM ").append(DbMailItem.getMailItemTableName(mbox, false)).append(" WHERE mailbox_id = ?").toString();
        queries[1] = (new StringBuilder()).append("SELECT item_id, mod_content, blob_digest FROM ").append(DbMailItem.getRevisionTableName(mbox, false)).append(" WHERE mailbox_id = ?").toString();
        queries[2] = (new StringBuilder()).append("SELECT id, mod_content, blob_digest FROM ").append(DbMailItem.getMailItemTableName(mbox, true)).append(" WHERE mailbox_id = ?").toString();
        queries[3] = (new StringBuilder()).append("SELECT item_id, mod_content, blob_digest FROM ").append(DbMailItem.getRevisionTableName(mbox, true)).append(" WHERE mailbox_id = ?").toString();
        arr$ = queries;
        len$ = arr$.length;
        i$ = 0;
_L2:
        String query;
        PreparedStatement stmt;
        ResultSet rs;
        if(i$ >= len$)
            break; /* Loop/switch isn't completed */
        query = arr$[i$];
        stmt = null;
        rs = null;
        try
        {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, mboxId);
            rs = stmt.executeQuery();
            do
            {
                if(!rs.next())
                    break;
                String id = rs.getString(1);
                String rev = rs.getString(2);
                String digest = rs.getString(3);
                if(!rs.wasNull() && digest != null && digest.length() > 0)
                {
                    String key = (new StringBuilder()).append(id).append("-").append(rev).toString();
                    map.put(key, digest);
                }
            } while(true);
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE("getting map of message id to blob digest", e);
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        break MISSING_BLOCK_LABEL_353;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        throw exception;
        i$++;
        if(true) goto _L2; else goto _L1
_L1:
        return map;
    }

    public static String getBlobDigest(DbPool.Connection conn, int mboxId, Pair idRev)
        throws ServiceException
    {
        String digest = getBlobDigest(conn, mboxId, idRev, false);
        if(digest == null)
            digest = getBlobDigest(conn, mboxId, idRev, true);
        return digest;
    }

    private static String getBlobDigest(DbPool.Connection conn, int mboxId, Pair idRev, boolean fromDumpster)
        throws ServiceException
    {
        String id;
        String rev;
        Mailbox mbox;
        PreparedStatement mailItemStmt;
        ResultSet mailItemRs;
        id = (String)idRev.getFirst();
        rev = (String)idRev.getSecond();
        mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailItemStmt = null;
        mailItemRs = null;
        String modContent;
        String mailItemQuery = (new StringBuilder()).append("SELECT mod_content, blob_digest FROM ").append(DbMailItem.getMailItemTableName(mbox, fromDumpster)).append(" WHERE mailbox_id = ? AND id = ? AND blob_digest IS NOT NULL").toString();
        mailItemStmt = conn.prepareStatement(mailItemQuery);
        mailItemStmt.setInt(1, mboxId);
        mailItemStmt.setString(2, id);
        mailItemRs = mailItemStmt.executeQuery();
        if(mailItemRs.next())
            break MISSING_BLOCK_LABEL_126;
        modContent = null;
        DbPool.closeResults(mailItemRs);
        DbPool.closeStatement(mailItemStmt);
        return modContent;
label0:
        {
            String s;
            try
            {
                modContent = mailItemRs.getString(1);
                String digest = mailItemRs.getString(2);
                if(rev != null && !rev.equals(modContent))
                    break label0;
                s = digest;
            }
            catch(SQLException e)
            {
                throw ServiceException.FAILURE("getting blob digest from mail_item table", e);
            }
            DbPool.closeResults(mailItemRs);
            DbPool.closeStatement(mailItemStmt);
            return s;
        }
        DbPool.closeResults(mailItemRs);
        DbPool.closeStatement(mailItemStmt);
        break MISSING_BLOCK_LABEL_216;
        Exception exception;
        exception;
        DbPool.closeResults(mailItemRs);
        DbPool.closeStatement(mailItemStmt);
        throw exception;
        PreparedStatement revisionStmt;
        ResultSet revisionRs;
        if(rev == null)
            break MISSING_BLOCK_LABEL_375;
        revisionStmt = null;
        revisionRs = null;
label1:
        {
            String s1;
            try
            {
                String revisionQuery = (new StringBuilder()).append("SELECT blob_digest FROM ").append(DbMailItem.getRevisionTableName(mbox, fromDumpster)).append(" WHERE mailbox_id = ? AND item_id = ? AND mod_content = ? AND blob_digest IS NOT NULL").toString();
                revisionStmt = conn.prepareStatement(revisionQuery);
                revisionStmt.setInt(1, mboxId);
                revisionStmt.setString(2, id);
                revisionStmt.setString(3, rev);
                revisionRs = revisionStmt.executeQuery();
                if(!revisionRs.next())
                    break label1;
                s1 = revisionRs.getString(1);
            }
            catch(SQLException e)
            {
                throw ServiceException.FAILURE("getting blob digest from revision table", e);
            }
            DbPool.closeResults(revisionRs);
            DbPool.closeStatement(revisionStmt);
            return s1;
        }
        DbPool.closeResults(revisionRs);
        DbPool.closeStatement(revisionStmt);
        break MISSING_BLOCK_LABEL_375;
        Exception exception1;
        exception1;
        DbPool.closeResults(revisionRs);
        DbPool.closeStatement(revisionStmt);
        throw exception1;
        return null;
    }

    public static Set getSystemTables(DbPool.Connection conn)
        throws ServiceException
    {
        Set tables;
        PreparedStatement stmt;
        ResultSet rs;
        tables = new HashSet();
        stmt = null;
        rs = null;
        Set set;
        try
        {
            stmt = conn.prepareStatement("SHOW TABLES");
            for(rs = stmt.executeQuery(); rs.next(); tables.add(rs.getString(1)));
            set = tables;
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE("getting system tables", e);
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return set;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        throw exception;
    }

    public static Set getMailboxTables(DbPool.Connection conn, Mailbox mbox)
        throws ServiceException
    {
        Set tables;
        PreparedStatement stmt;
        ResultSet rs;
        tables = new HashSet();
        stmt = null;
        rs = null;
        Set set;
        try
        {
            String dbName = DbMailbox.getDatabaseName(mbox);
            stmt = conn.prepareStatement((new StringBuilder()).append("USE ").append(dbName).toString());
            stmt.execute();
            DbPool.closeStatement(stmt);
            stmt = conn.prepareStatement("SHOW TABLES");
            for(rs = stmt.executeQuery(); rs.next(); tables.add(rs.getString(1)));
            set = tables;
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("getting tables for mailbox").append(mbox.getId()).toString(), e);
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return set;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        throw exception;
    }

    public static List getAccountsByOldestBackup(DbPool.Connection conn, long mustBackupIfOlderThan, long neverBackupIfAsRecentAs, int minCount, int maxCount, AutoGroupSelectionFilter filter)
        throws ServiceException
    {
        int mustBackupIfOlderThanSecs;
        int neverBackupIfAsRecentAsSecs;
        List accountIds;
        int count;
        PreparedStatement stmt;
        ResultSet rs;
        mustBackupIfOlderThanSecs = (int)(mustBackupIfOlderThan / 1000L);
        neverBackupIfAsRecentAsSecs = (int)(neverBackupIfAsRecentAs / 1000L);
        accountIds = new ArrayList(minCount);
        count = 0;
        stmt = null;
        rs = null;
        stmt = conn.prepareStatement("SELECT account_id, last_backup_at, comment FROM mailbox WHERE last_backup_at IS NULL OR last_backup_at < ? ORDER BY last_backup_at, id");
        stmt.setInt(1, mustBackupIfOlderThanSecs);
        rs = stmt.executeQuery();
_L6:
        String acctId;
        int backupAt;
        if(!rs.next())
            break MISSING_BLOCK_LABEL_200;
        acctId = rs.getString(1);
        backupAt = rs.getInt(2);
        if(!rs.wasNull()) goto _L2; else goto _L1
_L1:
        backupAt = -1;
          goto _L3
_L2:
        if(backupAt < neverBackupIfAsRecentAsSecs) goto _L3; else goto _L4
_L4:
        String email = accountIds;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return email;
_L3:
        email = rs.getString(3);
        if(!filter.accept(acctId, email, backupAt)) goto _L6; else goto _L5
_L5:
        accountIds.add(acctId);
        if(++count < maxCount) goto _L6; else goto _L7
_L7:
        String email = accountIds;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return email;
        int needMore = minCount - count;
        if(needMore <= 0)
            break MISSING_BLOCK_LABEL_386;
        stmt = conn.prepareStatement("SELECT account_id, last_backup_at, comment FROM mailbox WHERE last_backup_at >= ? ORDER BY last_backup_at, id LIMIT ?");
        stmt.setInt(1, mustBackupIfOlderThanSecs);
        stmt.setInt(2, needMore);
        rs = stmt.executeQuery();
_L13:
        String acctId;
        int backupAt;
        if(!rs.next())
            break MISSING_BLOCK_LABEL_386;
        acctId = rs.getString(1);
        backupAt = rs.getInt(2);
        if(!rs.wasNull()) goto _L9; else goto _L8
_L8:
        backupAt = -1;
          goto _L10
_L9:
        if(backupAt < neverBackupIfAsRecentAsSecs) goto _L10; else goto _L11
_L11:
        email = accountIds;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return email;
_L10:
        email = rs.getString(3);
        if(!filter.accept(acctId, email, backupAt)) goto _L13; else goto _L12
_L12:
        accountIds.add(acctId);
        if(++count < maxCount) goto _L13; else goto _L14
_L14:
        List list1 = accountIds;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return list1;
        List list;
        try
        {
            list = accountIds;
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE("getting mailboxes with oldest backup", e);
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return list;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        throw exception;
    }

    public static void updateMailboxBackupTime(DbPool.Connection conn, int mboxId, int backupTime)
        throws ServiceException
    {
        PreparedStatement stmt = null;
        try
        {
            stmt = conn.prepareStatement("UPDATE mailbox SET last_backup_at = ? WHERE id = ?");
            if(backupTime >= 0)
                stmt.setInt(1, backupTime);
            else
                stmt.setNull(1, 4);
            stmt.setInt(2, mboxId);
            stmt.executeUpdate();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("updating backup time for mailbox ").append(mboxId).toString(), e);
        }
        DbPool.closeStatement(stmt);
        break MISSING_BLOCK_LABEL_90;
        Exception exception;
        exception;
        DbPool.closeStatement(stmt);
        throw exception;
    }

    public static TableInfo getTableInfo(DbPool.Connection conn, String tableNameFQ)
        throws ServiceException
    {
        String sql;
        PreparedStatement stmt;
        ResultSet rs;
        int dot = tableNameFQ.indexOf(".");
        String databaseName;
        String tableName;
        if(dot != -1)
        {
            databaseName = tableNameFQ.substring(0, dot);
            tableName = tableNameFQ.substring(dot + 1, tableNameFQ.length());
        } else
        {
            databaseName = "zimbra";
            tableName = tableNameFQ;
            tableNameFQ = (new StringBuilder()).append(databaseName).append(".").append(tableName).toString();
        }
        sql = (new StringBuilder()).append("SELECT COLUMN_NAME, ORDINAL_POSITION, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '").append(databaseName).append("' AND TABLE_NAME = '").append(tableName).append("' ").append("ORDER BY ORDINAL_POSITION").toString();
        stmt = null;
        rs = null;
        TableInfo tableinfo;
        try
        {
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            List cinfos = new ArrayList();
            String columnName;
            int position;
            String columnType;
            boolean nullable;
            String defaultValue;
            for(; rs.next(); cinfos.add(new ColumnInfo(columnName, position, columnType, nullable, defaultValue)))
            {
                int pos = 1;
                columnName = rs.getString(pos++);
                position = rs.getInt(pos++);
                columnType = rs.getString(pos++);
                nullable = rs.getBoolean(pos++);
                defaultValue = rs.getString(pos++);
            }

            tableinfo = new TableInfo(tableNameFQ, cinfos);
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("getting table info for ").append(tableNameFQ).toString(), e);
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        return tableinfo;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        throw exception;
    }

    public static void fixupIndexVolume(DbPool.Connection conn, int mboxId)
        throws ServiceException
    {
        PreparedStatement stmt;
        ResultSet rs;
        stmt = null;
        rs = null;
        short mboxIndexVolId;
label0:
        {
            try
            {
                stmt = conn.prepareStatement("SELECT index_volume_id FROM mailbox WHERE id=?");
                stmt.setInt(1, mboxId);
                stmt.executeQuery();
                rs = stmt.executeQuery();
                if(rs.next())
                {
                    mboxIndexVolId = rs.getShort(1);
                    break label0;
                }
            }
            catch(SQLException e)
            {
                throw ServiceException.FAILURE((new StringBuilder()).append("getting index volume id for mailbox ").append(mboxId).toString(), e);
            }
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            return;
        }
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        break MISSING_BLOCK_LABEL_120;
        Exception exception;
        exception;
        DbPool.closeResults(rs);
        DbPool.closeStatement(stmt);
        throw exception;
        short currentIndexVolId;
        boolean needNewIndexVolume = true;
        if(mboxIndexVolId != -1)
            try
            {
                Volume vol = Volume.getById(mboxIndexVolId);
                needNewIndexVolume = vol == null || vol.getType() != 10;
            }
            catch(VolumeServiceException e)
            {
                if(!e.getCode().equals("volume.NO_SUCH_VOLUME"))
                    throw e;
            }
        if(!needNewIndexVolume)
            break MISSING_BLOCK_LABEL_266;
        currentIndexVolId = Volume.getCurrentIndexVolume().getId();
        stmt = null;
        try
        {
            stmt = conn.prepareStatement("UPDATE mailbox SET index_volume_id = ? WHERE id = ?");
            stmt.setShort(1, currentIndexVolId);
            stmt.setInt(2, mboxId);
            stmt.executeUpdate();
        }
        catch(SQLException e)
        {
            throw ServiceException.FAILURE((new StringBuilder()).append("updating index volume id for mailbox ").append(mboxId).toString(), e);
        }
        DbPool.closeStatement(stmt);
        break MISSING_BLOCK_LABEL_266;
        Exception exception1;
        exception1;
        DbPool.closeStatement(stmt);
        throw exception1;
    }

    public static final int CI_MAIL_ITEM_VOLUME_ID = 9;
    public static final String BACKUP_SCHEMA_NAME = "backup";
    private static final String FORMAT = " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"' LINES TERMINATED BY '\\n'";
}
