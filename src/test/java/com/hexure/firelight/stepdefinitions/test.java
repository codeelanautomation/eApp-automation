package com.hexure.firelight.stepdefinitions;

import com.jayway.jsonpath.JsonPath;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
    public static void main(String[] args) {
        String distinctRule = "1. Default = Blank.;2. Prefilled with Google address look up Postal code if looking is used. ;3. If a=b AND b=c AND (FLI_PRODUCT_CARRIER_NAME = Fidelity OR  D_Replacement_CompanyX_ProductType <> Variable Annuity Contract OR FLI_PRODUCT_TYPE_NAME <> SPIA) then DISABLE and HIDE.";
        String newStr = "";

        List<String> a = Arrays.asList(distinctRule.split(";"));
        for(String s : a) {
            if(s.toLowerCase().contains("(") & s.toLowerCase().contains("or")) {
                Pattern pattern = Pattern.compile("(.*?)\\((.*?)\\)(.*)");
                Matcher matcher = pattern.matcher(s);
                while (matcher.find()) {
                    List<String> orCondition = Arrays.asList(matcher.group(2).split(" OR "));
                    for(String orCond : orCondition)
                        newStr += matcher.group(1) + orCond + matcher.group(3);
                }
            }
            else
                newStr += s + ";";
        }
        System.out.println(newStr);

    }
}
