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
package io.nuls.dapp.index.processor;

import com.alibaba.fastjson.JSONObject;
import io.nuls.base.basic.AddressTool;
import io.nuls.dapp.index.event.AgentEvent;
import io.nuls.dapp.index.model.contract.EventJson;
import io.nuls.dapp.index.service.IEventProcessor;
import io.nuls.v2.txdata.ContractData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2019-08-12
 */
public class TestEventProcessor implements IEventProcessor {

    final Logger logger = LoggerFactory.getLogger(getClass());
    static final String AGENT_EVENT = "AgentEvent";

    @Override
    public void execute(String hash, int txType, ContractData contractData, EventJson eventJson) throws Exception{
        String event = eventJson.getEvent();
        switch (event) {
            case AGENT_EVENT: agentEvent(hash, txType, contractData, eventJson);break;
            default:
                logger.error("unkown event [{}]", event);
        }
    }

    private void agentEvent(String hash, int txType, ContractData contractData, EventJson eventJson) throws Exception {
        String contractAddress = eventJson.getContractAddress();
        logger.info("解析到添加节点事件 - {}", eventJson);
        String contractSender = AddressTool.getStringAddressByBytes(contractData.getSender());
        long blockNumber = eventJson.getBlockNumber();
        JSONObject payload = eventJson.getPayload();
        AgentEvent event = payload.toJavaObject(AgentEvent.class);
        String agentHash = event.getHash();
        BigInteger value = event.getValue();

        logger.info("hash: {}, contractAddress: {}, sender: {}, blockNumber: {}, agentHash: {}, value: {}",
                hash, contractAddress, contractSender, blockNumber, agentHash, value.toString());
        //TODO something else
    }
}
