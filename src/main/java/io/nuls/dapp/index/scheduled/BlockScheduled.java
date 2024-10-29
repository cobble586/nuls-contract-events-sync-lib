package io.nuls.dapp.index.scheduled;

import io.nuls.base.data.Block;
import io.nuls.dapp.index.config.ServerContext;
import io.nuls.dapp.index.model.SimpleBlockHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * 解析区块的定时器
 *
 * @author: PierreLuo
 * @date: 2019-08-09
 */
public class BlockScheduled implements Runnable {

    final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean initialLoaded = false;
    private boolean switchBlockSync = false;
    private SyncError syncError = new SyncError();

    ServerContext context;
    public BlockScheduled(ServerContext context) {
        this.context = context;
    }

    /**
     * 检测区块的高度变化 查询交易数据
     */
    @Override
    public void run() {
        if(syncError.isError()) {
            logger.error("同步区块错误 height: {}", syncError.getErrorHeight());
            return;
        }
        // 初始化系统统计数据
        initialLoaded = true;
        Long localBlockHeight = null;
        try {
            localBlockHeight = context.blockSimpleService.getLocalBlockHeader().getHeight();
            if(localBlockHeight == null) {
                localBlockHeight = context.syncDefaultStartHeight;
            }
            logger.info("每10秒检测区块高度,检测区块的高度变化 查询交易数据 height: {}", localBlockHeight);
            Long height = context.blockServiceApi.getNewestBlockHeight();
            Long between = height - localBlockHeight;
            logger.info("between {}", between);
            SimpleBlockHeader simpleBlockHeader;
            for (int i = 1; i <= between; i++) {
                localBlockHeight = localBlockHeight + 1;
                /**
                 * 同步并解析数据
                 */
                Block block = null;
                try {
                    if(!switchBlockSync) {
                        LinkedBlockingDeque<Block> queue = context.blockQueue;
                        block = queue.poll(8, TimeUnit.SECONDS);
                        if(block == null) {
                            if(!context.getInitBlockFutrue().isDone()) {
                                return;
                            } else {
                                switchBlockSync = true;
                                block = context.blockServiceApi.getBlockByHeight(localBlockHeight);
                                context.getSingleThreadPool().shutdown();
                            }
                        }
                    } else {
                        block = context.blockServiceApi.getBlockByHeight(localBlockHeight);
                    }

                    simpleBlockHeader = context.blockSyncService.syncBlock(block);
                    context.blockSimpleService.saveLocalBlockHeader(simpleBlockHeader);
                } catch (Exception e) {
                    if(!switchBlockSync && block != null) {
                        logger.error("syncHeight error height [{}]", block.getHeader().getHeight());
                        context.blockQueue.offerFirst(block);
                    }
                    logger.error("syncHeight error ", e);
                    syncError.setError();
                    syncError.setErrorHeight(localBlockHeight);
                    break;
                }
                logger.info("保存区块 {}", localBlockHeight);
            }
        } catch (Exception e) {
            logger.error("syncHeight error ", e);
            syncError.setError();
            syncError.setErrorHeight(localBlockHeight);
        }
    }

}
