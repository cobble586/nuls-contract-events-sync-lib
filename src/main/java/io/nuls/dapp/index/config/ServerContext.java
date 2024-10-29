package io.nuls.dapp.index.config;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Block;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.dapp.index.constant.Constant;
import io.nuls.dapp.index.rocksdb.service.RocksDBService;
import io.nuls.dapp.index.scheduled.BlockQueueTask;
import io.nuls.dapp.index.scheduled.BlockScheduled;
import io.nuls.dapp.index.service.BlockSimpleService;
import io.nuls.dapp.index.service.BlockSyncService;
import io.nuls.dapp.index.service.IEventProcessor;
import io.nuls.dapp.index.service.api.BlockServiceApi;
import io.nuls.dapp.index.util.NulsApiUtil;
import io.nuls.v2.NulsSDKBootStrap;
import io.nuls.v2.SDKContext;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class ServerContext {
    final Logger logger = LoggerFactory.getLogger(getClass());

    public String DEFAULT_ENCODING = "UTF-8";
    /**
     * 本链id
     */
    public int chainId = 2;
    /**
     * 本链主资产id
     */
    public int assetId = 1;
    /**
     * 项目验证码
     */
    public String uuid = "";
    public String providerHost = "https://api.nuls.io/";
    public Set<String> listeningAddressSet = new HashSet<>();
    public long syncDefaultStartHeight = 0L;
    public boolean devMode;

    public String rocksdbDataPath;
    public LinkedBlockingDeque<Block> blockQueue = new LinkedBlockingDeque<>();
    private Future<?> initBlockFutrue;
    private final ExecutorService singleThreadPool = Executors.newSingleThreadExecutor();

    public BlockScheduled blockScheduled = new BlockScheduled(this);
    public BlockServiceApi blockServiceApi = new BlockServiceApi(this);
    public BlockSimpleService blockSimpleService = new BlockSimpleService(this);
    public BlockSyncService blockSyncService = new BlockSyncService(this);

    public Future<?> getInitBlockFutrue() {
        return initBlockFutrue;
    }

    public void setInitBlockFutrue(Future<?> initBlockFutrue) {
        this.initBlockFutrue = initBlockFutrue;
    }

    public ExecutorService getSingleThreadPool() {
        return singleThreadPool;
    }

    boolean initialLoaded = false;

    public void addEventProcessor(IEventProcessor processor) {
        this.blockSyncService.addEventProcessor(processor);
    }

    public synchronized void initialLoad() throws Exception {
        if(initialLoaded) {
            return;
        }
        if (this.blockSyncService.getEventProcessorListSize() == 0) {
            throw new RuntimeException("empty event processors");
        }
        if (this.listeningAddressSet.isEmpty()) {
            throw new RuntimeException("empty listening Address Set");
        }

        initialLoaded = true;

        SDKContext.wallet_url = providerHost;
        RpcResult info = NulsApiUtil.jsonRpcRequest("info", ListUtil.of());
        Map result = (Map) info.getResult();
        Integer chainId = (Integer) result.get("chainId");
        this.chainId = chainId != null ? chainId : this.chainId;
        Integer assetId = (Integer) result.get("assetId");
        this.assetId = assetId != null ? assetId : this.assetId;
        if (this.syncDefaultStartHeight > 0) {
            this.syncDefaultStartHeight -= 1;
        }
        // initial SDK
        NulsSDKBootStrap.init(this.chainId, providerHost);

        String firstListening = this.listeningAddressSet.iterator().next();
        // initial address prefix
        String prefix = AddressTool.getPrefix(firstListening);
        AddressTool.addPrefix(this.chainId, prefix);
        SDKContext.addressPrefix = prefix;


        try {
            //数据文件存储地址
            RocksDBService.init(rocksdbDataPath);
            //模块配置表
            RocksDBService.createTable(Constant.DB_BLOCK_HEADER);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void run() throws Exception {
        this.initBlockFutrue = singleThreadPool.submit(new BlockQueueTask(this));
        ScheduledThreadPoolExecutor scheduledThreadPool = ThreadUtils.createScheduledThreadPool(1, new NulsThreadFactory("nuls-block-sync"));
        scheduledThreadPool.scheduleWithFixedDelay(blockScheduled, 1, 10, TimeUnit.SECONDS);
    }


}
