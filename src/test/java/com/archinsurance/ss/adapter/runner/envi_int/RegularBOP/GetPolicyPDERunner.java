package com.archinsurance.ss.adapter.runner.envi_int.RegularBOP;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

//@RunWith(Cucumber.class)
@CucumberOptions(features = {"src/test/resources/feature/ss/adapter/api/envi_int/GetPolicyPDE.feature"},
        glue = {"com/archinsurance/ss/adapter/api/stepdefinition"},
        plugin = {
                "html:target/cucumber-html-report-api.html",
                "json:target/cucumber-api.json",
                "pretty:target/cucumber-pretty-api.txt",
                "usage:target/cucumber-usage-api.json",
                "junit:target/cucumber-results-api.xml",
        })
public class GetPolicyPDERunner extends AbstractTestNGCucumberTests {
}
