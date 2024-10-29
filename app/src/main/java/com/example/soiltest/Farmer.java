package com.example.soiltest;

public class Farmer {
    private String name;
    private String village;
    private String mobile;
    public Farmer(String name, String village, String mobile) {
        this.name = name;
        this.village = village;
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public String getVillage() {
        return village;
    }

    public String getMobile() {
        return mobile;
    }

}
