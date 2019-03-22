/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2018
 *                                                                              
 *  Creation Date: 03.06.2014                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.openstack.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
//TODO: check it
//import org.oscm.app.common.intf.ControllerAccessTest;
import org.oscm.app.openstack.i18n.Messages;

public class OpenStackControllerAccessTest /*extends ControllerAccessTest*/ {

    @Test
    public void testGetControllerId() throws Exception {
        assertEquals(OpenStackController.ID,
                new OpenStackControllerAccess().getControllerId());
    }

    @Test
    public void testGetMessage() throws Exception {
        assertNotNull(new OpenStackControllerAccess()
                .getMessage(Messages.DEFAULT_LOCALE, "key", "args0"));
    }

    @Test
    public void testGetConfigKeys() throws Exception {
        List<String> controllerParameterKeys = new OpenStackControllerAccess()
                .getControllerParameterKeys();
        assertNotNull(controllerParameterKeys);
        assertEquals(7, controllerParameterKeys.size());
    }

    @Test
    @Ignore
    public void testProperties() throws Exception {
        //TODO: check it
        /*checkMessageProperties(Messages.DEFAULT_LOCALE,
                new OpenStackControllerAccess());*/
    }
}
