package com.hexure.firelight.stepdefinitions;

import com.jayway.jsonpath.JsonPath;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
    public static void main(String[] args) {
        String distinctRule = "If Owner_FirstName = blank then Issue Error Message: Owner First Name is required.";
//        Pattern pattern = Pattern.compile("If (.*?) = (.*?) then (.*?): (.*)");
//        Matcher matcher = pattern.matcher(distinctRule);
        System.out.println(Pattern.compile("If (.*?) = (.*?) then (.*?): (.*)").matcher(distinctRule).find());
//        while (matcher.find()) {
//            System.out.println(matcher.group(1));
//            System.out.println(matcher.group(2));
//            System.out.println(matcher.group(3));
//            System.out.println(matcher.group(4));
//        }

    }
}
