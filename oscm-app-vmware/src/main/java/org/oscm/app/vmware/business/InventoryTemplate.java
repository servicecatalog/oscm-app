/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 15 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import java.util.List;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.balancer.LoadBalancerConfiguration;
import org.oscm.app.vmware.business.model.VMwareHost;
import org.oscm.app.vmware.business.model.VMwareStorage;
import org.oscm.app.vmware.i18n.Messages;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.DatastoreHostMount;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/** @author worf */
public class InventoryTemplate {
  /** */
  private VMwareClient vmw;

  VMPropertyHandler paramHandler;

  private static final Logger logger = LoggerFactory.getLogger(InventoryTemplate.class);

  public InventoryTemplate(VMwareClient vmw, VMPropertyHandler paramHandler) {
    this.vmw = vmw;
    this.paramHandler = paramHandler;
  }

  /**
   * If host and storage are not defined as technical service parameter then the load balancing
   * mechanism is used to determine host and storage
   */
  public VirtualMachineRelocateSpec getHostAndStorageSpec(ManagedObjectReference vmDataCenter)
      throws Exception {
    String datacenter = paramHandler.getTargetDatacenter();
    String cluster = paramHandler.getTargetCluster();
    logger.debug("datacenter: " + datacenter + " cluster: " + cluster);
    String xmlData = paramHandler.getHostLoadBalancerConfig();
    VirtualMachineRelocateSpec relocSpec = new VirtualMachineRelocateSpec();

    String storageName = paramHandler.getServiceSetting(VMPropertyHandler.TS_TARGET_STORAGE);
    String hostName = paramHandler.getServiceSetting(VMPropertyHandler.TS_TARGET_HOST);
    VMwareDatacenterInventory inventory = readDatacenterInventory(vmw, datacenter, cluster);
    if (hostName == null || hostName.trim().length() == 0) {
      logger.debug("target host not set. get host and storage from loadbalancer");
      LoadBalancerConfiguration balancerConfig =
          createLoadBalancerConfiguration(xmlData, inventory);
      VMwareHost host = balancerConfig.getBalancer().next(paramHandler);
      hostName = host.getName();
      paramHandler.setSetting(VMPropertyHandler.TS_TARGET_HOST, hostName);
      VMwareStorage storage = host.getNextStorage(paramHandler);
      storageName = storage.getName();
    } else {
      if (storageName == null || storageName.trim().length() == 0) {
        logger.debug("target storage not set. get host and storage from inventory");
        VMwareHost host = inventory.getHost(hostName);
        VMwareStorage storage = host.getNextStorage(paramHandler);
        storageName = storage.getName();
      }
    }

    logger.info("Target Host: " + hostName + " Target Storage: " + storageName);

    ManagedObjectReference vmHost = getVMHost(vmDataCenter, hostName);
    ManagedObjectReference vmPool = getVMPool(hostName, vmHost);
    ManagedObjectReference vmDatastore = getVMDatastore(storageName, hostName, vmHost);

    relocSpec.setDatastore(vmDatastore);
    relocSpec.setPool(vmPool);
    relocSpec.setHost(vmHost);
    return relocSpec;
  }

  protected LoadBalancerConfiguration createLoadBalancerConfiguration(
      String xmlData, VMwareDatacenterInventory inventory) throws Exception {
    return new LoadBalancerConfiguration(xmlData, inventory);
  }

  protected ManagedObjectReference getVMDatastore(
      String storageName, String hostName, ManagedObjectReference vmHost)
      throws Exception, APPlatformException {
    ManagedObjectReference vmDatastore = null;
    Object vmHostDatastores = vmw.getServiceUtil().getDynamicProperty(vmHost, "datastore");
    if (vmHostDatastores instanceof List<?>) {
      for (Object vmHostDatastore : (List<?>) vmHostDatastores) {
        if (vmHostDatastore instanceof ManagedObjectReference) {
          String dsname =
              (String)
                  vmw.getServiceUtil()
                      .getDynamicProperty((ManagedObjectReference) vmHostDatastore, "summary.name");
          if (dsname.equalsIgnoreCase(storageName)) {
            vmDatastore = (ManagedObjectReference) vmHostDatastore;
            break;
          }
        } else {
          logger.warn(
              "Expected datastore information as 'ManagedObjectReference' but recieved object of type "
                  + (vmHostDatastore == null
                      ? "[null]"
                      : vmHostDatastore.getClass().getSimpleName()));
        }
      }
    }
    if (vmDatastore == null) {
      logger.error("Target datastore " + storageName + " not found");
      throw new APPlatformException(
          Messages.getAll("error_invalid_datastore", new Object[] {storageName, hostName}));
    }
    return vmDatastore;
  }

  protected ManagedObjectReference getVMPool(String hostName, ManagedObjectReference vmHost)
      throws Exception, InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException {
    ManagedObjectReference vmHostCluster =
        (ManagedObjectReference) vmw.getServiceUtil().getDynamicProperty(vmHost, "parent");
    ManagedObjectReference vmPool =
        vmw.getServiceUtil().getDecendentMoRef(vmHostCluster, "ResourcePool", "Resources");
    if (vmPool == null) {
      logger.error("Resourcepool not found");
      throw new APPlatformException(Messages.getAll("error_invalid_pool", new Object[] {hostName}));
    }
    return vmPool;
  }

