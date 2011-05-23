package com.csipsimple.utils;

import java.io.Serializable;


public class LoginResponse implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 889229068015962664L;

    private int accountId;
    private String userId;
    private String password;
    private String realm;
    private String name;
    private String displayName;
    private String host;
    private String port;
    
    
    public int getAccountId() {
        return accountId;
    }

    
    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getRealm() {
        return realm;
    }
    
    public void setRealm(String realm) {
        this.realm = realm;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getPort() {
        return port;
    }
    
    public void setPort(String port) {
        this.port = port;
    }

}
