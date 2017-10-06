package com.softavail.commsrouter.webservice.resources;

import com.softavail.commsrouter.api.dto.arg.CreateTaskArg;
import com.softavail.commsrouter.api.dto.arg.UpdateTaskArg;
import com.softavail.commsrouter.api.dto.facade.CreatedTaskFacade;
import com.softavail.commsrouter.api.dto.model.CreatedTaskDto;
import com.softavail.commsrouter.api.dto.model.RouterObjectId;
import com.softavail.commsrouter.api.dto.model.TaskDto;
import com.softavail.commsrouter.api.exception.CommsRouterException;
import com.softavail.commsrouter.api.interfaces.RouterObjectService;
import com.softavail.commsrouter.api.interfaces.TaskService;
import com.softavail.commsrouter.webservice.helpers.GenericRouterObjectResource;
import com.softavail.commsrouter.webservice.mappers.ExceptionPresentation;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

/**
 * Created by @author mapuo on 31.08.17.
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
@Api("/tasks")
public class TaskResource extends GenericRouterObjectResource<TaskDto> {

  private static final Logger LOGGER = LogManager.getLogger(TaskResource.class);

  private static final String X_QUEUE_SIZE = "X-Queue-Size";

  @Inject
  TaskService taskService;

  @Context
  private ResourceContext resourceContext;

  @Override
  protected RouterObjectService<TaskDto> getService() {
    return taskService;
  }

  @Override
  protected Response createResponse(TaskDto task) {
    ResponseBuilder builder = Response.status(Status.CREATED)
        .header(HttpHeaders.LOCATION, getLocation(task).toString())
        .entity(new CreatedTaskFacade(task))
        .type(MediaType.APPLICATION_JSON_TYPE);

    if (task instanceof CreatedTaskDto) {
      Long queueTasks = ((CreatedTaskDto) task).getQueueTasks();
      builder.header(X_QUEUE_SIZE, queueTasks);
    }

    return builder.build();
  }

  @POST
  @ApiOperation(
      value = "Add new Task",
      notes = "Create a new Task within a Router",
      response = CreatedTaskFacade.class,
      code = 201)
  @ApiResponses(
      @ApiResponse(
          code = 201,
          responseHeaders = {
              @ResponseHeader(
                  name = HttpHeaders.LOCATION,
                  response = URL.class,
                  description = "The path to the newly created resource"),
              @ResponseHeader(
                  name = X_QUEUE_SIZE,
                  response = Long.class,
                  description = "The number of tasks in the queue before that one")},
          message = "Created successfully"))
  public Response create(CreateTaskArg taskArg)
      throws CommsRouterException {

    RouterObjectId objectId = RouterObjectId.builder()
        .setRouterId(routerId)
        .build();

    LOGGER.debug("Creating Task: {}", taskArg);

    TaskDto task = taskService.create(taskArg, objectId);

    return createResponse(task);
  }

  @POST
  @Path("{resourceId}")
  @ApiOperation(
      code = 204,
      value = "Update an existing Task",
      notes = "Update some properties of an existing Task")
  @ApiResponses({
      @ApiResponse(code = 204, message = "Successful operation"),
      @ApiResponse(code = 400, message = "Invalid ID supplied",
          response = ExceptionPresentation.class),
      @ApiResponse(code = 404, message = "Task not found",
          response = ExceptionPresentation.class),
      @ApiResponse(code = 405, message = "Validation exception",
          response = ExceptionPresentation.class)})
  public void update(@PathParam("resourceId") String resourceId, UpdateTaskArg taskArg)
      throws CommsRouterException {

    RouterObjectId objectId = getRouterObjectId(resourceId);

    LOGGER.debug("Updating task: {}", taskArg);

    taskService.update(taskArg, objectId);
  }

  @PUT
  @Path("{resourceId}")
  @ApiOperation(
      code = 201,
      value = "Replace an existing Task",
      notes = "If the task with the specified id does not exist, it creates it")
  @ApiResponses({
      @ApiResponse(code = 201, message = "Successful operation"),
      @ApiResponse(code = 400, message = "Invalid ID supplied",
          response = ExceptionPresentation.class),
      @ApiResponse(code = 404, message = "Task not found",
          response = ExceptionPresentation.class),
      @ApiResponse(code = 405, message = "Validation exception",
          response = ExceptionPresentation.class)})
  public Response replace(
      @ApiParam(value = "The id of the task to be replaced", required = true)
      @PathParam("resourceId") String resourceId,
      @ApiParam("CreateTaskArg object specifying all the parameters") CreateTaskArg taskArg)
      throws CommsRouterException {

    LOGGER.debug("Replacing task: {}, with id: {}", taskArg, resourceId);

    RouterObjectId objectId = getRouterObjectId(resourceId);

    TaskDto task = taskService.replace(taskArg, objectId);

    return createResponse(task);
  }

  // Sub-resources

  @Path("{resourceId}/user_context")
  @ApiOperation(value = "/user_context", response = UserContextResource.class)
  public UserContextResource userContextResource(@PathParam("resourceId") String resourceId) {
    LOGGER.debug("Router {} Task {} Context", routerId, resourceId);

    RouterObjectId routerObjectId = getRouterObjectId(resourceId);

    UserContextResource resource = resourceContext.getResource(UserContextResource.class);
    resource.setRouterObject(routerObjectId);

    return resource;
  }

}
