/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2020
 *
 *  Creation Date: Nov 9, 2020
 *
 *******************************************************************************/
package org.oscm.app.business;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.v2_0.data.ServiceUser;
import org.oscm.provisioning.data.User;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({UserMapper.class})
public class UserMapperTest {

    @Test
    public void testToServiceUser() throws Exception {

        User fromUser = new User();
        fromUser.setApplicationUserId("TestUser");
        fromUser.setEmail("test@email.com");
        fromUser.setLocale("en");
        fromUser.setRoleIdentifier("user");
        fromUser.setUserLastName("User");
        fromUser.setUserFirstName("Test");
        ServiceUser serviceUser = new ServiceUser();
        PowerMockito.whenNew(ServiceUser.class).withNoArguments().thenReturn(serviceUser);

        ServiceUser result = UserMapper.toServiceUser(fromUser);

        assertEquals("Test", result.getFirstName());
    }

    @Test
    public void testToServiceUserUserNull() {

        assertNull(UserMapper.toServiceUser(null));
    }


    @Test
    public void testToProvisioningUser() throws Exception {

        ServiceUser serviceUser = new ServiceUser();
        serviceUser.setApplicationUserId("TestUser");
        serviceUser.setEmail("test@email.com");
        serviceUser.setLocale("en");
        serviceUser.setRoleIdentifier("user");
        serviceUser.setLastName("User");
        serviceUser.setFirstName("Test");
        User user = new User();
        PowerMockito.whenNew(User.class).withNoArguments().thenReturn(user);

        User result = UserMapper.toProvisioningUser(serviceUser);

        assertEquals("Test", result.getUserFirstName());
    }

    @Test
    public void testToProvisioningUserServiceUserNull() {

        assertNull(UserMapper.toProvisioningUser(null));
    }
}
