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

package com.softavail.commsrouter.api.interfaces;

import com.softavail.commsrouter.api.dto.misc.PaginatedList;
import com.softavail.commsrouter.api.dto.misc.PagingRequest;
import com.softavail.commsrouter.api.exception.CommsRouterException;

/**
 * Created by @author mapuo on 10.10.17.
 */
public interface PaginatedService<DTOENTITYT> extends ApiObjectService<DTOENTITYT> {

  String TOTAL_COUNT_HEADER = "X-Total-Count";
  String PAGE_NUMBER_PARAM = "page_num";
  String ITEMS_PER_PAGE_PARAM = "per_page";

  PaginatedList<DTOENTITYT> list(PagingRequest request)
      throws CommsRouterException;

}
