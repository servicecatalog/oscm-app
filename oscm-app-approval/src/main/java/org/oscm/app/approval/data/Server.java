/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: Sep 15, 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.data;

import org.oscm.app.v2_0.intf.ServerInformation;

import java.util.List;

public class Server implements ServerInformation {

  private static final long serialVersionUID = 1227697097068163229L;

  private String id;
  private String name;
  private String status;
  private String flavor;
  private List<String> floatingIP;
  private List<String> fixedIP;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getType() {
    return flavor;
  }

  @Override
  public void setType(String flavor) {
    this.flavor = flavor;
  }

  @Override
  public List<String> getPublicIP() {
    return floatingIP;
  }

  @Override
  public void setPublicIP(List<String> floatingIP) {
    this.floatingIP = floatingIP;
  }

  @Override
  public List<String> getPrivateIP() {
    return fixedIP;
  }

  @Override
  public void setPrivateIP(List<String> fixedIP) {
    this.fixedIP = fixedIP;
  }

  @Override
  public String getPrivateIPasString() {
    StringBuilder sb = new StringBuilder();
    for (String ip : fixedIP) {
      sb.append(ip);
      sb.append(",");
    }

    return sb.toString();
  }

  @Override
  public String getPublicIPasString() {
    StringBuilder sb = new StringBuilder();
    for (String ip : floatingIP) {
      sb.append(ip);
      sb.append(",");
    }

    return sb.toString();
  }
}
