package com.hexure.firelight.libraies.Enums;

import lombok.Getter;

@Getter
public enum EnumsExcelColumns {
    ENUMSEXCELCOLUMNS("Module Section Name, Order, Page, Section, List Options, Wizard Control Types, Common Tag, Rules Wizard, Validation Rules, Length, Format, Display Rules, Reason :  Skip for automation, 103 Mapping, Acord Mapping");
    private final String text;

    EnumsExcelColumns(String text) {
        this.text = text;
    }

}