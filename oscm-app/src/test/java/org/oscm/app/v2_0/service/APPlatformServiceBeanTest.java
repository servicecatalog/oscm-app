/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2019
 *                                                                                                                                 
 *  Creation Date: 09.04.2019                                                     
 *                                                                              
 *******************************************************************************/

package org.oscm.app.v2_0.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.oscm.app.domain.PlatformConfigurationKey;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class APPlatformServiceBeanTest {

  @Spy @InjectMocks private APPlatformServiceBean applatformService = new APPlatformServiceBean();

  @Mock private APPConfigurationServiceBean appConfigurationService;

  @SuppressWarnings("unchecked")
  @Test
  public void updateUserCredentials_isExecutedSuccessfully_ifAllControllersConfigured()
      throws Exception {

    // given
    List<String> controllers =
        Arrays.asList("PROXY", "ess.aws", "ess.openstack", "ess.azure", "ess.vmware");
    when(appConfigurationService.getUserConfiguredControllers(anyString())).thenReturn(controllers);
    doReturn(Optional.of("test")).when(applatformService).decryptPassword(anyString());
    
    // when
    applatformService.updateUserCredentials(1000, "test", "dsads1232ewqewe:321ed==esdsdczsfnb3");

    // then
    verify(appConfigurationService, times(1)).storeAppConfigurationSettings(any(HashMap.class));
    verify(appConfigurationService, times(4))
        .storeControllerConfigurationSettings(anyString(), any(HashMap.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void updateUserCredentials_hasNoInteractions_ifNoControllerIsConfigured()
      throws Exception {

    // given
    List<String> controllers = new ArrayList<>();
    when(appConfigurationService.getUserConfiguredControllers(anyString())).thenReturn(controllers);

    // when
    applatformService.updateUserCredentials(1000, "test", "test");

    // then
    verify(appConfigurationService, never()).storeAppConfigurationSettings(any(HashMap.class));
    verify(appConfigurationService, never())
        .storeControllerConfigurationSettings(anyString(), any(HashMap.class));
  }

  @Test
  public void isSsoMode_returnTrue_ifAuthModeIsOIDC() throws Exception{

    //given
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.BSS_AUTH_MODE)).thenReturn("OIDC");

    //when
    boolean ssoMode = applatformService.isSsoMode();

    //then
    assertTrue(ssoMode);
  }

  @Test
  public void isSsoMode_returnFalse_ifAuthModeIsInternal() throws Exception{

    //given
    when(appConfigurationService.getProxyConfigurationSetting(PlatformConfigurationKey.BSS_AUTH_MODE)).thenReturn("INTERNAL");

    //when
    boolean ssoMode = applatformService.isSsoMode();

    //then
    assertFalse(ssoMode);
  }
}
