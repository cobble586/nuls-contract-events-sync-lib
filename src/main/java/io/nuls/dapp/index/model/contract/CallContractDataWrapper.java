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

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.exception.NulsException;
import io.nuls.v2.txdata.CallContractData;
import io.nuls.v2.txdata.ContractData;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2019-10-10
 */
public class CallContractDataWrapper implements ContractData{

    private byte[] callContractDataBytes;
    private CallContractData callContractData;

    public CallContractDataWrapper(byte[] callContractDataBytes) {
        this.callContractDataBytes = callContractDataBytes;
    }

    private CallContractData getCallContractData() {
        if(callContractData == null) {
            callContractData = new CallContractData();
            try {
                callContractData.parse(new NulsByteBuffer(callContractDataBytes));
            } catch (NulsException e) {
                e.printStackTrace();
            }
        }
        return callContractData;
    }

    public byte[] getSender() {
        return getCallContractData().getSender();
    }

    public byte[] getCode() {
        return getCallContractData().getCode();
    }

    public byte[] getContractAddress() {
        return getCallContractData().getContractAddress();
    }

    public BigInteger getValue() {
        return getCallContractData().getValue();
    }

    public long getGasLimit() {
        return getCallContractData().getGasLimit();
    }

    public long getPrice() {
        return getCallContractData().getPrice();
    }

    public String getMethodName() {
        return getCallContractData().getMethodName();
    }

    public String getMethodDesc() {
        return getCallContractData().getMethodDesc();
    }

    public String[][] getArgs() {
        return getCallContractData().getArgs();
    }

}
