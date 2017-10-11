/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.softavail.commsrouter.jpa.test;

import com.softavail.commsrouter.api.dto.arg.CreatePlanArg;
import com.softavail.commsrouter.api.dto.arg.UpdatePlanArg;
import com.softavail.commsrouter.api.dto.model.PlanDto;
import com.softavail.commsrouter.api.dto.model.RouterObjectId;
import com.softavail.commsrouter.api.dto.model.RuleDto;
import com.softavail.commsrouter.api.exception.CommsRouterException;
import com.softavail.commsrouter.api.service.CorePlanService;
import com.softavail.commsrouter.app.AppContext;
import com.softavail.commsrouter.app.TaskDispatcher;
import com.softavail.commsrouter.domain.dto.mappers.EntityMappers;
import com.softavail.commsrouter.eval.CommsRouterEvaluator;
import com.softavail.commsrouter.jpa.JpaDbFacade;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author G.Ivanov
 */
public class CorePlanServiceJpaTest extends TestBase {

    private static CorePlanService planService;
    private static AppContext app;

    private static final Logger LOGGER = LogManager.getLogger(CorePlanServiceJpaTest.class);

    @BeforeClass
    public static void setTestCoreRouterService() {
        CommsRouterEvaluator ev = new CommsRouterEvaluator();
        JpaDbFacade db = new JpaDbFacade("mnf-pu-test");
        TaskDispatcher td = new TaskDispatcher(null, null, null);
        EntityMappers enm = new EntityMappers();
        app = new AppContext(db, ev, td, enm);
        planService = new CorePlanService(app);

    }

    public CreatePlanArg returnNewCreateRouterArg(String description,String predicate, String queueId) {
        CreatePlanArg args = new CreatePlanArg();
        RuleDto rule = new RuleDto();
        rule.setPredicate(predicate);
        rule.setQueueId(queueId);
        rule.setTag("tag");
        List<RuleDto> rules = new ArrayList();
        rules.add(rule);
        args.setDescription(description);
        args.setRules(rules);
        return args;
    }

    public UpdatePlanArg returnNewUpdateRouterArg(String description,String predicate, String queueId) {
        UpdatePlanArg args = new UpdatePlanArg();
        RuleDto rule = new RuleDto();
        rule.setPredicate(predicate);
        rule.setQueueId(queueId);
        rule.setTag("tag");
        List<RuleDto> rules = new ArrayList();
        rules.add(rule);
        args.setDescription(description);
        args.setRules(rules);
        return args;
    }

    //Testing the create method of the CorePlanService class
    @Test
    public void createTest() throws CommsRouterException {
        RouterObjectId id = new RouterObjectId("thisIsAnId_one","routerId_one");
        planService.create(returnNewCreateRouterArg("desctiption_one","predicate_one","queueId_one"), id);
        PlanDto createdPlan = planService.get(id);
        
        assertEquals(createdPlan.getDescription(), "desctiption_one");
    }

    //Testing the put method of the CorePlanService class
    @Test
    public void putTest() throws CommsRouterException {
        RouterObjectId id = new RouterObjectId("thisIsAnId_one","routerId_one");
        planService.create(returnNewCreateRouterArg("desctiption_one","predicate_one","queueId_one"), id);
        planService.put(returnNewCreateRouterArg("desctiption_two","predicate_two","queueId_two"), id);
        PlanDto createdPlan = planService.get(id);
        List<RuleDto> rules = createdPlan.getRules();
        
        assertEquals(createdPlan.getDescription(), "desctiption_two");
        assertEquals(rules.get(0).getPredicate(), "predicate_two");
        assertEquals(rules.get(0).getQueueId(), "queueId_two");
    }

    //Testing the update method of the CorePlanService class
    @Test
    public void updateTest() throws CommsRouterException {
        RouterObjectId id = new RouterObjectId("thisIsAnId_one","routerId_one");
        planService.create(returnNewCreateRouterArg("desctiption_one","predicate_one","queueId_one"), id);
        planService.update(returnNewUpdateRouterArg("desctiption_two","predicate_two","queueId_two"), id);
        PlanDto updatedPlan = planService.get(id);
        List<RuleDto> rules = updatedPlan.getRules();
        
        assertEquals(updatedPlan.getDescription(), "desctiption_two");
        assertEquals(rules.get(0).getPredicate(), "predicate_two");
        assertEquals(rules.get(0).getQueueId(), "queueId_two");
    }

    //Testing method list from CoreRouterObjectSercie
    @Test
    public void listTest() throws CommsRouterException {
        RouterObjectId id = new RouterObjectId("thisIsAnId_one","routerId_one");
        planService.create(returnNewCreateRouterArg("desctiption_one","predicate_one","queueId_one"), id);
        id.setId("thisIsAnId_two");
        planService.create(returnNewCreateRouterArg("desctiption_two","predicate_two","queueId_two"), id);
        List<PlanDto> plans = planService.list("routerId_one");
        assertEquals(plans.size(),2);
    }

    //Testing method delete from CoreRouterObjectSercie
    @Test
    public void deleteTest() throws CommsRouterException {
        RouterObjectId id = new RouterObjectId("thisIsAnId_one","routerId_one");
        planService.create(returnNewCreateRouterArg("desctiption_one","predicate_one","queueId_one"), id);
        List<PlanDto> plans = planService.list("routerId_one");
        assertEquals(plans.size(),1);
        planService.delete(id);
        plans = planService.list("routerId_one");
        assertEquals(plans.size(),0);
    }

    @Test
    public void getDtoEntityTest() {
        Class<PlanDto> newPlan = planService.getDtoEntityClass();
    }

}
