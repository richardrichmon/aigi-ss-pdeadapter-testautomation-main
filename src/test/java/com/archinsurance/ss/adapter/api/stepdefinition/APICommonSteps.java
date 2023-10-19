package com.archinsurance.ss.adapter.api.stepdefinition;

import com.archinsurance.config.Config;
import com.archinsurance.config.constants.Environment;
import com.archinsurance.ss.adapter.entity.AuthRequest;
import com.archinsurance.ss.adapter.utils.CommonUtils;
import com.archinsurance.io.ResourceUtil;
import com.archinsurance.log.SimpleLogger;
import com.archinsurance.log.constants.Status;
import com.archinsurance.report.manager.ExtentManager;
import com.archinsurance.system.util.DateAndTimeStamp;
import com.archinsurance.utils.api.APIProfile;
import com.archinsurance.utils.api.APIVerb;
import com.archinsurance.utils.api.ResponseObject;
import com.archinsurance.utils.api.RestAssuredUtils;
import com.archinsurance.utils.excel.ExcelObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relevantcodes.extentreports.ExtentTest;
import com.relevantcodes.extentreports.LogStatus;
import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.company.Company;
import io.codearte.jfairy.producer.person.Person;
import io.codearte.jfairy.producer.person.PersonProperties;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class APICommonSteps {
    private final static ExtentManager extentManager = ExtentManager.getInstance();
    private final static SimpleLogger logger = new SimpleLogger(APICommonSteps.class);

    private ExtentTest extentTest;

    private String environment;
    private APIProfile profile;
    private RestAssuredUtils restAssuredUtils;
    private Response response;
    private final static Map<String, String> apiData = new HashMap<>();

    // For Synthetic data creation
    private final Fairy fairy = Fairy.create();
    private final Company company = new Company("Arch Insurance", "archinsurance.com", "contactus@archinsurance.com", "123");

    //RA: PDE Specific variable
    private static String finalPayloadContent;

    // Hooks
    @Before
    public void startScenario(Scenario scenario) {
        extentTest = extentManager.getExtentReportInstance().startTest(scenario.getName());
        logger.logBeginTestCase(scenario.getName());
    }

    @After
    public void endScenario(Scenario scenario) {
        extentManager.getExtentReportInstance().endTest(extentTest);
        logger.logEndTestCase(scenario.getName());
        extentManager.getExtentReportInstance().flush();
    }

    @AfterAll
    public static void afterAll() {
        extentManager.getExtentReportInstance().close();
    }

    @Given("run API scenarios on environment {string}")
    public void runAPIScenariosOnEnvironment(String env) {
        Config.setEnvironmentForExecution(Environment.valueOf(env.toUpperCase()));
        environment = env;
    }

    @Given("User setup API configuration for {string} API")
    public void userSetupAPIConfigurationForAPI(String apiType) throws IOException, ConfigurationException {
        configureRestSpec(apiType);
    }

    @Given("User setup API configuration for {string} API with authorization as {string}")
    public void userSetupAPIConfigurationForAPI(String apiType, String isAuthorized) throws IOException, ConfigurationException {
        configureRestSpec(apiType, isAuthorized);
    }

    @And("User add header {string} as {string}")
    public void userAddHeaderAs(String header, String value) {
        Map<String, String> headers = new HashMap<>();
        headers.put(header, value);
        profile.setHeaders(headers);
        restAssuredUtils = new RestAssuredUtils(profile);
    }

    @When("User wait for {string} seconds")
    public void userWaitForSeconds(String waitTime) {
        DateAndTimeStamp.introduceDelay(Integer.parseInt(waitTime));
    }

    // GET / DELETE / OPTIONS

    @When("User hits {string} api endpoint {string}")
    public void userHitsApiEndpoint(String verb, String endpoint) throws MalformedURLException {
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()));
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with query params key {string} and value {string} pair")
    public void hitsApiEndpointWithQueryParamsKeyAndValuePair(String verb, String endpoint, String key, String value) throws MalformedURLException {
        String[] keys = key.split("~~");
        String[] values = value.split("~~");
        Map<String, String> queryParams = new HashMap<>();

        for (int i = 0; i < keys.length; i++) {
            queryParams.put(keys[i], values[i]);
        }

        response = restAssuredUtils.getResponse(queryParams, endpoint, APIVerb.valueOf(verb.toUpperCase()));
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} and parameters {string} and value {string}")
    public void userHitsApiEndpointAndParametersAndValue(String verb, String endpoint, String key, String value) throws MalformedURLException {
        Map<String, String> pathParams = new HashMap<>();

        if (key.contains(",")) {
            String[] keys = key.split(",");
            String[] values = value.split(",");

            for (int i = 0; i < keys.length; i++) {
                pathParams.put(keys[i], values[i]);
            }
        } else {
            pathParams.put(key, value);
        }
        response = restAssuredUtils.getResponse(endpoint, pathParams, APIVerb.valueOf(verb.toUpperCase()));
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    // POST / PUT / PATCH

    @When("User hits {string} api endpoint {string} with payload JSON {string}")
    public void userHitsApiEndpointWithPayloadJSON(String verb, String endpoint, String payloadBody) throws MalformedURLException {
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with payload file {string}")
    public void userHitsApiEndpointWithPayload(String verb, String endpoint, String payloadFilePath) throws IOException {
        String responseFilePath = getResponseFilePath(payloadFilePath);
        String payloadBody = CommonUtils.readFileAsString(responseFilePath);
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with payload file {string} in {string} folder")
    public void userHitsApiEndpointWithPayloadFileInFolder(String verb, String endpoint, String payloadFilePath, String apiFolder) throws IOException {
        String responseFilePath = getResponseFilePath(payloadFilePath, apiFolder);
        String payloadBody = CommonUtils.readFileAsString(responseFilePath);

        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with payload file {string} using {string} and {string}")
    public void userHitsApiEndpointWithPayloadFile(String verb, String endpoint, String payloadFilePath, String inputVariables, String inputVariablesValues) throws IOException {
        String responseFilePath = getResponseFilePath(payloadFilePath);
        String payloadBody = CommonUtils.formatPrettyJSON(CommonUtils.readFileAsString(responseFilePath));
        payloadBody = formatInputPayload(payloadBody, inputVariables, inputVariablesValues);
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), com.archinsurance.log.constants.Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with payload file {string} in {string} folder using {string} and {string}")
    public void userHitsApiEndpointWithPayloadFileInFolder(String verb, String endpoint, String payloadFilePath, String folderName, String inputVariables, String inputVariablesValues) throws IOException {
        String responseFilePath = getResponseFilePath(payloadFilePath, folderName);
        String payloadBody = CommonUtils.readFileAsString(responseFilePath);
        payloadBody = formatInputPayload(payloadBody, inputVariables, inputVariablesValues);
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), com.archinsurance.log.constants.Status.INFO);
    }



    @When("User hits {string} api endpoint {string} with payload file {string} in {string} folder having {string} with {string} and keys {string}")
    public void userHitsApiEndpointWithPayloadFile(String verb, String endpoint, String payloadFilePath, String apiFolder, String inputVariables, String inputVariablesValues, String randomKeys) throws IOException {
        String responseFilePath = getResponseFilePath(payloadFilePath, apiFolder);
        String payloadBody = CommonUtils.readFileAsString(responseFilePath);
        payloadBody = formatInputPayload(payloadBody, randomKeys, inputVariables, inputVariablesValues);
        extentTest.log(LogStatus.INFO, "Request body : " + payloadBody);
        logger.logTestStepInText("Request body : " + payloadBody, Status.INFO);
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with payload file {string} in {string} folder using input from workbook {string} and sheet {string}")
    public void userHitsApiEndpointWithPayload(String verb, String endpoint, String payloadFilePath, String folderName, String workbookName, String sheetName) throws IOException {
        String responseFilePath = getResponseFilePath(payloadFilePath, folderName);
        String payloadBody = CommonUtils.formatPrettyJSON(CommonUtils.readFileAsString(responseFilePath));
        payloadBody = formatInputPayloadUsingExcel(payloadBody, workbookName, sheetName, folderName);
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), com.archinsurance.log.constants.Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with {string} and {string}")
    public void userHitsApiEndpointWithFormDataKeyAndValuePairWithAnd(String verb, String endpoint, String fileType, String fileName) throws IOException {
        String folderPath = new File(ResourceUtil.getTestDataPath() + File.separator + fileName).getCanonicalPath();
        Map<String, String> multipart = new HashMap<>();
        multipart.put(fileType, folderPath);
        response = restAssuredUtils.getResponseMultiPart(endpoint, APIVerb.valueOf(verb), null, multipart);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with {string} and {string} in {string} folder")
    public void userHitsApiEndpointWithAndInFolder(String verb, String endpoint, String fileType, String fileName, String folderName) throws IOException {
        String folderPath = new File(ResourceUtil.getTestDataPath() + File.separator + folderName + File.separator + fileName).getCanonicalPath();
        Map<String, String> multipart = new HashMap<>();
        multipart.put(fileType, folderPath);

        response = restAssuredUtils.getResponseMultiPart(endpoint, APIVerb.valueOf(verb), null, multipart);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with {string} and {string} in {string} folder having keep_input_in_json as {string}")
    public void userHitsApiEndpointWithAndInFolder(String verb, String endpoint, String fileType, String fileName, String folderName, String keep_input_as_json) throws IOException {
        String folderPath = new File(ResourceUtil.getTestDataPath() + File.separator + folderName + File.separator + fileName).getCanonicalPath();
        Map<String, String> multipart = new HashMap<>();
        multipart.put(fileType, folderPath);
        if(keep_input_as_json.equals("true")) {
            multipart.put("keep_input_in_json","true");
        }
        response = restAssuredUtils.getResponseMultiPart(endpoint, APIVerb.valueOf(verb), null, multipart);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    @When("User hits {string} api endpoint {string} with payload file {string} in {string} folder using input from workbook {string} and sheet {string} having append_to_existing_document as {string}")
    public void userHitsApiEndpointWithPayloadHavingAppendExistingDocumentAsBlank(String verb, String endpoint, String payloadFilePath, String folderName, String workbookName, String sheetName, String appendExistingDocumentValue) throws IOException {
        String responseFilePath = getResponseFilePath(payloadFilePath, folderName);
        String payloadBody = CommonUtils.formatPrettyJSON(CommonUtils.readFileAsString(responseFilePath));
        payloadBody = formatInputPayloadUsingExcel(payloadBody, workbookName, sheetName, folderName);
        payloadBody = payloadBody.replace("false", appendExistingDocumentValue);
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), com.archinsurance.log.constants.Status.INFO);
    }

    // Validation

    @Then("User should get response code as {int}")
    public void userShouldGetResponseCodeAs(int expectedResponseCode) {
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        if (responseMap.get(ResponseObject.StatusCode).equals(String.valueOf(expectedResponseCode))) {
            extentTest.log(LogStatus.PASS, "Response code is as per expectation, expected : " + expectedResponseCode);
            logger.logTestStepInText("Response code is as per expectation, expected : " + expectedResponseCode, Status.PASS);
        } else {
            extentTest.log(LogStatus.FAIL, "Response code is not as per expectation, expected : " + expectedResponseCode + " actual : " + responseMap.get(ResponseObject.StatusCode));
            logger.logTestStepInText("Response code is not as per expectation, expected : " + expectedResponseCode + " actual : " + responseMap.get(ResponseObject.StatusCode), Status.FAIL);
            Assert.fail("Response code is not as per expectation, expected : " + expectedResponseCode + " actual : " + responseMap.get(ResponseObject.StatusCode));
        }
    }

    @And("User should validate that response body is coming as blank array")
    public void userShouldValidateThatResponseBodyIsComingAsBlankArray() {
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        JSONArray array = new JSONArray(responseBody);

        if (array.length() == 0) {
            extentTest.log(LogStatus.PASS, "Response body is coming as blank array");
            logger.logTestStepInText("Response body is coming as blank array", Status.PASS);
        } else {
            extentTest.log(LogStatus.FAIL, "Response code is not coming as blank array");
            logger.logTestStepInText("Response code is not coming as blank array", Status.FAIL);
            Assert.fail("Response code is not coming as blank array");
        }
    }

    @And("User should validate that response body is not coming as blank")
    public void userShouldValidateThatResponseBodyIsNotComingAsBlank() {
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        if (responseBody.length() > 0) {
            extentTest.log(LogStatus.PASS, "Response body is coming not as blank " + responseBody);
            logger.logTestStepInText("Response body is coming not as blank ", Status.PASS);
        } else {
            extentTest.log(LogStatus.FAIL, "Response body coming as blank ");
            logger.logTestStepInText("Response body coming as blank ", Status.FAIL);
            Assert.fail("Response body coming as blank ");
        }
    }

    @And("User should validate that response body is not coming as blank array")
    public void userShouldValidateThatResponseBodyIsNotComingAsBlankArray() {
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        JSONArray array = new JSONArray(responseBody);

        if (array.length() > 0) {
            extentTest.log(LogStatus.PASS, "Response body is coming not as blank array" + array);
            logger.logTestStepInText("Response body is coming not as blank array", Status.PASS);
        } else {
            extentTest.log(LogStatus.FAIL, "Response body coming as blank array");
            logger.logTestStepInText("Response body coming as blank array", Status.FAIL);
            Assert.fail("Response body coming as blank array");
        }
    }

    @And("User should validate that response body contains values {string} for keys {string}")
    public void userShouldValidateThatResponseBodyContainsValuesForKeys(String expValues, String keys) {
        String[] arrExpValues;
        String[] arrKeys;
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        JSONObject responseObj = new JSONObject(responseBody);

        if (expValues.contains("~~")) {
            arrExpValues = expValues.split("~~");
            arrKeys = keys.split("~~");
        } else {
            arrExpValues = new String[]{expValues};
            arrKeys = new String[]{keys};
        }

        for (int i = 0; i < arrKeys.length; i++) {
            extentTest.log(LogStatus.INFO, "Validating whether value for field  " + arrKeys[i] + " exists in response");
            if (responseObj.getString(arrKeys[i]).equalsIgnoreCase(arrExpValues[i])) {
                extentTest.log(LogStatus.PASS, "Response body field " + arrKeys[i] + " value is as per expectation : " + arrExpValues[i]);
            } else {
                extentTest.log(LogStatus.FAIL, "Response body field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.getString(arrKeys[i]));
                logger.logTestStepInText("Response body field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.getString(arrKeys[i]), Status.FAIL);
                Assert.fail("Response body field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.getString(arrKeys[i]));
            }
        }
    }

    @And("User should validate that response body {string} contains {string} for keys {string}")
    public void userShouldValidateThatResponseBodyContainsForKeys(String arrayName, String expValues, String keys) {
        String[] arrExpValues;
        String[] arrKeys;

        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        JSONArray responseArray = (new JSONObject(responseBody)).getJSONArray(arrayName);

        if (expValues.contains("~~")) {
            arrExpValues = expValues.split("~~");
            arrKeys = keys.split("~~");
        } else {
            arrExpValues = new String[]{expValues};
            arrKeys = new String[]{keys};
        }
        for (int j = 0; j < responseArray.length(); j++) {
            JSONObject responseObj = responseArray.getJSONObject(j);
            for (int i = 0; i < arrKeys.length; i++) {
                extentTest.log(LogStatus.INFO, "Validating whether value for field  " + arrKeys[i] + " exists in " + arrayName);
                if (responseObj.has(arrKeys[i])) {
                    if (arrKeys[i].equals("error_message")) {
                        if (responseObj.get(arrKeys[i]).toString().contains(arrExpValues[i])) {
                            extentTest.log(LogStatus.PASS, "Response body " + arrayName + " field " + arrKeys[i] + " value is as per expectation : " + arrExpValues[i]);
                        } else {
                            extentTest.log(LogStatus.FAIL, "Response body " + arrayName + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                            logger.logTestStepInText("Response body " + arrayName + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]), com.archinsurance.log.constants.Status.FAIL);
                            Assert.fail("Response body " + arrayName + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                        }
                    } else {
                        if (responseObj.get(arrKeys[i]).toString().equalsIgnoreCase(arrExpValues[i])) {
                            extentTest.log(LogStatus.PASS, "Response body " + arrayName + " field " + arrKeys[i] + " value is as per expectation : " + arrExpValues[i]);
                        } else {
                            extentTest.log(LogStatus.FAIL, "Response body " + arrayName + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                            logger.logTestStepInText("Response body " + arrayName + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]), com.archinsurance.log.constants.Status.FAIL);
                            Assert.fail("Response body " + arrayName + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                        }
                    }
                }
            }
        }
    }

    @And("User should validate that response body {string} contain {string} for keys {string}")
    public void userShouldValidateThatResponseBodyContainsErrors(String arrayName, String expValues, String keys) {
        String[] arrMultipleErrors;
        String[] arrExpValues;
        String[] arrKeys;
        JSONObject responseObj;
        try {
            Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
            String responseBody = responseMap.get(ResponseObject.Body);
            JSONArray responseArray = (new JSONObject(responseBody)).getJSONArray(arrayName);

            if (expValues.contains("~~")) {
                arrExpValues = expValues.split("~~");
            } else {
                arrExpValues = new String[]{expValues};
            }
            if (keys.contains("~~")) {
                arrKeys = keys.split("~~");
            } else {
                arrKeys = new String[]{keys};
            }

            if (responseArray.length() > 0) {
                for (int i = 0; i < responseArray.length(); i++) {
                    responseObj = responseArray.getJSONObject(i);
                    for (int j = 0; j < arrKeys.length; j++) {
                        extentTest.log(LogStatus.INFO, "Validating whether value for field  " + arrKeys[j] + " exists in " + arrayName);
                        if (arrKeys[j].equals("message")) {
                            boolean isErrorExist = false;
                            if (arrExpValues[j].contains("@@")) {
                                arrMultipleErrors = arrExpValues[j].split("@@");
                            } else {
                                arrMultipleErrors = new String[]{arrExpValues[j]};
                            }
                            for (String error : arrMultipleErrors) {
                                if (responseObj.get(arrKeys[j]).toString().equalsIgnoreCase(error)) {
                                    extentTest.log(LogStatus.PASS, "Response body " + arrayName + " field " + arrKeys[j] + " value is as per expectation : " + arrExpValues[j]);
                                    isErrorExist = true;
                                    break;
                                }
                            }
                            if (!isErrorExist) {
                                extentTest.log(LogStatus.FAIL, "Response body " + arrayName + " field " + arrKeys[j] + " value is not as per expectation : " + arrExpValues[j] + " actual value is " + responseObj.get(arrKeys[j]));
                                logger.logTestStepInText("Response body " + arrayName + " field " + arrKeys[j] + " value is not as per expectation : " + arrExpValues[j] + " actual value is " + responseObj.get(arrKeys[j]), com.archinsurance.log.constants.Status.FAIL);
                                Assert.fail("Response body " + arrayName + " field " + arrKeys[j] + " value is not as per expectation : " + arrExpValues[j] + " actual value is " + responseObj.get(arrKeys[j]));
                            }
                        } else {
                            if (responseObj.get(arrKeys[j]).toString().equalsIgnoreCase(arrExpValues[j])) {
                                extentTest.log(LogStatus.PASS, "Response body " + arrayName + " field " + arrKeys[j] + " value is as per expectation : " + arrExpValues[j]);
                            } else {
                                extentTest.log(LogStatus.FAIL, "Response body " + arrayName + " field " + arrKeys[j] + " value is not as per expectation : " + arrExpValues[j] + " actual value is " + responseObj.get(arrKeys[j]));
                                logger.logTestStepInText("Response body " + arrayName + " field " + arrKeys[j] + " value is not as per expectation : " + arrExpValues[j] + " actual value is " + responseObj.get(arrKeys[j]), com.archinsurance.log.constants.Status.FAIL);
                                Assert.fail("Response body " + arrayName + " field " + arrKeys[j] + " value is not as per expectation : " + arrExpValues[j] + " actual value is " + responseObj.get(arrKeys[j]));
                            }
                        }
                    }
                }
            } else {
                extentTest.log(LogStatus.FAIL, "Response body doesn't contain messages in " + arrayName);
                logger.logTestStepInText("Response body doesn't contain messages in " + arrayName, com.archinsurance.log.constants.Status.FAIL);
                Assert.fail("Response body doesn't contain messages in " + arrayName);
            }
        } catch (Exception e) {
            extentTest.log(LogStatus.FAIL, "Exception occured " + e.getMessage());
            Assert.fail("Exception occured " + e.getMessage());
        }
    }

    @And("User should validate that response body contain values {string} for keys {string}")
    public void userShouldValidateThatResponseBodyContainValuesForKeys(String expValues, String keys) {
        String[] arrExpValues;
        String[] arrKeys;
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);

        JSONObject responseObj = new JSONObject(responseBody);

        if (expValues.contains("~~")) {
            arrExpValues = expValues.split("~~");
            arrKeys = keys.split("~~");
        } else {
            arrExpValues = new String[]{expValues};
            arrKeys = new String[]{keys};
        }

        for (int i = 0; i < arrKeys.length; i++) {
            extentTest.log(LogStatus.INFO, "Validating whether value for field  " + arrKeys[i] + " exists in response");
            if (responseObj.toString().contains(arrExpValues[i])) {
                extentTest.log(LogStatus.PASS, "Response body field " + arrKeys[i] + " value is as per expectation : " + arrExpValues[i]);
            } else {
                extentTest.log(LogStatus.FAIL, "Response body field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.getString(arrKeys[i]));
                logger.logTestStepInText("Response body field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.getString(arrKeys[i]), com.archinsurance.log.constants.Status.FAIL);
                Assert.fail("Response body field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.getString(arrKeys[i]));
            }
        }
    }

    @And("User should validate that response body contains value for keys {string} is not coming as blank")
    public void userShouldValidateThatResponseBodyContainsForKeysIsNotComingAsBlank(String keys) {
        String[] arrKeys;
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        JSONObject responseObj = new JSONObject(responseBody);

        if (keys.contains("~~")) {
            arrKeys = keys.split("~~");
        } else {

            arrKeys = new String[]{keys};
        }

        for (String arrKey : arrKeys) {
            extentTest.log(LogStatus.INFO, "Validating whether value for field  " + arrKey);
            if (!responseObj.getString(arrKey).isEmpty()) {
                extentTest.log(LogStatus.PASS, "Response body field " + arrKey + " value is as per expectation and actual value is-" + responseObj.getString(arrKey));
            } else {
                extentTest.log(LogStatus.FAIL, "Response body field " + arrKey + " value is not as per expectation and actual value is- " + responseObj.getString(arrKey));
                logger.logTestStepInText("Response body field " + arrKey + " actual value is " + responseObj.getString(arrKey), Status.FAIL);
                Assert.fail("Response body field " + arrKey + " actual value is " + responseObj.getString(arrKey));
            }
        }
    }

    @And("User should validate that response body contains values for keys {string}")
    public void userShouldValidateThatResponseBodyContainsValuesForKeys(String keys) {
        String[] arrKeys;
        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        JSONObject responseObj = new JSONObject(responseBody);

        if (keys.contains("~~")) {
            arrKeys = keys.split("~~");
        } else {
            arrKeys = new String[]{keys};
        }

        for (int i = 0; i < responseObj.length(); i++) {
            for (String arrKey : arrKeys) {
                extentTest.log(LogStatus.INFO, "Validating whether key  " + arrKey + " exists ");
                if (responseObj.getJSONObject("error").has(arrKey)) {
                    extentTest.log(LogStatus.PASS, "Response body contain key " + arrKey + " with values " + responseObj.getJSONObject("error").getString(arrKey));
                } else {
                    extentTest.log(LogStatus.FAIL, "Response body not contain key " + arrKey + " value is not present " + responseObj.getJSONObject("error"));
                    logger.logTestStepInText("Response body field " + arrKey + " value is not present in response " + responseObj.getJSONObject("error"), Status.FAIL);
                    Assert.fail("Response body field " + arrKey + " value is not not present in response " + responseObj.getJSONObject("error"));
                }
            }
        }
    }

    @And("User should store value of response path {string} with key {string}")
    public void userShouldStoreValueOfResponsePath(String jsonPathExp, String key) {
        try {
            JsonPath jsonPath = JsonPath.from(response.asPrettyString());
            Object actualValue = jsonPath.get(jsonPathExp);
            apiData.put(key.toLowerCase(), actualValue.toString());
        } catch (Exception e) {
            extentTest.log(LogStatus.FAIL, "Exception occured while fetching value form JSON path " + jsonPathExp);
            logger.logTestStepInText("Exception Occured while fetching value form JSON path " + jsonPathExp, Status.FAIL);
            Assert.fail("Exception Occured while fetching value form JSON path " + jsonPathExp);
        }
    }

    @When("User hits {string} api endpoint {string} and parameters {string} and value {string} with {string} and {string} in {string} folder having keep_input_in_json as {string}")
    public void userHitsApiEndpointAndParametersAndValueAndMultipart(String verb, String endpoint, String key, String value, String fileType, String fileName, String folderName, String keepJson) throws IOException {
        Map<String, String> queryParams = new HashMap<>();

        if (key.contains(",")) {
            String[] keys = key.split(",");
            String[] values = value.split(",");

            for (int i = 0; i < keys.length; i++) {
                queryParams.put(keys[i], values[i]);
            }
        } else {
            queryParams.put(key, value);
        }
        String folderPath = new File(ResourceUtil.getTestDataPath() + File.separator + folderName + File.separator + fileName).getCanonicalPath();
        Map<String, String> multipart = new HashMap<>();
        multipart.put(fileType, folderPath);
        if(keepJson.equals("true")) {
            multipart.put("keep_input_in_json", "true");
        }
        response = restAssuredUtils.getResponseMultiPartAndParam(endpoint, APIVerb.valueOf(verb), null, multipart, queryParams);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    //RA: PDE Specific Methods

    @And("SAN is sliced into 10 character - policyNum")
    public void sliceSanToPolicyNumber(){
        String policyNumber = StringUtils.left(apiData.get("san"),10);
        apiData.put("policyNum",policyNumber);
        System.out.println("SAN: " + apiData.get("san"));
        System.out.println("Policy Number: " + policyNumber);
    }

    @When("Read JSON payload file {string} in folder {string}")
    public void readJSON(String payloadFilePath, String folderName) throws IOException {
        String jsonFilePath = getResponseFilePath(payloadFilePath, folderName);
        finalPayloadContent = "";

        try {
            FileReader reader = new FileReader(jsonFilePath);
            int character;

            while ((character = reader.read()) != -1) {
                finalPayloadContent = finalPayloadContent + (char) character;
            }
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @And("Replace JSON attribute {string} with {string} and store in payload key name {string}")
    public void replaceJsonAttr(String attrKey, String attrKeyValue, String finalPayloadContentKey) throws JSONException, IOException
    {
        String finalKeyValue = "";

        if(attrKeyValue.length()>3 && attrKeyValue.substring(0,4).equals("key_")){
            finalKeyValue = apiData.get(attrKeyValue.substring(4));
        }
        else{
            finalKeyValue = attrKeyValue;
        }
        finalPayloadContent = finalPayloadContent.replace(attrKey,finalKeyValue);
        apiData.put(finalPayloadContentKey,finalPayloadContent);
    }

    @When("User hits {string} api endpoint {string} with payload key name {string}")
    public void userHitsApiEndpointWithPayloadKey(String verb, String endpoint, String payloadKeyName) throws IOException {
        String payloadBody = apiData.get(payloadKeyName);
        response = restAssuredUtils.getResponse(endpoint, APIVerb.valueOf(verb.toUpperCase()), payloadBody);
        extentTest.log(LogStatus.INFO, "Response body : " + response.asPrettyString());
        logger.logTestStepInText("Response body : " + response.asPrettyString(), Status.INFO);
    }

    // RA: PDE Specific Methods - End

    // Private methods

    private void configureRestSpec(String apiType, String... isAuthorized) throws ConfigurationException, IOException {
        String apiPhrase;

        String apiConfigPath = new File(ResourceUtil.getTestResourceFolderPath() + File.separator + "config" + File.separator + "APIConfig.properties").getCanonicalPath();
        PropertiesConfiguration config = new PropertiesConfiguration(apiConfigPath);
        APIProfile.setConfig(apiConfigPath);
        profile = new APIProfile();

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        profile.setHeaders(headers);

        //reading auth payload to retrieve basic auth inputs
        String responseFilePath = getResponseFilePath("authPayload.json");
        String payloadBody = CommonUtils.readFileAsString(responseFilePath);
        ObjectMapper mapper = new ObjectMapper();
        AuthRequest authReq = mapper.readValue(payloadBody, AuthRequest.class);

        String baseURL = config.getString(apiType.toUpperCase() + Config.getEnvironmentForExecution().name() + "BaseURL");

        switch (apiType.toUpperCase()) {
            case "PDEADAPTER":
            case "PDEADAPTER_PEAPI_V2":
                apiPhrase = "";
                break;
            default:
                throw new IllegalArgumentException("Incorrect API Type " + apiType + " supplied");
        }

        if (StringUtils.isBlank(apiPhrase)) {
            profile.setBaseURI(baseURL + "/");
        } else {
            profile.setBaseURI(baseURL + "/" + apiPhrase);
        }

        headers = new HashMap<>();
        if (isAuthorized.length == 0) {
            //headers.put("Authorization", getBasicAuthStr(authReq.getClientId(), authReq.getClientSecret()));
            //RA: PDE Specific Authorization
            headers.put("Authorization", "Bearer null");
            profile.setHeaders(headers);
        }
        restAssuredUtils = new RestAssuredUtils(profile);
    }

    private String getBasicAuthStr(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
        return "Basic " + new String(encodedAuth);
    }

    private String formatInputPayload(String payload, String randomKeys, String inputVariables, String inputVariablesValues) {
        String finalPayload = payload;
        String[] arrRandomKeys;
        String[] arrInputVariables;
        String[] arrInputVariablesValue;

        Person person = fairy.person(PersonProperties.telephoneFormat("#########"), PersonProperties.withCompany(company));
        Random random = new Random();

        if (randomKeys.contains("~~")) {
            arrRandomKeys = randomKeys.split("~~");
        } else {
            arrRandomKeys = new String[]{randomKeys};
        }

        for (String arrRandomKey : arrRandomKeys) {
            if (finalPayload.contains(arrRandomKey)) {
                switch (arrRandomKey) {
                    case "{$randomInt}":
                        finalPayload = finalPayload.replace(arrRandomKey, String.valueOf(random.nextInt(1000)));
                        break;
                    case "{$randomCompanyName}":
                        finalPayload = finalPayload.replace(arrRandomKey, person.getCompany().getName());
                        break;
                    case "{$randomStreetName}":
                        finalPayload = finalPayload.replace(arrRandomKey, person.getAddress().getStreet());
                        break;
                    case "{$today}":
                        finalPayload = finalPayload.replace(arrRandomKey, DateTimeFormatter.ofPattern("MM/dd/yyyy").format(LocalDateTime.now()));
                        break;
                    case "{$futureDate}":
                        finalPayload = finalPayload.replace(arrRandomKey, DateTimeFormatter.ofPattern("MM/dd/yyyy").format(LocalDateTime.now().plusYears(1)));
                        break;
                    case "{$name}":
                        finalPayload = finalPayload.replace(arrRandomKey, CommonUtils.randomString());
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid random key provided ");
                }
            }
        }

        if (inputVariables.contains("~~") && inputVariablesValues.contains("~~")) {
            arrInputVariables = inputVariables.split("~~");
            arrInputVariablesValue = inputVariablesValues.split("~~");
        } else {
            arrInputVariables = new String[]{inputVariables};
            arrInputVariablesValue = new String[]{inputVariablesValues};
        }

        for (int i = 0; i < arrInputVariables.length; i++) {
            if (finalPayload.contains(arrInputVariables[i])) {
                if (inputVariablesValues.endsWith("~~") && i == arrInputVariables.length - 1) {
                    finalPayload = finalPayload.replace(arrInputVariables[i], "");
                } else {
                    finalPayload = finalPayload.replace(arrInputVariables[i], arrInputVariablesValue[i]);
                }
            }
        }
        return finalPayload;
    }

    private static String formatInputPayload(String payload, String variables, String varValues) {
        String finalPayload = payload;

        List<String> vars = Arrays.asList(variables.split("~~"));
        List<String> values = new ArrayList<>(Arrays.asList(varValues.split("~~")));
        int minCount = values.size();
        int maxCount = vars.size();

        // If keys and values are not equal sized
        if (minCount < maxCount) {
            // Adding blank element in values to make it equal sized of keys
            for (int i = minCount; i < maxCount; i++) {
                values.add("");
            }
        }

        for (int i = 0; i < vars.size(); i++) {
            if (StringUtils.isBlank(values.get(i))) {
                String key = vars.get(i).replace("$", "")
                        .replace("{", "")
                        .replace("}", "")
                        .replace("_", "")
                        .toLowerCase();
                if (apiData.containsKey(key)) {
                    values.set(i, apiData.get(key));
                }
            }

            if (finalPayload.contains(vars.get(i))) {
                if (values.get(i).equals("{}")) {
                    finalPayload = finalPayload.replace(vars.get(i), "");
                } else {
                    if (varValues.endsWith("~~") && i == vars.size() - 1) {
                        finalPayload = finalPayload.replace(vars.get(i), "");
                    } else {
                        finalPayload = finalPayload.replace(vars.get(i), values.get(i));
                    }
                }
            }
        }
        return finalPayload;
    }

    private static String formatInputPayloadUsingExcel(String payload, String workbookName, String sheetName, String folderName) {
        String finalPayload = payload;
        Map<String, String> inputValues = getInputValuesFromExcel(workbookName, sheetName, folderName);

        for (Map.Entry<String, String> entry : inputValues.entrySet()) {
            if (finalPayload.contains(entry.getKey())) {
                if (entry.getValue().endsWith(".0")) {
                    finalPayload = finalPayload.replace(entry.getKey(), entry.getValue().replace(".0", ""));
                } else {
                    finalPayload = finalPayload.replace(entry.getKey(), entry.getValue());
                }
            }
        }
        return finalPayload;
    }

    private static Map<String, String> getInputValuesFromExcel(String dataFile, String sheetName, String folderName) {
        ExcelObject excelObject = null;
        List<List<Object>> expValuesList = null;
        Map<String, String> inputValues = new HashMap<>();

        try {
            String testDataPath = new File(ResourceUtil.getTestDataPath() + File.separator + folderName + File.separator + dataFile).getCanonicalPath();
            excelObject = new ExcelObject(testDataPath);
            expValuesList = excelObject.getExcelData(sheetName);
        } catch (IOException e) {
            //do nothing
        } finally {
            if (excelObject != null) {
                try {
                    excelObject.closeWorkbook();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }

        for (int i = 0; i < Objects.requireNonNull(expValuesList).get(0).size(); i++) {
            inputValues.put(expValuesList.get(0).get(i).toString(), expValuesList.get(1).get(i).toString());
        }
        return inputValues;
    }

    private static String getResponseFilePath(String expectedResponseFile) throws IOException {
        return new File(ResourceUtil.getTestDataPath() + File.separator + expectedResponseFile).getCanonicalPath();
    }

    private static String getResponseFilePath(String expectedResponseFile, String folderName) throws IOException {
        return new File(ResourceUtil.getTestDataPath() + File.separator + folderName + File.separator + expectedResponseFile).getCanonicalPath();
    }

    @And("User should validate that response body  contains {string} for keys {string} in {string}")
    public void userShouldValidateThatResponseBodyContainsForKeysIn(String expValues, String keys, String objectPath) {
        String[] arrExpValues;
        String[] arrKeys;

        Map<ResponseObject, String> responseMap = restAssuredUtils.getResponseMap(response);
        String responseBody = responseMap.get(ResponseObject.Body);
        JSONObject responseObj = new JSONObject(responseBody);
        if (objectPath.contains("->")) {
            String[] paths = objectPath.split("->");
            for (String path : paths) {
                responseObj = responseObj.getJSONObject(path);
            }
        } else {
            responseObj = (new JSONObject(responseBody)).getJSONObject(objectPath);
        }

        if (expValues.contains("~~")) {
            arrExpValues = expValues.split("~~");
            arrKeys = keys.split("~~");
        } else {
            arrExpValues = new String[]{expValues};
            arrKeys = new String[]{keys};
        }

        for (int i = 0; i < arrKeys.length; i++) {
            extentTest.log(LogStatus.INFO, "Validating whether value for field  " + arrKeys[i] + " exists in " + objectPath);
            if (responseObj.has(arrKeys[i])) {
                if (arrKeys[i].equals("error_message")) {
                    if (responseObj.get(arrKeys[i]).toString().contains(arrExpValues[i])) {
                        extentTest.log(LogStatus.PASS, "Response body " + objectPath + " field " + arrKeys[i] + " value is as per expectation : " + arrExpValues[i]);
                    } else {
                        extentTest.log(LogStatus.FAIL, "Response body " + objectPath + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                        logger.logTestStepInText("Response body " + objectPath + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]), com.archinsurance.log.constants.Status.FAIL);
                        Assert.fail("Response body " + objectPath + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                    }
                } else {
                    if (responseObj.get(arrKeys[i]).toString().equalsIgnoreCase(arrExpValues[i])) {
                        extentTest.log(LogStatus.PASS, "Response body " + objectPath + " field " + arrKeys[i] + " value is as per expectation : " + arrExpValues[i]);
                    } else {
                        extentTest.log(LogStatus.FAIL, "Response body " + objectPath + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                        logger.logTestStepInText("Response body " + objectPath + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]), com.archinsurance.log.constants.Status.FAIL);
                        Assert.fail("Response body " + objectPath + " field " + arrKeys[i] + " value is not as per expectation : " + arrExpValues[i] + " actual value is " + responseObj.get(arrKeys[i]));
                    }
                }
            }
        }

    }

}