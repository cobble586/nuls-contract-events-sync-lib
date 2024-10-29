package io.nuls.dapp.index.service.api;

import com.alibaba.fastjson.JSONObject;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Block;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import io.nuls.dapp.index.config.ServerContext;
import io.nuls.dapp.index.model.SimpleBlockHeader;
import io.nuls.dapp.index.util.NulsApiUtil;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.model.dto.TransactionDto;
import io.nuls.v2.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 区块接口服务
 * Created by wangkun23 on 2018/9/5.
 */
public class BlockServiceApi {

    final Logger logger = LoggerFactory.getLogger(getClass());

    ServerContext context;
    public BlockServiceApi(ServerContext context) {
        this.context = context;
    }
    /**
     * 获取最新区块头
     *
     * @return
     */
    public SimpleBlockHeader getNewestBlockHeader() throws Exception {
        RpcResult rpcResult = NulsApiUtil.jsonRpcRequest("getBestBlockHeader", ListUtil.of(context.chainId));
        if(rpcResult == null) {
            logger.error("empty block about getting newest block!!!");
            throw new Exception("empty Block");
        }
        if(rpcResult.getError() != null) {
            logger.error("error block about getting newest block !!! - {[]}", rpcResult.getError().toString());
            throw new Exception(rpcResult.getError().toString());
        }
        Map result = (Map) rpcResult.getResult();
        SimpleBlockHeader header = new SimpleBlockHeader();
        header.setHash((String) result.get("hash"));
        header.setPreHash((String) result.get("preHash"));
        header.setHeight(Long.parseLong(result.get("height").toString()));
        return header;
    }

    /**
     * 获取最新高度
     *
     * @return
     */
    public Long getNewestBlockHeight() throws Exception {
        RpcResult rpcResult = NulsApiUtil.jsonRpcRequest("getLatestHeight", ListUtil.of(context.chainId));
        if(rpcResult == null) {
            logger.error("empty block about getting newest block height[0]!!!");
            throw new Exception("empty Block height[0]");
        }
        if(rpcResult.getError() != null) {
            logger.error("error block about getting newest block height!!! - {[]}", rpcResult.getError().toString());
            throw new Exception(rpcResult.getError().toString());
        }
        Object result = rpcResult.getResult();
        if(result == null) {
            logger.error("empty block about getting newest block height[1]!!!");
            throw new Exception("empty Block height[1]");
        }
        Long height = Long.parseLong(result.toString());
        return height;
    }

    /**
     * 根据高度获取区块
     *
     * @return
     */
    public Block getBlockByHeight(Long height) throws Exception {
        RpcResult rpcResult = NulsApiUtil.jsonRpcRequest("getBlockSerializationByHeight", ListUtil.of(context.chainId, height));
        if(rpcResult == null) {
            logger.error("empty block about getting block by height!!!");
            throw new Exception("empty Block");
        }
        if(rpcResult.getError() != null) {
            logger.error("error block about getting block by height!!! - {[]}", rpcResult.getError().toString());
            throw new Exception(rpcResult.getError().toString());
        }
        String blockHex = (String) rpcResult.getResult();
        Block block = new Block();
        block.parse(new NulsByteBuffer(HexUtil.decode(blockHex)));
        return block;
    }

    /**
     * 根据hash获取区块
     *
     * @return
     */
    public Block getBlockByHash(String hash) throws Exception {
        RpcResult rpcResult = NulsApiUtil.jsonRpcRequest("getBlockSerializationByHash", ListUtil.of(context.chainId, hash));
        String blockHex = (String) rpcResult.getResult();
        Block block = new Block();
        block.parse(new NulsByteBuffer(HexUtil.decode(blockHex)));
        return block;
    }

    public RpcResult<Map<String, Map>> getContractTxResultList(List<String> hashList) throws InterruptedException {
        RpcResult<Map<String, Map>> rpcResult = NulsApiUtil.jsonRpcRequest("getContractTxResultList", ListUtil.of(context.chainId, hashList));
        return rpcResult;
    }

    /**
     * 根据hash查询交易
     *
     * @param hash
     * @return
     */
    public TransactionDto getTxByHash(String hash) throws Exception {
        if(StringUtils.isBlank(hash)) {
            return null;
        }
        RpcResult rpcResult = NulsApiUtil.jsonRpcRequest("getTx", ListUtil.of(context.chainId, hash));
        if(rpcResult == null) {
            logger.error("empty tx!!!");
            throw new Exception("empty tx");
        }
        if(rpcResult.getError() != null) {
            logger.error("error tx!!! - {[]}", rpcResult.getError().toString());
            throw new Exception(rpcResult.getError().toString());
        }
        JSONObject jsonObject = new JSONObject((Map<String, Object>) rpcResult.getResult());
        TransactionDto transactionDto = jsonObject.toJavaObject(TransactionDto.class);
        return transactionDto;
    }
}
