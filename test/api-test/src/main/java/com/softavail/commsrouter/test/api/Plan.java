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

package com.softavail.commsrouter.test.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

import com.softavail.commsrouter.api.dto.arg.CreatePlanArg;
import com.softavail.commsrouter.api.dto.arg.UpdatePlanArg;
import com.softavail.commsrouter.api.dto.model.ApiObjectRef;
import com.softavail.commsrouter.api.dto.model.PlanDto;
import io.restassured.response.ValidatableResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Plan extends Resource {

  private static final Logger LOGGER = LogManager.getLogger(Plan.class);

  public Plan(HashMap<CommsRouterResource, String> state) {
    super(state);
  }

  public List<PlanDto> list() {
    PlanDto[] routers = given()
        .pathParam("routerRef", state().get(CommsRouterResource.ROUTER))
        .when().get("/routers/{routerRef}/plans")
        .then().statusCode(200)
        .extract().as(PlanDto[].class);
    return Arrays.asList(routers);
  }

  public ApiObjectRef replace(CreatePlanArg args) {
    String ref = state().get(CommsRouterResource.PLAN);
    ApiObjectRef oid = given()
        .contentType("application/json")
        .pathParam("routerRef", state().get(CommsRouterResource.ROUTER))
        .pathParam("ref", ref)
        .body(args)
        .when().put("/routers/{routerRef}/plans/{ref}")
        .then().statusCode(201)
        .extract()
        .as(ApiObjectRef.class);
    state().put(CommsRouterResource.PLAN, oid.getRef());
    return oid;
  }

  public ApiObjectRef create(CreatePlanArg args) {
    ApiObjectRef oid = given().pathParam("routerRef", state().get(CommsRouterResource.ROUTER))
        .contentType("application/json")
        .body(args)
        .when().post("/routers/{routerRef}/plans")
        .then().statusCode(201)
        .body("ref", not(isEmptyString()))
        .extract()
        .as(ApiObjectRef.class);
    String id = oid.getRef();
    state().put(CommsRouterResource.PLAN, id);
    return oid;
  }

  public void delete() {
    deleteResponse().statusCode(204);
  }

  public ValidatableResponse deleteResponse() {
    String ref = state().get(CommsRouterResource.PLAN);
    return given()
        .pathParam("routerRef", state().get(CommsRouterResource.ROUTER))
        .pathParam("ref", ref)
        .when().delete("/routers/{routerRef}/plans/{ref}")
        .then();
  }

  public PlanDto get() {
    String ref = state().get(CommsRouterResource.PLAN);
    return given()
        .pathParam("routerRef", state().get(CommsRouterResource.ROUTER))
        .pathParam("ref", ref)
        .when().get("/routers/{routerRef}/plans/{ref}")
        .then().statusCode(200)
        .body("ref", equalTo(ref))
        .extract()
        .as(PlanDto.class);
  }

  public ValidatableResponse updateResponse(UpdatePlanArg args) {
    return given()
          .contentType("application/json")
          .pathParam("routerRef", state().get(CommsRouterResource.ROUTER))
          .pathParam("ref", state().get(CommsRouterResource.PLAN))
          .body(args)
          .when().post("/routers/{routerRef}/plans/{ref}")
          .then();
  }

  public void update(UpdatePlanArg args) {
    updateResponse(args).statusCode(204);
  }

}
