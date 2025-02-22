package io.nuls.dapp.index.rocksdb.manager;

import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.dapp.index.rocksdb.constant.DBErrorCode;
import io.nuls.dapp.index.rocksdb.model.Entry;
import io.nuls.dapp.index.rocksdb.util.DBUtils;
import org.rocksdb.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionDBManager {

    static {
        TransactionDB.loadLibrary();
    }

    /**
     * 数据库已打开的连接缓存.
     */
    private static final ConcurrentHashMap<String, TransactionDB> TABLES = new ConcurrentHashMap<>();

    /**
     * 数据表基础文件夹名.
     */
    private static final String BASE_DB_NAME = "rocksdb";

    /**
     * 数据库是否已经初始化.
     */
    private static volatile boolean isInit = false;

    /**
     * 数据操作同步锁.
     */
    private static ReentrantLock lock = new ReentrantLock();

    /**
     * 数据库根目录绝对路径.
     */
    private static String dataPath;

    /**
     * 根据传入的数据库路径将已存在的数据库连接打开，并缓存DB连接.
     * 如果有数据表连接被关闭需要重新打开连接也可以，执行初始化连接
     *
     * @param path 数据库地址
     * @throws Exception 数据库打开连接异常
     */
    public static void init(final String path) throws Exception {
        synchronized (TransactionDBManager.class) {
            isInit = true;
            File dir = DBUtils.loadDataPath(path);
            dataPath = dir.getPath();
            Log.info("TransactionDBManager dataPath is " + dataPath);

            File[] tableFiles = dir.listFiles();
            TransactionDB db;
            String dbPath = null;
            for (File tableFile : tableFiles) {
                //缓存中已存在的数据库连接不再重复打开
                if (!tableFile.isDirectory() || TABLES.get(tableFile.getName()) != null) {
                    continue;
                }
                try {
                    dbPath = tableFile.getPath() + File.separator + BASE_DB_NAME;
                    db = initOpenDB(dbPath);
                    if (db != null) {
                        TABLES.put(tableFile.getName(), db);
                    }
                } catch (Exception e) {
                    Log.warn("load table failed, tableName: " + tableFile.getName() + ", dbPath: " + dbPath, e);
                    throw e;
                }
            }
        }
    }

    /**
     * @param dbPath 数据库地址
     * @return TransactionDB 数据库连接对象
     * @throws RocksDBException 数据库连接异常
     */
    private static TransactionDB initOpenDB(final String dbPath) throws RocksDBException {
        File checkFile = new File(dbPath + File.separator + "CURRENT");
        if (!checkFile.exists()) {
            return null;
        }

        Options options = getCommonOptions(false);
        return TransactionDB.open(options, new TransactionDBOptions(), dbPath);
    }

    /**
     * 根据名称创建对应的数据库.
     * Create database based by name
     *
     * @param tableName 数据库表名称
     * @return Result 创建结果
     */
    public static boolean createTable(final String tableName) throws Exception {
        lock.lock();
        try {
            if (StringUtils.isBlank(tableName)) {
                throw new Exception(DBErrorCode.NULL_PARAMETER);
            }
            if (TABLES.containsKey(tableName)) {
                throw new Exception(DBErrorCode.DB_TABLE_EXIST);
            }
            if (StringUtils.isBlank(dataPath) || !DBUtils.checkPathLegal(tableName)) {
                throw new Exception(DBErrorCode.DB_TABLE_CREATE_PATH_ERROR);
            }
            try {
                File dir = new File(dataPath + File.separator + tableName);
                if (!dir.exists()) {
                    dir.mkdir();
                }
                String filePath = dataPath + File.separator + tableName + File.separator + BASE_DB_NAME;
                Options options = getCommonOptions(true);
                TransactionDB db = TransactionDB.open(options, new TransactionDBOptions(), filePath);
                TABLES.put(tableName, db);
            } catch (Exception e) {
                Log.error("error create table: " + tableName, e);
                throw new Exception(DBErrorCode.DB_TABLE_CREATE_ERROR);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }


    /**
     * 根据名称获得对应的数据库对象.
     * Get database objects by name
     *
     * @param tableName 数据库表名称
     * @return RocksDB
     */
    public static TransactionDB getTable(final String tableName) {
        return TABLES.get(tableName);
    }

    /**
     * 根据名称删除对应的数据库.
     * Delete database by name
     *
     * @param tableName 数据库表名称
     * @return Result
     */
    public static boolean destroyTable(final String tableName) throws Exception {
        if (StringUtils.isBlank(dataPath) || !DBUtils.checkPathLegal(tableName)) {
            throw new Exception(DBErrorCode.DB_TABLE_CREATE_PATH_ERROR);
        }
        if (!baseCheckTable(tableName)) {
            throw new Exception(DBErrorCode.DB_TABLE_NOT_EXIST);
        }
        try {
            TransactionDB db = TABLES.remove(tableName);
            db.close();
            File dir = new File(dataPath + File.separator + tableName);
            if (!dir.exists()) {
                throw new Exception(DBErrorCode.DB_TABLE_NOT_EXIST);
            }
            String filePath = dataPath + File.separator + tableName + File.separator + BASE_DB_NAME;
            destroyDB(filePath);
        } catch (Exception e) {
            Log.error("error destroy table: " + tableName, e);
            throw new Exception(DBErrorCode.DB_TABLE_DESTROY_ERROR);
        }
        return true;
    }

    /**
     * 删除数据表.
     *
     * @param dbPath 数据库名称
     * @throws RocksDBException 数据库连接异常
     */
    private static void destroyDB(final String dbPath) throws RocksDBException {
        Options options = new Options();
        TransactionDB.destroyDB(dbPath, options);
    }


    /**
     * 查询所有的数据库名称.
     * query all table names
     *
     * @return 所有数据表名称
     */
    public static String[] listTable() {
        int i = 0;
        Enumeration<String> keys = TABLES.keys();
        String[] tables = new String[TABLES.size()];
        int length = tables.length;
        while (keys.hasMoreElements()) {
            tables[i++] = keys.nextElement();
            if (i == length) {
                break;
            }
        }
        return tables;
    }


    /**
     * 关闭所有数据库连接.
     * close all table
     */
    public static void close() {
        Set<Map.Entry<String, TransactionDB>> entries = TABLES.entrySet();
        for (Map.Entry<String, TransactionDB> entry : entries) {
            try {
                TABLES.remove(entry.getKey());
                entry.getValue().close();
            } catch (Exception e) {
                Log.warn("close rocksdb error", e);
            }
        }
    }


    /**
     * 新增或者修改数据.
     * Add or modify entity to specified table
     *
     * @param table 表名
     * @param key   数据键
     * @param value 数据值
     * @return 保存是否成功
     */
    public static boolean put(final String table, final byte[] key, final byte[] value) throws Exception {
        if (!baseCheckTable(table)) {
            throw new Exception(DBErrorCode.DB_TABLE_NOT_EXIST);
        }
        if (key == null || value == null) {
            throw new Exception(DBErrorCode.NULL_PARAMETER);
        }
        try {
            TransactionDB db = TABLES.get(table);
            db.put(key, value);
            return true;
        } catch (Exception e) {
            Log.error(e);
            throw new Exception(DBErrorCode.DB_UNKOWN_EXCEPTION);
        }
    }

    /**
     * 批量保存数据.
     * batch save entity
     *
     * @param table 数据库表名称
     * @param kvs   保存数据的键值对
     * @return 批量保存是否成功
     */
    public static boolean batchPut(final String table, final Map<byte[], byte[]> kvs) throws Exception {
        if (!baseCheckTable(table)) {
            throw new Exception(DBErrorCode.DB_TABLE_NOT_EXIST);
        }
        if (kvs == null || kvs.size() == 0) {
            throw new Exception(DBErrorCode.NULL_PARAMETER);
        }
        try (WriteBatch writeBatch = new WriteBatch()) {
            TransactionDB db = TABLES.get(table);
            for (Map.Entry<byte[], byte[]> entry : kvs.entrySet()) {
                writeBatch.put(entry.getKey(), entry.getValue());
            }
            db.write(new WriteOptions(), writeBatch);
            return true;
        } catch (Exception ex) {
            Log.error(ex);
            throw new Exception(DBErrorCode.DB_UNKOWN_EXCEPTION);
        }
    }

    /**
     * 删除数据.
     * delete entity from specified table
     *
     * @param table 数据库表名称
     * @param key   删除标识
     * @return 删除是否成功
     */
    public static boolean delete(final String table, final byte[] key) throws Exception {
        if (!baseCheckTable(table)) {
            throw new Exception(DBErrorCode.DB_TABLE_NOT_EXIST);
        }
        if (key == null) {
            throw new Exception(DBErrorCode.NULL_PARAMETER);
        }
        try {
            TransactionDB db = TABLES.get(table);
            db.delete(key);
            return true;
        } catch (Exception e) {
            Log.error(e);
            throw new Exception(DBErrorCode.DB_UNKOWN_EXCEPTION);
        }
    }


    /**
     * 批量删除数据.
     * batch delete entity
     *
     * @param table 数据库表名称
     * @param keys  批量删除标识
     * @return 批量删除是否成功
     */
    public static boolean deleteKeys(final String table, final List<byte[]> keys) throws Exception {
        if (!baseCheckTable(table)) {
            throw new Exception(DBErrorCode.DB_TABLE_NOT_EXIST);
        }
        if (keys == null || keys.size() == 0) {
            throw new Exception(DBErrorCode.NULL_PARAMETER);
        }
        try (WriteBatch writeBatch = new WriteBatch()) {
            TransactionDB db = TABLES.get(table);
            for (byte[] key : keys) {
                writeBatch.delete(key);
            }
            db.write(new WriteOptions(), writeBatch);
            return true;
        } catch (Exception ex) {
            Log.error(ex);
            throw new Exception(DBErrorCode.DB_UNKOWN_EXCEPTION);
        }
    }

    /**
     * 根据key查询数据.
     * query entity in a specified table by key
     *
     * @param table 数据库表名称
     * @param key   查询关键字
     * @return 查询结果
     */
    public static byte[] get(final String table, final byte[] key) {
        if (!baseCheckTable(table)) {
            return null;
        }
        if (key == null) {
            return null;
        }
        try {
            TransactionDB db = TABLES.get(table);
            return db.get(key);
        } catch (Exception e) {
            return null;
        }
    }




    /**
     * 查询指定表的key-List集合.
     * query the key-List collection of the specified table
     *
     * @param table 数据库表名称
     * @return 该表的所有键
     */
    public static List<byte[]> keyList(final String table) {
        if (!baseCheckTable(table)) {
            return null;
        }
        List<byte[]> list = new ArrayList<>();
        try {
            TransactionDB db = TABLES.get(table);
            try (RocksIterator iterator = db.newIterator()) {
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    list.add(iterator.key());
                }
            }
            return list;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 查询指定表的value-List集合.
     * query the value-List collection of the specified table
     *
     * @param table 数据库表名称
     * @return 该表的所有值
     */
    public static List<byte[]> valueList(final String table) {
        if (!baseCheckTable(table)) {
            return null;
        }
        List<byte[]> list = new ArrayList<>();
        try {
            TransactionDB db = TABLES.get(table);
            try (RocksIterator iterator = db.newIterator()) {
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    list.add(iterator.value());
                }
            }
            return list;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 查询指定表的entry-List集合.
     * query the entry-List collection of the specified table
     *
     * @param table 数据库表名称
     * @return 该表所有键值对集合
     */
    public static List<Entry<byte[], byte[]>> entryList(final String table) {
        if (!baseCheckTable(table)) {
            return null;
        }
        List<Entry<byte[], byte[]>> entryList = new ArrayList<>();
        try {
            TransactionDB db = TABLES.get(table);
            try (RocksIterator iterator = db.newIterator()) {
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    entryList.add(new Entry(iterator.key(), iterator.value()));
                }
            }
            return entryList;
        } catch (Exception ex) {
            return null;
        }
    }

    public static Transaction openSession(final String table) {
        WriteOptions options = new WriteOptions();
       // options.setSync(false);
        TransactionDB db = TABLES.get(table);
        return db.beginTransaction(options);
    }

    public static void commit(Transaction tx) throws RocksDBException {
        try {
            tx.commit();
        }finally {
            tx.close();
        }
    }

    public static void rollBack(Transaction tx) throws RocksDBException {
        try {
            tx.rollback();
        }finally {
            tx.close();
        }
    }

    /**
     * 数据库基本校验.
     * Basic database check
     *
     * @param tableName 数据库表名称
     * @return boolean 校验是否成功
     */
    private static boolean baseCheckTable(final String tableName) {
        if (StringUtils.isBlank(tableName) || !TABLES.containsKey(tableName)) {
            return false;
        }
        return true;
    }


    /**
     * 获得公共的数据库连接属性.
     *
     * @param createIfMissing 是否默认表
     * @return 数据库连接属性
     */
    private static synchronized Options getCommonOptions(final boolean createIfMissing) {
        Options options = new Options();
        options.setCreateIfMissing(createIfMissing);
        /**
         * 优化读取性能方案
         */
        options.setAllowMmapReads(true);
        options.setCompressionType(CompressionType.NO_COMPRESSION);
        options.setMaxOpenFiles(-1);

        BlockBasedTableConfig tableOption = new BlockBasedTableConfig();
        tableOption.setNoBlockCache(true);
        tableOption.setBlockRestartInterval(4);
        tableOption.setFilterPolicy(new BloomFilter(10, true));
        options.setTableFormatConfig(tableOption);

        return options;
    }
}
