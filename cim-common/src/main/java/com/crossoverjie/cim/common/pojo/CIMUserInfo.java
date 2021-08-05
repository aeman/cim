package com.crossoverjie.cim.common.pojo;

/**
 * Function: 用户信息
 *
 * @author crossoverJie
 * Date: 2018/12/24 02:33
 * @since JDK 1.8
 */
public class CIMUserInfo {

    private Long userId;
    private String userName;
    private String token;

    public CIMUserInfo(Long userId, String userName, String token) {
        this.userId = userId;
        this.userName = userName;
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "CIMUserInfo{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                '}';
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
