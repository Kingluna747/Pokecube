/*****************************************************************************

JEP - Java Math Expression Parser 2.3.1
      January 26 2006
      (c) Copyright 2004, Nathan Funk and Richard Morris
      See LICENSE.txt for license information.

*****************************************************************************/
package org.nfunk.jep.function;

import java.util.Stack;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.type.Complex;

/**
 * Log bass 10.
 * <p>
 * RJM change return real results for positive real arguments.
 * Speedup by using static final fields.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class Logarithm extends PostfixMathCommand
{
	private static final double LOG10 = Math.log(10);
	private static final Complex CLOG10 = new Complex(Math.log(10),0);
	
	public Logarithm() {
		numberOfParameters = 1;
	}

	@Override
	public void run(Stack inStack) throws ParseException {
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(log(param));//push the result on the inStack
		return;
	}
	

	public Object log(Object param) throws ParseException 
	{
		if (param instanceof Complex) {
		   return ((Complex)param).log().div(CLOG10);
		}
		else if (param instanceof Number) 
		{
			double num = ((Number) param).doubleValue();
			if( num > 0)
				return new Double(Math.log(num)/LOG10);
			else
			{	
				Complex temp = new Complex(num);
				return temp.log().div(CLOG10);
			}
		}
		throw new ParseException("Invalid parameter type");
	}
	

}
