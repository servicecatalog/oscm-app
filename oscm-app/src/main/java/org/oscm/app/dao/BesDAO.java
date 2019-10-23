/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2018
 *
 *  Creation Date: 2014-02-25
 *
 *******************************************************************************/
package org.oscm.app.dao;

import org.oscm.app.business.exceptions.BESNotificationException;
import org.oscm.app.domain.Operation;
import org.oscm.app.domain.PlatformConfigurationKey;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.i18n.Messages;
import org.oscm.app.v2_0.data.LocalizedText;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.exceptions.AuthenticationException;
import org.oscm.app.v2_0.exceptions.ConfigurationException;
import org.oscm.app.v2_0.service.APPConfigurationServiceBean;
import org.oscm.intf.IdentityService;
import org.oscm.intf.SubscriptionService;
import org.oscm.provisioning.data.InstanceInfo;
import org.oscm.provisioning.data.InstanceResult;
import org.oscm.security.SOAPSecurityHandler;
import org.oscm.string.Strings;
import org.oscm.types.enumtypes.OperationStatus;
import org.oscm.types.enumtypes.UserRoleType;
import org.oscm.types.exceptions.ObjectNotFoundException;
import org.oscm.types.exceptions.SaaSApplicationException;
import org.oscm.types.exceptions.SubscriptionStateException;
import org.oscm.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.Handler;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
public class BesDAO {

  private static final Logger LOGGER = LoggerFactory.getLogger(BesDAO.class);

  @EJB protected APPConfigurationServiceBean configService;

  /**
   * Reads the WSDL for any OSCM web service and returns an interface to the service implementation.
   * When a service instance is given, the respective BES credentials will be set. The APP specific
   * credentials will be used otherwise.
   *
   * @param serviceClass the class of the requested service interface
   * @param serviceInstance the service instance to retrieve the client for (optional)
   * @param controllerId
   * @return a service interface to the requested OSCM service
   */
  public <T> T getBESWebService(
      Class<T> serviceClass, ServiceInstance serviceInstance, Optional<String> controllerId)
      throws APPlatformException {

    try {
      Map<String, Setting> proxySettings = configService.getAllProxyConfigurationSettings();
      T client = getServicePort(serviceClass, proxySettings);

      PasswordAuthentication pwAuth =
          configService.getWebServiceAuthentication(serviceInstance, proxySettings, controllerId);

      String userName = pwAuth.getUserName();
      String password = pwAuth.getPassword();

      if (isSsoMode(proxySettings)) {
        password = addPasswordPrefix(password);
      }

      setBinding((BindingProvider) client, userName, password);
      return client;
    } catch (MalformedURLException e) {
        throw new ConfigurationException(
            e.getMessage(), PlatformConfigurationKey.BSS_WEBSERVICE_URL.name());
    } catch (APPlatformException e) {
      throw e;
    } catch (Exception e) {
      APPlatformException pe = new APPlatformException(e.getMessage(), e);
      LOGGER.warn("Retrieving the OSCM service client failed.", pe);
      throw pe;
    }
  }

  public void setBinding(BindingProvider client, String userName, String password) {
    final Binding binding = client.getBinding();
    List<Handler> handlerList = binding.getHandlerChain();

    if (handlerList == null) handlerList = new ArrayList<>();

    List<Handler> handlers =
        handlerList.stream()
            .filter(handler -> !(handler instanceof SOAPSecurityHandler))
            .collect(Collectors.toList());

    handlers.add(new SOAPSecurityHandler(userName, password));
    binding.setHandlerChain(handlers);
  }

  public Service createWebService(URL wsdlUrl, QName serviceQName) {
    return Service.create(wsdlUrl, serviceQName);
  }

