package com.alibaba.dubbo.rpc.cluster.router.custom;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
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
    private static CuratorFramework client;
    private final URL url;

    public CustomRouter(URL url) {
        this.url = url;
        String connectString = url.getBackupAddress();
        client = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .sessionTimeoutMs(3000)
                .connectionTimeoutMs(5000)
                .retryPolicy(retryPolicy)
                .build();
    }

    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers, URL url, final Invocation invocation) throws RpcException {
        List<Invoker<T>> result = new ArrayList<Invoker<T>>();
        for (Invoker invoker : invokers) {
            String providerIp = getIp(invoker.getUrl());
            // TODO: 2019/4/26 这里需要更改为从zk中获取规则ip
            long startTimeMills = System.currentTimeMillis();
            List<String> limitIpList = limitIpList(url);
            long endTimeMills = System.currentTimeMillis();
            logger.info("###获取黑名单耗时=" + (endTimeMills - startTimeMills) + "毫秒##limitIpList=" + limitIpList);
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

    private List<String> limitIpList(URL url) {
        List<String> limitIpList = new ArrayList<String>();
        try {
            if (client.getState() == CuratorFrameworkState.LATENT) {
                client.start();
            } else if (client.getState() == CuratorFrameworkState.STOPPED) {
                String connectString = url.getBackupAddress();
                CuratorFrameworkFactory.builder()
                        .connectString(connectString)
                        .sessionTimeoutMs(3000)
                        .connectionTimeoutMs(5000)
                        .retryPolicy(retryPolicy)
                        .build();
            }
            byte[] bytes = client.getData().forPath("/dubbo-custom-router/blacklist");
            String blackList = new String(bytes);
            String[] split = blackList.split(",");
            return Arrays.asList(split);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return limitIpList;
    }
}