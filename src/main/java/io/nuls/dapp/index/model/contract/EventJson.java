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
package io.nuls.dapp.index.model.contract;

import com.alibaba.fastjson.JSONObject;

/**
 * @author: PierreLuo
 * @date: 2019-08-12
 */
public class EventJson {
    private String contractAddress;
    private long blockNumber;
    private String event;
    private JSONObject payload;

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventJson eventJson = (EventJson) o;

        if (blockNumber != eventJson.blockNumber) return false;
        if (!contractAddress.equals(eventJson.contractAddress)) return false;
        if (!event.equals(eventJson.event)) return false;
        return payload.equals(eventJson.payload);
    }

    @Override
    public int hashCode() {
        int result = contractAddress.hashCode();
        result = 31 * result + (int) (blockNumber ^ (blockNumber >>> 32));
        result = 31 * result + event.hashCode();
        result = 31 * result + payload.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"contractAddress\":")
                .append('\"').append(contractAddress).append('\"');

        sb.append(",\"blockNumber\":")
                .append(blockNumber);

        sb.append(",\"event\":")
                .append('\"').append(event).append('\"');

        sb.append(",\"payload\":")
                .append('{').append(payload).append('}');

        sb.append('}');
        return sb.toString();
    }
}
