package io.nuls.dapp.index.model;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;

import java.io.IOException;
import java.io.Serializable;

/**
 * 同步区块的高度
 *
 * @author wangkun23
 */
public class SimpleBlockHeader extends BaseNulsData implements Serializable {

    private Long height;
    private String hash;
    private String preHash;
    private Long blockTime;
    private Long createTime;

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreHash() {
        return preHash;
    }

    public void setPreHash(String preHash) {
        this.preHash = preHash;
    }

    public Long getBlockTime() {
        return blockTime;
    }

    public void setBlockTime(Long blockTime) {
        this.blockTime = blockTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfUint32();
        size += NulsHash.HASH_LENGTH;
        size += NulsHash.HASH_LENGTH;
        size += SerializeUtils.sizeOfUint32();
        size += SerializeUtils.sizeOfUint32();
        return size;
    }

    @Override
    public void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeUint32(height);
        stream.write(HexUtil.decode(hash));
        stream.write(HexUtil.decode(preHash));
        stream.writeUint32(blockTime);
        stream.writeUint32(createTime);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        height = byteBuffer.readUint32();
        hash = HexUtil.encode(byteBuffer.readBytes(NulsHash.HASH_LENGTH));
        preHash = HexUtil.encode(byteBuffer.readBytes(NulsHash.HASH_LENGTH));
        blockTime = byteBuffer.readUint32();
        createTime = byteBuffer.readUint32();
    }
}