package com.hexure.firelight.libraies;

import lombok.Getter;

@Getter
public enum EnumsCommon {
    ABSOLUTE_CLIENTFILES_PATH(System.getProperty("user.dir") + "\\src\\test\\resources\\testdata\\Client\\"),
    FIELD("Common Tag"),
    JURISDICTION("Jurisdiction"),
    E2EDATAITEMID("Data Item ID"),
    E2ETITLE("Title"),
    E2ETESTDATA("Test Data"),
    E2EWIZARDNAME("Wizard Name"),
    TOVISIBLE("ToVisible"),
    TOCLICKABLE("ToClickable"),
    TOINVISIBLE("ToInvisible"),
    ABSOLUTE_FILES_PATH(System.getProperty("user.dir") + "\\src\\test\\resources\\testdata\\"),
    FIREFOXBROWSER("Firefox"),
    FEATUREFILESPATH(System.getProperty("user.dir") + "/src/test/resources/features/"),
    RUNNERFILESPATH(System.getProperty("user.dir") + "/src/test/java/com/hexure/firelight/runner/");

    private final String text;

    EnumsCommon(String text) {
        this.text = text;
    }

}
