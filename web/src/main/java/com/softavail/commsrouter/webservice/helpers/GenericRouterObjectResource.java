/*
 * Copyright 2017 SoftAvail Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softavail.commsrouter.webservice.helpers;

import com.google.common.collect.Lists;

import com.softavail.commsrouter.api.dto.misc.PaginatedList;
import com.softavail.commsrouter.api.dto.model.ApiObjectRef;
import com.softavail.commsrouter.api.dto.model.RouterObjectRef;
import com.softavail.commsrouter.api.exception.CommsRouterException;
import com.softavail.commsrouter.api.exception.ExceptionPresentation;
import com.softavail.commsrouter.api.interfaces.RouterObjectService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

/**
 * Created by @author mapuo on 04.09.17.
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public abstract class GenericRouterObjectResource<T extends RouterObjectRef>
    extends RouterObjectResource {

  private static final Logger LOGGER = LogManager.getLogger(GenericRouterObjectResource.class);

  protected abstract RouterObjectService<T> getService();

  protected URI getLocation(ApiObjectRef routerObject) {
    return entryPoint.clone()
        .path("{resourceId}").build(routerRef, routerObject.getRef());
  }

  protected Response createResponse(ApiObjectRef routerObject) {
    URI createLocation = getLocation(routerObject);

    return Response.status(Status.CREATED)
        .header(HttpHeaders.LOCATION, createLocation.toString())
        .entity(routerObject)
        .build();
  }

  @GET
  @ApiOperation(
      value = "List all resources",
      notes = "Default paging will be applied",
      responseContainer = "List")
  @ApiResponses(
      @ApiResponse(
          code = 200,
          message = "Successful operation",
          responseHeaders = {
              @ResponseHeader(
                  name = RouterObjectService.TOTAL_COUNT_HEADER,
                  description = "The total items from this listing",
                  response = Integer.class)}))
  public Response list(
      @ApiParam(
          value = "The current page of the listing",
          defaultValue = "1",
          allowableValues = "range[1, infinity]")
      @Valid
      @Min(value = 1L, message = "{resource.list.min.page.number}")
      @DefaultValue("01")
      @QueryParam(RouterObjectService.PAGE_NUMBER_PARAM) int pageNum,
      @ApiParam(
          value = "Number of items per page (Maximum 50)",
          defaultValue = "10",
          allowableValues = "range[1, 50]")
      @Valid
      @Min(value = 1L, message = "{resource.list.min.items.per.page}")
      @Max(value = 50, message = "{resource.list.max.items.per.page}")
      @DefaultValue("10")
      @QueryParam(RouterObjectService.ITEMS_PER_PAGE_PARAM) int perPage)
      throws CommsRouterException {

    PaginatedList<T> pagedList = getService().list(routerRef, pageNum, perPage);

    LOGGER.debug("Listing page {}/{} for router {}: {}",
        pageNum, perPage, routerRef, pagedList);

    PaginationHelper paginationHelper = new PaginationHelper(
        () -> entryPoint.clone(),
        (builder) -> builder.build(routerRef));
    Link[] links = paginationHelper.getLinks(pagedList);

    GenericEntity<List<T>> genericEntity = new GenericEntity<>(pagedList.getList(), List.class);
    return Response.ok()
        .header(RouterObjectService.TOTAL_COUNT_HEADER, pagedList.getTotalCount())
        .links(links)
        .entity(genericEntity)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .build();
  }

  @GET
  @Path("{resourceRef}")
  @ApiOperation(value = "Get resource by ID", notes = "Returns resource by the given ID")
  public T get(@PathParam("resourceRef") String resourceRef)
      throws CommsRouterException {

    RouterObjectRef routerObjectId = getRouterObjectRef(resourceRef);

    LOGGER.debug("Getting {}", routerObjectId);

    return getService().get(routerObjectId);
  }

  @DELETE
  @Path("{resourceRef}")
  @ApiOperation(value = "Deletes an existing resource by ID")
  @ApiResponses({
      @ApiResponse(code = 200, message = "Successful operation"),
      @ApiResponse(code = 400, message = "Invalid ID supplied",
          response = ExceptionPresentation.class),
      @ApiResponse(code = 404, message = "Router not found",
          response = ExceptionPresentation.class)})
  public void delete(@PathParam("resourceRef") String resourceRef)
      throws CommsRouterException {

    RouterObjectRef routerObjectRef = getRouterObjectRef(resourceRef);

    LOGGER.debug("Deleting {}", routerObjectRef);

    getService().delete(routerObjectRef);
  }

}
