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
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.vmware.business.statemachine.api.StateMachineException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({States.class, APPlatformServiceFactory.class})
public class StatesTest {

  private State mockState;
  private States states;
  private InstanceStatus instanceStatus;
  private APPlatformService platformService;

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

    parameters.put("SM_STATE", new Setting("state1", "BEGIN"));
    parameters.put("SM_STATE_HISTORY", new Setting("state2", ""));
    parameters.put("SM_STATE_MACHINE", new Setting("state3", "CREATE_VM"));
    configSettings.put("key1", new Setting("name1", "value1"));
    configSettings.put("key2", new Setting("name2", "value2"));
    attributes.put("attr1", new Setting("key1", "value1"));
    attributes.put("attr2", new Setting("key2", "value2"));
    customAttributes.put("cuAttr1", new Setting("key1", "value1"));
    customAttributes.put("cuAttr2", new Setting("key2", "value2"));

    ps = new ProvisioningSettings(parameters, attributes, customAttributes, configSettings, "en");
  }

  @Before
  public void setUp() {
    mockState = mock(State.class);
    instanceStatus = mock(InstanceStatus.class);
    platformService = mock(APPlatformService.class);

    PowerMockito.mockStatic(APPlatformServiceFactory.class);
    when(APPlatformServiceFactory.getInstance()).thenReturn(platformService);
    MockitoAnnotations.initMocks(this);

    states = PowerMockito.spy(new States());
  }

  @Test
  public void testInvokeAction() throws Exception {
    // given
    states.setClass("org.oscm.app.vmware.business.statemachine.CreateActions");
    when(mockState.getAction()).thenReturn("reserveIPAddress");
    // when
    String result = states.invokeAction(mockState, "Instance ID", ps, instanceStatus);
    // then
    assertEquals("failed", result);
  }

  @Test(expected = StateMachineException.class)
  public void testInvokeActionThrowException() throws StateMachineException {
    // given
    states.setClass("java.lang.String");
    when(mockState.getAction()).thenReturn("toString");
    // when
    states.invokeAction(mockState, "Instance ID", ps, instanceStatus);
  }
}
