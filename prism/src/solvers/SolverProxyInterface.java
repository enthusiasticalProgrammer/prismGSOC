//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package solvers;

import java.util.Map;
import prism.PrismException;

/**
 * Classes implementing this interface should provide access to LP solvers.
 * The intention is to have one common API for calling solvers, independent
 * of the actual LP solver used.
 */
public interface SolverProxyInterface
{
	/**
	 * This enum  lists the possible comparator operators for the LP.
	 */
	public static enum Comparator {
		EQ, GE, LE
	}

	/**
	 * Adds new row to the problem.
	 * 
	 * @param row Gives the left hand side, where the entry (i,d) says that column i should have constant d. Here i is indexed from 0
	 * @param rhs Right hand side constant of the row
	 * @param eq The operator, either EQ, GE or LE
	 * @param name Name to be given t
	 * @throws PrismException if the LP-solver throws an exception
	 */
	public void addRowFromMap(Map<Integer, Double> row, double rhs, Comparator eq, String name) throws PrismException;

	/**
	 * Solves the LP problem, and checks if the solution is positive
	 * 
	 * @return true if the solution of the LP is positive and else false
	 * @throws PrismException if something during the LP-solving goes wrong
	 */
	public boolean solveIsPositive() throws PrismException;

	/**
	 * Solves the previously generated LP problem
	 * 
	 * @return a code defined in lpsolve.LpSolve indicating the outcome (e.g. infeasible, optimal, ...)
	 * @throws PrismException if something goes wrong while solving the LP
	 */
	public int solve() throws PrismException;

	/**
	 * Returns the boolean result. Useful when
	 * only constraints were given, and no objective.
	 * @return true if the LP is solvable and false if not
	 * @throws PrismException if there happens to be an unexpected result-status of the lP
	 */
	public boolean getBoolResult() throws PrismException;

	/**
	 * @return value of the objective function. If infeasible, returns NaN
	 * @throws PrismException if an unexpected result-status of the LP is present or if the solver throws an exception
	 */
	public double getDoubleResult() throws PrismException;

	/**
	 * Sets name of the variable (=column) idx, indexed from 0. Useful for debugging.
	 * 
	 * @param idx the desired index
	 * @param name the name which the variable is chosen to have
	 * @throws PrismException if the LP-solver throws an exception
	 */
	public void setVarName(int idx, String name) throws PrismException;

	/**
	 * Sets lower and upper bounds for the variable values
	 * 
	 * @param idx The variable to be changed, indexed from .
	 * @param lo Lower bound on the value
	 * @param up Upper bound on the value
	 * @throws PrismException if the LP-solver throws an exception
	 */
	public void setVarBounds(int idx, double lo, double up) throws PrismException;

	/**
	 * Sets the objective function.
	 * 
	 * @param row the corresponding row
	 * @param max true for maximisation, false for minimisation
	 * @throws PrismException if the solver throws an exception
	 */
	public void setObjFunct(Map<Integer, Double> row, boolean max) throws PrismException;

	/**
	 * Gets the array of variable values. Should be called only after solve was called. 
	 * 
	 * @return the variable values such that getVariableValues()[i] is the value of the i-th variable. 
	 */
	public double[] getVariableValues();
}
