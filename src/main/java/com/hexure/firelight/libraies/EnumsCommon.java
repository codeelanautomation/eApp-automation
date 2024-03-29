package com.hexure.firelight.libraies;

public enum EnumsCommon {
    TOVISIBLE("ToVisible"),
    ABSOLUTE_CLIENTFILES_PATH(System.getProperty("user.dir") + "\\src\\test\\resources\\testdata\\Client\\"),
    FIELD("Common Tag"),
    E2EDATAITEMID("Data Item ID"),
    E2ETITLE("Title"),
    E2ETESTDATA("Test Data"),
    E2EWIZARDNAME("Wizard Name"),
    SECTION("Section"),
    TOCLICKABLE("ToClickable"),
    CLICK("click"),
    UITYPE_REACT("react"),
    UITYPE_MVC("mvc"),
    RELATIVE_DOWNLOADFILES_PATH("./src/test/resources/testdata/downloadedFiles/"),
    ABSOLUTE_FILES_PATH(System.getProperty("user.dir") + "\\src\\test\\resources\\testdata\\"),
    FIREFOXBROWSER("Firefox");

    private final String text;

    EnumsCommon(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}