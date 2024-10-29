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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author: PierreLuo
 * @date: 2019-08-16
 */
public class TimeUtil {
    private static final ZoneOffset ZONE = ZoneOffset.of("+8");
    private static final String PATTERN = "yyyyMMddHHmmss";
    private static DateTimeFormatter df = DateTimeFormatter.ofPattern(PATTERN);
    private static DateTimeFormatter dfStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Long now() {
        LocalDateTime localDateTime = LocalDateTime.now(ZONE);
        return Long.parseLong(localDateTime.format(df));
    }

    public static Long revertToTimeMillisUTC(Long now) {
        LocalDateTime time = LocalDateTime.parse(now.toString(), df);
        return time.toInstant(ZONE).toEpochMilli();
    }

    /**
     * 时间戳转LocalDateTime
     * @param timestamp 时间戳
     * @return LocalDateTime
     */
    private static LocalDateTime timestampToLocalDateTime(long timestamp) {
        try {
            Instant instant = Instant.ofEpochMilli(timestamp);
            return LocalDateTime.ofInstant(instant, ZONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String timeFormat(long timestamp) {
        LocalDateTime localDateTime = timestampToLocalDateTime(timestamp);
        return localDateTime.format(dfStr);
    }
}
