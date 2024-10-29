package io.nuls.dapp.index.service;

import com.alibaba.fastjson.JSON;
import io.nuls.base.data.Block;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.dapp.index.config.ServerContext;
import io.nuls.dapp.index.model.SimpleBlockHeader;
import io.nuls.dapp.index.model.contract.CallContractDataWrapper;
import io.nuls.dapp.index.model.contract.CreateContractDataWrapper;
import io.nuls.dapp.index.model.contract.EventJson;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.txdata.ContractData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 区块同步
 *
 * @author: PierreLuo
 * @date: 2019-08-09
 */
public class BlockSyncService {



    final Logger logger = LoggerFactory.getLogger(getClass());
    ServerContext context;
    public BlockSyncService(ServerContext context) {
        this.context = context;
    }

    private List<IEventProcessor> eventProcessorList = new ArrayList<>();

    public int getEventProcessorListSize() {
        return eventProcessorList.size();
    }

    public void addEventProcessor(IEventProcessor processor) {
        if (eventProcessorList.contains(processor)) {
            return;
        }
        this.eventProcessorList.add(processor);
    }

    /**
     * 解析区块
     */
    public SimpleBlockHeader syncBlock(Block block) throws Exception {
        BlockHeader header = block.getHeader();
        Long height = header.getHeight();
        List<Transaction> txs = block.getTxs();
        List<String> hashList = new ArrayList<>();
        Map<String, Transaction> contractTxMap = new HashMap<>();
        int txType;
        String txHash;
        for (Transaction tx : txs) {
            txType = tx.getType();
            if (txType == TxType.CALL_CONTRACT) {
                txHash = tx.getHash().toHex();
                contractTxMap.put(txHash, tx);
                hashList.add(txHash);
            }
        }
        do {
            if(hashList.isEmpty()) {
                break;
            }
            RpcResult<Map<String, Map>> rpcResult = context.blockServiceApi.getContractTxResultList(hashList);
            if(rpcResult.getError() != null) {
                throw new RuntimeException(rpcResult.getError().toString());
            }
            Map<String, Map> result = rpcResult.getResult();
            if(result != null) {
                for (String hash : hashList) {
                    Map contractResult = result.get(hash);
                    Transaction ctx = contractTxMap.get(hash);
                    int type = ctx.getType();
                    // 处理合约事件
                    List<String> events = (List<String>) contractResult.get("events");
                    if(events.isEmpty()) {
                        continue;
                    }
                    ContractData contractData = parseContractTxData(ctx);

                    for (String event : events) {
                        EventJson eventJson = JSON.parseObject(event, EventJson.class);
                        if (!context.listeningAddressSet.contains(eventJson.getContractAddress())) {
                            // 跳过未监听的合约地址
                            continue;
                        }
                        // 合约事件处理器
                        for(IEventProcessor processor : eventProcessorList) {
                            processor.execute(hash, type, contractData, eventJson);
                        }
                    }
                }
            }
        } while (false);
        SimpleBlockHeader simpleBlockHeader = new SimpleBlockHeader();
        simpleBlockHeader.setHeight(height);
        simpleBlockHeader.setHash(header.getHash().toHex());
        simpleBlockHeader.setPreHash(header.getPreHash().toHex());
        simpleBlockHeader.setBlockTime(header.getTime());
        return simpleBlockHeader;
    }

    private ContractData parseContractTxData(Transaction tx) {
        int txType = tx.getType();
        if(txType == TxType.CREATE_CONTRACT) {
            return new CreateContractDataWrapper(tx.getTxData());
        } else if(txType == TxType.CALL_CONTRACT) {
            return new CallContractDataWrapper(tx.getTxData());
        } else {
            return null;
        }
    }

}
