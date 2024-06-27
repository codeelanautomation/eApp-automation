package com.hexure.firelight.stepdefinitions;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hexure.firelight.libraies.EnumsCommon;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class test {
    public static void main(String[] args) throws IOException, ParseException {
        File file = new File(EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText()  + "SGB_ExternalPath.xlsx");
        boolean result = Files.deleteIfExists(file.toPath());
//        List<String> lines = Arrays.asList("Testout", "inout");
//        lines.replaceAll(n -> n.replaceAll("out", ""));
//        System.out.println(lines);
//        List<String> lines1 = new ArrayList<>(Arrays.asList("Test", "inp"));
//        lines1.removeIf(lines::contains);
////        lines1.removeIf()removeAll(Collections.singletonList(lines));
//        System.out.println(lines1);
//        String s = "312312-2365";
//        System.out.println(s.replaceAll("[^\\x00-\\x7F^]", ""));
//        String appUrl =  "https://flqa.insurancetechnologies.com/EGApp/EditWizardApplication?AppGuid=eb16a280-fe9c-45d9-aef5-6e2d2419af02&RecordMetric=True#";
//        System.out.println(appUrl.substring(appUrl.indexOf("AppGuid=") + 8, appUrl.indexOf("&")));
//        ArrayList<String> lines = new ArrayList<>();
//        File f = new File("src/test/resources/testdata/eAppTest-TestData.json");
//        InputStream is = Files.newInputStream(f.toPath());
//        String reader = IOUtils.toString(is, StandardCharsets.UTF_8);
//        String reader = new String(Files.readAllBytes(Paths.get("src/test/resources/testdata/eAppTest-TestData.json")));
//        JSONParser parser = new JSONParser();
//        Object obj = parser.parse(new FileReader("src/test/resources/testdata/eAppTest-TestData.json"));
//
//        JSONObject jsonTestData =  (JSONObject) obj;
//        JSONObject jsonData =  (JSONObject) jsonTestData.get("testData");
//        String reader = new FileReader("").toString();
//        JsonElement json = JsonParser.parseReader( new InputStreamReader(new FileInputStream("src/test/resources/testdata/eAppTest-TestData.json"), "UTF-8") );
////        JSONObject s1 = new JSONObject (json);
//        System.out.println(jsonData.get("SGB"));
//        System.out.println(jsonObject.get("testData"));
//        String line = "";
//        while ((line = reader.readLine()) != null) {
//            line = line.replace("Attached Text (\"+e.mediaType+\")", "");
//            line = line.replace("\"details\",null,c.createElement", "\"detailss\",null,c.createElement");
//            lines.add(line);
//        }
//        reader.close();
//        File tempFile = new File("D:/Desktop/Latest report/26-05/cucumberFILIAllocationsModuleAll.html");
//        FileWriter runnerFile = new FileWriter(tempFile);
//        BufferedWriter writer = new BufferedWriter(runnerFile);
//        for (String line1 : lines)
//            writer.write(line1 + "\n");
//        writer.close();
    }
}