  protected ManagedObjectReference getVMHost(ManagedObjectReference vmDataCenter, String hostName)
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException {
    ManagedObjectReference vmHost =
        vmw.getServiceUtil().getDecendentMoRef(vmDataCenter, "HostSystem", hostName);
    if (vmHost == null) {
      logger.error("Target host " + hostName + " not found");
      throw new APPlatformException(Messages.getAll("error_invalid_host", new Object[] {hostName}));
    }
    return vmHost;
  }

  @SuppressWarnings("unchecked")
  VMwareDatacenterInventory readDatacenterInventory(
      VMwareClient appUtil, String datacenter, String cluster) throws Exception {
    logger.debug("datacenter: " + datacenter + " cluster: " + cluster);

    ManagedObjectAccessor serviceUtil = appUtil.getServiceUtil();

    ManagedObjectReference dcMoRef = serviceUtil.getDecendentMoRef(null, "Datacenter", datacenter);

    ManagedObjectReference clusterMoRef =
        serviceUtil.getDecendentMoRef(dcMoRef, "ClusterComputeResource", cluster);

    List<ManagedObjectReference> hostMoRefs =
        (List<ManagedObjectReference>) serviceUtil.getDynamicProperty(clusterMoRef, "host");

    VMwareDatacenterInventory inventory = new VMwareDatacenterInventory();
    for (ManagedObjectReference hostRef : hostMoRefs) {
      List<DynamicProperty> dps = setHostSystemPropertyToInventory(serviceUtil, inventory, hostRef);
      String host = getHostName(dps);
      setStoragePropertyToInventory(serviceUtil, inventory, hostRef, host);
      setVMToInventory(serviceUtil, inventory, hostRef);
    }
    inventory.initialize();
    return inventory;
  }

  protected void setVMToInventory(
      ManagedObjectAccessor serviceUtil,
      VMwareDatacenterInventory inventory,
      ManagedObjectReference hostRef)
      throws Exception {
    List<DynamicProperty> dps;
    List<ManagedObjectReference> vmRefs =
        (List<ManagedObjectReference>) serviceUtil.getDynamicProperty(hostRef, "vm");
    for (ManagedObjectReference vmRef : vmRefs) {
      dps =
          serviceUtil.getDynamicProperty(
              vmRef,
              new String[] {
                "name", "summary.config.memorySizeMB", "summary.config.numCpu", "runtime.host"
              });
      inventory.addVirtualMachine(dps, serviceUtil);
    }
  }

  protected void setStoragePropertyToInventory(
      ManagedObjectAccessor serviceUtil,
      VMwareDatacenterInventory inventory,
      ManagedObjectReference hostRef,
      String host)
      throws Exception {
    List<DynamicProperty> dps;
    List<ManagedObjectReference> storageRefs =
        (List<ManagedObjectReference>) serviceUtil.getDynamicProperty(hostRef, "datastore");
    for (ManagedObjectReference storageRef : storageRefs) {

      dps =
          serviceUtil.getDynamicProperty(
              storageRef, new String[] {"summary.name", "summary.capacity", "summary.freeSpace"});

      String storageName = getStorageName(dps);

      List<DatastoreHostMount> hostMounts =
          (List<DatastoreHostMount>) serviceUtil.getDynamicProperty(storageRef, "host");

      for (DatastoreHostMount hm : hostMounts) {
        ManagedObjectReference hostMor = hm.getKey();
        String hostThatLinksToThisStoarge =
            (String) serviceUtil.getDynamicProperty(hostMor, "name");
        if (host.equals(hostThatLinksToThisStoarge)
            && hm.getMountInfo().isAccessible().booleanValue()
            && hm.getMountInfo().isMounted().booleanValue()
            && !hm.getMountInfo().getAccessMode().equals("readOnly")) {

          logger.debug("storage: " + storageName);
          inventory.addStorage(host, dps);
        }
      }
    }
  }

  protected String getStorageName(List<DynamicProperty> dps) {
    String storageName = "";
    for (DynamicProperty dp : dps) {
      String key = dp.getName();
      if ("summary.name".equals(key) && dp.getVal() != null) {
        storageName = dp.getVal().toString();
      }
    }
    return storageName;
  }

  protected List<DynamicProperty> setHostSystemPropertyToInventory(
      ManagedObjectAccessor serviceUtil,
      VMwareDatacenterInventory inventory,
      ManagedObjectReference hostRef)
      throws Exception {

    List<DynamicProperty> dps =
        serviceUtil.getDynamicProperty(
            hostRef,
            new String[] {"name", "summary.hardware.memorySize", "summary.hardware.numCpuCores"});
    inventory.addHostSystem(dps);
    return dps;
  }

  protected String getHostName(List<DynamicProperty> dps) {
    String host = "";
    for (DynamicProperty dp : dps) {
      String key = dp.getName();
      if ("name".equals(key) && dp.getVal() != null) {
        host = dp.getVal().toString();
      }
    }
    logger.debug("addHostSystem host: " + host);
    return host;
  }
}
