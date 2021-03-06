/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2019
 *
 * <p>Creation Date: 2019-02-22
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import java.util.ArrayList;
import java.util.List;

import org.oscm.app.vmware.remote.vmware.VMClientPool;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VMwareProcessor {
  private static final Logger logger = LoggerFactory.getLogger(VMwareProcessor.class);

  public List<Server> getServersDetails(VMPropertyHandler ph) {

    String instanceName = "";
    String vcenter = ph.getServiceSetting(VMPropertyHandler.TS_TARGET_VCENTER_SERVER);
    VMwareClient vmClient = null;
    List<Server> servers = new ArrayList<>();
    ArrayList<String> VMDetails = new ArrayList<String>();
    ArrayList<String> serverInformation = new ArrayList<String>();
    try {
      vmClient = getVMWareClient(vcenter);
      instanceName = ph.getInstanceName();
      VM vm = createVM(instanceName, vmClient);
      VMDetails.add("CPUs: " + String.valueOf(ph.getConfigCPUs()));
      VMDetails.add("Disc Space MB: " + String.valueOf(ph.getConfigDiskSpaceMB()));
      VMDetails.add("Memory MB: " + String.valueOf(ph.getConfigMemoryMB()));
      VMDetails.add("Memory used MB: " + vm.getGuestMemoryUsage());
      VMDetails.add("CPU used MHz:  " + vm.getOverallCpuUsage());
      VMDetails.add("Uptime sec: " + vm.getUptimeSeconds());
      serverInformation.add("Cluster: " + ph.getTargetCluster());
      serverInformation.add("Datacenter: " + ph.getTargetDatacenter());
      serverInformation.add("VCenter: " + ph.getTargetVCenterServer());
      serverInformation.addAll(vm.getSnashotsAsList());
      Server server = new Server();
      server.setStatus(vm.getStatus());
      server.setId(instanceName);
      server.setName(vm.getGuestFullName());
      server.setPublicIP(serverInformation);
      server.setPrivateIP(VMDetails);
      server.setType(ph.getAccessInfo().toString().replace(instanceName + ", ", ""));
      servers.add(server);
    } catch (Exception e) {
      logger.error("Failed to create serverlist");
    } finally {
      if (vmClient != null) {
        try {
          returnVMWareClient(vcenter, vmClient);
        } catch (Exception e) {
          logger.error("Failed to return VMware client into pool", e);
        }
      }
    }
    return servers;
  }

  protected VM createVM(String instanceName, VMwareClient vmClient) throws Exception {
    return new VM(vmClient, instanceName);
  }

  protected void returnVMWareClient(String vcenter, VMwareClient vmClient) throws Exception {
    VMClientPool.getInstance().getPool().returnObject(vcenter, vmClient);
  }

  protected VMwareClient getVMWareClient(String vcenter) throws Exception {
    return VMClientPool.getInstance().getPool().borrowObject(vcenter);
  }

  public String getVmAccessUrl(VMPropertyHandler ph) {
    String instanceName = "";
    String vcenter = ph.getServiceSetting(VMPropertyHandler.TS_TARGET_VCENTER_SERVER);
    VMwareClient vmClient = null;
    String url = "";
    try {
      vmClient = getVMWareClient(vcenter);
      instanceName = ph.getInstanceName();
      VM vm = createVM(instanceName, vmClient);
      url = vm.createVmUrl(ph);
    } catch (Exception e) {
      logger.error("Failed to create serverlist");
    } finally {
      if (vmClient != null) {
        try {
          returnVMWareClient(vcenter, vmClient);
        } catch (Exception e) {
          logger.error("Failed to return VMware client into pool", e);
        }
      }
    }
    return url;
  }
}
