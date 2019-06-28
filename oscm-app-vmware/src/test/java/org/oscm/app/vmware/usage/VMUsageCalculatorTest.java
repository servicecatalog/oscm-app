package org.oscm.app.vmware.usage;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;

import com.vmware.vim25.RuntimeFaultFaultMsg;

public class VMUsageCalculatorTest {
    
    
    private VMUsageCalculator calculator;
    private VMPropertyHandler ph;
    VMMetricCollector collector;
    
    @Before
    public void before() {
        collector = mock(VMMetricCollector.class);
        calculator = spy(new VMUsageCalculator());
        calculator.ph = ph = mock(VMPropertyHandler.class);
        calculator.collector = collector = mock(VMMetricCollector.class);


    }

    @Test
    public void testCalculateMemUsage() throws APPlatformException, RuntimeFaultFaultMsg {
        //given
        
        when(collector.getMemUsagePercent()).thenReturn(20L); //number is given with 2 decimal places without separating point
        when(ph.getConfigMemoryMB()).thenReturn(1024L);

        //when
        long result = calculator.calculateMemUsageMB();
        long expected = (long) 204;
        
        //then
        assertEquals(expected, result);
    }
    
    @Test
    public void calculateDiskUsageGB() throws APPlatformException, RuntimeFaultFaultMsg {
        //given
        
        when(collector.getDiskUsageTotalKB()).thenReturn(9549836L); //number is given with 2 decimal places without separating point
        when(ph.getConfigMemoryMB()).thenReturn(1024L);

        //when
        long result = calculator.calculateDiskUsageGB();
        long expected = (long) 9;
        
        //then
        assertEquals(expected, result);
    }

}
