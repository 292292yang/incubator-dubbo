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
    private static RetryPolicy retryPolicy = new ExponentialBackoffRetry(3000, 3);
    //一直重试,重试间隔时间为3秒
//    private static RetryPolicy retryPolicy = new RetryForever(3000);
    private static final String PATH = "/dubbo-custom-router/blacklist";
    private static CuratorFramework client;
    private static List<String> limitIpList = new ArrayList<String>();
    private final URL url;

    public CustomRouter(URL url) {
        this.url = url;
        try {
            if(client==null || client.getState()== CuratorFrameworkState.STOPPED){
                System.out.println("###开始创建zk连接###");
                logger.info("###开始创建zk连接###");
                String connectString = url.getBackupAddress();
                client = CuratorFrameworkFactory.builder()
                        .connectString(connectString)
                        .sessionTimeoutMs(3000)
                        .connectionTimeoutMs(5000)
                        .retryPolicy(retryPolicy)
                        .build();
                client.start();
                logger.info("###zkclient已启动###");
                final NodeCache cache = new NodeCache(client, PATH);
                NodeCacheListener listener = new NodeCacheListener() {
                    @Override
                    public void nodeChanged() {
                        ChildData data = cache.getCurrentData();
                        if (null != data) {
                            String limitIpStr = null;
                            Object obj = SerializationUtils.deserialize(cache.getCurrentData().getData());
                            if (obj != null) {
                                limitIpStr = obj.toString();
                                //去除limitStr头尾空白
                                limitIpStr = limitIpStr.trim();
                            }
                            logger.info("###" + PATH + "节点数据:" + limitIpStr);
                            if (StringUtils.isNotEmpty(limitIpStr)) {
                                limitIpList = Arrays.asList(limitIpStr.split(","));
                            } else {
                                limitIpList = new ArrayList<String>();
                            }
                        } else {
                            limitIpList = new ArrayList<String>();
                            logger.info("###" + PATH + "节点数据为空###");
                        }
                        logger.info("###limitIpList=" + limitIpList);
                        System.out.println("###limitIpList=" + limitIpList);
                    }
                };
                cache.getListenable().addListener(listener);
                cache.start();
            }
        } catch (Exception e) {
            logger.error("###自定义路由连接zk失败##失败原因:", e);
        }
    }

    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers, URL url, final Invocation invocation) throws RpcException {
        List<Invoker<T>> result = new ArrayList<Invoker<T>>();
        for (Invoker invoker : invokers) {
            String providerIp = getIp(invoker.getUrl());
            System.out.println("###limitIpList=" + limitIpList);
            logger.info("###limitIpList=" + limitIpList);
            if (!limitIpList.contains(providerIp)) {
                result.add(invoker);
            }
        }
        logger.info("###custom-router##url=" + url);
        return result;
    }

    private String getIp(URL url) {
        System.out.println("###url=" + url);
        String sURL = url.toString();
        int begin = sURL.indexOf("://");
        int end = sURL.lastIndexOf("/");
        String substring = sURL.substring(begin + 3, end);
        String[] split = substring.split(":");
        String ip = split[0];
        System.out.println("###ip=" + ip);
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