package com.hexure.firelight.libraies.Enums;

public enum EnumsExcelColumns {
    ENUMSEXCELCOLUMNS("Display Rule, Length, Format, Module Section Name, Order, Field Name, Page, Section, Data Type, Options, Wizard Control Types, Display on PDF, Choose From, Add/Delete/Change, Common Tag, Prefill, Rules for Wizard, Validation Rules, Length, Format, Mask, Validation, Field Value, Invalid Data");
    private final String text;

    EnumsExcelColumns(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}