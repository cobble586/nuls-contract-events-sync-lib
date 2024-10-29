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
package io.nuls.dapp.index.util;

import io.nuls.core.basic.Result;
import io.nuls.core.model.StringUtils;
import io.nuls.v2.SDKContext;
import io.nuls.v2.model.dto.ContractViewCallForm;
import io.nuls.v2.model.dto.RpcResult;
import io.nuls.v2.model.dto.RpcResultError;
import io.nuls.v2.util.HttpClientUtil;
import io.nuls.v2.util.JsonRpcUtil;
import io.nuls.v2.util.NulsSDKTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2019-08-18
 */
public class NulsApiUtil {

    static final Logger logger = LoggerFactory.getLogger(NulsApiUtil.class);
    public final static BigInteger ONE_NULS = BigInteger.valueOf(100000000L);

    public static BigDecimal toNuls(BigInteger na) {
        return new BigDecimal(na).movePointLeft(8);
    }

    public static BigInteger toNa(BigDecimal nuls) {
        return nuls.scaleByPowerOfTen(8).toBigInteger();
    }


    public static Result invokeView(ContractViewCallForm form) throws InterruptedException {
        Result result = NulsSDKTool.invokeView(form);
        int retryTimes = 3;
        int count = 0;
        while (isTimedOut(result)) {
            logger.warn("InvokeView - url [{}], request [{}] timed out!", SDKContext.wallet_url, form.getMethodName());
            result = NulsSDKTool.invokeView(form);
            if(++count >= retryTimes) {
                break;
            }
            Thread.sleep(500);
        }
        return result;
    }

    public static RpcResult jsonRpcRequest(String url, String method, List<Object> params, int retryTimes) throws InterruptedException {
        RpcResult rpcResult = JsonRpcUtil.request(url, method, params);
        int count = 0;
        while (isTimedOut(rpcResult)) {
            logger.warn("JsonRpcRequest - url [{}], request [{}] timed out!", url, method);
            rpcResult = JsonRpcUtil.request(url, method, params);
            if(retryTimes != -1 && ++count >= retryTimes) {
                break;
            }
            if(count % 3 == 0) {
                HttpClientUtil.resetHttpClient(url);
            }
            Thread.sleep(500);
        }
        return rpcResult;
    }

    public static RpcResult jsonRpcRequest(String url, String method, List<Object> params) throws InterruptedException {
        return jsonRpcRequest(url, method, params, 3);
    }

    public static RpcResult jsonRpcRequest(String method, List<Object> params, int retryTimes) throws InterruptedException {
        return jsonRpcRequest(SDKContext.wallet_url + "jsonrpc", method, params, retryTimes);
    }

    public static RpcResult jsonRpcRequest(String method, List<Object> params) throws InterruptedException {
        return jsonRpcRequest(SDKContext.wallet_url + "jsonrpc", method, params, 3);
    }

    private static boolean isTimedOut(Result result) {
        if(result.isSuccess()) {
            return false;
        }
        String errorMsg = result.getMsg();
        if(StringUtils.isBlank(errorMsg)) {
            return false;
        }
        return errorMsg.contains("timed out");
    }

    private static boolean isTimedOut(RpcResult result) {
        RpcResultError error = result.getError();
        if(error == null) {
            return false;
        }
        String errorMsg = error.getMessage();
        if(StringUtils.isBlank(errorMsg)) {
            return false;
        }
        return errorMsg.contains("timed out");
    }




}
