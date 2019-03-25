/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2018
 *
 *  Creation Date: 10.04.2017
 *
 *******************************************************************************/
package org.oscm.app.common.ui;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.Template;
import org.oscm.app.v2_0.intf.APPTemplateService;
import org.oscm.app.v2_0.intf.ControllerAccess;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.OutputStream;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/** Unit test of template bean */
@RunWith(MockitoJUnitRunner.class)
public class TemplateBeanTest {

  @Spy @InjectMocks private TemplateBean templateBean = new TemplateBean();

  @Mock private APPTemplateService templateService;

  @Mock private ControllerAccess controllerAccess;

  @Mock private UploadedFile uploadedFile;

  @Before
  public void setup() {

    PasswordAuthentication sampleAuthentication = new PasswordAuthentication("test", "test");
    doReturn(sampleAuthentication).when(templateBean).getAuthentication();
  }

  @Test
  public void testLoad() {

    // given
    assertNull(templateBean.getModel());

    // when
    templateBean.load();

    // then
    assertNotNull(templateBean.getModel());
  }

  @Test
  public void testSave() {

    // given
    when(uploadedFile.getName()).thenReturn("test.txt");
    assertNull(templateBean.getStatus());

    // when
    templateBean.save();

    // then
    assertNotNull(templateBean.getStatus());
  }

  @Test
  public void testDelete() {

    // given
    assertNull(templateBean.getModel());

    // when
    templateBean.delete("file");

    // then
    assertNotNull(templateBean.getModel());
  }

  @Test
  public void testExport() throws Exception {

    // given
    Template template = givenTemplate();

    FacesContext facesContext = mock(FacesContext.class);
    ExternalContext externalContext = mock(ExternalContext.class);
    OutputStream os = mock(OutputStream.class);

    doReturn(facesContext).when(templateBean).getContext();
    when(templateService.getTemplate(anyString(), anyString(), any(PasswordAuthentication.class)))
        .thenReturn(template);
    when(facesContext.getExternalContext()).thenReturn(externalContext);
    when(externalContext.getResponseOutputStream()).thenReturn(os);

    // when
    templateBean.export("file");

    // then
    Mockito.verify(facesContext).responseComplete();
  }

  private Template givenTemplate() {

    Template template = new Template();
    template.setFileName("file");
    template.setContent("test".getBytes());
    template.setLastChange(new Date());

    return template;
  }
}
