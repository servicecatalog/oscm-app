/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2019                                           
 *                                                                                                                                 
 *  Creation Date: 12.12.2019                                                      
 *                                                                              
 *******************************************************************************/
package org.oscm.app.sample.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;


/**
 * @author goebel
 *
 */
public class EmailTest {

    static ProvisioningSettings ps;

    @BeforeClass
    public static void setup() {
        HashMap<String, Setting> parameters = new HashMap<String, Setting>();
        HashMap<String, Setting> configSettings = new HashMap<String, Setting>();

        parameters.put("key1", new Setting("name1", "value1"));
        parameters.put("key2", new Setting("name2", "value2"));
        parameters.put("CSSSTYLE", new Setting("name2", "value2"));
        configSettings.put("key1", new Setting("name1", "value1"));
        configSettings.put("key2", new Setting("name2", "value2"));

        ps = new ProvisioningSettings(parameters, configSettings, "en");
    }

    @Test
    public void testEmail() {
        // given
        givenCSSIsNotUsed();

        // when
        Email e = getEmail();

        // then
        assertTextType(e);
    }

    @Test
    public void testHtmlEmail() {
        // given
        givenCSSIsUsed();
        
        // when
        Email e = getEmail();

        // then
        assertHTMLType(e);
    }

    @Test
    public void testEmail_filterPWD() {

        setPassword();
        givenCSSIsUsed();

        Email e = getEmail();

        // when
        String body = e.getBody();

        assertNoPassword(body);
    }
    
  @Test
  public void testCreateConfirmationLink() throws APPlatformException {
      //given
      givenCSSIsNotUsed();
      setAppBaseUrl();
      setAppControllerID();
      Email e = getEmail();
      //when
      String result = e.createConfirmationLink("1");
      
      //then
      assertConfirmationLink(result);
  }
  
  @Test
  public void testCreateConfirmationLinkHTML() throws APPlatformException {
      //given
      givenCSSIsUsed();
      setAppBaseUrl();
      setAppControllerID();
      Email e = getEmail();
      
      //when
      String result = e.createConfirmationLink("1");
      
      //then
      assertHTMLConfirmationLink(result);
  }
  
    @Test
    public void testSampleSubject() {
        // given
        givenCSSIsNotUsed();
        Email e = getEmail();
        String instanceId = "1";
        Status currentState = Status.CREATION_STEP2;

        // when
        String subject = e.getSubject(instanceId, currentState);

        // then
        assertSampleSubject(subject);
    }

    @Test
    public void testConfiguredSubject() {
        // given
        givenCSSIsNotUsed();
        setMailSubject();
        Email e = getEmail();
        String instanceId = "1";
        Status currentState = Status.MANUAL_CREATION;
        
        // when
        String subject = e.getSubject(instanceId, currentState);
 
        // then
        assertConfiguredSubject(subject);
    }
    
    @Test
    public void testSampleText() {
        // given
        givenCSSIsNotUsed();
        setSampleEmailText();
        Email e = getEmail();
        String instanceId = "1";
        Status currentState = Status.CREATION_STEP2;

        // when
        String text = e.getText(instanceId, currentState);

        // then
        assertSampleText(text);
    }
    
    @Test
    public void testConfiguredText() {
        // given
        givenCSSIsNotUsed();
        setAppBaseUrl();
        setAppControllerID();
        setSampleEmailText();
        Email e = getEmail();
        String instanceId = "1";
        Status currentState = Status.MANUAL_CREATION;

        // when
        String text = e.getText(instanceId, currentState);

        // then
        assertConfiguredText(text);
    }
    
    private void assertConfiguredText(String text) {
        assertEquals("Some message", text);
    }

    private void assertSampleText(String text) {
        assertEquals(
                "The sample instance '1' is currently being provisioned."
                        + " Current status: CREATION_STEP2.\n\n Some message",
                text);
    }

    private void assertConfiguredSubject(String subject) {
        assertEquals("This is a test subject", subject);
    }
    
    private void assertSampleSubject(String subject) {
        assertEquals("Sample instance '1' is currently being provisioned.", subject);
    }
  
    private void assertNoPassword(String body) {
        assertFalse(body.contains("PWD"));
    }
    
    private void assertHTMLConfirmationLink(String result) {
        assertEquals(getHTMLConfirmationLink(), result);
    }

    private void assertConfirmationLink(String result) {
        assertEquals(getConfirmationLink(), result);
    }
    
    private void assertHTMLType(Email e) {
        assertTrue(e.getBody().contains("<html>"));
        assertTrue(e.getBody().contains("meta content=\"text/html"));
        assertEquals("text/html;charset=UTF-8", e.getContentType());

    }

    private void assertTextType(Email e) {
        assertFalse(e.getBody().contains("<html>"));
        assertFalse(e.getBody().contains("text/html"));
        assertEquals("text/plain; charset=UTF-8", e.getContentType());
    }

    private void givenCSSIsUsed() {
        HashMap<String, Setting> params = ps.getParameters();
        params.put(Email.CSS_STYLE, new Setting(Email.CSS_STYLE,
                "td, th, caption  { font: 15px arial;  }   caption  { font: bold 15px arial; background-color: #F2F2F2; margin:5px;}        .tcol       { width: 200px; }     .vcol       { width: 400px; }      .ckey        {  font: 15px arial; }      .cval        {  font: 15px arial; background-color: #F2F2F2; }  "));

    }

    private void setPassword() {
        HashMap<String, Setting> params = ps.getParameters();
        params.put("CUSTOMER_PWD", new Setting("CUSTOMER_PWD", "test123"));
    }

    private void givenCSSIsNotUsed() {
        HashMap<String, Setting> params = ps.getParameters();
        params.put(Email.CSS_STYLE, new Setting(Email.CSS_STYLE, ""));
    }

    private Email getEmail() {
        Email e = Email.get(ps);
        e.mainText = "Das ist eine Test Email";
        System.out.println(e.getBody());
        return e;
    }
    
    private String getConfirmationLink() {
        return "https://fujitsu.com/global/notify?sid=1&controllerid=ess.sample&_resume=yes";
    }
    
    private String getHTMLConfirmationLink() {
        return "<a href=https://fujitsu.com/global/notify?sid=1&controllerid=ess.sample&_resume=yes>";
    }

    private void setAppControllerID() {
        HashMap<String, Setting> params = ps.getParameters();
        params.put("APP_CONTROLLER_ID", new Setting("APP_CONTROLLER_ID", "ess.sample"));
    }
    
    private void setAppBaseUrl() {
        HashMap<String, Setting> params = ps.getParameters();
        params.put("APP_BASE_URL_FOR_NOTIFICATION", new Setting("APP_BASE_URL_FOR_NOTIFICATION", "https://fujitsu.com/global"));
    }
    
    private void setMailSubject() {
        HashMap<String, Setting> params = ps.getParameters();
        params.put("EMAIL_SUBJECT", new Setting("EMAIL_SUBJECT", "This is a test subject"));
    }

    private void setSampleEmailText() {
        HashMap<String, Setting> params = ps.getParameters();
        params.put("PARAM_MESSAGETEXT",
                new Setting("PARAM_MESSAGETEXT", "Some message"));
    }

}
