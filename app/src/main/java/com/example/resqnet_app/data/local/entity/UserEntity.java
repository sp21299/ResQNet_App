package com.example.resqnet_app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String email;
    private String address;
    private String mobile;

    public String getBirth() {
        return birth;
    }

    public void setBirth(String birth) {
        this.birth = birth;
    }

    private String birth;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getMobile() {
        return mobile;
    }

    //constructor
    public UserEntity(String name, String email, String address, String mobile)
    {
        this.name = name;
        this.email = email;
        this.address = address;
        this.mobile = mobile;
    }

    public UserEntity() {
    }
}
