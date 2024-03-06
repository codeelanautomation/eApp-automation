package com.hexure.firelight.libraies.Enums;

public enum EnumsJSONProp {
    PRODUCT("product"),
    JURISDICTION("jurisdiction"),
    NEWPRODUCTNAME("newProductName")
    ;
    private final String text;

    EnumsJSONProp(String text){ this.text = text;}

    public String getText(){ return  text;}
}