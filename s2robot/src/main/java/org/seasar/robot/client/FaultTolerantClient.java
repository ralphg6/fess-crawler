/*
 * Copyright 2004-2013 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.robot.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.seasar.robot.Constants;
import org.seasar.robot.MaxLengthExceededException;
import org.seasar.robot.RobotMultipleCrawlAccessException;
import org.seasar.robot.client.fs.ChildUrlsException;
import org.seasar.robot.entity.ResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shinsuke
 * 
 */
public class FaultTolerantClient implements S2RobotClient {

    private static final Logger logger = LoggerFactory // NOPMD
        .getLogger(FaultTolerantClient.class);

    protected S2RobotClient client;

    protected int maxRetryCount = 5;

    protected long retryInterval = 500;

    protected RequestListener listener;

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.seasar.robot.client.S2RobotClient#setInitParameterMap(java.util.Map)
     */
    @Override
    public void setInitParameterMap(final Map<String, Object> params) {
        client.setInitParameterMap(params);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.seasar.robot.client.S2RobotClient#doGet(java.lang.String)
     */
    @Override
    public ResponseData doGet(final String url) {
        return doRequest(Constants.GET_METHOD, url, new Requester() {
            @Override
            public ResponseData execute(final String url) {
                return client.doGet(url);
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.seasar.robot.client.S2RobotClient#doHead(java.lang.String)
     */
    @Override
    public ResponseData doHead(final String url) {
        return doRequest(Constants.HEAD_METHOD, url, new Requester() {
            @Override
            public ResponseData execute(final String url) {
                return client.doHead(url);
            }
        });
    }

    protected ResponseData doRequest(final String method, final String url,
            final Requester requester) {
        if (listener != null) {
            listener.onRequestStart(this, method, url);
        }

        List<Exception> exceptionList = null;
        try {
            int count = 0;
            while (count < maxRetryCount) {
                if (listener != null) {
                    listener.onRequest(this, method, url, count);
                }

                try {
                    return requester.execute(url);
                } catch (final MaxLengthExceededException e) {
                    throw e;
                } catch (final ChildUrlsException e) {
                    throw e;
                } catch (final Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to access to " + url, e);
                    }

                    if (listener != null) {
                        listener.onException(this, method, url, count, e);
                    }

                    if (exceptionList == null) {
                        exceptionList = new ArrayList<Exception>();
                    }
                    exceptionList.add(e);
                }

                try {
                    Thread.sleep(retryInterval);
                } catch (final InterruptedException e) {
                    // ignore
                }
                count++;
            }
            throw new RobotMultipleCrawlAccessException(
                "Failed to access to " + url,
                exceptionList.toArray(new Throwable[exceptionList.size()]));
        } finally {
            if (listener != null) {
                listener.onRequestEnd(this, method, url, exceptionList);
            }
        }
    }

    public S2RobotClient getRobotClient() {
        return client;
    }

    public void setRobotClient(final S2RobotClient client) {
        this.client = client;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(final int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(final long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public RequestListener getRequestListener() {
        return listener;
    }

    public void setRequestListener(final RequestListener listener) {
        this.listener = listener;
    }

    interface Requester {
        ResponseData execute(String url);
    }

    public interface RequestListener {

        void onRequestStart(FaultTolerantClient client, String method,
                String url);

        void onRequest(FaultTolerantClient client, String method, String url,
                int count);

        void onRequestEnd(FaultTolerantClient client, String method,
                String url, List<Exception> exceptionList);

        void onException(FaultTolerantClient client, String method, String url,
                int count, Exception e);

    }
}
