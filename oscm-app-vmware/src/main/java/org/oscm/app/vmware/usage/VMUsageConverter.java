package org.oscm.app.vmware.usage;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VM;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.remote.vmware.VMClientPool;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.oscm.types.exceptions.ObjectNotFoundException;
import org.oscm.types.exceptions.OrganizationAuthoritiesException;
import org.oscm.types.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfEntityMetricCSV;
import com.vmware.vim25.PerfInterval;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricSeriesCSV;
import com.vmware.vim25.PerfProviderSummary;
import com.vmware.vim25.PerfQuerySpec;

public class VMUsageConverter {

    static final String EVENT_DISK = "EVENT_DISK_GIGABYTE_HOURS";
    static final String EVENT_CPU = "EVENT_CPU_HOURS";
    static final String EVENT_RAM = "EVENT_RAM_MEGABYTE_HOURS";
    static final String EVENT_TOTAL = "EVENT_TOTAL_HOURS";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(VMUsageConverter.class);

    VMPropertyHandler ph;

    public VMUsageConverter() {
    }

    public VMUsageConverter(VMPropertyHandler ph) throws MalformedURLException {
        this.ph = ph;
    }

    public void registerUsageEvents(String startTime, String endTime)
            throws MalformedURLException, ObjectNotFoundException,
            OrganizationAuthoritiesException, ValidationException,
            APPlatformException {

        VM vm = getVM(ph.getInstanceName());
    }

