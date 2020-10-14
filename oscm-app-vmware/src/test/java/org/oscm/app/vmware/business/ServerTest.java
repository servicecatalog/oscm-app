/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 14 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** @author worf */
public class ServerTest {

  @Test
  public void getPrivateIPasString() {

    // given
    List<String> ips = new ArrayList<String>();
    ips.add("127.0.0.1");
    ips.add("128.0.0.2");
    Server server = new Server();
    server.setPrivateIP(ips);

    String expected = "127.0.0.1\n" + "128.0.0.2";

    // when
    String result = server.getPrivateIPasString();

    // then
    assertEquals(expected, result);
  }

  @Test
  public void getPublicIPasString() {

    // given
    List<String> ips = new ArrayList<String>();
    ips.add("127.0.0.1");
    ips.add("128.0.0.2");
    Server server = new Server();
    server.setPublicIP(ips);

    String expected = "127.0.0.1\n" + "128.0.0.2";

    // when
    String result = server.getPublicIPasString();

    // then
    assertEquals(expected, result);
  }
}
