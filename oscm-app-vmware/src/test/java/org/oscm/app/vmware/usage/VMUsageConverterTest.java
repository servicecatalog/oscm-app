package org.oscm.app.vmware.usage;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.intf.EventService;
import org.oscm.vo.VOGatheredEvent;

public class VMUsageConverterTest {
    
    private VMUsageConverter converter;
    private VMPropertyHandler ph;
    
    @Before
    public void before() {
        converter = spy(new VMUsageConverter());
        converter.ph = ph = mock(VMPropertyHandler.class);

    }

    /**
     * No event should be generated if the multiplier is less than 0.
     */
    @Test
    public void submit_invalidMultiplier1() throws Exception {
        // given
        long multiplier = 0;
        String eventId = "id";
        String occurence = "2011-12-03T10:15:30";
        EventService svc = mock(EventService.class);
        doReturn(svc).when(ph).getWebService(EventService.class);
        
        // when
        converter.submit(eventId, multiplier, occurence);

        // then
        verifyZeroInteractions(svc); 
    }

    /**
     * No event should be generated if the multiplier is less than 0.
     */
    @Test
    public void submit_invalidMultiplier2() throws Exception {
        // given
        long multiplier = -1;
        String eventId = "id";
        String occurence = "2011-12-03T10:15:30";
        EventService svc = mock(EventService.class);
        doReturn(svc).when(ph).getWebService(EventService.class);
        
        // when
        converter.submit(eventId, multiplier, occurence);

        // then
        verifyZeroInteractions(svc);
    }

    @Test
    public void submit() throws Exception {
        // given
        long multiplier = 1;
        String eventId = "eventId";
        String occurence = "1970-01-01T00:00:01";

        EventService svc = mock(EventService.class);
        PasswordAuthentication pwAuth = mock(PasswordAuthentication.class);
        when(ph.getTPAuthentication()).thenReturn(pwAuth);
        when(pwAuth.getUserName()).thenReturn("user");
        doReturn(svc).when(ph).getWebService(EventService.class);
        
        ArgumentCaptor<VOGatheredEvent> event = forClass(VOGatheredEvent.class);

        // when
        converter.submit(eventId, multiplier, occurence);

        // then
        verify(svc).recordEventForInstance(anyString(), anyString(),
                event.capture());
        assertEquals(1L, event.getValue().getMultiplier());
        assertEquals(1000L, event.getValue().getOccurrenceTime());
        assertEquals("eventId_1970-01-01T00:00:01",
                event.getValue().getUniqueId());
    }

}
