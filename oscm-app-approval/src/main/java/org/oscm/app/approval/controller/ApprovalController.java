/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: Sep 8, 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.oscm.app.approval.data.State;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.ControllerSettings;
import org.oscm.app.v2_0.data.InstanceDescription;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.InstanceStatusUsers;
import org.oscm.app.v2_0.data.LocalizedText;
import org.oscm.app.v2_0.data.OperationParameter;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.ServiceUser;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformController;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.v2_0.intf.ControllerAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless(mappedName = "bss/app/controller/ess.approval")
@Remote(APPlatformController.class)
public class ApprovalController implements APPlatformController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApprovalController.class);
  public static final String ID = "ess.approval";
  private APPlatformService platformService;

  ApprovalControllerAccess controllerAccess;

  @PostConstruct
  public void initialize() {
    LOGGER.debug("ApprovalController @PostConstruct");
    try {
      platformService = APPlatformServiceFactory.getInstance();
    } catch (IllegalStateException e) {
      LOGGER.error(e.getMessage());
      throw e;
    }
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceDescription createInstance(ProvisioningSettings settings)
      throws APPlatformException {
    PropertyHandler paramHandler = new PropertyHandler(settings);
    paramHandler.setState(State.CREATION_REQUESTED);
    
   
    checkIfAlreadyExisting(settings.getOrganizationId());
    
    InstanceDescription id = new InstanceDescription();
    id.setInstanceId("Instance_" + System.currentTimeMillis());
    id.setChangedParameters(settings.getParameters());
    id.setChangedAttributes(settings.getAttributes());

    return id;
  }

  private void checkIfAlreadyExisting(String org) throws APPlatformException {
    Object data = new ApprovalInstanceAccess().getInstancesForOrganization(org);
    if (data != null)
      throw new APPlatformException(
          String.format(
              "An approval service is already subscribed for the organization ID %s.",
              org));
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatus modifyInstance(
      String instanceId, ProvisioningSettings currentSettings, ProvisioningSettings newSettings) {
    PropertyHandler paramHandler = new PropertyHandler(newSettings);

    paramHandler.setState(State.MODIFICATION_REQUESTED);

    InstanceStatus result = new InstanceStatus();
    result.setChangedParameters(newSettings.getParameters());
    result.setChangedAttributes(newSettings.getAttributes());
    return result;
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatus deleteInstance(String instanceId, ProvisioningSettings settings) {
    PropertyHandler paramHandler = new PropertyHandler(settings);
    paramHandler.setState(State.DELETION_REQUESTED);

    InstanceStatus result = new InstanceStatus();
    result.setChangedParameters(settings.getParameters());
    result.setChangedAttributes(settings.getAttributes());
    return result;
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatus getInstanceStatus(String instanceId, ProvisioningSettings settings)
      throws APPlatformException {
    PropertyHandler paramHandler = new PropertyHandler(settings);

    Dispatcher dp = new Dispatcher(platformService, instanceId, paramHandler);
    InstanceStatus status = dp.dispatch();
    return status;
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatus notifyInstance(
      String s, ProvisioningSettings provisioningSettings, Properties properties)
      throws APPlatformException {
    return null;
  }

  @Override
  public InstanceStatus activateInstance(String s, ProvisioningSettings provisioningSettings)
      throws APPlatformException {
    return null;
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatus deactivateInstance(String s, ProvisioningSettings provisioningSettings)
      throws APPlatformException {
    return null;
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatusUsers createUsers(
      String s, ProvisioningSettings provisioningSettings, List<ServiceUser> list)
      throws APPlatformException {
    return null;
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatus deleteUsers(
      String s, ProvisioningSettings provisioningSettings, List<ServiceUser> list)
      throws APPlatformException {
    return null;
  }

  @Override
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public InstanceStatus updateUsers(
      String s, ProvisioningSettings provisioningSettings, List<ServiceUser> list)
      throws APPlatformException {
    return null;
  }

  @Override
  public List<LocalizedText> getControllerStatus(ControllerSettings controllerSettings)
      throws APPlatformException {
    return null;
  }

  @Override
  public List<OperationParameter> getOperationParameters(
      String s, String s1, String s2, ProvisioningSettings provisioningSettings)
      throws APPlatformException {
    return null;
  }

  @Override
  public InstanceStatus executeServiceOperation(
      String s,
      String s1,
      String s2,
      String s3,
      List<OperationParameter> list,
      ProvisioningSettings provisioningSettings)
      throws APPlatformException {
    return null;
  }

  @Override
  public void setControllerSettings(ControllerSettings settings) {
    if (controllerAccess != null) {
      controllerAccess.storeSettings(settings);
    }
  }

  @Inject
  public void setControllerAccess(final ControllerAccess access) {
    this.controllerAccess = (ApprovalControllerAccess) access;
  }
}
