package io.nuls.dapp.index.scheduled;

import io.nuls.base.data.Block;
import io.nuls.dapp.index.config.ServerContext;
import io.nuls.dapp.index.service.BlockSimpleService;
import io.nuls.dapp.index.service.api.BlockServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 查询区块的定时器
 *
 * @author: PierreLuo
 * @date: 2019-08-09
 */
public class BlockQueueTask implements Runnable {

    final Logger logger = LoggerFactory.getLogger(getClass());
    private final SyncError syncError = new SyncError();
    private final BlockServiceApi blockServiceApi;
    private final BlockSimpleService blockSimpleService;
    private final List<Block> list = new ArrayList<>();

    ServerContext context;
    public BlockQueueTask(ServerContext context) {
        this.context = context;
        this.blockServiceApi = context.blockServiceApi;
        this.blockSimpleService = context.blockSimpleService;
    }

    private void add(Block block) {
        synchronized (list) {
            list.add(block);
        }
    }

    /**
     * 检测区块的高度变化 查询区块数据
     */
    @Override
    public void run() {
        synchronized (blockServiceApi) {
            if (syncError.isError()) {
                logger.error("获取区块错误 height: {}", syncError.getErrorHeight());
                return;
            }
            Long localBlockHeight = null;
            try {
                localBlockHeight = blockSimpleService.getLocalBlockHeader().getHeight();
                if(localBlockHeight == null) {
                    localBlockHeight = context.syncDefaultStartHeight;
                }
                logger.info("每10秒 查询区块 height: {}", localBlockHeight);
                Long height = blockServiceApi.getNewestBlockHeight();
                Long between = height - localBlockHeight;
                logger.info("between {}", between);
                Long tempHeight = localBlockHeight;
                int threadCount;
                ExecutorService threadPool;
                if (between == 1) {
                    threadCount = 1;
                    threadPool = Executors.newSingleThreadExecutor();
                } else if (between < 5) {
                    threadCount = 2;
                    threadPool = Executors.newFixedThreadPool(threadCount);
                } else if (between < 10) {
                    threadCount = 5;
                    threadPool = Executors.newFixedThreadPool(threadCount);
                } else {
                    threadCount = 30;
                    threadPool = Executors.newFixedThreadPool(threadCount);
                }
                for (int i = 1; i <= between; i = i + threadCount) {
                    long tempTempHeight = tempHeight;
                    // 计算当前批次要下载的区块数量（最后一个批次数量可能小于threadCount）
                    int size;
                    if ((between - i) < threadCount) {
                        size = (int) (between - i + 1);
                    } else {
                        size = threadCount;
                    }
                    int queueSize;
                    while((queueSize = context.blockQueue.size()) > 1000) {
                        logger.info("下载区块未处理队列大于1000，等待10秒再下载，当前队列大小: {}", queueSize);
                        TimeUnit.SECONDS.sleep(10);
                    }
                    CountDownLatch countDownLatch = new CountDownLatch(size);
                    for (int t = 0; t < size; t++) {
                        tempHeight = tempHeight + 1;
                        threadPool.submit(new GetBlock(blockServiceApi, tempHeight, countDownLatch));
                    }
                    countDownLatch.await();
                    // 判断是否全部成功下载区块
                    if(list.size() != size) {
                        logger.warn("该批次未完整下载区块, 区间: [{} - {}]", tempTempHeight, tempTempHeight + size);
                        // 没有全部成功，还原批次变量，重新下载这个批次的区块
                        i = i - threadCount;
                        tempHeight = tempTempHeight;
                        list.clear();
                        continue;
                    }
                    list.sort(new Comparator<Block>() {
                        @Override
                        public int compare(Block o1, Block o2) {
                            long height1 = o1.getHeader().getHeight();
                            long height2 = o2.getHeader().getHeight();
                            if (height1 > height2) {
                                return 1;
                            } else if (height1 < height2) {
                                return -1;
                            }
                            return 0;
                        }
                    });
                    list.stream().forEach(block -> context.blockQueue.offer(block));
                    list.clear();
                }
                threadPool.shutdown();
            } catch (Exception e) {
                logger.error("syncHeight error ", e);
                syncError.setError();
                syncError.setErrorHeight(localBlockHeight);
            }
        }
    }

    class GetBlock implements Runnable {
        private long height;
        private CountDownLatch countDownLatch;
        private BlockServiceApi blockServiceApi;

        public GetBlock(BlockServiceApi blockServiceApi, long height, CountDownLatch countDownLatch) {
            this.blockServiceApi = blockServiceApi;
            this.height = height;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                Block block = blockServiceApi.getBlockByHeight(height);
                BlockQueueTask.this.add(block);
                logger.debug("成功下载区块，高度: {}", height);
            } catch (Exception e) {
                logger.error("下载区块失败", e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }

}
