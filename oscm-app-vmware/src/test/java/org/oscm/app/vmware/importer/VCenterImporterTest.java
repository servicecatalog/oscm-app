/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 11 Dec 2020
 *
 * <p>*****************************************************************************
 */

package org.oscm.app.vmware.importer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.vmware.business.balancer.XMLHelper;
import org.oscm.app.vmware.encryption.AESEncrypter;
import org.oscm.app.vmware.parser.VCenterParser;
import org.oscm.app.vmware.parser.model.VCenter;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({VCenterImporter.class, XMLHelper.class, AESEncrypter.class})
public class VCenterImporterTest {

  private DataAccessService das;
  private VCenterImporter vCenterImporter;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private VCenter vCenter;
  private VCenterParser vCenterParser;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    vCenterImporter = PowerMockito.spy(new VCenterImporter(das));
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    vCenterParser = mock(VCenterParser.class);
    PowerMockito.mockStatic(AESEncrypter.class);
    vCenter = new VCenter();
    vCenter.tKey = "12000";
    vCenter.name = "name";
    vCenter.identifier = "identifier";
    vCenter.url = "url";
    vCenter.userId = "userId";
    vCenter.password = "password";

    when(das.getDatasource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }

  @Test(expected = Exception.class)
  public void testGetVCenterIdThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(vCenterImporter, "getVCenterId", "12000");
  }

  @Test
  public void testLoadSave() throws Exception {
    // given
    PowerMockito.whenNew(VCenterParser.class).withAnyArguments().thenReturn(vCenterParser);
    when(vCenterParser.readNextObject()).thenReturn(vCenter, null);
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    vCenterImporter.load(any());
    // then
    PowerMockito.verifyPrivate(vCenterImporter, times(1)).invoke("save", vCenter);
    verify(preparedStatement, times(1)).setString(2, "name");
    verify(preparedStatement, times(1)).setString(3, "identifier");
  }

  @Test
  public void testLoadUpdate() throws Exception {
    // given
    PowerMockito.whenNew(VCenterParser.class).withAnyArguments().thenReturn(vCenterParser);
    when(vCenterParser.readNextObject()).thenReturn(vCenter, null);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(anyString())).thenReturn(10);
    when(resultSet.getString(1)).thenReturn("1200");
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    vCenterImporter.load(any());
    // then
    PowerMockito.verifyPrivate(vCenterImporter, times(1)).invoke("update", vCenter);
    verify(preparedStatement, times(1)).setString(1, "name");
    verify(preparedStatement, times(1)).setString(2, "identifier");
  }
}
