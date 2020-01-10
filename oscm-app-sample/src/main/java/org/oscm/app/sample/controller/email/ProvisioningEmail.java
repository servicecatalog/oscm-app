/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2020                                           
 *                                                                                                                                 
 *  Creation Date: 8 Jan 2020                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.sample.controller.email;


import org.oscm.app.v2_0.data.ProvisioningSettings;

/**
 * @author worf
 *
 */
public class ProvisioningEmail extends TextEmail{

    public ProvisioningEmail(ProvisioningSettings ps) {
        super(ps);
    }

}
