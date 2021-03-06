/*
 * Copyright 2017 SoftAvail, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.softavail.api.test;

import com.softavail.commsrouter.test.api.Queue;
import com.softavail.commsrouter.test.api.Plan;
import com.softavail.commsrouter.test.api.CommsRouterResource;
import com.softavail.commsrouter.test.api.Agent;
import com.softavail.commsrouter.test.api.Task;
import com.softavail.commsrouter.test.api.Router;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.allOf;

import com.softavail.commsrouter.api.dto.arg.CreateAgentArg;
import com.softavail.commsrouter.api.dto.arg.CreatePlanArg;
import com.softavail.commsrouter.api.dto.arg.CreateQueueArg;
import com.softavail.commsrouter.api.dto.arg.CreateRouterArg;
import com.softavail.commsrouter.api.dto.arg.CreateTaskArg;
import com.softavail.commsrouter.api.dto.model.AgentDto;
import com.softavail.commsrouter.api.dto.model.AgentState;
import com.softavail.commsrouter.api.dto.model.ApiObjectRef;
import com.softavail.commsrouter.api.dto.model.CreatedTaskDto;
import com.softavail.commsrouter.api.dto.model.RouteDto;
import com.softavail.commsrouter.api.dto.model.TaskDto;
import com.softavail.commsrouter.api.dto.model.TaskState;
import com.softavail.commsrouter.api.dto.model.attribute.StringAttributeValueDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.AfterEach;
import java.io.OutputStreamWriter;
import org.hamcrest.Matchers;
import org.hamcrest.MatcherAssert;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.io.BufferedReader;


/**
 * Unit test for Agents.
 */
// @TestInstance(Lifecycle.PER_CLASS)
@DisplayName("Agent Test")
public class AgentTest {

  private static final Logger LOGGER = LogManager.getLogger(Agent.class);

  private HashMap<CommsRouterResource, String> state = new HashMap<CommsRouterResource, String>();
  private Router r = new Router(state);
  private Queue q = new Queue(state);
  private Agent a = new Agent(state);
  private Task t = new Task(state);
  private Plan p = new Plan(state);
  private ServerSocket server;
  @BeforeAll
  public static void beforeAll() throws Exception {
    Assumptions.assumeTrue(System.getProperty("autHost") != null, "autHost is set");
  }


  @BeforeEach
  public void setup() throws IOException {
    server = new ServerSocket(0);
    r.create(new CreateRouterArg());
    q.create(
        new CreateQueueArg.Builder().description("queue description").predicate("1==1").build());
  }

  @AfterEach
  public void cleanup() throws IOException {
    server.close();
  }

  private String waitToConnect(int timeout) throws IOException {

      server.setSoTimeout(timeout);
      Socket socket = server.accept();
      OutputStreamWriter ow = new OutputStreamWriter(socket.getOutputStream(),"UTF-8");
      ow.write("HTTP1/0 200 OK\r\n\r\n");
      //InputStreamReader r = new InputStreamReader(socket.getInputStream(),"UTF-8");
      return (new BufferedReader(new InputStreamReader(socket.getInputStream()))
                         .lines().collect(Collectors.joining("\n")));
  }

  @Test
  @DisplayName("Create new agent.")
  public void createAgent() {
    a.create(new CreateAgentArg());
    AgentDto resource = a.get();
    assertThat(resource.getCapabilities(), nullValue());
    assertThat(String.format("Check state (%s) to be offline.", resource.getState()),
        resource.getState(), is(AgentState.offline));
  }

  @Test
  @DisplayName("Create new agent and bind to queue.")
  public void createAgentWithCapabilities() {
    a.create("en");
    AgentDto resource = a.get();
    assertThat(String.format("Check attribute language (%s) is 'en'.",
        ((StringAttributeValueDto) resource.getCapabilities().get("language")).getValue()),
        ((StringAttributeValueDto) resource.getCapabilities().get("language")).getValue(),
        is("en"));
    assertThat(String.format("Check state (%s) to be offline.", resource.getState()),
        resource.getState(), is(AgentState.offline));

  }

