package org.oscm.app.vmware.usage;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.remote.vmware.VMwareClient;

import com.vmware.vim25.ElementDescription;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfEntityMetricCSV;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricSeriesCSV;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.PerfSummaryType;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VimPortType;

public class VMMetricCollectorTest {
    
    private VMMetricCollector collector;
    private VMPropertyHandler ph;
    private ManagedObjectReference vmInstance;
    private ManagedObjectReference performanceManager;
    private VMwareClient vmw;
    
    @Before
    public void before() {
        collector = spy(new VMMetricCollector(ph));
        collector.ph = ph = mock(VMPropertyHandler.class);
        collector.vmInstance = vmInstance = mock(ManagedObjectReference.class);
        collector.performanceManager = performanceManager = mock(ManagedObjectReference.class);
        collector.vmw = vmw = mock(VMwareClient.class);
    }

    @Test
    public void testCreateCounterToNameMapping() {
        //given
        ArrayList<PerfCounterInfo> infos = new ArrayList<PerfCounterInfo>();
        PerfCounterInfo perfCounterInfo = mock(PerfCounterInfo.class);
        ElementDescription desc = mock(ElementDescription.class);
        PerfSummaryType sum1 =  PerfSummaryType.AVERAGE;
        when(perfCounterInfo.getKey()).thenReturn(1);
        when(perfCounterInfo.getGroupInfo()).thenReturn(desc);
        when(perfCounterInfo.getNameInfo()).thenReturn(desc);
        when(perfCounterInfo.getRollupType()).thenReturn(sum1);
        when(desc.getKey()).thenReturn("1");
        infos.add(perfCounterInfo);
        
        //when
        collector.createCounterToNameMapping(infos);
        Integer expected = 1;
        Integer result = collector.countersIdMap.get("1.1.AVERAGE");
        
        
        //then
        assertEquals(expected, result);
    }
    
    @Test
    public void testGetLastDayValue() {
        //given
        String input = "5,2,5,2,6,6,9";
        String expected = "9";
        
        //when
        String result = collector.getLastDayValue(input);
                
        //then
        assertEquals(expected, result);
    }
    
    @Test
    public void testResultListToString() {
        //given
        ArrayList<String> input = new ArrayList<String>();
        input.add("t");
        input.add("e");
        input.add("s");
        input.add("t");
        String expected = "test";
        
        //when
        String result = collector.resultListToString(input);
        
        //then
        assertEquals(expected, result);
    }
    
    @Test
    public void testGetResults() {
        //given
        ArrayList<String> expected = new ArrayList<String>();
        expected.add("test");
        
        ArrayList<PerfMetricSeriesCSV> metrics = new ArrayList<PerfMetricSeriesCSV>();
        PerfMetricSeriesCSV csv = mock(PerfMetricSeriesCSV.class);
        metrics.add(csv);
        
        PerfMetricId id = mock(PerfMetricId.class);
        when(csv.getId()).thenReturn(id);
        when(id.getInstance()).thenReturn("");
        when(csv.getValue()).thenReturn("test");
        
        
        //when
        ArrayList<String> result = collector.getResults(metrics);
        
        //then
        assertEquals(expected, result);
    }
    
    @Test(expected = APPlatformException.class)
    public void testCreateResultFromStats() throws APPlatformException {
        
        //given
        ArrayList<PerfEntityMetricBase> retrievedStats = new ArrayList<PerfEntityMetricBase>();
        PerfEntityMetricCSV metric = mock(PerfEntityMetricCSV.class);
        retrievedStats.add(metric);
        when(metric.getValue()).thenReturn(new ArrayList<PerfMetricSeriesCSV>());
        
        //when
        collector.createResultFromStats(retrievedStats);
        
        //then exception
    }
    
    @Test
    public void testCreateMetrics() {

        //given
        ArrayList<PerfMetricId> expected = new ArrayList<PerfMetricId>();
        PerfMetricId metricId = new PerfMetricId();
        metricId.setCounterId(1);
        String instanceType = "*";
        metricId.setInstance(instanceType); 
        expected.add(metricId);
        String counterName = "name";
        
        collector.countersIdMap.put(counterName, 1); 
        
        //when
        List<PerfMetricId> result = collector.createMetrics(counterName);
        
        //then
        assertEquals(expected.get(0).getInstance(), result.get(0).getInstance());
        assertEquals(expected.get(0).getCounterId(), result.get(0).getCounterId());
    }
    
    @Test
    public void testCreatePerfQuerySpec() {
        //given
        ManagedObjectReference vmInstance = mock(ManagedObjectReference.class);
        List<PerfQuerySpec> expected = new ArrayList<PerfQuerySpec>();
        List<PerfMetricId> perfMetricIds = new ArrayList<PerfMetricId>();
        PerfMetricId id = mock(PerfMetricId.class);
        perfMetricIds.add(id);
        PerfQuerySpec querySpecification = new PerfQuerySpec();
        querySpecification.setEntity(vmInstance);
        querySpecification.setIntervalId(86400);
        querySpecification.setFormat("csv");
        querySpecification.getMetricId().addAll(perfMetricIds);
        expected.add(querySpecification);
        
        //when
        List<PerfQuerySpec> result =  collector.createPerfQuerySpec(vmInstance, perfMetricIds, 86400);
        
        //then
        assertEquals(expected.get(0).getMetricId(), result.get(0).getMetricId());
    }
    
    @Test(expected = APPlatformException.class)
    public void testCreateMetricResult() throws RuntimeFaultFaultMsg, APPlatformException{
        //given
        String name = "name";
        List<PerfEntityMetricBase> retrievedStats = new ArrayList<PerfEntityMetricBase>();
        PerfEntityMetricCSV metric = mock(PerfEntityMetricCSV.class);
        retrievedStats.add(metric);
        
        collector.countersIdMap.put(name, 1); 
        collector.vmInstance  = mock(ManagedObjectReference.class);
        collector.vmw = vmw = mock(VMwareClient.class);
        
        VimPortType port = mock(VimPortType.class);
        
        when(metric.getValue()).thenReturn(new ArrayList<PerfMetricSeriesCSV>());
        when(vmw.getService()).thenReturn(port);
        when(port.queryPerf(Mockito.anyObject(), Mockito.<PerfQuerySpec>anyList())).thenReturn(retrievedStats);
        
        
        //when
        ArrayList<String> result = collector.createMetricResult(name);
        result.get(0); 
    }
    
    @Test
    public void testInitialize(){
        
        //given
        
        //when
        collector.initialize();
        //then
        Mockito.verify(collector, Mockito.times(1)).initialize();
    }
    
    @Test
    public void testCreateAvaerage() {
        //given
        ArrayList<String> values = new ArrayList<String>();
        ArrayList<String> expected = new ArrayList<String>();
        String value = "1,2,3,4,5,6,7,8,9";
        String expectedValue = "5";
        values.add(value);
        expected.add(expectedValue);
        
        //when
        ArrayList<String> result = collector.createAvaerage(values);
        
        //then
        assertEquals(expected, result);
    }
    
}
