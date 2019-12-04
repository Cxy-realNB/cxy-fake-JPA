package test.entity;

import base.annotation.Column;
import base.annotation.Entity;
import base.annotation.Key;

import java.util.Date;

/**
 * @author Wonder Chen
 */
@Entity(tableName = "user")
public class User {
    @Key
    @Column(name = "id")
    private long id;
    @Column(name = "user_name")
    private String username;
    @Column(name = "password")
    private String password;
    @Column(name = "enabled")
    private boolean enable;
    @Column(name = "create_time")
    private Date createTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
