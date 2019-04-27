package com.alibaba.dubbo.rpc.cluster.router.custom;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yangzhibin@kaiyuan.net
 * 自定义路由
 * 功能:
 * 根据服务器IP去路由服务
 */
public class CustomRouter implements Router {

    private static final Logger logger = LoggerFactory.getLogger(CustomRouter.class);
    private static RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    private static final String PATH = "/dubbo-custom-router/blacklist";
    private static CuratorFramework client;
    private List<String> limitIpList = new ArrayList<String>();
    private final URL url;

    public CustomRouter(URL url) {
        this.url = url;
        try {
            String connectString = url.getBackupAddress();
            client = CuratorFrameworkFactory.builder()
                    .connectString(connectString)
                    .sessionTimeoutMs(3000)
                    .connectionTimeoutMs(5000)
                    .retryPolicy(retryPolicy)
                    .build();
            client.start();
            final NodeCache cache = new NodeCache(client, PATH);
            NodeCacheListener listener = new NodeCacheListener() {
                @Override
                public void nodeChanged() {
                    ChildData data = cache.getCurrentData();
                    if (null != data) {
                        String limitIpStr = new String(cache.getCurrentData().getData());
                        //去除limitStr头尾空白
                        limitIpStr = limitIpStr.trim();
                        logger.info("###" + PATH + "节点数据:" + limitIpStr);
                        if (StringUtils.isNotEmpty(limitIpStr)) {
                            limitIpList = Arrays.asList(limitIpStr.split(","));
                        }else {
                            limitIpList = new ArrayList<String>();
                        }
                    } else {
                        limitIpList = new ArrayList<String>();
                        logger.info("###" + PATH + "节点数据为空###");
                    }
                }
            };
            cache.getListenable().addListener(listener);
            cache.start();
        } catch (Exception e) {
            logger.error("###自定义路由连接zk失败##失败原因:", e);
        }
    }

    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers, URL url, final Invocation invocation) throws RpcException {
        List<Invoker<T>> result = new ArrayList<Invoker<T>>();
        for (Invoker invoker : invokers) {
            String providerIp = getIp(invoker.getUrl());
            logger.info("###limitIpList=" + limitIpList);
            if (!limitIpList.contains(providerIp)) {
                result.add(invoker);
            }
        }
        logger.info("###custom-router##url=" + url);
        return result;
    }

    private String getIp(URL url) {
        String sURL = url.toString();
        int begin = sURL.indexOf("://");
        int end = sURL.lastIndexOf("/");
        String substring = sURL.substring(begin + 3, end);
        String[] split = substring.split(":");
        String ip = split[0];
        return ip;
    }

    @Override
    public URL getUrl() {
        return null;
    }


    @Override
    public int compareTo(Router o) {
        return 1;
    }
}