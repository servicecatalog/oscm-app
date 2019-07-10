/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2018
 *
 * <p>Creation Date: 2019-07-09
 *
 * <p>*****************************************************************************
 */


package org.oscm.app.vmware.usage;

import static java.time.LocalDateTime.parse;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.format.DateTimeParseException;

import org.junit.Before;
import org.junit.Test;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;

import com.vmware.vim25.RuntimeFaultFaultMsg;
/**
 * 
 * @author worf
 */
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
    public void testCalculateDiskUsageGB() throws APPlatformException, RuntimeFaultFaultMsg {
        //given
        
        when(collector.getDiskUsageTotalKB()).thenReturn(9549836L); //number is given with 2 decimal places without separating point
        when(ph.getConfigMemoryMB()).thenReturn(1024L);

        //when
        long result = calculator.calculateDiskUsageGB();
        long expected = (long) 9;
        
        //then
        assertEquals(expected, result);
    }

    @Test
    public void testCalculateTimeframeCorrect() {
        //given
        String startTime = "2019-07-05T12:43:45.088";
        String endTime = "2019-07-08T10:45:58.77";
        long expected = 70;
        
        //when
        long result = calculator.calculateTimeframe(startTime, endTime);
        
        //then 
        assertEquals(expected, result);
    }
    
    @Test
    public void testCalculateTimeframeIncorrectFormat() {
        //given
        String startTime = "2019-07-05Z12:43:45.088";
        String endTime = "2019-07-08T´10:45:58.77";
        long expected = 24;
        
        //when
        long result = calculator.calculateTimeframe(startTime, endTime);
        
        //then 
        assertEquals(expected, result);
    }
    
    @Test
    public void testCalculateShortTimeframe() {
        //given
        String startTime = "2019-07-05T12:43:45.088";
        String endTime = "2019-07-05T12:45:58.77";
        long expected = 1;
        
        //when
        long result = calculator.calculateTimeframe(startTime, endTime);
        
        //then 
        assertEquals(expected, result);
    }


}
