/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */

package com.softavail.commsrouter.api.dto.model.attribute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

/**
 *
 * @author ikrustev
 */
@JsonDeserialize(using = AttributeValueDeserializer.class)
@JsonSerialize(using = AttributeValueSerializer.class)
public abstract class AttributeValueDto {

  private Boolean scalar = true;

  @Override
  public String toString() {
    return getValueAsString();
  }


  public abstract String getValueAsString();

  public abstract void accept(AttributeValueVisitor visitor) throws IOException;

  public Boolean isScalar() {
    return scalar;
  }

  public void setScalar(Boolean scalar) {
    this.scalar = scalar;
  }

}
