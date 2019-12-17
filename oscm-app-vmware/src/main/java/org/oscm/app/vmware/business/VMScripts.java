/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2019                                           
 *                                                                                                                                 
 *  Creation Date: 16 Dec 2019                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.vmware.business;

/**
 * @author worf
 *
 */
public class VMScripts {

    private static final String LINUX_SCRIPT_HEADER = "#!/bin/sh";
    
    public static String updateLinuxVMRootPassword(String password) {
        StringBuilder script = new StringBuilder();
        script.append(LINUX_SCRIPT_HEADER);
        script.append("\n");
        script.append("echo -e ");
        script.append("\"");
        script.append(password);
        script.append("\n");
        script.append(password);
        script.append("\"");
        script.append(" | passwd root");
        return script.toString();
    }
}