  /** Get all technology managers with specified email address for the service instance. */
  public List<VOUserDetails> getBESTechnologyManagers(ServiceInstance si) {
    List<VOUserDetails> mailUsers = new ArrayList<>();
    try {
      // Get all technology managers of TP organization
      IdentityService is = getBESWebService(IdentityService.class, si, Optional.empty());
      List<VOUserDetails> orgUsers = is.getUsersForOrganization();
      for (VOUserDetails user : orgUsers) {
        if (user.getUserRoles().contains(UserRoleType.TECHNOLOGY_MANAGER)
            && !Strings.isEmpty(user.getEMail())) {
          mailUsers.add(user);
        }
      }
    } catch (Exception ex) {
      LOGGER.warn(
          "Technology managers mail addresses cannot be retrieved from CT_MG. [Cause: "
              + ex.getMessage()
              + "]",
          ex);
    }
    if (mailUsers.isEmpty()) {
      LOGGER.warn("No technology managers mails set.");
    }
    return mailUsers;
  }

  <T> T getServicePort(Class<T> serviceClass, Map<String, Setting> settings)
      throws MalformedURLException {

    String targetNamespace = serviceClass.getAnnotation(WebService.class).targetNamespace();
    QName serviceQName = new QName(targetNamespace, serviceClass.getSimpleName());

    Service service = createWebService(getWsdlUrl(serviceClass, settings), serviceQName);

    return service.getPort(serviceClass);
  }

  <T> URL getWsdlUrl(Class<T> serviceClass, Map<String, Setting> settings)
      throws MalformedURLException {
    String wsdlUrl =
        settings.get(PlatformConfigurationKey.BSS_WEBSERVICE_WSDL_URL.name()).getValue();
    wsdlUrl = wsdlUrl.replace("{SERVICE}", serviceClass.getSimpleName());

    return new URL(wsdlUrl);
  }

  boolean isSsoMode(Map<String, Setting> settings) {
    return "OIDC".equals(settings.get(PlatformConfigurationKey.BSS_AUTH_MODE.name()).getValue());
  }

  public void terminateSubscription(ServiceInstance currentSI, String locale)
      throws BESNotificationException {

    VOSubscription vo;
    try {
      SubscriptionService subServ =
          getBESWebService(SubscriptionService.class, currentSI, Optional.empty());

      vo =
          subServ.getSubscriptionForCustomer(
              currentSI.getOrganizationId(), currentSI.getOriginalSubscriptionId());
      String reason = Messages.get(locale, "terminate_subscription_reason");
      subServ.terminateSubscription(vo, reason);
    } catch (Exception e) {
      throw new BESNotificationException("The subscription cannot be terminated.", e);
    }
  }

  public void notifyAsyncSubscription(
      ServiceInstance currentSI,
      InstanceResult instanceResult,
      boolean isCompleted,
      APPlatformException cause)
      throws BESNotificationException {

    if (currentSI.isDeleted()) {
      return;
    }

    try {
      SubscriptionService subServ =
          getBESWebService(SubscriptionService.class, currentSI, Optional.empty());

      if (isCompleted) {

        VOInstanceInfo voInstanceInfo = getInstanceInfo(currentSI, instanceResult);
        subServ.completeAsyncSubscription(
            currentSI.getSubscriptionId(), currentSI.getOrganizationId(), voInstanceInfo);
      } else {
        subServ.abortAsyncSubscription(
            currentSI.getSubscriptionId(),
            currentSI.getOrganizationId(),
            cause == null ? null : toBES(cause.getLocalizedMessages()));
      }

    } catch (SubscriptionStateException se) {
      handleSubscriptionStateException(currentSI, instanceResult, isCompleted, se);
    } catch (ObjectNotFoundException onfe) {
      handleObjectNotFoundException(currentSI, instanceResult);
    } catch (Exception e) {
      handleException(currentSI, e);
    }
  }

