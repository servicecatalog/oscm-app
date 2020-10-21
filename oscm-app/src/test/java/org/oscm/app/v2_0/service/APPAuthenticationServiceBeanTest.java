/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 08.10.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.v2_0.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.oscm.app.dao.BesDAO;
import org.oscm.app.dao.ServiceInstanceDAO;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.v2_0.data.ControllerConfigurationKey;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.exceptions.AuthenticationException;
import org.oscm.types.enumtypes.UserRoleType;
import org.oscm.vo.VOUser;
import org.oscm.vo.VOUserDetails;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class APPAuthenticationServiceBeanTest {

  @Spy @InjectMocks APPAuthenticationServiceBean serviceBean = new APPAuthenticationServiceBean();
  @Mock protected APPConfigurationServiceBean configService;
  @Mock protected ServiceInstanceDAO instanceDAO;
  @Mock protected BesDAO besDAO;

  @Test
  public void authenticateAdministrator() throws Exception {
    // given
    PasswordAuthentication passwordAuthentication =
        new PasswordAuthentication("username", "password");
    VOUserDetails userDetails = new VOUserDetails();

    doReturn(userDetails)
        .when(serviceBean)
        .authenticateUser(
            any(ServiceInstance.class),
            anyString(),
            any(PasswordAuthentication.class),
            any(UserRoleType.class),
            any());

    // when
    serviceBean.authenticateAdministrator(passwordAuthentication);

    // then
    verify(serviceBean, times(1))
        .authenticateUser(
            null, null, passwordAuthentication, UserRoleType.ORGANIZATION_ADMIN, Optional.empty());
  }

  @Test
  public void authenticateTMForInstance() throws Exception {
    // given
    PasswordAuthentication passwordAuthentication =
        new PasswordAuthentication("username", "password");
    String controllerId = "controller_id";
    String instanceId = "instance_id";
    VOUserDetails userDetails = new VOUserDetails();
    ServiceInstance serviceInstance = new ServiceInstance();
    doReturn(userDetails)
        .when(serviceBean)
        .authenticateUser(
            any(ServiceInstance.class),
            anyString(),
            any(PasswordAuthentication.class),
            any(UserRoleType.class),
            any());
    when(instanceDAO.getInstanceById(anyString(), anyString())).thenReturn(serviceInstance);

    // when
    serviceBean.authenticateTMForInstance(controllerId, instanceId, passwordAuthentication);

    // then
    verify(serviceBean, times(1))
        .authenticateUser(
            serviceInstance,
            null,
            passwordAuthentication,
            UserRoleType.TECHNOLOGY_MANAGER,
            Optional.of(controllerId));
  }

  @Test
  public void authenticateTMForController() throws Exception {
    // given
    PasswordAuthentication authentication = new PasswordAuthentication("username", "password");
    String controllerId = "controller_id";
    doReturn(new VOUserDetails())
        .when(serviceBean)
        .getAuthenticatedTMForController(anyString(), any(PasswordAuthentication.class));

    // when
    serviceBean.authenticateTMForController(controllerId, authentication);

    // then
    verify(serviceBean, times(1)).getAuthenticatedTMForController(controllerId, authentication);
  }

  @Test
  public void getAuthenticatedTMForController() throws Exception {
    // given
    PasswordAuthentication passwordAuthentication =
        new PasswordAuthentication("username", "password");
    String controllerId = "controller_id";
    String orgId = "org_id";
    HashMap<String, Setting> settings = new HashMap<>();
    settings.put(
        ControllerConfigurationKey.BSS_ORGANIZATION_ID.name(),
        new Setting("BSS_ORGANIZATION_ID", orgId));
    when(configService.getControllerConfigurationSettings(anyString())).thenReturn(settings);

    VOUserDetails userDetails = new VOUserDetails();
    doReturn(userDetails)
        .when(serviceBean)
        .authenticateUser(
            any(ServiceInstance.class),
            anyString(),
            any(PasswordAuthentication.class),
            any(UserRoleType.class),
            any());

    // when
    serviceBean.getAuthenticatedTMForController(controllerId, passwordAuthentication);

    // then
    verify(serviceBean, times(1))
        .authenticateUser(
            null,
            orgId,
            passwordAuthentication,
            UserRoleType.TECHNOLOGY_MANAGER,
            Optional.of(controllerId));
  }

  @Test
  public void authenticateUser() throws Exception {
    // given
    PasswordAuthentication passwordAuthentication =
        new PasswordAuthentication("username", "password");
    String controllerId = "controller_id";

    ServiceInstance serviceInstance = new ServiceInstance();
    String orgId = "org_id";
    VOUserDetails userDetails = new VOUserDetails();
    userDetails.setOrganizationId(orgId);
    userDetails.setUserRoles(
        new HashSet<>(Collections.singletonList(UserRoleType.PLATFORM_OPERATOR)));
    when(besDAO.getUserDetails(any(ServiceInstance.class), any(VOUser.class), anyString(), any()))
        .thenReturn(userDetails);

    // when
    VOUserDetails authenticatedUser =
        serviceBean.authenticateUser(
            serviceInstance,
            orgId,
            passwordAuthentication,
            UserRoleType.PLATFORM_OPERATOR,
            Optional.of(controllerId));

    // then
    assertEquals(userDetails, authenticatedUser);
    verify(besDAO, times(1))
        .getUserDetails(any(ServiceInstance.class), any(VOUser.class), anyString(), any());
  }

  @Test
  public void authenticateUser_whenOrganizationIdIsNull() throws Exception {
    // given
    PasswordAuthentication passwordAuthentication = new PasswordAuthentication("10000", "password");
    String controllerId = "controller_id";

    ServiceInstance serviceInstance = new ServiceInstance();
    String orgId = "org_id";
    VOUserDetails userDetails = new VOUserDetails();
    userDetails.setOrganizationId(orgId);
    userDetails.setUserId("10000");
    userDetails.setUserRoles(
        new HashSet<>(Collections.singletonList(UserRoleType.PLATFORM_OPERATOR)));
    when(besDAO.getUserDetails(any(ServiceInstance.class), any(VOUser.class), anyString(), any()))
        .thenReturn(userDetails);

    // when
    VOUserDetails authenticatedUser =
        serviceBean.authenticateUser(
            serviceInstance,
            null,
            passwordAuthentication,
            UserRoleType.PLATFORM_OPERATOR,
            Optional.of(controllerId));

    // then
    assertEquals(userDetails, authenticatedUser);
    verify(besDAO, times(2))
        .getUserDetails(any(ServiceInstance.class), any(VOUser.class), anyString(), any());
  }

  @Test(expected = AuthenticationException.class)
  public void authenticateUser_whenOrganizationIdIsDifferent() throws Exception {
    // given
    PasswordAuthentication passwordAuthentication = new PasswordAuthentication("10000", "password");
    String controllerId = "controller_id";

    ServiceInstance serviceInstance = new ServiceInstance();
    String orgId = "org_id";
    VOUserDetails userDetails = new VOUserDetails();
    userDetails.setOrganizationId("orgId22");
    userDetails.setUserId("10000");
    when(besDAO.getUserDetails(any(ServiceInstance.class), any(VOUser.class), anyString(), any()))
        .thenReturn(userDetails);

    // when
    serviceBean.authenticateUser(
        serviceInstance,
        orgId,
        passwordAuthentication,
        UserRoleType.PLATFORM_OPERATOR,
        Optional.of(controllerId));
  }

  @Test(expected = AuthenticationException.class)
  public void authenticateUser_whenRolesCheckFails() throws Exception {
    // given
    PasswordAuthentication passwordAuthentication = new PasswordAuthentication("10000", "password");
    String controllerId = "controller_id";

    ServiceInstance serviceInstance = new ServiceInstance();
    String orgId = "org_id";
    VOUserDetails userDetails = new VOUserDetails();
    userDetails.setOrganizationId(orgId);
    userDetails.setUserId("10000");
    userDetails.setUserRoles(
        new HashSet<>(Collections.singletonList(UserRoleType.RESELLER_MANAGER)));

    when(besDAO.getUserDetails(any(ServiceInstance.class), any(VOUser.class), anyString(), any()))
        .thenReturn(userDetails);

    // when
    serviceBean.authenticateUser(
        serviceInstance,
        orgId,
        passwordAuthentication,
        UserRoleType.PLATFORM_OPERATOR,
        Optional.of(controllerId));
  }
}
