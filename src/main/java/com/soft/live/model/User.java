package com.soft.live.model;

import javax.persistence.*;
import java.io.Serializable;

@Table(name = "user")
public class User implements Serializable {
    @Id
    private Long id;

    private String name;

    @Column(name = "username")
    private String username;

    private String password;

    @Column(name = "live_name")
    private String liveName;

    private static final long serialVersionUID = 1L;

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * @return login_name
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username
     */
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    /**
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password
     */
    public void setPassword(String password) {
        this.password = password == null ? null : password.trim();
    }

    /**
     * @return live_name
     */
    public String getLiveName() {
        return liveName;
    }

    /**
     * @param liveName
     */
    public void setLiveName(String liveName) {
        this.liveName = liveName == null ? null : liveName.trim();
    }
}