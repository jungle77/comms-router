/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */

package com.softavail.commsrouter.api.dto.arg;

import com.softavail.commsrouter.api.dto.model.RouteDto;
import com.softavail.commsrouter.api.dto.model.RuleDto;

import java.util.List;

/**
 *
 * @author ikrustev
 */
public class CreatePlanArg {

  private String description;
  private List<RuleDto> rules;
  private RouteDto defaultRoute;


  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the rules
   */
  public List<RuleDto> getRules() {
    return rules;
  }

  /**
   * @param rules the rules to set
   */
  public void setRules(List<RuleDto> rules) {
    this.rules = rules;
  }

  public RouteDto getDefaultRoute() {
    return defaultRoute;
  }

  public void setDefaultRoute(RouteDto defaultRoute) {
    this.defaultRoute = defaultRoute;
  }

  public static class Builder{
    private CreatePlanArg planArg = new CreatePlanArg();

    public Builder(String description) {
      planArg.setDescription(description);
    }

    public Builder rules(List<RuleDto> rules) {
      planArg.setRules(rules);
      return this;
    }

    public Builder defaultRoute(RouteDto defaultRoute) {
      planArg.setDefaultRoute(defaultRoute);
      return this;
    }

    public CreatePlanArg build() {
      return planArg;
    }

  }

}
