/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2019                                           
 *                                                                                                                                 
 *  Creation Date: 12.12.2019                                                      
 *                                                                              
 *******************************************************************************/
package org.oscm.app.sample.controller;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;

/**
 * @author goebel
 *
 */
public class EmailTest {

    static ProvisioningSettings ps;
    
    @BeforeClass
    public static void setup() {
        HashMap<String, Setting> parameters = new HashMap<String, Setting>() ;
        HashMap<String, Setting> configSettings  = new HashMap<String, Setting>() ;
        
        parameters.put("key1", new Setting("name1", "value1"));
        parameters.put("key2", new Setting("name2", "value2"));
        parameters.put("CSSSTYLE", new Setting("name2", "value2")); 
        configSettings.put("key1", new Setting("name1", "value1"));
        configSettings.put("key2", new Setting("name2", "value2"));
        
        ps  = new ProvisioningSettings(parameters, configSettings,
                "en");
    }

   @Test 
   public void testEmail() {
       setNoCSS();
       
       Email e = Email.get(ps);
       e.mainText = "Das ist eine Test Email";
       System.out.println(e.getBody());
   }
   
   
   @Test 
   public void testHtmlEmail() {
       setCSS();
       Email e = Email.get(ps);
       e.mainText = "Das ist eine Test Email";
       System.out.println(e.getBody());
   }
   
   private void setCSS() {
       HashMap<String, Setting> params = ps.getParameters();
       params.put(Email.CSS_STYLE, new Setting(Email.CSS_STYLE, "td, th, caption  { font: 15px arial;  }   caption  { font: bold 15px arial; background-color: #F2F2F2; margin:5px;}        .tcol       { width: 200px; }     .vcol       { width: 400px; }      .ckey        {  font: 15px arial; }      .cval        {  font: 15px arial; background-color: #F2F2F2; }  "));
       
   }
   
   private void setNoCSS() {
       HashMap<String, Setting> params = ps.getParameters();
       params.put(Email.CSS_STYLE, new Setting(Email.CSS_STYLE, ""));
   }
}
