package com.hexure.firelight.libraies.Enums;

import lombok.Getter;

@Getter
public enum EnumsJSONProp {
    PRODUCT("product"),
    JURISDICTION("jurisdiction"),
    REVIEWERPASSCODE("reviewerPasscode"),
    REVIEWERURL("reviewerUrl"),
    APPURL("appUrl"),
    APPGUID("appGuid"),
    NEWPRODUCTNAME("newProductName");

    private final String text;

    EnumsJSONProp(String text) {
        this.text = text;
    }

}