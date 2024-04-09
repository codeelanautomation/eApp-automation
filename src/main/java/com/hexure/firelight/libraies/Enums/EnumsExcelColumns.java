package com.hexure.firelight.libraies.Enums;

public enum EnumsExcelColumns {
    ENUMSEXCELCOLUMNS("Module Section Name, Order, Field Name, Page, Section, Data Type, List Options, Wizard Control Types, Display on PDF, Common Tag, Rules Wizard, Validation Rules, Length, Format, Display Rules, Reason for skip");
    private final String text;

    EnumsExcelColumns(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}