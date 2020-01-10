/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2019                                           
 *                                                                                                                                 
 *  Creation Date: 04.12.2019                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.sample.controller.email;

import java.io.PrintStream;
import java.util.HashMap;

import org.oscm.app.sample.controller.Email;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;

/**
 * @author goebel
 *
 */
public class HTMLEmail extends Email {

    private String css;

    public HTMLEmail(ProvisioningSettings ps, String css) {
        super(ps);
        this.css = css;
    }
    
    @Override
    protected String createConfirmationLink(String instanceId)
            throws APPlatformException {
        StringBuilder eventLink = new StringBuilder();
        eventLink.append("<a href=");
        eventLink.append(super.createConfirmationLink(instanceId));
        eventLink.append(">");
        return eventLink.toString();
    }

    @Override
    protected void writeHeader(PrintStream out) {
        out.println("<html>");
        out.println("   <head>");
        out.println("      <meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\">");
        out.println("      <style>");
        out.println(css);
        out.println("      </style>");
        out.println("   </head>");

        out.println("   <body>");
    }

    @Override
    protected void writeTable(String caption, HashMap<String, Setting> rows, PrintStream out) {
        if (!rows.isEmpty()) {
            out.println("      <table>");
            
            out.print("         <caption>");
            out.print(caption);
            out.println("</caption>");
            out.println("         <colgroup>");                        
            out.println("            <col class=\"tcol\">");      
            out.println("            <col class=\"vcol\">");
            out.println("         </colgroup>");
                        
            out.println("         <tbody>");
            writeSettings(rows, out);
            out.println("         </tbody>");
            out.println("      </table>");
            out.println("<p/>");
        }
    }
    
    protected String row(String name, String value) {
        return String.format("<tr><td>%s</td><td class=\"cval\">%s</td></tr>", name, value);
    }

    @Override
    protected String getContentType() {
        return "text/html;charset=UTF-8";
    }

    @Override
    protected void writeText(PrintStream out) {
        out.print("      <p>");
        out.println(getText());
        out.println("</p>");
    }

    @Override
    protected void writeFooter(PrintStream out) {
        out.println("   </body>");
        out.println("</html>");
    }

}
