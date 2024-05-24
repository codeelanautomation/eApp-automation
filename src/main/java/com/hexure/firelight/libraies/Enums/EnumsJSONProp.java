package com.hexure.firelight.libraies.Enums;

import lombok.Getter;

@Getter
public enum EnumsJSONProp {
    PRODUCT("product"),
    JURISDICTION("jurisdiction"),
    NEWPRODUCTNAME("newProductName");

    private final String text;

    EnumsJSONProp(String text) {
        this.text = text;
    }

}