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

package com.softavail.commsrouter.eval;

import net.sourceforge.jeval.EvaluationConstants;
import net.sourceforge.jeval.Evaluator;
import net.sourceforge.jeval.function.Function;
import net.sourceforge.jeval.function.FunctionConstants;
import net.sourceforge.jeval.function.FunctionException;
import net.sourceforge.jeval.function.FunctionHelper;
import net.sourceforge.jeval.function.FunctionResult;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 *
 * @author Ergyun Syuleyman
 * 
 *         This class is a function that executes within Evaluator. The function check for the first
 *         object item exists or not in the second array parameter. See the
 *         ArrayList.contains(Object) method in the JDK for a complete description of how this
 *         function works.
 */
public class InFunction implements Function {

  final private ExpressionEvaluator exprEvaluator;

  public InFunction(ExpressionEvaluator evaluator) {
    this.exprEvaluator = evaluator;
  }

  /**
   * Returns the name of the function - "IN".
   * 
   * @return The name of this function class.
   */
  @Override
  public String getName() {
    return "IN";
  }

  /**
   * Executes the function for the specified argument. This method is called internally by
   * Evaluator.
   * 
   * @param evaluator An instance of Evaluator.
   * @param arguments A string argument that will be converted into two string arguments. The first
   *        argument is the item object for check existing or not in the the second array argument
   *        and second argument is the array represented as string. The string argument(s) HAS to be
   *        enclosed in quotes. White space that is not enclosed within quotes will be trimmed.
   *        Quote characters in the first and last positions of any string argument (after being
   *        trimmed) will be removed also. The quote characters used must be the same as the quote
   *        characters used by the current instance of Evaluator. If there are multiple arguments,
   *        they must be separated by a comma (",").
   * 
   * @return Returns an integer value of zero if the exist in the array, an integer value greater
   *         than zero if the array does not contains the object.
   * 
   * @exception FunctionException Thrown if the argument(s) are not valid for this function.
   */
  @Override
  public FunctionResult execute(final Evaluator evaluator, final String arguments)
      throws FunctionException {
    Integer result = null;
    String exceptionMessage = "Two arguments as strings required.";

    ArrayList<?> strings =
        FunctionHelper.getStrings(arguments, EvaluationConstants.FUNCTION_ARGUMENT_SEPARATOR);

    if (strings.size() != 2) {
      throw new FunctionException(exceptionMessage);
    }

    String argumentTwo = (String) strings.get(1);
    try {
      if (exprEvaluator.isValidation()) {
        String variable = EvaluatorHelpers.validationTryReplaceArrayVariable(argumentTwo);
        if (variable != null) {
          argumentTwo = variable;
        }
      } else {
        argumentTwo = EvaluatorHelpers.trySupportSingleArraysElement(argumentTwo);
      }
      String argumentOne = EvaluatorHelpers.trimAndRemoveQuoteCharsIfNeed((String) strings.get(0),
          evaluator.getQuoteCharacter());
      boolean isDouble = EvaluatorHelpers.isDouble(argumentOne);
      argumentTwo = EvaluatorHelpers.trimAndRemoveQuoteCharsIfNeed(argumentTwo,
          evaluator.getQuoteCharacter());
      ArrayList<String> list = new ArrayList<>();
      JSONArray jsonArray = new JSONArray(argumentTwo);
      int len = jsonArray.length();
      for (int i = 0; i < len; i++) {
        String item = jsonArray.get(i).toString();
        if (isDouble) {
          String boolVariable = EvaluatorHelpers.resolveBooleanVariable(item);
          if (boolVariable != null) {
            item = boolVariable;
          } else {
            item = Double.valueOf(item).toString();
          }
        }
        list.add(item);
      }
      result = (list.contains(argumentOne) ? 1 : 0);
    } catch (JSONException e) {
      throw new FunctionException(String.format("function %s() second argument is \"%s\": %s",
          getName(), argumentTwo, e.getMessage()));
    } catch (NumberFormatException e) {
      throw new FunctionException(exceptionMessage, e);
    }

    return new FunctionResult(result.toString(), FunctionConstants.FUNCTION_RESULT_TYPE_NUMERIC);
  }
}
