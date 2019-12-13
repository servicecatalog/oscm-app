/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2019                                           
 *                                                                                                                                 
 *  Creation Date: 12.12.2019                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.sample.controller;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;

import org.oscm.app.sample.controller.email.HTMLEmail;
import org.oscm.app.sample.controller.email.TextEmail;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;

/**
 * @author goebel
 */
public abstract class Email {

    final static String CSS_STYLE = "CSSSTYLE";
    private HashMap<String, Setting> attributes;
    private HashMap<String, Setting> parameters;
    private HashMap<String, Setting> configsettings;
    private HashMap<String, Setting> customAttributes;
    protected String mainText;
    protected PrintStream ps;
  
    static Email get(ProvisioningSettings ps) {
        final Setting style = ps.getParameters().get(CSS_STYLE);
        if (style != null) {
            String CSS = style.getValue();
            if (CSS.length() > 0)
                return new HTMLEmail(ps, CSS);
        }
        return new TextEmail(ps);
    }

    protected Email(ProvisioningSettings ps) {
        attributes = ps.getAttributes();
        parameters = ps.getParameters();
        configsettings = ps.getConfigSettings();
        customAttributes = ps.getCustomAttributes();
    }
   
    public String getText() {
        return mainText;
    }

    /**
     * Create the email body displaying available parameters and attributes.
     */
    protected String getBody() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);

        writeHeader(ps);
        
        writeText(ps);

        writeTable("Service Parameters", parameters, ps);
        writeTable("Service Attributes", attributes, ps);
        writeTable("Configuration Settings",  configsettings, ps);
        writeTable("Custom Attributes", customAttributes, ps);
        
        writeFooter(ps);

        return bos.toString();
    }

    void send(List<String> recipients, String subject, String text) throws APPlatformException {

        mainText = text;
        
        String body = getBody();
        String format = getContentType();
        
        Session mailSession = getMailSession();
        MimeMessage message = initMessage(mailSession);
        
        List<InternetAddress> addrs = getAddresses(recipients);
        
        try {

            message.setRecipients(Message.RecipientType.TO, (InternetAddress[]) addrs.toArray());
            message.setSubject(subject, "UTF-8");
            message.setSentDate(new Date());

            message.setContent(body, format);

            Transport.send(message);

        } catch (Exception e) {
            throw new APPlatformException("Failed to send email.", e);
        }
    }

    private List<InternetAddress> getAddresses(List<String> recipients) throws APPlatformException {
        List<String> failed = Collections.emptyList();
        List<InternetAddress> addrs = new ArrayList<InternetAddress>();
        recipients.stream().forEach(r -> {
            try {
                addrs.add(new InternetAddress(r));
            } catch (AddressException e) {
                failed.add(r);
            }
        });

        if (!failed.isEmpty()) {
            throw new APPlatformException(String.format("Failed to send email to %s", failed.toString()));
        }
        return addrs;
    }
    
    private MimeMessage initMessage(Session mailSession) throws APPlatformException {
        MimeMessage message;
        try {
        String from = mailSession.getProperty("mail.smtp.from");
        message = new MimeMessage(mailSession);
        message.setFrom(new InternetAddress(from));
        } catch (Exception e) {
            throw new APPlatformException("Failed to send email.", e);
        }
        return message;
    }

    private Session getMailSession() throws APPlatformException {
        try {
            return (Session) new InitialContext().lookup("java:openejb/Resource/APPMail");
        } catch (Exception e) {
            throw new  APPlatformException(String.format(
                    "Session ressource %s not found. Check resource configuration in tomee.xml. Details: %s",
                    "APPMail", e.getMessage()));
        }
    }
    
    
    protected abstract void writeHeader(PrintStream out);
    
    protected abstract void writeFooter(PrintStream out);
    
    protected abstract void writeText(PrintStream out);

    protected abstract String getContentType();

    protected abstract void writeTable(String caption, HashMap<String, Setting> rows, PrintStream out);


}
