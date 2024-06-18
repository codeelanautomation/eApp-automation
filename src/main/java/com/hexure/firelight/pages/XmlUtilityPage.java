package com.hexure.firelight.pages;

import com.hexure.firelight.libraies.EnumsCommon;
import com.hexure.firelight.libraies.FLUtilities;
import com.hexure.firelight.libraies.TestContext;
import io.cucumber.java.hu.Ha;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class XmlUtilityPage extends FLUtilities {
    private TestContext testContext;
    private WebDriver driver;

    public XmlUtilityPage(WebDriver driver) {
        initElements(driver);
    }

    private void initElements(WebDriver driver) {
        PageFactory.initElements(driver, this);
    }


    public HashMap<String, String> findNodeValueById(String filePath, String tagName, String idValue, String originatingObjectID, String innerXPath, int countXML, String wizardControlType) {
        try {
            Document document = getDocument(filePath);
            HashMap<String, String> mapXMLValues = new HashMap<>();
            String resultFormat = "";
            XPath xpath = getXPath();
            String expression = "";
            XPathExpression xPathExpr;
            StringBuilder result = new StringBuilder();
            int count = 0;

            List<String> listXPath = Arrays.asList(innerXPath.split(","));

            if (countXML > 0)
                count = countXML;
            for (String xPath : listXPath) {
                if (wizardControlType.equalsIgnoreCase("Complex Rule"))
                    mapXMLValues.put("//" + tagName + xPath, extractResult(tagName, idValue, originatingObjectID, xPath, document, count));
                else {
                    if (mapXMLValues.containsKey("RelativeValue"))
                        mapXMLValues.put("RelativeValue", mapXMLValues.get("RelativeValue").toString() + extractResult(tagName, idValue, originatingObjectID, xPath, document, count));
                    else
                        mapXMLValues.put("RelativeValue", extractResult(tagName, idValue, originatingObjectID, xPath, document, count));
                }
            }
            return mapXMLValues;

        } catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    public String extractResult(String tagName, String idValue, String originatingObjectID, String xPath, Document document, int count) throws XPathExpressionException {
        StringBuilder result = new StringBuilder();
        try {
            Node node1 = getNode(idValue, tagName, xPath, document, count);
            if (node1.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node1;
                result.append(element.getTextContent());
            }
//            node1.getAttributes().getNamedItem("tc").getTextContent()
        } catch (NullPointerException e) {
            try {
                Node node1 = getNode(originatingObjectID, tagName, xPath, document, count);
                if (node1.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node1;
                    result.append(element.getTextContent());
                }
            } catch (NullPointerException e1) {
                try {
                    xPath = xPath.substring(0, xPath.lastIndexOf("/"));
                    Node node1 = getNode(originatingObjectID, tagName, xPath, document, count);
                    if (node1.getNodeType() == Node.ELEMENT_NODE) {
                        result.append(node1.getAttributes().getNamedItem("tc").getTextContent());
                    }
                } catch (NullPointerException e2) {
                    result.append("");
                }
            }
        }
        return result.toString();
    }

    public Node getNode(String idValue, String tagName, String xPath, Document document, int count) {
        try {
            XPath xpath = getXPath();
            String expression = "";
            if (idValue.equalsIgnoreCase(""))
                expression = "//" + tagName + xPath;
            else
                expression = "//" + tagName + "[@id='" + idValue + "']" + xPath;
            StringBuilder result = new StringBuilder();
            XPathExpression xPathExpr = xpath.compile(expression);
            NodeList nodeList1 = (NodeList) xPathExpr.evaluate(document, XPathConstants.NODESET);
            return nodeList1.item(count);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> extractRelatedObjectID(String xmlFilePath, String targetRelationRoleCode, String
            targetRelationRoleValue) {
        try {
            Document document = getDocument(xmlFilePath);

            NodeList relations = document.getElementsByTagName("Relation");
            String relatedObjectID = "";
            String originatingObjectID = "";

            for (int i = 0; i < relations.getLength(); i++) {
                Element relation = (Element) relations.item(i);
                String roleCode = relation.getElementsByTagName("RelationRoleCode").item(0).getTextContent();
                String roleValue = relation.getElementsByTagName("RelationRoleCode").item(0).getAttributes().getNamedItem("tc").getTextContent();

                if (targetRelationRoleCode.equals(roleValue) && targetRelationRoleValue.equals(roleCode)) {
                    relatedObjectID = relation.getAttribute("RelatedObjectID");
                    originatingObjectID = relation.getAttribute("OriginatingObjectID");
                    break;
                }
            }

            return Arrays.asList(relatedObjectID, originatingObjectID);

        } catch (Exception e) {
            handleException(e);
            return Collections.singletonList("");
        }
    }

    public String readXPathFromExcel(String excelFilePath, String sheetName, int rowIndex, int columnIndex) throws
            IOException {
        try (FileInputStream fileInputStream = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            Sheet sheet = workbook.getSheet(sheetName);
            Row row = sheet.getRow(rowIndex);

            if (row != null) {
                try {
                    Cell cell = row.getCell(columnIndex);
                    return cell.getStringCellValue();
                } catch (NullPointerException e) {
                    return "";
                }
            }

            return null;
        }
    }

    private Document getDocument(String filePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(filePath));
    }

    private XPath getXPath() {
        return XPathFactory.newInstance().newXPath();
    }

    public void handleException(Exception e) {
        e.printStackTrace();
    }

    public String getExcelColumnValue(String excelFilePath, String sheetName, int rowIndex, int columnIndex) {
        try {
            Workbook workbook = WorkbookFactory.create(new File(excelFilePath));
            Sheet sheet = workbook.getSheet(sheetName);

            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' not found in the workbook.");
            }

            Row row = sheet.getRow(rowIndex);
            Cell cell = row.getCell(columnIndex);

            String excelValue;
            if (cell != null && cell.getCellType() == CellType.STRING) {
                excelValue = cell.getStringCellValue();
            } else if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                excelValue = String.valueOf(((XSSFCell) cell).getRawValue());
            } else {
                excelValue = "";
            }

            workbook.close();

            return excelValue;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String extractRelatedObjectIDForOutbound(String xmlFilePath, String targetRelationRoleCode, String
            targetRelationRoleValue) {
        try {
            Document document = getDocument(xmlFilePath);

            NodeList relations = document.getElementsByTagName("Relation");
            String relatedObjectID = null;

            for (int i = 0; i < relations.getLength(); i++) {
                Element relation = (Element) relations.item(i);
                String roleCode = relation.getElementsByTagName("RelationRoleCode").item(0).getTextContent();
                String roleValue = relation.getElementsByTagName("RelationRoleCode").item(0).getAttributes().getNamedItem("tc").getTextContent();
                if (targetRelationRoleCode.replaceAll(" ", "").equals(roleValue.replaceAll(" ", "")) && targetRelationRoleValue.replaceAll(" ", "").toUpperCase().equals(roleCode.replaceAll(" ", ""))) {
                    relatedObjectID = relation.getAttribute("RelatedObjectID");
                    break;
                }
            }

            return relatedObjectID;

        } catch (Exception e) {
            handleException(e);
            return null;
        }
    }

    public HashMap<String, String> processXmlDataWithFileName(String xmlFile, String acordMapping, String
            xmlMapping, String wizardControlType) {
        try {
            String xmlFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + xmlFile;
            Document document = getDocument(xmlFilePath);
            List<String> listRelationCodes = Arrays.asList(acordMapping.split(";"));
            List<String> listRelationCode = Arrays.asList(listRelationCodes.get(0).split(","));
            String targetRelationRoleCode = "";
            String targetRelationRoleValue = "";
            String relatedObjectID = "";
            String originatingObjectID = "";
            XPath xpath = getXPath();
            String expression = "";
            XPathExpression xPathExpr;
            int count = 0;
            int countXML = 0;

            if (listRelationCode.get(0).contains("-")) {
                while (relatedObjectID.equals("")) {
                    targetRelationRoleCode = listRelationCode.get(count).split("=")[1].split("-")[0].trim();
                    targetRelationRoleValue = listRelationCode.get(count).split("=")[1].split("-")[1].trim();
                    List<String> objectIDs = extractRelatedObjectID(xmlFilePath, targetRelationRoleCode, targetRelationRoleValue);
                    relatedObjectID = objectIDs.get(0);
                    originatingObjectID = objectIDs.get(1);
                    count++;
                }
            }

            if (!listRelationCodes.get(0).equals("")) {
                if (!(relatedObjectID.equalsIgnoreCase(""))) {
                    String attributeKey = listRelationCode.get(1).split("=")[1].split("-")[0].trim();
                    String attributeValue = listRelationCode.get(1).split("=")[1].split("-")[1].trim();
                    String xmlPath = listRelationCode.get(1).split("=")[0];
                    int firstSlashIndex = xmlPath.indexOf('/');
                    int secondSlashIndex = xmlPath.indexOf('/', firstSlashIndex + 3);
                    String tagName = xmlPath.substring(0, secondSlashIndex);
                    String modifiedInnerXPath = xmlPath.replace(tagName, "");

                    expression = tagName + "[@id='" + relatedObjectID + "']" + modifiedInnerXPath;

                    xPathExpr = xpath.compile(expression);

                    NodeList nodeList = (NodeList) xPathExpr.evaluate(document, XPathConstants.NODESET);

                    for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
                        Node node = nodeList.item(nodeNum);
                        String attributeValueXML = node.getAttributes().getNamedItem(attributeKey.toLowerCase()).getTextContent();

                        if (!attributeValueXML.equals(attributeValue))
                            countXML += 1;
                        else
                            break;
                    }
                } else if (relatedObjectID.equalsIgnoreCase("")) {
                    String attributeKey = listRelationCode.get(0).split("=")[1].trim();
                    String xmlPath = listRelationCode.get(0).split("=")[0];
                    int firstSlashIndex = xmlPath.indexOf('/');
                    int secondSlashIndex = xmlPath.indexOf('/', firstSlashIndex + 3);
                    String tagName = xmlPath.substring(0, secondSlashIndex);
                    String modifiedInnerXPath = xmlPath.replace(tagName, "");

                    expression = tagName + modifiedInnerXPath;

                    xPathExpr = xpath.compile(expression);

                    NodeList nodeList = (NodeList) xPathExpr.evaluate(document, XPathConstants.NODESET);

                    for (int nodeNum = 0; nodeNum < nodeList.getLength(); nodeNum++) {
                        Node node = nodeList.item(nodeNum);
                        if (!node.getTextContent().equals(attributeKey))
                            countXML += 1;
                        else
                            break;
                    }
                }
            }
            System.out.println("RelatedObjectID: " + relatedObjectID);
            int firstSlashIndex = xmlMapping.indexOf('/');
            int secondSlashIndex = xmlMapping.indexOf('/', firstSlashIndex + 3);
            String tagName = xmlMapping.substring(0, secondSlashIndex);
            String modifiedInnerXPath = xmlMapping.replace(tagName, "");
            return findNodeValueById(xmlFilePath, tagName.replace("//", ""), relatedObjectID, originatingObjectID, modifiedInnerXPath, countXML, wizardControlType);
        } catch (Exception e) {
            handleException(e);
        }
        return null;
    }

//    public HashMap<String, String> processOutboundXmlDataWithFileName(String excelFilePath, String sheetName, int xpathColumnIndex, int rule1ColumnIndex, int rule2ColumnIndex, int rowIndex, String xmlFile, String controller) {
//        try {
//            String innerXPath = readXPathFromExcel(excelFilePath, sheetName, rowIndex, xpathColumnIndex).replace("\n", "");
//            String xmlFilePath = EnumsCommon.ABSOLUTE_CLIENTFILES_PATH.getText() + xmlFile;
//            String targetRelationRoleCode = readXPathFromExcel(excelFilePath, sheetName, rowIndex, rule1ColumnIndex).split("=")[1].split("-")[0];
//            String targetRelationRoleValue = readXPathFromExcel(excelFilePath, sheetName, rowIndex, rule1ColumnIndex).split("=")[1].split("-")[1];
//            String relatedObjectID = extractRelatedObjectIDForOutbound(xmlFilePath, targetRelationRoleCode, "OLI_REL_" + targetRelationRoleValue);
//            if (relatedObjectID == null) {
//                targetRelationRoleCode = readXPathFromExcel(excelFilePath, sheetName, rowIndex, rule2ColumnIndex).split("=")[1].split("-")[0];
//                targetRelationRoleValue = readXPathFromExcel(excelFilePath, sheetName, rowIndex, rule2ColumnIndex).split("=")[1].split("-")[1];
//                relatedObjectID = extractRelatedObjectID(xmlFilePath, targetRelationRoleCode, "OLI_REL_" + targetRelationRoleValue);
//            }
//
//            System.out.println("RelatedObjectID: " + relatedObjectID);
//            int firstSlashIndex = innerXPath.indexOf('/');
//            int secondSlashIndex = innerXPath.indexOf('/', firstSlashIndex + 3);
//            String tagName = innerXPath.substring(0, secondSlashIndex);
//            String modifiedInnerXPath = innerXPath.replace(tagName, "");
////            return findNodeValueById(xmlFilePath, tagName.replace("//", ""), relatedObjectID, modifiedInnerXPath);
//        } catch (IOException | RuntimeException e) {
//            handleException(e);
//        }
//        return null;
//    }

}