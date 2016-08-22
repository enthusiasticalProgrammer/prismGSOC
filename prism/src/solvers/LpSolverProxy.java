//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	
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
import java.util.Map.Entry;

import prism.PrismException;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class LpSolverProxy implements SolverProxyInterface
{
	private LpSolve solver;
	int result;
	private boolean isAddrowMode = false;

	public LpSolverProxy(int numRealVars, int numBinaryVars) throws PrismException
	{
		try {
			solver = LpSolve.makeLp(0, numRealVars + numBinaryVars);

			for (int i = numRealVars + 1; i < numRealVars + numBinaryVars + 1; i++)
				solver.setBinary(i, true);

			solver.setVerbose(lpsolve.LpSolve.CRITICAL);
		} catch (LpSolveException ex) {
			throw new PrismException("Exception thrown when working with lpsolve solver: " + ex);
		}
	}

	@Override
	public void addRowFromMap(Map<Integer, Double> row, double rhs, Comparator op, String name) throws PrismException
	{
		if (!isAddrowMode) {
			isAddrowMode = true;
			solver.setAddRowmode(true);
		}

		try {
			if (row == null || row.size() == 0)
				return; //nothing to do

			int count = row.size();

			int[] indices = new int[count];
			double[] values = new double[count];

			int i = 0;
			for (Entry<Integer, Double> entry : row.entrySet()) {
				indices[i] = entry.getKey() + 1;
				values[i] = entry.getValue();
				i++;
			}
			int opG;
			if (op == SolverProxyInterface.Comparator.GE)
				opG = LpSolve.GE;
			else if (op == SolverProxyInterface.Comparator.LE)
				opG = LpSolve.LE;
			else if (op == SolverProxyInterface.Comparator.EQ)
				opG = LpSolve.EQ;
			else
				throw new UnsupportedOperationException("unknown comparison operator");

			solver.addConstraintex(count, values, indices, opG, rhs);
			solver.setRowName(solver.getNrows(), name);
		} catch (LpSolveException ex) {
			throw new PrismException("Exception thrown when working with lpsolve solver: " + ex);
		}
	}

	@Override
	public boolean solveIsPositive() throws PrismException
	{
		solve();
		return getDoubleResult() > 0;
	}

	@Override
	public int solve() throws PrismException
	{
		try {
			solver.setAddRowmode(false);
			this.result = solver.solve();
			return this.result;
		} catch (LpSolveException ex) {
			throw new PrismException("Exception thrown when working with lpsolve solver: " + ex);
		}
	}

	@Override
	public boolean getBoolResult() throws PrismException
	{
		if (this.result == lpsolve.LpSolve.INFEASIBLE)
			return false;
		else if (this.result == lpsolve.LpSolve.OPTIMAL)
			return true;
		else {
			throw new PrismException("Unexpected result of LP solving, when boolean value is expected: " + this.result);
		}
	}

	@Override
	public double getDoubleResult() throws PrismException
	{
		if (this.result == lpsolve.LpSolve.INFEASIBLE) {//TODO other results
			return Double.NaN;
		} else if (this.result == lpsolve.LpSolve.OPTIMAL) {
			try {
				double resultSolver = solver.getObjective();
				return resultSolver;
			} catch (LpSolveException ex) {
				throw new PrismException("Exception thrown when working with lpsolve solver: " + ex);
			}
		} else {
			throw new PrismException("Unexpected result of LP solving, when double value is expected: " + this.result);
		}
	}

	@Override
	public void setVarName(int idx, String name) throws PrismException
	{
		try {
			solver.setColName(idx + 1, name); //indexing +1
		} catch (LpSolveException ex) {
			throw new PrismException("Exception thrown when working with lpsolve solver: " + ex);
		}
	}

	@Override
	public void setVarBounds(int idx, double lo, double up) throws PrismException
	{
		try {
			solver.setBounds(idx + 1, lo, up); //indexing +1	
		} catch (LpSolveException ex) {
			throw new PrismException("Exception thrown when working with lpsolve solver: " + ex);
		}
	}

	@Override
	public void setObjFunct(Map<Integer, Double> row, boolean max) throws PrismException
	{
		try {
			if (row == null || row.size() == 0)
				return; //nothing to do

			int count = row.size();

			int[] indices = new int[count];
			double[] values = new double[count];

			int i = 0;
			for (Entry<Integer, Double> entry : row.entrySet()) {
				indices[i] = entry.getKey() + 1;
				values[i] = entry.getValue();
				i++;
			}

			solver.setObjFnex(count, values, indices);
			if (max)
				solver.setMaxim();
			else
				solver.setMinim();
		} catch (LpSolveException ex) {
			throw new PrismException("Exception thrown when working with lpsolve solver: " + ex);
		}
	}

	@Override
	public double[] getVariableValues()
	{
		try {
			return solver.getPtrVariables();
		} catch (LpSolveException ex) {
			throw new RuntimeException("Exception thrown when working with lpsolve solver: " + ex);
		}
	}
}