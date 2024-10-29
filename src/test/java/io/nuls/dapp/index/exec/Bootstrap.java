/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.dapp.index.exec;

import io.nuls.dapp.index.config.ServerContext;
import io.nuls.dapp.index.processor.TestEventProcessor;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * @author: PierreLuo
 * @date: 2024/10/29
 */
public class Bootstrap {
    final Logger logger = LoggerFactory.getLogger(getClass());
    ServerContext context;

    @Before
    public void before() throws Exception {
        context = new ServerContext();// 实例化 ServerContext
        context.addEventProcessor(new TestEventProcessor());// 添加事件处理器
        context.providerHost = "https://beta.api.nuls.io/";// NULS节点api url
        context.listeningAddressSet.add("tNULSeBaN1KADdp1qr2wTeSKpRfDdACU9DpJ6h");// 要监听的合约地址
        context.syncDefaultStartHeight = 11298818;// 同步区块的起始高度
        context.rocksdbDataPath = "/Users/pierreluo/IdeaProjects/nuls-contract-events-sync-lib/data";// 记录区块高度
        context.initialLoad();
    }

    @Test
    public void runTest() throws Exception {
        context.run();// 执行同步

        // 阻塞线程终止 - 根据项目情况来定是否需要阻塞线程
        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("减少计数，线程终止");
            latch.countDown();
        }));
        latch.await();
    }
}