  public void notifyAsyncModifySubscription(
      ServiceInstance currentSI,
      InstanceResult instanceResult,
      boolean isCompleted,
      APPlatformException cause)
      throws BESNotificationException {

    if (currentSI.isDeleted()) {
      return;
    }

    VOInstanceInfo voInstanceInfo = getInstanceInfo(currentSI, instanceResult);

    try {
      SubscriptionService subServ =
          getBESWebService(SubscriptionService.class, currentSI, Optional.empty());
      if (isCompleted) {
        subServ.completeAsyncModifySubscription(
            currentSI.getSubscriptionId(), currentSI.getOrganizationId(), voInstanceInfo);
      } else {
        subServ.abortAsyncModifySubscription(
            currentSI.getSubscriptionId(),
            currentSI.getOrganizationId(),
            cause == null ? null : toBES(cause.getLocalizedMessages()));
      }

    } catch (SubscriptionStateException se) {
      // If the subscription is now in a wrong state we can skip the
      // complete request and proceed as usual...
      handleSubscriptionStateException(currentSI, instanceResult, isCompleted, se);
    } catch (ObjectNotFoundException onfe) {
      // If the subscription is not recognized in BES, we cannot notify
      handleObjectNotFoundException(currentSI, instanceResult);
    } catch (Exception e) {
      handleException(currentSI, e);
    }
  }

  public void notifySubscriptionAboutVmsNumber(ServiceInstance currentSI)
      throws BESNotificationException {
    if (currentSI.isDeleted()) {
      return;
    }
    VOInstanceInfo voInstanceInfo = new VOInstanceInfo();
    voInstanceInfo.setVmsNumber(currentSI.getVmsNumber());
    try {
      SubscriptionService subServ =
          getBESWebService(SubscriptionService.class, currentSI, Optional.empty());
      subServ.notifySubscriptionAboutVmsNumber(
          currentSI.getSubscriptionId(), currentSI.getOrganizationId(), voInstanceInfo);
    } catch (Exception e) {
      handleException(currentSI, e);
    }
  }

  public void notifyAsyncOperationStatus(
      ServiceInstance currentSI,
      String transactionId,
      OperationStatus status,
      List<LocalizedText> list)
      throws BESNotificationException {

    if (currentSI.isDeleted()) {
      return;
    }

    SubscriptionService subServ;
    try {
      subServ = getBESWebService(SubscriptionService.class, currentSI, Optional.empty());
      subServ.updateAsyncOperationProgress(transactionId, status, toBES(list));
      if (currentSI.getServiceAccessInfo() != null) {
        VOInstanceInfo vo = new VOInstanceInfo();
        vo.setInstanceId(currentSI.getInstanceId());
        vo.setAccessInfo(currentSI.getServiceAccessInfo());
        subServ.updateAccessInformation(
            currentSI.getSubscriptionId(), currentSI.getOrganizationId(), vo);
      }
    } catch (Exception e) {
      handleException(currentSI, e);
    }
  }

  public void notifyInstanceStatusOfAsyncOperation(ServiceInstance currentSI)
      throws BESNotificationException {

    if (currentSI.isDeleted()) {
      return;
    }

    SubscriptionService subServ;
    try {
      subServ = getBESWebService(SubscriptionService.class, currentSI, Optional.empty());
      VOInstanceInfo vo = new VOInstanceInfo();
      vo.setInstanceId(currentSI.getInstanceId());
      vo.setAccessInfo(currentSI.getServiceAccessInfo());
      subServ.updateAsyncSubscriptionStatus(
          currentSI.getSubscriptionId(), currentSI.getOrganizationId(), vo);

    } catch (Exception e) {
      handleException(currentSI, e);
    }
  }

  String getTransactionIdByServiceInstance(ServiceInstance currentSI) {
    String transactionId = "";
    for (Operation operation : currentSI.getOperations()) {
      if (!operation.isForQueue()) {
        transactionId = operation.getTransactionId();
      }
    }
    return transactionId;
  }