  public void completeTask() throws MalformedURLException, InterruptedException {
      //TimeUnit.SECONDS.sleep(2);
    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));
    TaskDto task = t.get();
    assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
        task.getState(), is(TaskState.assigned));

    t.setState(TaskState.completed);

    TimeUnit.SECONDS.sleep(2);

    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be ready.", resource.getState()),
        resource.getState(), is(AgentState.ready));
  }


  public void completeTaskAndCreateQueue() throws MalformedURLException, InterruptedException {
      //TimeUnit.SECONDS.sleep(2);
    Integer last=50;
    while(last >0){
      AgentDto resource = a.get();
      assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
                 resource.getState(), is(AgentState.busy));
      TaskDto task = t.get();
      assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
                 task.getState(), is(TaskState.assigned));
      t.setState(TaskState.completed);
      q.create(new CreateQueueArg.Builder().description("queue description").predicate("1==1").build());
      resource = a.get();
      assertThat(String.format("Check agent state (%s) to be ready.", resource.getState()),
                 resource.getState(), is(AgentState.ready));
      last--;
    }
  }

  @Test
  @DisplayName("Create new agent and complete a task.")
  public void agentHandlesTask() throws MalformedURLException, InterruptedException {
    a.create("en");
    AgentDto resource = a.get();
    assertThat(String.format("Check attribute language (%s) is 'en'.",
        ((StringAttributeValueDto) resource.getCapabilities().get("language")).getValue()),
        ((StringAttributeValueDto) resource.getCapabilities().get("language")).getValue(),
        is("en"));
    assertThat(String.format("Check state (%s) to be offline.", resource.getState()),
        resource.getState(), is(AgentState.offline));
    assertThat(q.size(), is(0));
    a.setState(AgentState.ready);

    t.createQueueTask();
    assertThat(q.size(), is(0));
    completeTask();
    t.delete();
    a.setState(AgentState.offline);
  }

  //@Test
  @DisplayName("Create new agent and complete two tasks.")
  public void agentHandlesTwoTasks() throws MalformedURLException, InterruptedException {
    a.create("en");
    a.setState(AgentState.ready);
    t.createQueueTask();
    completeTask();
    t.delete();
    t.createQueueTask();
    completeTask();
    t.delete();
  }

  private URL testServer() throws MalformedURLException {
    return new URL("http://localhost:" + server.getLocalPort());
  }

  @Test
  @DisplayName("Create new agent task - no ready agents, set agent ready to complete task.")
  public void agentOfflineTaskReady() throws MalformedURLException, InterruptedException, IOException {
    a.create("en");
    assertThat(q.size(), is(0));
    t.createQueueTask(testServer());
    assertThat(q.size(), is(1));
    a.setState(AgentState.ready);
    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString((state.get(CommsRouterResource.TASK)))));
    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));
    TaskDto task = t.get();
    assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
        task.getState(), is(TaskState.assigned));

    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(1);

    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be ready.", resource.getState()),
        resource.getState(), is(AgentState.ready));

    t.delete();
  }

  @Test
  @DisplayName("Create new agent and cancel the assigned task.")
  public void taskWithCancel() throws MalformedURLException, InterruptedException, IOException {
    a.create("en");
    assertThat(q.size(), is(0));

    t.createQueueTask(testServer());

    assertThat(q.size(), is(1));

    a.setState(AgentState.ready);

    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString((state.get(CommsRouterResource.TASK)))));

    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));
    TaskDto task = t.get();
    assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.waiting);
    assertThat(q.size(), is(1));
    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be unavailable as the task was canceled.",
        resource.getState()),
        resource.getState(), is(AgentState.unavailable));

    //TimeUnit.SECONDS.sleep(1);

    a.setState(AgentState.ready);
    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString((state.get(CommsRouterResource.TASK)))));


    resource = a.get();
    assertThat(String
            .format("Check agent state (%s) to be busy when there is a task.", resource.getState()),
        resource.getState(), is(AgentState.busy));
    t.setState(TaskState.completed);
    t.delete();
  }

  @Test
  @DisplayName("Check that bad url does not influence processing task.")
  public void taskRejectedBadCallbackUrl() throws MalformedURLException, InterruptedException {
    a.create("en");
    assertThat(q.size(), is(0));

    t.createQueueTask(new URL("http://not-existing-google.com/not-found"));
    assertThat(q.size(), is(1));

    a.setState(AgentState.ready);
    // callback TimeUnit.SECONDS.sleep(1);

    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));

    t.setState(TaskState.waiting);
    TaskDto task = t.get();
    assertThat(String.format("Check task state (%s) to be waiting.", task.getState()),
        task.getState(), is(TaskState.waiting));

    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be unavailable.", resource.getState()),
        resource.getState(), is(AgentState.unavailable));

    t.setState(TaskState.canceled);

    assertThat(q.size(), is(0));

    TimeUnit.SECONDS.sleep(2);

    a.setState(AgentState.ready);

    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be ready after it was unavailable.",
        resource.getState()),
        resource.getState(), is(AgentState.ready));

    t.delete();
  }

  @Test
  @DisplayName("Multiple agents compete for a task.")
  public void multipleAgentsPerTask() throws MalformedURLException, InterruptedException, IOException {

    ApiObjectRef ref1 = a.create("en");
    assertThat(q.size(), is(0));
    a.setState(AgentState.ready);
    TimeUnit.SECONDS.sleep(1);

    ApiObjectRef ref2 = a.create("en");
    assertThat(q.size(), is(0));
    a.setState(AgentState.ready);

    t.createQueueTask(testServer());
    TimeUnit.SECONDS.sleep(1);
    assertThat(q.size(), is(0));

    state.put(CommsRouterResource.AGENT, ref1.getRef());
    AgentDto resource = a.get();
    assertThat(String.format("First Agent1 is created after that Agent2. Check that fresh created task is handled by Agent1."
                             , resource.getState())
               , resource.getState()
               , is(AgentState.busy));

    t.setState(TaskState.completed);

    assertThat(q.size(), is(0));

    assertThat(waitToConnect(3000),allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                         containsString((state.get(CommsRouterResource.TASK)))));


    resource = a.get();
    assertThat(String
            .format("Check agent state (%s) to be ready after the task has been completed."
                    , resource.getState())
               , resource.getState()
               , is(AgentState.ready));

    t.createQueueTask();
    TimeUnit.SECONDS.sleep(1);
    assertThat(q.size(), is(0));

    state.put(CommsRouterResource.AGENT, ref2.getRef());

    assertThat("Check the oldest's agent state to be busy."
               , a.get().getState()
               , is(AgentState.busy));

    t.setState(TaskState.completed);
    assertThat(q.size(), is(0));

    TimeUnit.SECONDS.sleep(2);

    resource = a.get();
    assertThat(String
            .format("Check agent state (%s) to be ready after the task has been completed.",
                resource.getState()),
        resource.getState(), is(AgentState.ready));

    t.delete();
  }

  @Test
  @DisplayName("Multiple agents cancel task and go to the next.")
  public void multipleAgentsAndCancelTask() throws MalformedURLException, InterruptedException {

    ApiObjectRef ref1 = a.create("en");
    a.setState(AgentState.ready);
    TimeUnit.SECONDS.sleep(1);

    ApiObjectRef ref2 = a.create("en");
    a.setState(AgentState.ready);

    t.createQueueTask();
    TimeUnit.SECONDS.sleep(1);
    assertThat(q.size(), is(0));

    state.put(CommsRouterResource.AGENT, ref1.getRef());
    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));

    t.setState(TaskState.waiting);
    TimeUnit.SECONDS.sleep(1);
    assertThat(q.size(), is(0));

    state.put(CommsRouterResource.AGENT, ref2.getRef());

    resource = a.get();
    assertThat(String
            .format("Check that next agent takes care of the task and state (%s) to be busy.",
                resource.getState()),
        resource.getState(), is(AgentState.busy));
    t.setState(TaskState.completed);
    // TimeUnit.SECONDS.sleep(2);// in order to ensure enough time

    t.delete();
  }

  @Test
  @DisplayName("Multiple agents last busy starts a task.")
  public void multipleAgentsLastBusyStartsTask()
      throws MalformedURLException, InterruptedException {

    ApiObjectRef a1_ref = a.create("en");
    a.setState(AgentState.ready);
    CreatedTaskDto task1 = t.createQueueTask();
    TimeUnit.SECONDS.sleep(2);// in order to ensure enough time granularity
    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));

    ApiObjectRef a2_ref = a.create("en");
    assertThat(String.format("Router (%s): debug. a1(%s) a2(%s)"
        , state.get(CommsRouterResource.ROUTER), a1_ref.getRef(), a2_ref.getRef()), a1_ref.getRef(),
        not(is(a2_ref.getRef())));

    a.setState(AgentState.ready);
    assertThat(q.size(), is(0));

    CreatedTaskDto task2 = t.createQueueTask();
    TimeUnit.SECONDS.sleep(2);// in order to ensure enough time granularity
    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));
    TaskDto task = t.get();
    assertThat(String.format("Router (%s): Check task is assigned to the latest agent.a1(%s) a2(%s)"
        , state.get(CommsRouterResource.ROUTER), a1_ref.getRef(), a2_ref.getRef()), task.getAgentRef(),
        is(a2_ref.getRef()));

    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);// in order to ensure enough time granularity

    state.put(CommsRouterResource.TASK, task1.getRef());
    task = t.get();
    assertThat(String.format("Router (%s): Check task is assigned to the latest agent.a1(%s) a2(%s)"
        , state.get(CommsRouterResource.ROUTER), a1_ref.getRef(), a2_ref.getRef()), task.getAgentRef(),
        is(a1_ref.getRef()));

    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);// in order to ensure enough time granularity

    t.createQueueTask();
    TimeUnit.SECONDS.sleep(2);
    assertThat(q.size(), is(0));

    task = t.get();
    assertThat(String.format("Router (%s): Check task state (%s) to be assigned.",
        state.get(CommsRouterResource.ROUTER), task.getState()),
        task.getState(), is(TaskState.assigned));

    assertThat(String.format("Router (%s): Check task is assigned to the latest agent.a1(%s) a2(%s)",
            state.get(CommsRouterResource.ROUTER), a1_ref.getRef(), a2_ref.getRef()),
        task.getAgentRef(), is(a2_ref.getRef()));
    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);// in order to ensure enough time granularity

    t.delete();
  }

  @Test
  @DisplayName("Two tasks in a row.")
  public void twoTaskInARow() throws MalformedURLException, InterruptedException {

    ApiObjectRef a1_ref = a.create("en");
    assertThat(q.size(), is(0));
    CreatedTaskDto task1 = t.createQueueTask();
    TimeUnit.SECONDS.sleep(2);
    CreatedTaskDto task2 = t.createQueueTask();
    assertThat(q.size(), is(2));
    a.setState(AgentState.ready);
    TimeUnit.SECONDS.sleep(2);

    assertThat(q.size(), is(1));

    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));

    state.put(CommsRouterResource.TASK, task1.getRef());
    TaskDto task = t.get();
    assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    // callcack TimeUnit.SECONDS.sleep(2);

    resource = a.get();
    assertThat(String
            .format("Check agent state (%s) to be busy with the second task.", resource.getState()),
        resource.getState(), is(AgentState.busy));

    state.put(CommsRouterResource.TASK, task2.getRef());
    task = t.get();
    assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);

    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be ready when all tasks are completed.",
        resource.getState()),
        resource.getState(), is(AgentState.ready));
  }

  @Test
  @DisplayName("Create task, create agent, create task- first one should be handled first.")
  public void twoTaskBeforeAndAfterAgent() throws MalformedURLException, InterruptedException {
    CreatedTaskDto task1 = t.createQueueTask();
    TimeUnit.SECONDS.sleep(2);

    ApiObjectRef a1_ref = a.create("en");
    assertThat(q.size(), is(1));
    CreatedTaskDto task2 = t.createQueueTask();
    assertThat(q.size(), is(2));
    a.setState(AgentState.ready);
    TimeUnit.SECONDS.sleep(2);

    assertThat(q.size(), is(1));

    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be busy.", resource.getState()),
        resource.getState(), is(AgentState.busy));

    state.put(CommsRouterResource.TASK, task1.getRef());
    TaskDto task = t.get();
    assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);

    resource = a.get();
    assertThat(String
            .format("Check agent state (%s) to be busy with the second task.", resource.getState()),
        resource.getState(), is(AgentState.busy));

    state.put(CommsRouterResource.TASK, task2.getRef());
    task = t.get();
    assertThat(String.format("Check task state (%s) to be assigned.", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);

    resource = a.get();
    assertThat(String.format("Check agent state (%s) to be ready when all tasks are completed.",
        resource.getState()),
        resource.getState(), is(AgentState.ready));
  }

  @Test
  @DisplayName("Three tasks with different priority.")
  public void handleWithPriority() throws MalformedURLException, InterruptedException, IOException {
    a.create("en");
    p.create(new CreatePlanArg.Builder("priority 0")
        .defaultRoute(
            new RouteDto.Builder(state.get(CommsRouterResource.QUEUE)).priority(0L).build())
        .build());
    CreatedTaskDto task0 = t.createWithPlan(new CreateTaskArg.Builder()
                                            .callback(testServer())
        .build());
    p.create(new CreatePlanArg.Builder("priority 5")
        .defaultRoute(
            new RouteDto.Builder(state.get(CommsRouterResource.QUEUE)).priority(5L).build())
        .build());
    CreatedTaskDto task5 = t.createWithPlan(new CreateTaskArg.Builder()
                                            .callback(testServer())
        .build());
    p.create(new CreatePlanArg.Builder("priority 3")
        .defaultRoute(
            new RouteDto.Builder(state.get(CommsRouterResource.QUEUE)).priority(3L).build())
        .build());
    CreatedTaskDto task3 = t.createWithPlan(new CreateTaskArg.Builder()
                                            .callback(testServer())
        .build());

    assertThat(q.size(), is(3));
    a.setState(AgentState.ready);
    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString(task5.getRef())));

    assertThat(q.size(), is(2));
    TaskDto task;
    state.put(CommsRouterResource.TASK, task5.getRef());
    task = t.get();
    assertThat(String.format("Check task with highest priority is assigned (%s).", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString(task3.getRef())));


    state.put(CommsRouterResource.TASK, task3.getRef());
    task = t.get();
    assertThat(String.format("Check task with priority 3 is assigned (%s).", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);

    state.put(CommsRouterResource.TASK, task0.getRef());
    task = t.get();
    assertThat(String.format("Check task with priority 0 is assigned (%s).", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);

    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be ready when all tasks are completed.",
        resource.getState()),
        resource.getState(), is(AgentState.ready));
  }

  @Test
  @DisplayName("Three tasks two queues with different priority.")
  public void handleWithPriorityTwoQueues() throws MalformedURLException, InterruptedException, IOException {
    a.create("en");
    q.create(
        new CreateQueueArg.Builder().description("queue priority 0").predicate("1==1").build());
    p.create(new CreatePlanArg.Builder("priority 0")
        .defaultRoute(
            new RouteDto.Builder(state.get(CommsRouterResource.QUEUE)).priority(0L).build())
        .build());
    CreatedTaskDto task0 = t.createWithPlan(new CreateTaskArg.Builder()
                                            .callback(testServer())
        .build());
    q.create(
        new CreateQueueArg.Builder().description("queue priority 5").predicate("1==1").build());
    p.create(new CreatePlanArg.Builder("priority 5")
        .defaultRoute(
            new RouteDto.Builder(state.get(CommsRouterResource.QUEUE)).priority(5L).build())
        .build());
    CreatedTaskDto task5 = t.createWithPlan(new CreateTaskArg.Builder()
                                            .callback(testServer())
        .build());
    q.create(
        new CreateQueueArg.Builder().description("queue priority 3").predicate("1==1").build());
    p.create(new CreatePlanArg.Builder("priority 3")
        .defaultRoute(
            new RouteDto.Builder(state.get(CommsRouterResource.QUEUE)).priority(3L).build())
        .build());
    CreatedTaskDto task3 = t.createWithPlan(new CreateTaskArg.Builder()
                                            .callback(testServer())
        .build());
    a.setState(AgentState.ready);
    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString(task5.getRef())));

    TaskDto task;
    state.put(CommsRouterResource.TASK, task5.getRef());
    task = t.get();
    assertThat(String.format("Check task with highest priority is assigned (%s).", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString(task3.getRef())));


    state.put(CommsRouterResource.TASK, task3.getRef());
    task = t.get();
    assertThat(String.format("Check task with priority 3 is assigned (%s).", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    assertThat(waitToConnect(3000), allOf(containsString(state.get(CommsRouterResource.AGENT)),
                                          containsString(task0.getRef())));


    state.put(CommsRouterResource.TASK, task0.getRef());
    task = t.get();
    assertThat(String.format("Check task with priority 0 is assigned (%s).", task.getState()),
        task.getState(), is(TaskState.assigned));
    t.setState(TaskState.completed);
    TimeUnit.SECONDS.sleep(2);

    AgentDto resource = a.get();
    assertThat(String.format("Check agent state (%s) to be ready when all tasks are completed.",
        resource.getState()),
        resource.getState(), is(AgentState.ready));
  }

}
