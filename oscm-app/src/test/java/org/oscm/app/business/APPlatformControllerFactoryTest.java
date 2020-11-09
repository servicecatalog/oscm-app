/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2020
 *
 *  Creation Date: Nov 9, 2020
 *
 *******************************************************************************/
package org.oscm.app.business;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.adapter.APPlatformControllerAdapter;
import org.oscm.app.v2_0.intf.APPlatformController;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.naming.InitialContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({APPlatformControllerFactory.class, APPlatformController.class})
public class APPlatformControllerFactoryTest {

    private APPlatformControllerFactory controllerFactory;
    private InitialContext context;
    private APPlatformController lookup;

    @Before
    public void setUp() {
        controllerFactory = PowerMockito.spy(new APPlatformControllerFactory());
        PowerMockito.mockStatic(APPlatformController.class);
        context = mock(InitialContext.class);
        lookup = mock(APPlatformController.class);
    }

    @Test
    public void testGetInstance() throws Exception {
        PowerMockito.whenNew(InitialContext.class).withAnyArguments().thenReturn(context);
        when(context.lookup(anyString())).thenReturn(lookup);
        PowerMockito.when(APPlatformController.class.isAssignableFrom(lookup.getClass())).thenReturn(true);

        APPlatformControllerAdapter result = Whitebox.invokeMethod(controllerFactory, "getInstance", "ControllerID");

        assertEquals(APPlatformControllerAdapter.class, result.getClass());
    }

    @Test(expected = Exception.class)
    public void testGetInstanceThrowsExce() throws Exception {
        PowerMockito.whenNew(InitialContext.class).withAnyArguments().thenReturn(context);
        when(context.lookup(anyString())).thenReturn(lookup);

        APPlatformControllerAdapter result = Whitebox.invokeMethod(controllerFactory, "getInstance", "ControllerID");
    }

}
