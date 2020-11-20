/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 20.11.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.sample.controller;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.data.InstanceDescription;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({SampleController.class})
public class SampleControllerTest {

  @InjectMocks
  private SampleController sampleController;
  private PropertyHandler propertyHandler;
  private InstanceDescription instanceDescription;
  private InstanceStatus instanceStatus;
  private Properties properties;

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

    parameters.put("PARAM_MESSAGETEXT", new Setting("message", "Test message"));
    parameters.put("PARAM_EMAIL", new Setting("email", "test@fujitsu.com"));
    parameters.put("PARAM_USER", new Setting("user", "administrator"));
    parameters.put("PARAM_PWD", new Setting("password", "password123"));
    parameters.put("CSSSTYLE", new Setting("name2", "value2"));
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
    sampleController = PowerMockito.spy(new SampleController());
    properties = mock(Properties.class);

    instanceDescription = new InstanceDescription();
    instanceStatus = new InstanceStatus();
    propertyHandler = new PropertyHandler(ps);
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreateInstance() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(InstanceDescription.class).withNoArguments().thenReturn(instanceDescription);

    InstanceDescription result = sampleController.createInstance(ps);

    assertEquals(Status.CREATION_REQUESTED, propertyHandler.getState());
    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
    assertEquals(instanceDescription, result);
  }

  @Test
  public void testDeleteInstance() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(InstanceStatus.class).withNoArguments().thenReturn(instanceStatus);

    InstanceStatus result = sampleController.deleteInstance("InstanceId", ps);

    assertEquals(Status.DELETION_REQUESTED, propertyHandler.getState());
    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
    assertEquals(instanceStatus, result);
  }

  @Test
  public void testModifyInstance() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(InstanceStatus.class).withNoArguments().thenReturn(instanceStatus);

    InstanceStatus result = sampleController.modifyInstance("InstanceId", ps, ps);

    assertEquals(Status.MODIFICATION_REQUESTED, propertyHandler.getState());
    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
    assertEquals(instanceStatus, result);
  }

  @Test
  public void testGetInstanceStatus() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    propertyHandler.setState(Status.FINISHED);

    InstanceStatus result = sampleController.getInstanceStatus("InstanceId", ps);

    assertEquals(true, result.isReady());
    assertEquals(parameters, result.getChangedParameters());
  }

  @Test
  public void testNotifyInstance() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(InstanceStatus.class).withNoArguments().thenReturn(instanceStatus);

    propertyHandler.setState(Status.WAITING_FOR_ACTIVATION);

    InstanceStatus result = sampleController.notifyInstance("InstanceId", ps, properties);

    assertEquals(Status.FINSIHING_MANUAL_PROVISIONING, propertyHandler.getState());
    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
  }

  @Test(expected = APPlatformException.class)
  public void testNotifyInstanceThrowException() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(InstanceStatus.class).withNoArguments().thenReturn(instanceStatus);

    sampleController.notifyInstance("InstanceId", ps, properties);
  }

  @Test
  public void testActivateInstance() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(InstanceStatus.class).withNoArguments().thenReturn(instanceStatus);

    InstanceStatus result = sampleController.activateInstance("InstanceId", ps);

    assertEquals(Status.ACTIVATION_REQUESTED, propertyHandler.getState());
    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
  }

  @Test
  public void testDeactivateInstance() throws Exception {
    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.whenNew(InstanceStatus.class).withNoArguments().thenReturn(instanceStatus);

    InstanceStatus result = sampleController.deactivateInstance("InstanceId", ps);

    assertEquals(Status.DEACTIVATION_REQUESTED, propertyHandler.getState());
    assertEquals(parameters, result.getChangedParameters());
    assertEquals(attributes, result.getChangedAttributes());
  }

  @Test(expected = APPlatformException.class)
  public void testValidateParameters() throws Exception {
    propertyHandler = mock(PropertyHandler.class);

    Whitebox.invokeMethod(sampleController, "validateParameters", propertyHandler);
  }
}
