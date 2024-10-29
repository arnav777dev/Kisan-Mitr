package com.example.soiltest;

import java.util.Date;

public class Field {
    private String name;
    private String farmerUID;
    private String N_value;
    private String P_value;
    private String K_value;
    private String date;
    Field(String name, String farmerUID, String N_value, String P_value, String K_value, String date) {
        this.name = name;
        this.farmerUID = farmerUID;
        this.N_value = N_value;
        this.P_value = P_value;
        this.K_value = K_value;
        this.date = date;
    }
//
//    Field(String name, String farmerUID) {
//        this.name = name;
//        this.farmerUID = farmerUID;
//        this.N_value = "0";
//        this.P_value = "0";
//        this.K_value = "0";
//    }

    public String getName() {
        return name;
    }
    public String getFarmerUID() {
        return farmerUID;
    }
    public String getN_value() {
        return N_value;
    }
    public String getP_value() {
        return P_value;
    }
    public String getK_value() {
        return K_value;
    }
    public String getDate() { return date; }
}
