/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softavail.commsrouter.jpa.test;

import com.softavail.commsrouter.api.dto.arg.CreateRouterArg;
import com.softavail.commsrouter.api.dto.model.ApiObjectRef;
import com.softavail.commsrouter.api.dto.model.RouterDto;
import com.softavail.commsrouter.api.exception.CommsRouterException;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author G.Ivanov
 */
public class CoreRouterServiceJpaTest extends TestBase {

    //Testing the get method inherited from CoreApiObjectService
    @Test
    public void getTest() throws CommsRouterException {
        RouterDto router = routerService.get("01");
        assertEquals("name_one",router.getName());
        assertEquals("description_one",router.getDescription());
    }

    //Testing the list method inherited from CoreApiObjectService
    @Test
    public void listTest() throws CommsRouterException {
        List<RouterDto> routers = routerService.list();
        assertEquals(2,routers.size());
    }

    //Testing the delete method inherited from CoreApiObjectService
    @Test
    public void deleteTest() throws CommsRouterException {
        List<RouterDto> routers = routerService.list();
        assertEquals(2,routers.size());
        routerService.delete("01");
        routers = routerService.list();
        assertEquals(1,routers.size());
    }

    //Testing the replace method
    @Test
    public void createTestOne() throws CommsRouterException {
        CreateRouterArg createArg = newCreateRouterArg("name_three", "description_three");
        ApiObjectRef newRouter = routerService.create(createArg);
        //Get the new router by ID
        RouterDto router = routerService.get(newRouter.getRef());
        assertEquals("name_three", router.getName());
        assertEquals("description_three", router.getDescription());
    }

    //Testing the replace method that also takes Id
    @Test
    public void createTestTwo() throws CommsRouterException {
        ApiObjectRef newRouter = routerService.replace(newCreateRouterArg("name_three", "description_three"), "03");
        //Get the new router by ID
        RouterDto router = routerService.get(newRouter.getRef());
        assertEquals("name_three", router.getName());
        assertEquals("description_three", router.getDescription());
        assertEquals("03", router.getRef());
    }

    //Testing the update method
    @Test
    public void updateTest() throws CommsRouterException {
        routerService.update(newUpdateRouterArg("name_nine", "description_nine"), "02");
        //Get updated router by ID
        RouterDto router = routerService.get("02");
        assertEquals("name_nine", router.getName());
        assertEquals("description_nine", router.getDescription());
    }

    @Test
    public void getDtoEntityTest() {
        Class<RouterDto> newRouter = routerService.getDtoEntityClass();
    }

}
