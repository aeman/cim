package com.crossoverjie.cim.route.service.impl;

import com.crossoverjie.cim.common.pojo.CIMUserInfo;
import com.crossoverjie.cim.route.service.UserInfoCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.crossoverjie.cim.route.constant.Constant.ACCOUNT_PREFIX;
import static com.crossoverjie.cim.route.constant.Constant.LOGIN_STATUS_PREFIX;

/**
 * Function:
 *
 * @author crossoverJie
 * Date: 2018/12/24 11:06
 * @since JDK 1.8
 */
@Service
public class UserInfoCacheServiceImpl implements UserInfoCacheService {

    /**
     * todo 本地缓存，为了防止内存撑爆，后期可换为 LRU。
     */
    private final static Map<Long, CIMUserInfo> USER_INFO_MAP = new ConcurrentHashMap<>(64);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public CIMUserInfo loadUserInfoByUserId(Long userId) {

        //优先从本地缓存获取
        CIMUserInfo cimUserInfo = USER_INFO_MAP.get(userId);
        if (cimUserInfo != null) {
            return cimUserInfo;
        }

        //load redis
        String sendUserName = redisTemplate.opsForValue().get(ACCOUNT_PREFIX + userId);
        if (sendUserName != null) {
            cimUserInfo = new CIMUserInfo(userId, sendUserName, "");
            USER_INFO_MAP.put(userId, cimUserInfo);
        }

        return cimUserInfo;
    }

    @Override
    public boolean saveAndCheckUserLoginStatus(Long userId, String token) throws Exception {
        Long add = redisTemplate.opsForSet().add(LOGIN_STATUS_PREFIX + userId, token);
        if (add == 0) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void removeLoginStatus(Long userId, String token) throws Exception {
        redisTemplate.opsForSet().remove(LOGIN_STATUS_PREFIX + userId, token);
    }

    @Override
    public Set<CIMUserInfo> onlineUser() {
        Set<CIMUserInfo> set = new HashSet<>(64);

        Set<String> keys = redisTemplate.keys(LOGIN_STATUS_PREFIX + "*");
        for (String key : keys) {
            Long userId = Long.valueOf(key.replace(LOGIN_STATUS_PREFIX, ""));
            CIMUserInfo cimUserInfo = loadUserInfoByUserId(userId);
            set.add(cimUserInfo);
        }

        return set;
    }

}
