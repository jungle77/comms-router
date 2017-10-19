/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */

package com.softavail.commsrouter.app;

import com.softavail.commsrouter.api.dto.model.AgentState;
import com.softavail.commsrouter.api.dto.model.TaskAssignmentDto;
import com.softavail.commsrouter.api.dto.model.TaskState;
import com.softavail.commsrouter.api.exception.AssignmentRejectedException;
import com.softavail.commsrouter.api.exception.CommsRouterException;
import com.softavail.commsrouter.api.exception.NotFoundException;
import com.softavail.commsrouter.api.interfaces.TaskEventHandler;
import com.softavail.commsrouter.domain.Agent;
import com.softavail.commsrouter.domain.Task;
import com.softavail.commsrouter.domain.dto.mappers.EntityMappers;
import com.softavail.commsrouter.jpa.JpaDbFacade;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

/**
 * @author ikrustev
 */
public class TaskDispatcher {

  private static final Logger LOGGER = LogManager.getLogger(TaskDispatcher.class);

  private final JpaDbFacade db;
  private final EntityMappers mappers;
  private final TaskEventHandler taskEventHandler;
  private final ScheduledThreadPoolExecutor threadPool;

  public TaskDispatcher(
      JpaDbFacade db,
      TaskEventHandler taskEventHandler,
      EntityMappers dtoMappers,
      int threadPoolSize) {

    this.db = db;
    this.mappers = dtoMappers;
    this.taskEventHandler = taskEventHandler;
    this.threadPool = new ScheduledThreadPoolExecutor(threadPoolSize);
    this.threadPool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
  }

  public void close() {
    // @todo: logs and config
    threadPool.shutdown();
    try {
      if (threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
        LOGGER.info("Dispatcher thread pool down.");
      } else {
        LOGGER.warn("Dispatcher thread pool shutdown timeout. Forcing ...");
        threadPool.shutdownNow();
        if (threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
          LOGGER.info("Dispatcher thread pool down after being forced.");
        } else {
          LOGGER.error("Dispatcher thread pool did not shut down.");
        }
      }
    } catch (InterruptedException ex) {
      LOGGER.warn("Interrupted while waiting for the dispatcher thread pool to go shut down.");
    }
  }

  public void dispatchTask(String taskId) {
    threadPool.submit(() -> {
      try {
        doDispatchTask(taskId);
      } catch (Exception ex) {
        LOGGER.error("Dispatch task {}: failure: {}", taskId, ex, ex);
      }
    });
  }

  public void dispatchAgent(String agentId) {
    threadPool.submit(() -> {
      try {
        doDispatchAgent(agentId);
      } catch (Exception ex) {
        LOGGER.error("Dispatch agent: {}: failure: {}", agentId, ex, ex);
      }
    });
  }

  private void redispatchAssignment(String taskId)
      throws CommsRouterException {

    db.transactionManager.execute(em -> rejectAssignment(em, taskId))
        .ifPresent(this::dispatchTask);
  }

  public Optional<String> rejectAssignment(EntityManager em, String taskId)
      throws NotFoundException {

    Task task = db.task.get(em, taskId);
    Agent agent = task.getAgent();

    if (task.getState().isAssigned() && agent != null) {
      agent.setState(AgentState.unavailable);
      task.setState(TaskState.waiting);
      task.setAgent(null);

      return Optional.of(task.getId());
    }

    return Optional.empty();
  }

  @SuppressWarnings("unchecked")
  private void doDispatchTask(String taskId) throws CommsRouterException {

    TaskAssignmentDto taskAssignment =
        db.transactionManager.executeWithLockRetry((EntityManager em) -> {

          String qlString = "SELECT t, a FROM Task t "
              + "JOIN t.queue q JOIN q.agents a WHERE t.id = :taskId and a.state = :agentState"
              + " ORDER BY t.priority DESC";

          List<Object[]> result = em.createQuery(qlString)
              .setParameter("taskId", taskId)
              .setParameter("agentState", AgentState.ready)
              .setMaxResults(1)
              .getResultList();

          if (result.isEmpty()) {
            LOGGER.info("Dispatch task {}: no suitable agent", taskId);
            return null;
          }

          Task task = (Task) result.get(0)[0];

          if (task.getState() != TaskState.waiting) {
            LOGGER.info("Dispatch task {}: task already taken", taskId);
            return null;
          }

          Agent agent = (Agent) result.get(0)[1];

          em.lock(task, LockModeType.OPTIMISTIC);
          em.lock(agent, LockModeType.OPTIMISTIC);

          assignTask(task, agent);

          return new TaskAssignmentDto(mappers.task.toDto(task), mappers.agent.toDto(agent));
        });

    if (taskAssignment != null) {
      LOGGER.info("Dispatch task {}: task {} assigned to agent {}", taskId,
          taskAssignment.getTask(), taskAssignment.getAgent());
      try {
        taskEventHandler.onTaskAssigned(taskAssignment);
      } catch (AssignmentRejectedException e) {
        redispatchAssignment(taskAssignment.getTask().getId());
      }
    } else {
      LOGGER.info("Dispatch task {}: miss", taskId);
    }
  }

  @SuppressWarnings("unchecked")
  private void doDispatchAgent(String agentId) throws CommsRouterException {

    TaskAssignmentDto taskAssignment = db.transactionManager.executeWithLockRetry((em) -> {

      String qlString = "SELECT t, a FROM Task t JOIN t.queue q JOIN q.agents a "
          + "WHERE a.id = :agentId and a.state = :agentState and t.state = :taskState "
          + "ORDER BY t.priority DESC";

      List<Object[]> result = em.createQuery(qlString)
          .setParameter("agentId", agentId)
          .setParameter("agentState", AgentState.ready)
          .setParameter("taskState", TaskState.waiting)
          .setMaxResults(1)
          .getResultList();

      if (result.isEmpty()) {
        return null;
      }

      Task task = (Task) result.get(0)[0];
      Agent agent = (Agent) result.get(0)[1];

      em.lock(task, LockModeType.OPTIMISTIC);
      em.lock(agent, LockModeType.OPTIMISTIC);

      assignTask(task, agent);
      return new TaskAssignmentDto(mappers.task.toDto(task), mappers.agent.toDto(agent));
    });

    if (taskAssignment != null) {
      LOGGER.info("Dispatch agent {}: task {} assigned to agent {}",
          agentId, taskAssignment.getTask(), taskAssignment.getAgent());
      try {
        taskEventHandler.onTaskAssigned(taskAssignment);
      } catch (AssignmentRejectedException e) {
        redispatchAssignment(taskAssignment.getTask().getId());
      }
    } else {
      LOGGER.info("Dispatch agent {}: no suitable task or agent already busy", agentId);
    }
  }

  private void assignTask(Task task, Agent agent) {
    agent.setState(AgentState.busy);
    task.setState(TaskState.assigned);
    task.setAgent(agent);
  }

}
