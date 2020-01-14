/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2020                                           
 *                                                                                                                                 
 *  Creation Date: 9 Jan 2020                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.sample.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;

/**
 * @author worf
 *
 */
public class DispatcherTest {

    @Mock private APPlatformService platformService;
    private PropertyHandler paramHandler;
    private ProvisioningSettings ps;
    private Dispatcher dispatcher;
    
    
    @Before
    public void setUp() throws Exception {
        ps = new ProvisioningSettings(new HashMap<String, Setting>(), new HashMap<String, Setting>(), "en");
        paramHandler = new PropertyHandler(ps);
        MockitoAnnotations.initMocks(this);
        dispatcher = spy(new Dispatcher(platformService, "1", paramHandler));
        initMocks();
    }
    
  private void initMocks() throws Exception {
      doNothing().when(platformService).unlockServiceInstance(any(), any(), any());
      doReturn(true).when(platformService).lockServiceInstance(any(), any(), any());
    }
    
    @Test
    public void sampleProvisioning() throws APPlatformException{
        //given
        paramHandler.getSettings().getParameters().put("APP_BASE_URL", new Setting("APP_BASE_URL", ""));
        paramHandler.setState(Status.CREATION_STEP1);
        
        //when
        dispatcher.dispatch();

        //then
        assertEquals(Status.CREATION_STEP2, paramHandler.getState());
    }
    
    @Test
    public void manualProvisioning_CreationStep1() throws APPlatformException{
        //given
        setAppBaseUrl();
        paramHandler.setState(Status.CREATION_STEP1);
        
        //when
        dispatcher.dispatch();

        //then
        assertEquals(Status.MANUAL_CREATION, paramHandler.getState());
    }
    
    @Test
    public void manualProvisioning_ManualCreation() throws APPlatformException{
        //given
        setAppBaseUrl();
        paramHandler.setState(Status.MANUAL_CREATION);
        doNothing().when(dispatcher).sendMail(any(), any());
        //when
        dispatcher.dispatch();

        //then
        assertEquals(Status.WAITING_FOR_ACTIVATION, paramHandler.getState());
    }
    
    @Test
    public void manualProvisioning_WaitingForActivation() throws APPlatformException{
        //given
        setAppBaseUrl();
        paramHandler.setState(Status.WAITING_FOR_ACTIVATION);
        
        //when
        dispatcher.dispatch();

        //then
        assertEquals(Status.WAITING_FOR_ACTIVATION, paramHandler.getState());
    }
    
    
    @Test
    public void manualProvisioning_FinishingManualProvisiong() throws APPlatformException{
        //given
        setAppBaseUrl();
        paramHandler.setState(Status.FINSIHING_MANUAL_PROVISIONING);
        
        //when
        dispatcher.dispatch();

        //then
        assertEquals(Status.FINISHED, paramHandler.getState());
    }

    private void setAppBaseUrl() {
        paramHandler.getSettings().getParameters().put("APP_BASE_URL", new Setting("APP_BASE_URL", "https://fujitsu.com/global"));
    }
    

}