  void handleException(ServiceInstance currentSI, Exception e) throws BESNotificationException {
    String statusText = "unknown";
    if (currentSI.getProvisioningStatus() != null) {
      statusText = currentSI.getProvisioningStatus().name();
    }
    // Forward mapped BES notification exception
    BESNotificationException bne =
        new BESNotificationException(
            "Could not notify OSCM on processing result. Current status is " + statusText, e);
    LOGGER.error(bne.getMessage(), e);
    throw bne;
  }

  void handleObjectNotFoundException(ServiceInstance currentSI, InstanceResult instanceResult) {
    LOGGER.info(
        "The processing of service instance '{}' failed with return code '{}' and description '{}', but OSCM doesn't recognize the subscription",
            currentSI.getInstanceId(), Long.valueOf(instanceResult.getRc()), instanceResult.getDesc());
  }

  void handleSubscriptionStateException(
      ServiceInstance currentSI,
      InstanceResult instanceResult,
      boolean isCompleted,
      SubscriptionStateException se) {
    if (isCompleted) {
      LOGGER.info(
          "The processing of service instance '{}' was completed, but OSCM couldn't be informed because of the wrong subscription state '{}'",
          new Object[] {currentSI.getInstanceId(), se.getFaultInfo().getReason().toString()});
    } else {
      LOGGER.info(
          "The processing of service instance '{}' failed with return code '{}' and description '{}'. OSCM couldn't be informed because of the wrong subscription state '{}'",
              currentSI.getInstanceId(),
              Long.valueOf(instanceResult.getRc()),
              instanceResult.getDesc(),
              se.getFaultInfo().getReason().toString());
    }
  }

  public void notifyAsyncUpgradeSubscription(
      ServiceInstance currentSI,
      InstanceResult instanceResult,
      boolean isCompleted,
      APPlatformException cause)
      throws BESNotificationException {

    if (currentSI.isDeleted()) {
      return;
    }

    VOInstanceInfo voInstanceInfo = getInstanceInfo(currentSI, instanceResult);

    try {
      SubscriptionService subServ =
          getBESWebService(SubscriptionService.class, currentSI, Optional.empty());
      if (isCompleted) {
        subServ.completeAsyncUpgradeSubscription(
            currentSI.getSubscriptionId(), currentSI.getOrganizationId(), voInstanceInfo);
      } else {
        subServ.abortAsyncUpgradeSubscription(
            currentSI.getSubscriptionId(),
            currentSI.getOrganizationId(),
            cause == null ? null : toBES(cause.getLocalizedMessages()));
      }
    } catch (SubscriptionStateException se) {
      handleSubscriptionStateException(currentSI, instanceResult, isCompleted, se);
    } catch (ObjectNotFoundException onfe) {
      handleObjectNotFoundException(currentSI, instanceResult);
    } catch (Exception e) {
      handleException(currentSI, e);
    }
  }

  VOInstanceInfo getInstanceInfo(ServiceInstance currentSI, InstanceResult instanceResult) {
    InstanceInfo instanceInfo = instanceResult.getInstance();
    VOInstanceInfo voInstanceInfo = new VOInstanceInfo();
    voInstanceInfo.setInstanceId(currentSI.getInstanceId());
    voInstanceInfo.setAccessInfo(instanceInfo.getAccessInfo());
    voInstanceInfo.setBaseUrl(instanceInfo.getBaseUrl());
    voInstanceInfo.setLoginPath(instanceInfo.getLoginPath());
    voInstanceInfo.setVmsNumber(currentSI.getVmsNumber());
    return voInstanceInfo;
  }

