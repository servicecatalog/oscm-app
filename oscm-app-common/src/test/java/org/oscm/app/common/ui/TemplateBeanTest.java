/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2018
 *
 * <p>Creation Date: 10.04.2017
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.common.ui;

import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.Template;
import org.oscm.app.v2_0.intf.APPTemplateService;
import org.oscm.app.v2_0.intf.ControllerAccess;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;

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
  @Mock private ExternalContext externalCtx;
  @Mock private HttpSession httpSession;
  @Mock private FacesContext facesCtx;

  @Before
  public void setup() {
    doReturn(facesCtx).when(templateBean).getContext();
    when(facesCtx.getExternalContext()).thenReturn(externalCtx);
    when(externalCtx.getSession(anyBoolean())).thenReturn(httpSession);
  }

  @Test
  public void testInit() {
    // given
    UIViewRoot viewRoot = mock(UIViewRoot.class);
    when(facesCtx.getViewRoot()).thenReturn(viewRoot);
    when(viewRoot.getLocale()).thenReturn(Locale.getDefault());

    // when
    templateBean.init();

    // then
    verify(templateBean, times(1)).load();
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

    OutputStream os = mock(OutputStream.class);

    when(templateService.getTemplate(anyString(), anyString(), any(PasswordAuthentication.class)))
        .thenReturn(template);
    when(externalCtx.getResponseOutputStream()).thenReturn(os);

    // when
    templateBean.export("file");

    // then
    verify(facesCtx).responseComplete();
  }

  private Template givenTemplate() {

    Template template = new Template();
    template.setFileName("file");
    template.setContent("test".getBytes());
    template.setLastChange(new Date());

    return template;
  }
}
