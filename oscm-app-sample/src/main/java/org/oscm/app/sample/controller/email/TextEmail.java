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

/**
 * @author goebel
 *
 */
public class TextEmail extends Email {

    public TextEmail(ProvisioningSettings ps) {
        super(ps);
    }

    @Override
    protected String getContentType() {
        return "text/plain; charset=UTF-8";
    }

    @Override
    protected void writeHeader(PrintStream out) {

    }

    @Override
    protected void writeTable(String caption, HashMap<String, Setting> rows, PrintStream out) {
        if (!rows.isEmpty()) {
            out.println("\r\n"+  caption);
            for (char c : caption.toCharArray())
                out.print("-");
            out.print("\n");
            writeSettings(rows, out);
        }
    }

    @Override
    protected void writeText(PrintStream out) {
        out.println(getText());
    }

    @Override
    protected void writeFooter(PrintStream out) {
    }

    @Override
    protected String row(String name, String value) {
      return String.format("%s : %s", name,
                value);
    }

}
