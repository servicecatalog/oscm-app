/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2020
 *
 *  Creation Date: 2020-11-20
 *
 *******************************************************************************/
package org.oscm.app.vmware.business.statemachine;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.business.VMwareDatacenterInventory;
import org.oscm.app.vmware.business.balancer.DynamicEquipartitionStorageBalancer;
import org.oscm.app.vmware.business.balancer.XMLHelper;
import org.oscm.app.vmware.business.model.VMwareStorage;
import org.oscm.app.vmware.business.statemachine.api.StateMachineAction;
import org.oscm.app.vmware.business.statemachine.api.StateMachineException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({StateMachine.class, Thread.class})
public class StateMachineTest {

    private StateMachine stateMachine;
    private Thread thread;
    private ClassLoader loader;

    static ProvisioningSettings ps;
    static HashMap<String, Setting> parameters;
    static HashMap<String, Setting> configSettings;
    static HashMap<String, Setting> attributes;
    static HashMap<String, Setting> customAttributes;

    @BeforeClass
    public static void setup() {
        parameters = new HashMap<>();
        configSettings = new HashMap<>();
        attributes = new HashMap<>();
        customAttributes = new HashMap<>();

        parameters.put("SM_STATE", new Setting("state1", "delete_vm.xml"));
        parameters.put("SM_STATE_HISTORY", new Setting("state2", "Value2"));
        parameters.put("SM_STATE_MACHINE", new Setting("state3", "delete_vm.xml"));
        configSettings.put("key1", new Setting("name1", "value1"));
        configSettings.put("key2", new Setting("name2", "value2"));
        attributes.put("attr1", new Setting("key1", "value1"));
        attributes.put("attr2", new Setting("key2", "value2"));
        customAttributes.put("cuAttr1", new Setting("key1", "value1"));
        customAttributes.put("cuAttr2", new Setting("key2", "value2"));

        ps = new ProvisioningSettings(parameters, attributes, customAttributes, configSettings, "en");
    }

    @Before
    public void setUp() throws StateMachineException, FileNotFoundException {
        PowerMockito.mockStatic(Thread.class);
        thread = mock(Thread.class);
        loader = mock(ClassLoader.class);

        final File initialFile = new File("test-jar/src/main/resources/statemachines/delete_vm.xml");
        final InputStream targetStream =
            new DataInputStream(new FileInputStream(initialFile));

        when(Thread.currentThread()).thenReturn(thread);
        when(thread.getContextClassLoader()).thenReturn(loader);
        when(loader.getResourceAsStream(anyString())).thenReturn(targetStream);
        stateMachine = PowerMockito.spy(new StateMachine(ps));
    }

    @Test
    public void testLoadStateMachine() throws Exception {

        Whitebox.invokeMethod(stateMachine, "loadStateMachine", "oscm-app-vmware-statemachines/src/main/resources/statemachines/delete_vm.xml");
    }
}
