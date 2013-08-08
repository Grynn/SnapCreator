// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   BackupLC.java

package com.zimbra.cs.backup;

import com.zimbra.common.localconfig.KnownKey;
import java.io.File;

public class BackupLC
{

    public BackupLC()
    {
    }

    public static final KnownKey backup_restore_cache_dir = new KnownKey("backup_restore_cache_dir");
    public static final KnownKey backup_disable_links;
    public static final KnownKey backup_progress_report_threshold;
    public static final KnownKey backup_file_copier_method;
    public static final KnownKey backup_file_copier_iotype;
    public static final KnownKey backup_file_copier_oio_copy_buffer_size;
    public static final KnownKey backup_file_copier_async_queue_capacity;
    public static final KnownKey backup_file_copier_parallel_workers;
    public static final KnownKey backup_file_copier_pipes;
    public static final KnownKey backup_file_copier_pipe_buffer_size;
    public static final KnownKey backup_file_copier_readers_per_pipe;
    public static final KnownKey backup_file_copier_writers_per_pipe;
    public static final KnownKey backup_accounts_dir_depth;
    public static final KnownKey backup_archived_redolog_keep_time;
    public static final KnownKey backup_debug_use_old_zip_format;
    public static final KnownKey backup_zip_copier_copy_buffer_size;
    public static final KnownKey backup_zip_copier_queue_capacity;
    public static final KnownKey backup_zip_copier_private_blob_zips;
    public static final KnownKey backup_zip_copier_deflate_level;
    public static final KnownKey backup_disable_shared_blobs;
    public static final KnownKey backup_shared_blobs_dir_depth;
    public static final KnownKey backup_shared_blobs_chars_per_dir;
    public static final KnownKey backup_shared_blobs_zip_copier_threads;
    public static final KnownKey backup_shared_blobs_zip_name_digest_chars;
    public static final KnownKey backup_verify_restored_blob_digest;
    public static final KnownKey backup_disable_blob_digest_preloading;
    public static final KnownKey backup_out_of_disk_threshold;
    public static final KnownKey mboxmove_enable_compression;

    static 
    {
        String FS = File.separator;
        backup_disable_links = new KnownKey("backup_disable_links");
        backup_disable_links.setDefault("false");
        backup_progress_report_threshold = new KnownKey("backup_progress_report_threshold");
        backup_progress_report_threshold.setDefault("10");
        backup_file_copier_method = new KnownKey("backup_file_copier_method");
        backup_file_copier_method.setDefault("PARALLEL");
        backup_file_copier_iotype = new KnownKey("backup_file_copier_iotype");
        backup_file_copier_iotype.setDefault("OIO");
        backup_file_copier_oio_copy_buffer_size = new KnownKey("backup_file_copier_oio_copy_buffer_size");
        backup_file_copier_oio_copy_buffer_size.setDefault("16384");
        backup_file_copier_async_queue_capacity = new KnownKey("backup_file_copier_async_queue_capacity");
        backup_file_copier_async_queue_capacity.setDefault("10000");
        backup_file_copier_parallel_workers = new KnownKey("backup_file_copier_parallel_workers");
        backup_file_copier_parallel_workers.setDefault("8");
        backup_file_copier_pipes = new KnownKey("backup_file_copier_pipes");
        backup_file_copier_pipes.setDefault("8");
        backup_file_copier_pipe_buffer_size = new KnownKey("backup_file_copier_pipe_buffer_size");
        backup_file_copier_pipe_buffer_size.setDefault("1048576");
        backup_file_copier_readers_per_pipe = new KnownKey("backup_file_copier_readers_per_pipe");
        backup_file_copier_readers_per_pipe.setDefault("1");
        backup_file_copier_writers_per_pipe = new KnownKey("backup_file_copier_writers_per_pipe");
        backup_file_copier_writers_per_pipe.setDefault("1");
        backup_accounts_dir_depth = new KnownKey("backup_accounts_dir_depth");
        backup_accounts_dir_depth.setDefault("2");
        backup_archived_redolog_keep_time = new KnownKey("backup_archived_redolog_keep_time");
        backup_archived_redolog_keep_time.setDefault("3600");
        backup_debug_use_old_zip_format = new KnownKey("backup_debug_use_old_zip_format");
        backup_debug_use_old_zip_format.setDefault("false");
        backup_zip_copier_copy_buffer_size = new KnownKey("backup_zip_copier_copy_buffer_size");
        backup_zip_copier_copy_buffer_size.setDefault("16384");
        backup_zip_copier_queue_capacity = new KnownKey("backup_zip_copier_queue_capacity");
        backup_zip_copier_queue_capacity.setDefault("100");
        backup_zip_copier_private_blob_zips = new KnownKey("backup_zip_copier_private_blob_zips");
        backup_zip_copier_private_blob_zips.setDefault("4");
        backup_zip_copier_deflate_level = new KnownKey("backup_zip_copier_deflate_level");
        backup_zip_copier_deflate_level.setDefault("-1");
        backup_disable_shared_blobs = new KnownKey("backup_disable_shared_blobs");
        backup_disable_shared_blobs.setDefault("false");
        backup_shared_blobs_dir_depth = new KnownKey("backup_shared_blobs_dir_depth");
        backup_shared_blobs_dir_depth.setDefault("5");
        backup_shared_blobs_chars_per_dir = new KnownKey("backup_shared_blobs_chars_per_dir");
        backup_shared_blobs_chars_per_dir.setDefault("2");
        backup_shared_blobs_zip_copier_threads = new KnownKey("backup_shared_blobs_zip_copier_threads");
        backup_shared_blobs_zip_copier_threads.setDefault("8");
        backup_shared_blobs_zip_name_digest_chars = new KnownKey("backup_shared_blobs_zip_name_digest_chars");
        backup_shared_blobs_zip_name_digest_chars.setDefault("1");
        backup_verify_restored_blob_digest = new KnownKey("backup_verify_restored_blob_digest");
        backup_verify_restored_blob_digest.setDefault("false");
        backup_disable_blob_digest_preloading = new KnownKey("backup_disable_blob_digest_preloading");
        backup_disable_blob_digest_preloading.setDefault("false");
        backup_out_of_disk_threshold = new KnownKey("backup_out_of_disk_threshold");
        backup_out_of_disk_threshold.setDefault("1MB");
        mboxmove_enable_compression = new KnownKey("mboxmove_enable_compression");
        mboxmove_enable_compression.setDefault("false");
    }
}
