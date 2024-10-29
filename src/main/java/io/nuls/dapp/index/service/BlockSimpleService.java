package io.nuls.dapp.index.service;

import io.nuls.dapp.index.config.ServerContext;
import io.nuls.dapp.index.constant.Constant;
import io.nuls.dapp.index.model.SimpleBlockHeader;
import io.nuls.dapp.index.rocksdb.service.RocksDBService;

import java.util.concurrent.ConcurrentHashMap;

import static io.nuls.dapp.index.rocksdb.util.DBUtils.stringToBytes;

/**
 * 区块高度同步操作
 */
public class BlockSimpleService {
    private static final SimpleBlockHeader EMPTY_HEADER = new SimpleBlockHeader();
    private static final String LOCAL_LATEST_BLOCK_HEADER_KEY = "LOCAL_LATEST_BLOCK_HEADER";
    private static final byte[] LOCAL_LATEST_BLOCK_HEADER_KEY_BYTES = stringToBytes(LOCAL_LATEST_BLOCK_HEADER_KEY);
    private final String KEY_PREFIX = "HEADER-";
    private static final ConcurrentHashMap<String, SimpleBlockHeader> localBlockHeaderMaps = new ConcurrentHashMap<>();

    ServerContext context;
    public BlockSimpleService(ServerContext context) {
        this.context = context;
    }

    /**
     * 保存本地区块头
     *
     * @return
     */
    public void saveLocalBlockHeader(SimpleBlockHeader blockHeader) throws Exception {
        localBlockHeaderMaps.put(LOCAL_LATEST_BLOCK_HEADER_KEY, blockHeader);
        this.save(blockHeader);
    }


    /**
     * 查询最新区块
     *
     * @return
     */
    public SimpleBlockHeader findByLatest() throws Exception {
        byte[] bytes = RocksDBService.get(Constant.DB_BLOCK_HEADER, LOCAL_LATEST_BLOCK_HEADER_KEY_BYTES);
        if (bytes == null) {
            return null;
        }
        SimpleBlockHeader header = new SimpleBlockHeader();
        header.parse(bytes, 0);
        return header;
    }

    /**
     * 获取本地的缓存的区块高度
     *
     * @return
     */
    public SimpleBlockHeader getLocalBlockHeader() throws Exception {
        //写到一个是否有，没有的话需要从0开始
        SimpleBlockHeader localBlockHeader = localBlockHeaderMaps.get(LOCAL_LATEST_BLOCK_HEADER_KEY);
        if (localBlockHeader == null) {
            localBlockHeader = this.findByLatest();
            if(localBlockHeader == null) {
                return EMPTY_HEADER;
            }
            localBlockHeaderMaps.putIfAbsent(LOCAL_LATEST_BLOCK_HEADER_KEY, localBlockHeader);
        }
        return localBlockHeader;
    }


    public SimpleBlockHeader findByHeight(long height) throws Exception {
        byte[] bytes = RocksDBService.get(Constant.DB_BLOCK_HEADER, stringToBytes(KEY_PREFIX + height));
        if (bytes == null) {
            return null;
        }
        SimpleBlockHeader header = new SimpleBlockHeader();
        header.parse(bytes, 0);
        return header;
    }

    /**
     * 保存
     */
    private void save(SimpleBlockHeader blockHeader) throws Exception {
        blockHeader.setCreateTime(System.currentTimeMillis() / 1000);
        byte[] blockBytes = blockHeader.serialize();
        RocksDBService.put(Constant.DB_BLOCK_HEADER, stringToBytes(KEY_PREFIX + blockHeader.getHeight()), blockBytes);
        RocksDBService.put(Constant.DB_BLOCK_HEADER, LOCAL_LATEST_BLOCK_HEADER_KEY_BYTES, blockBytes);
        // 只保留最近三个区块
        deleteByHeight(blockHeader.getHeight() - 3);
    }

    public void deleteByHeight(Long height) throws Exception {
        RocksDBService.delete(Constant.DB_BLOCK_HEADER, stringToBytes(KEY_PREFIX + height));
        SimpleBlockHeader latest = this.findByLatest();
        if (latest.getHeight().longValue() == height.longValue()) {
            SimpleBlockHeader header = this.findByHeight(height - 1);
            if(header != null) {
                RocksDBService.put(Constant.DB_BLOCK_HEADER, LOCAL_LATEST_BLOCK_HEADER_KEY_BYTES, header.serialize());
                localBlockHeaderMaps.put(LOCAL_LATEST_BLOCK_HEADER_KEY, header);
            } else {
                RocksDBService.delete(Constant.DB_BLOCK_HEADER, LOCAL_LATEST_BLOCK_HEADER_KEY_BYTES);
                localBlockHeaderMaps.remove(LOCAL_LATEST_BLOCK_HEADER_KEY);
            }
        }
    }
}