    private VM getVM(String instanceName) {

        String vcenter = ph
                .getServiceSetting(VMPropertyHandler.TS_TARGET_VCENTER_SERVER);
        VM vm = null;
        try {
            VMwareClient vmw = VMClientPool.getInstance().getPool()
                    .borrowObject(vcenter);
            ManagedObjectReference vmInstance = vmw.getServiceUtil()
                    .getDecendentMoRef(null, "VirtualMachine", instanceName);

            ManagedObjectReference performanceManager = vmw.getConnection()
                    .getServiceContent().getPerfManager();

            List<PerfInterval> perfIntervals = (List<PerfInterval>) vmw
                    .getServiceUtil().getDynamicProperty(performanceManager,
                            "historicalInterval");

            List<PerfCounterInfo> perfCounters = (List<PerfCounterInfo>) vmw
                    .getServiceUtil()
                    .getDynamicProperty(performanceManager, "perfCounter");

            PerfProviderSummary summary = vmw.getConnection().getService()
                    .queryPerfProviderSummary(performanceManager, vmInstance);
            int perfInterval = summary.getRefreshRate();

            HashMap<String, Integer> countersIdMap = new HashMap<String, Integer>();
            HashMap<Integer, PerfCounterInfo> countersInfoMap = new HashMap<Integer, PerfCounterInfo>();

            ArrayList<String> names = new ArrayList<String>();
            for (PerfCounterInfo perfCounter : perfCounters) {

                Integer counterId = new Integer(perfCounter.getKey());

                countersInfoMap.put(counterId, perfCounter);

                String counterGroup = perfCounter.getGroupInfo().getKey();
                String counterName = perfCounter.getNameInfo().getKey();
                String counterRollupType = perfCounter.getRollupType()
                        .toString();
                String fullCounterName = counterGroup + "." + counterName + "."
                        + counterRollupType;
                names.add(fullCounterName);

                LOGGER.info(counterGroup + "." + counterName + "."
                        + counterRollupType);
                countersIdMap.put(fullCounterName, counterId);

            }

            String[] counterNames = new String[] { "disk.provisioned.LATEST",
                    "mem.granted.AVERAGE", "power.power.AVERAGE",
                    "cpu.usage.AVERAGE", "disk.usage.AVERAGE", "cpu.usage.NONE",
                    "cpu.usage.MINIMUM", "cpu.usage.MAXIMUM",
                    "cpu.usagemhz.NONE", "cpu.usagemhz.AVERAGE",
                    "cpu.usagemhz.MINIMUM", "cpu.usagemhz.MAXIMUM",
                    "cpu.reservedCapacity.AVERAGE", "cpu.system.SUMMATION",
                    "cpu.wait.SUMMATION" };

            List<PerfMetricId> perfMetricIds = new ArrayList<PerfMetricId>();
            for (int i = 0; i < counterNames.length; i++) {

                PerfMetricId metricId = new PerfMetricId();

                metricId.setCounterId(countersIdMap.get(counterNames[i]));
                metricId.setInstance("*");
                perfMetricIds.add(metricId);

            }

            int intervalId =    86400;
            PerfQuerySpec querySpecification = new PerfQuerySpec();
            querySpecification.setEntity(vmInstance);
            querySpecification.setIntervalId(intervalId);
            querySpecification.setFormat("csv");
            querySpecification.getMetricId().addAll(perfMetricIds);

            List<PerfQuerySpec> pqsList = new ArrayList<PerfQuerySpec>();
            pqsList.add(querySpecification);

            List<PerfEntityMetricBase> retrievedStats = vmw.getConnection()
                    .getService().queryPerf(performanceManager, pqsList);

            for (PerfEntityMetricBase singleEntityPerfStats : retrievedStats) {

                /*
                 * Cast the base type (PerfEntityMetricBase) to the csv-specific
                 * sub-class.
                 */
                PerfEntityMetricCSV entityStatsCsv = (PerfEntityMetricCSV) singleEntityPerfStats;

                /* Retrieve the list of sampled values. */
                List<PerfMetricSeriesCSV> metricsValues = entityStatsCsv
                        .getValue();

                if (metricsValues.isEmpty()) {
                    System.out.println("No stats retrieved. "
                            + "Check whether the virtual machine is powered on.");
                    throw new Exception();
                }
                String csvTimeInfoAboutStats = entityStatsCsv
                        .getSampleInfoCSV();
                /* Print the time and interval information. */
                LOGGER.info(
                        "Collection: interval (seconds),time (yyyy-mm-ddThh:mm:ssZ)");
                LOGGER.info(csvTimeInfoAboutStats);

                for (PerfMetricSeriesCSV csv : metricsValues) {

                    /*
                     * Use the counterId to obtain the associated
                     * PerfCounterInfo object
                     */
                    PerfCounterInfo pci = countersInfoMap
                            .get(csv.getId().getCounterId());

                    /* Print out the metadata for the counter. */
                    LOGGER.info("----------------------------------------");
                    LOGGER.info(pci.getGroupInfo().getKey() + "."
                            + pci.getNameInfo().getKey() + "."
                            + pci.getRollupType() + " - "
                            + pci.getUnitInfo().getKey());
                    LOGGER.info("Instance: " + csv.getId().getInstance());
                    LOGGER.info("Values: " + csv.getValue());

                }
            }

            // List<PerfMetricId> queryAvailablePerfMetric = vmw.getConnection()
            // .getService().queryAvailablePerfMetric(performanceManager,
            // vmInstance, null, null, perfInterval);
            //
            // int SELECTED_COUNTER_ID = 1;
            // ArrayList<PerfMetricId> list = new ArrayList<PerfMetricId>();
            //
            // for (int i2 = 0; i2 < queryAvailablePerfMetric.size(); i2++) {
            // PerfMetricId perfMetricId = queryAvailablePerfMetric.get(i2);
            // if (SELECTED_COUNTER_ID == perfMetricId.getCounterId()) {
            // list.add(perfMetricId);
            // }
            // }
            //
            // ArrayList<PerfQuerySpec> qSpecList = new
            // ArrayList<PerfQuerySpec>();
            // PerfQuerySpec qSpec = new PerfQuerySpec();
            // qSpec.setEntity(vmInstance);
            // qSpec.setIntervalId(perfInterval);
            // qSpecList.add(qSpec);
            //
            // List<PerfEntityMetricBase> pembs =
            // vmw.getConnection().getService()
            // .queryPerf(performanceManager, qSpecList);
            //
            //

            // for (int i = 0; pembs != null && i < pembs.size(); i++) {
            // PerfEntityMetricBase val = pembs.get(i);
            // PerfEntityMetric pem = (PerfEntityMetric) val;
            // List<PerfMetricSeries> vals = pem.getValue();
            // List<PerfSampleInfo> infos = pem.getSampleInfo();
            //
            // for (int j = 0; vals != null && j < vals.size(); ++j) {
            // PerfMetricIntSeries val1 = (PerfMetricIntSeries) vals
            // .get(j);
            // List<Long> longs = val1.getValue();
            // for (int k = 0; k < longs.size(); k++) {
            // System.out.println(infos.get(k).getTimestamp() + " : "
            // + longs.get(k));
            // LOGGER.info(infos.get(k).getTimestamp() + " : "
            // + longs.get(k));
            // }
            // System.out.println();
            // }
            // }

            // for (PerfInterval histPerfInterval : perfIntervals) {
            // LOGGER.info("key = " + histPerfInterval.getKey());
            // LOGGER.info("length = " + histPerfInterval.getLength());
            // LOGGER.info("samplingPeriod = "
            // + histPerfInterval.getSamplingPeriod());
            // LOGGER.info("level = " + histPerfInterval.getLevel());
            // LOGGER.info("name = " + histPerfInterval.getName());
            // LOGGER.info(
            // "periode = " + histPerfInterval.getSamplingPeriod());
            //
            // System.out.println("key = " + histPerfInterval.getKey());
            // System.out.println("length = " + histPerfInterval.getLength());
            // System.out.println("samplingPeriod = "
            // + histPerfInterval.getSamplingPeriod());
            // System.out.println("level = " + histPerfInterval.getLevel());
            // System.out.println("name = " + histPerfInterval.getName());
            // System.out.println(
            // "periode = " + histPerfInterval.getSamplingPeriod());
            // }

            vm = new VM(vmw, ph.getInstanceName());
        } catch (Exception e) {
            LOGGER.error("Can´t gather usage data for instance "
                    + ph.getInstanceId() + "\n" + e.getMessage());
        }
        return vm;
    }
}
