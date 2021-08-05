package com.crossoverjie.cim.server.util;

import com.crossoverjie.cim.common.pojo.CIMUserInfo;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 22/05/2018 18:33
 * @since JDK 1.8
 */
public class SessionSocketHolder {
    private static final Map<String, NioSocketChannel> CHANNEL_MAP = new ConcurrentHashMap<>(16);
    private static final Map<String, CIMUserInfo> SESSION_MAP = new ConcurrentHashMap<>(16);

    public static void saveSession(String token, CIMUserInfo user) {
        SESSION_MAP.put(token, user);
    }

    public static void removeSession(String token) {
        SESSION_MAP.remove(token);
    }

    /**
     * Save the relationship between the userId and the channel.
     *
     * @param token
     * @param socketChannel
     */
    public static void put(String token, NioSocketChannel socketChannel) {
        CHANNEL_MAP.put(token, socketChannel);
    }

    public static NioSocketChannel get(String token) {
        return CHANNEL_MAP.get(token);
    }

    public static Map<String, NioSocketChannel> getRelationShip() {
        return CHANNEL_MAP;
    }

    public static void remove(NioSocketChannel nioSocketChannel) {
        CHANNEL_MAP.entrySet().stream().filter(entry -> entry.getValue() == nioSocketChannel).forEach(entry -> CHANNEL_MAP.remove(entry.getKey()));
    }

    /**
     * 获取注册用户信息
     *
     * @param nioSocketChannel
     * @return
     */
    public static CIMUserInfo getUserId(NioSocketChannel nioSocketChannel) {
        for (Map.Entry<String, NioSocketChannel> entry : CHANNEL_MAP.entrySet()) {
            NioSocketChannel value = entry.getValue();
            if (nioSocketChannel == value) {
                String key = entry.getKey();
                CIMUserInfo info = SESSION_MAP.get(key);
                return info;
            }
        }

        return null;
    }
}
