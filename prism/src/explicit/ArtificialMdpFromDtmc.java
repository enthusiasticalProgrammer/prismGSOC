//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Christopher Ziegler <ga25suc@mytum.de>
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

package explicit;

import java.io.File;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

/**
 * Basically this class tries to take a DTMC and it emulates it as MDP,
 * it est it supports the nondeterministic choices etc. in a way such that
 * there are choices, but only one. It is rather a wrapper-class without adding much functionality.
 */
public class ArtificialMdpFromDtmc implements MDP
{
	private final DTMC dtmc;

	/**
	 * @param dtmc the underlying DTMC
	 */
	public ArtificialMdpFromDtmc(DTMC dtmc)
	{
		this.dtmc = dtmc;
	}

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	@Override
	public int getNumStates()
	{
		return dtmc.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return dtmc.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return dtmc.getInitialStates();
	}

	@Override
	public int getFirstInitialState()
	{
		return dtmc.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(int i)
	{
		return dtmc.isInitialState(i);
	}

	@Override
	public int getNumDeadlockStates()
	{
		return dtmc.getNumDeadlockStates();
	}

	@Override
	public Iterable<Integer> getDeadlockStates()
	{
		return dtmc.getDeadlockStates();
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		return dtmc.getDeadlockStatesList();
	}

	@Override
	public int getFirstDeadlockState()
	{
		return dtmc.getFirstDeadlockState();
	}

	@Override
	public boolean isDeadlockState(int i)
	{
		return dtmc.isDeadlockState(i);
	}

	@Override
	public List<State> getStatesList()
	{
		return dtmc.getStatesList();
	}

	@Override
	public VarList getVarList()
	{
		return dtmc.getVarList();
	}

	@Override
	public Values getConstantValues()
	{
		return dtmc.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		return dtmc.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return dtmc.getLabels();
	}

	@Override
	public boolean hasLabel(String name)
	{
		return dtmc.hasLabel(name);
	}

	@Override
	public int getNumTransitions()
	{
		return dtmc.getNumTransitions();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int s)
	{
		return dtmc.getSuccessorsIterator(s);
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		return dtmc.isSuccessor(s1, s2);
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		return dtmc.allSuccessorsInSet(s, set);
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		return dtmc.someSuccessorsInSet(s, set);
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		dtmc.findDeadlocks(fix);
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		dtmc.checkForDeadlocks();
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		dtmc.checkForDeadlocks(except);
	}

	@Override
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		dtmc.exportToPrismExplicit(baseFilename);
	}

	@Override
	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		dtmc.exportToPrismExplicitTra(filename);
	}

	@Override
	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		dtmc.exportToPrismExplicitTra(file);
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog log)
	{
		dtmc.exportToPrismExplicitTra(log);
	}

	@Override
	public void exportToDotFile(String filename) throws PrismException
	{
		dtmc.exportToDotFile(filename);
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		dtmc.exportToDotFile(filename, mark);
	}

	@Override
	public void exportToDotFile(PrismLog out)
	{
		dtmc.exportToDotFile(out);
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark)
	{
		dtmc.exportToDotFile(out, mark);
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark, boolean showStates)
	{
		dtmc.exportToDotFile(out, mark, showStates);
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		dtmc.exportToPrismLanguage(filename);
	}

	@Override
	public void exportStates(int exportType, VarList varList, PrismLog log) throws PrismException
	{
		dtmc.exportStates(exportType, varList, log);
	}

	@Override
	public String infoString()
	{
		return dtmc.infoString();
	}

	@Override
	public String infoStringTable()
	{
		return dtmc.infoStringTable();
	}

	@Override
	public boolean hasStoredPredecessorRelation()
	{
		return dtmc.hasStoredPredecessorRelation();
	}

	@Override
	public PredecessorRelation getPredecessorRelation(PrismComponent parent, boolean storeIfNew)
	{
		return dtmc.getPredecessorRelation(parent, storeIfNew);
	}

	@Override
	public void clearPredecessorRelation()
	{
		dtmc.clearPredecessorRelation();
	}

	@Override
	public int getNumChoices(int s)
	{
		return 1;
	}

	@Override
	public int getMaxNumChoices()
	{
		return 1;
	}

	@Override
	public int getNumChoices()
	{
		return 1;
	}

	@Override
	public Object getAction(int s, int i)
	{
		return null;
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return false;
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		Iterator<Integer> successors = dtmc.getSuccessorsIterator(s);
		int result = 0;
		for (; successors.hasNext(); successors.next()) {
			result++;
		}
		return result;
	}

	@Override
	public boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		Iterator<Integer> successors = dtmc.getSuccessorsIterator(s);
		while (successors.hasNext()) {
			Integer succ = successors.next();
			if (!set.get(succ)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(int s, int i, BitSet set)
	{
		Iterator<Integer> successors = dtmc.getSuccessorsIterator(s);
		while (successors.hasNext()) {
			Integer succ = successors.next();
			if (set.get(succ)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int s, int i)
	{
		if (i != 0) {
			throw new IllegalArgumentException();
		}
		return dtmc.getSuccessorsIterator(s);
	}

	@Override
	public Model constructInducedModel(MDStrategy strat)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat)
	{
		throw new UnsupportedOperationException();

	}

	//------------------------------MDP-methods---------------------------------------------

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int state, int action)
	{
		if (action != 0) {
			throw new IllegalArgumentException();
		}
		return dtmc.getTransitionsIterator(state);
	}

	@Override
	public void prob0step(BitSet subset, BitSet u, boolean forall, BitSet result)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public void prob1Astep(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public void prob1Estep(BitSet subset, BitSet u, BitSet v, BitSet result, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public void prob1step(BitSet subset, BitSet u, BitSet v, boolean forall, BitSet result)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public boolean prob1stepSingle(int s, int i, BitSet u, BitSet v)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public void mvMultMinMax(double[] vect, boolean min, double[] result, BitSet subset, boolean complement, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultMinMaxSingle(int s, double[] vect, boolean min, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public List<Integer> mvMultMinMaxSingleChoices(int s, double[] vect, boolean min, double val)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultSingle(int s, int i, double[] vect)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultGSMinMax(double[] vect, boolean min, BitSet subset, boolean complement, boolean absolute, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultJacMinMaxSingle(int s, double[] vect, boolean min, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultJacSingle(int s, int i, double[] vect)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public void mvMultRewMinMax(double[] vect, MDPRewards mdpRewards, boolean min, double[] result, BitSet subset, boolean complement, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultRewMinMaxSingle(int s, double[] vect, MDPRewards mdpRewards, boolean min, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultRewSingle(int s, int i, double[] vect, MCRewards mcRewards)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultRewGSMinMax(double[] vect, MDPRewards mdpRewards, boolean min, BitSet subset, boolean complement, boolean absolute, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public double mvMultRewJacMinMaxSingle(int s, double[] vect, MDPRewards mdpRewards, boolean min, int[] strat)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double[] vect, MDPRewards mdpRewards, boolean min, double val)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}

	@Override
	public void mvMultRight(int[] states, int[] strat, double[] source, double[] dest)
	{
		throw new UnsupportedOperationException("Not yet implemented, because it is not needed in this context");
	}
}