  public void notifyOnProvisioningStatusUpdate(ServiceInstance currentSI, List<LocalizedText> list)
      throws BESNotificationException {
    if (list == null || list.isEmpty()) {
      return;
    }

    if (currentSI.isDeleted()) {
      return;
    }

    try {
      SubscriptionService subServ =
          getBESWebService(SubscriptionService.class, currentSI, Optional.empty());
      subServ.updateAsyncSubscriptionProgress(
          currentSI.getSubscriptionId(), currentSI.getOrganizationId(), toBES(list));

      LOGGER.info(
          "Updated status for service instance '{}' with message '{}'",
          currentSI.getInstanceId(),
          getEnglishOrFirst(list));

    } catch (SubscriptionStateException se) {
      // If the subscription is now in a wrong state we can skip the
      // complete request and proceed as usual (e.g. already completed)
      LOGGER.info(
          "Updated status for service instance '{}' with message '{}', but OSCM couldn't be informed because of the wrong subscription state '{}'",
              currentSI.getInstanceId(),
              getEnglishOrFirst(list),
              se.getFaultInfo().getReason().toString());

    } catch (Exception e) {
      // Forward mapped BES notification exception
      BESNotificationException bne =
          new BESNotificationException(
              "Could not notify OSCM on new service provisioning status", e);
      LOGGER.error(bne.getMessage(), bne);
      throw bne;
    }
  }

  static String getEnglishOrFirst(List<LocalizedText> list) {
    String result = null;
    if (list != null) {
      for (LocalizedText text : list) {
        if ("en".equals(text.getLocale())) {
          return text.getText();
        } else if (result == null) {
          result = text.getText();
        }
      }
    }
    return result;
  }

  /**
   * Gets the user details from BES. If user is null, the current user details will be returned,
   * otherwise of the specified user.
   *
   * @param si
   * @param user
   * @param password
   * @param controllerId
   * @return
   * @throws APPlatformException
   * @throws BESNotificationException
   */
  public VOUserDetails getUserDetails(
      ServiceInstance si, VOUser user, String password, Optional<String> controllerId)
      throws APPlatformException {

    VOUserDetails userDetails = null;
    IdentityService idServ = getBESWebService(IdentityService.class, si, controllerId);

    try {
      userDetails = idServ.getCurrentUserDetails();
    } catch (Exception e) {
      throw new APPlatformException(e.getMessage(), e);
    }

    return userDetails;
  }

  public VOUser getUser(ServiceInstance si, VOUser user, Optional<String> controllerId)
      throws APPlatformException {
    VOUser retrunUser = null;
    IdentityService idServ = getBESWebService(IdentityService.class, si, controllerId);
    try {
      retrunUser = idServ.getUser(user);
    } catch (SaaSApplicationException e) {
      AuthenticationException ae = new AuthenticationException(e.getMessage(), e);
      String userId = (user.getUserId() != null) ? user.getUserId() : Long.toString(user.getKey());
      LOGGER.debug("User {} could not be authenticated => call to retrieve user failed", userId);
      throw ae;
    } catch (Exception e) {
      throw new APPlatformException(e.getMessage(), e);
    }
    return retrunUser;
  }

  public List<VOLocalizedText> toBES(List<LocalizedText> texts) {
    List<VOLocalizedText> result = new ArrayList<>();
    if (texts != null) {
      for (LocalizedText text : texts) {
        result.add(new VOLocalizedText(text.getLocale(), text.getText()));
      }
    }
    return result;
  }

  public boolean isBESAvalible() {
    try {
      IdentityService is = getBESWebService(IdentityService.class, null, Optional.empty());
      is.getCurrentUserDetails();
    } catch (APPlatformException e) {
      return !isCausedByConnectionException(e);
    }
    return true;
  }

  public boolean isCausedByConnectionException(Throwable th) {
    Throwable connectException = null;
    if (th != null) {
      connectException = th.getCause();
      while (connectException != null) {
        if (connectException instanceof ConnectException) {
          return true;
        }
        connectException = findCause(connectException);
      }
    }
    return false;
  }

  Throwable findCause(Throwable e) {
    Throwable cause = null;
    if (e instanceof EJBException && e.getCause() instanceof Exception) {
      cause = ((EJBException) e).getCausedByException();
    }
    if (cause == null) {
      cause = e.getCause();
    }
    return cause;
  }

  String addPasswordPrefix(String password) {
    return "WS" + password;
  }
}
