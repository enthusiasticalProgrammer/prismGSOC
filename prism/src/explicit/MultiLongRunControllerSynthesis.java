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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import acceptance.AcceptanceControllerSynthesis;
import acceptance.AcceptanceControllerSynthesis.AccControllerPair;
import acceptance.AcceptanceControllerSynthesis.MDPCondition;
import acceptance.AcceptanceGenRabinTransition.GenRabinPair;
import common.IterableBitSet;
import ltl.parser.Comparison;
import prism.PrismException;
import solvers.SolverProxyInterface;
import solvers.SolverProxyInterface.Comparator;

/**
 * The functionality of this class is described in "Controller synthesis for MDPs and Frequency LTL\GU".
 * We compute here all states, which are in any MEC regarding a generalized Rabin acceptance with MDP-conditions
 */
public class MultiLongRunControllerSynthesis
{
	private final MDP model;
	private final AcceptanceControllerSynthesis acceptance;

	/**
	 * method for LP solving to be used
	 */
	private final String method;

	/**
	 * xOffset[i][s] is the solver's variable (column) for the first action of state s and MP-condition i, i.e. for x_{i,s,0}
	 */
	private int[][] xOffsetArr;

	public MultiLongRunControllerSynthesis(MDP model, AcceptanceControllerSynthesis acceptance, String method)
	{
		this.model = model;
		this.acceptance = acceptance;
		this.method = method;
	}

	private int[][] computeXOffsets(AcceptanceControllerSynthesis.AccControllerPair pair)
	{
		int[][] result = new int[acceptance.accList.size()][model.getNumStates()];
		int currentIndex = 0;
		for (int acceptanceCondition = 0; acceptanceCondition < necessaryNFromPaperForAcceptance(pair); acceptanceCondition++) {
			for (int state = 0; state < model.getNumStates(); state++) {
				result[acceptanceCondition][state] = currentIndex;
				currentIndex += model.getNumChoices(state);
			}
		}
		return result;
	}

	/**
	 * This method returns basically max(amount of lim-sup-conditions, 1).
	 * It is necessary, that this method returns at least 1, because otherwise the LP
	 * had no equation even though there are some lim-inf-conditions present. 
	 */
	private int necessaryNFromPaperForAcceptance(AcceptanceControllerSynthesis.AccControllerPair pair)
	{
		int size = (int) pair.mdpCondition.stream().filter(mdp -> !mdp.isLimInf).count();
		if (size == 0) {
			return 1;
		}
		return size;
	}

	/**
	 * This method computes the set of all states, which are located in any accepting MEC
	 * according to paper "Controller synthesis for MDPs and Frequency LTL(\GU)"
	 * @result A BitSet containing each state, which is inside of any MEC
	 * @throws PrismException might get thrown when something during ECCComputation fails
	 */
	public BitSet computeStatesInAcceptingMECs() throws PrismException
	{
		BitSet result = new BitSet();
		for (GenRabinPair acc : acceptance.accList) {
			List<BitSet> mecsRegardingThisPair = computeMecsForPair(acc);
			if (acc instanceof AccControllerPair) {
				AccControllerPair pair = (AccControllerPair) acc;
				xOffsetArr = computeXOffsets(pair);
				for (BitSet mec : mecsRegardingThisPair) {
					SolverProxyInterface solver = AbstractLPStakeholder.initialiseSolver(computeNumLPVars(pair), method);
					makeLpForMec(mec, pair, solver);
					solver.solve();
					if (solver.getBoolResult()) {
						result.or(mec);
					}
				}
			} else {
				//should never occur
				throw new PrismException("A horrible bug has happened. Please check acceptance.AcceptanceControllerSynthesis to fix it");
			}
		}
		return result;
	}

	private int computeNumLPVars(AccControllerPair pair)
	{
		int result = 0;
		for (int state = 0; state < model.getNumStates(); state++) {
			result += model.getNumChoices(state);
		}
		result *= this.necessaryNFromPaperForAcceptance(pair);
		return result;
	}

	/**
	 * This method feeds the solver with all equations from paper "Controller synthesis for MDPs and Frequency LTL\GU"
	 * Furthermore it sets x_{i,a} to zero if we cannot use Action a if we want to stay in the MEC forever.
	 * 
	 *  @param mec The MEC, for which we set up the equation
	 *  @param pair The generalised Rabin pair with MDP-condition for which we set up the equations
	 *  @param solver The solver which we feed the equations
	 *  @throws PrismException is thrown if something in the solver is wrong
	 */
	private void makeLpForMec(BitSet mec, AccControllerPair pair, SolverProxyInterface solver) throws PrismException
	{
		setSumXiToOne(mec, pair, solver);
		setKirchhofLawOfFlow(mec, pair, solver);
		try {
			setInferiorLimits(mec, pair, solver);
		} catch (RuntimeException e) {
			System.err.println("The following exception ocurred during the generation of the LP in Controller Synthesis");
			e.printStackTrace(System.err);
			throw new PrismException(e.getMessage());
		}
		setSuperiorLimits(mec, pair, solver);
	}

	/**
	 * This corresponds to equation 8 from the controller-synthesis paper.
	 * In addition it sets x_{i,a} to zero, if a is a forbidden action (in the sense that it gets out of the MEC)
	 * 
	 * @throws PrismException because the solver throws PrismExceptions on error.
	 */
	private void setSumXiToOne(BitSet mec, AccControllerPair pair, SolverProxyInterface solver) throws PrismException
	{
		for (int i = 0; i < necessaryNFromPaperForAcceptance(pair); i++) {
			Map<Integer, Double> row = new HashMap<>();
			for (int state : IterableBitSet.getSetBits(mec)) {
				for (int action = 0; action < model.getNumChoices(state); action++) {
					if (isForbidden(state, action, mec)) {
						Map<Integer, Double> forbiddenRow = new HashMap<>();
						forbiddenRow.put(xOffsetArr[i][state] + action, 1.0);
						solver.addRowFromMap(forbiddenRow, 0.0, SolverProxyInterface.Comparator.EQ, "sum");
					}
					row.put(xOffsetArr[i][state] + action, 1.0);
				}
			}
			solver.addRowFromMap(row, 1.0, SolverProxyInterface.Comparator.EQ, "sum");
		}
	}

	/**
	 * Checks if the action from state may get out of MEC, or if state is not in the MEC in the first place.
	 * We say that the action is forbidden if this is the case.
	 * 
	 * @param state The state number from which the action takes place
	 * @param action The action number
	 * @param mec The MEC as BitSet
	 */
	private boolean isForbidden(int state, int action, BitSet mec)
	{
		if (!mec.get(state)) {
			return true;
		}
		Iterator<Entry<Integer, Double>> it = model.getTransitionsIterator(state, action);
		while (it.hasNext()) {
			Entry<Integer, Double> entry = it.next();
			if (!mec.get(entry.getKey())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This corresponds to equation 9 from the controller synthesis paper 
	 * 
	 * @throws PrismException because the solver throws it sometimes on error
	 */
	private void setKirchhofLawOfFlow(BitSet mec, AccControllerPair pair, SolverProxyInterface solver) throws PrismException
	{
		for (int i = 0; i < necessaryNFromPaperForAcceptance(pair); i++) {
			for (int state : IterableBitSet.getSetBits(mec)) {
				Map<Integer, Double> row = new HashMap<>();

				//outflow
				for (int action = 0; action < model.getNumChoices(state); action++) {
					row.put(xOffsetArr[i][state] + action, row.getOrDefault(xOffsetArr[i][state] + action, 0.0) - 1.0);
				}

				//inflow
				for (int preState = 0; preState < model.getNumStates(); preState++) {
					for (int action = 0; action < model.getNumChoices(preState); action++) {
						Iterator<Entry<Integer, Double>> it = model.getTransitionsIterator(preState, action);
						double valueToAdd = 0.0;
						while (it.hasNext()) {
							Entry<Integer, Double> entry = it.next();
							if (entry.getKey() == state) {
								valueToAdd += entry.getValue();
							}
						}
						row.put(xOffsetArr[i][preState] + action, row.getOrDefault(xOffsetArr[i][preState] + action, 0.0) + valueToAdd);
					}
				}

				solver.addRowFromMap(row, 0.0, SolverProxyInterface.Comparator.EQ, "sum");
			}

		}
	}

	/**
	 * This sets the equation 10 of the controller synthesis paper.
	 * These equations correspond to the lim-inf-conditions. 
	 * Note that this method sometimes has to turn around the equation (because the LP-solver supports only GEQ/LEQ
	 * and not GT/LT. It may throw a RuntimeException
	 */
	private void setInferiorLimits(BitSet mec, AccControllerPair pair, SolverProxyInterface solver)
	{
		int n = necessaryNFromPaperForAcceptance(pair);
		IntStream.range(0, n).forEach(i -> {
			for (MDPCondition mdp : pair.mdpCondition) {
				if (mdp.isLimInf) {
					Map<Integer, Double> row = new HashMap<>();
					double signum = (mdp.cmpOperator.equals(Comparison.GT) || mdp.cmpOperator.equals(Comparison.LT) ? -1.0 : 1.0);
					for (Entry<BitSet, Integer> reward : mdp.acceptanceSet.entrySet()) {
						acceptance.transformToStateSet(reward.getKey()).stream().filter(mec::get).forEach(state -> {
							for (int action = 0; action < model.getNumChoices(state); action++) {
								row.put(xOffsetArr[i][state] + action, signum * reward.getValue());
							}
						});
					}
					SolverProxyInterface.Comparator comp;
					if (mdp.cmpOperator.equals(Comparison.GEQ) || mdp.cmpOperator.equals(Comparison.LT)) {
						comp = Comparator.GE;
					} else {
						comp = Comparator.LE;
					}
					try {
						if (row.isEmpty()) { //apparently the LPSolverProxy cannot deal with empty rows
							row.put(0, 0.0);
						}
						solver.addRowFromMap(row, signum * mdp.bound, comp, "sum");
					} catch (PrismException e) {
						// streams do not appreciate thrown exceptions
						throw new RuntimeException(e);
					}
				}
			}
		});
	}

	/**
	 * This sets the equation 11 of the controller synthesis paper.
	 * These equations correspond to the lim-sup-conditions. 
	 * Note that this method sometimes has to turn around the equation (because the LP-solver supports only GEQ/LEQ
	 * and not GT/LT
	 * 
	 * @throws PrismException is thrown in the solver, if an error occurs.
	 */
	private void setSuperiorLimits(BitSet mec, AccControllerPair pair, SolverProxyInterface solver) throws PrismException
	{
		int i = 0;
		for (MDPCondition mdp : pair.mdpCondition) {
			if (!mdp.isLimInf) {
				Map<Integer, Double> row = new HashMap<>();
				int k = i; //such that k is effectively final
				double signum = (mdp.cmpOperator.equals(Comparison.GT) || mdp.cmpOperator.equals(Comparison.LT) ? -1.0 : 1.0);
				for (Entry<BitSet, Integer> reward : mdp.acceptanceSet.entrySet()) {
					acceptance.transformToStateSet(reward.getKey()).stream().filter(mec::get).forEach(state -> {
						for (int action = 0; action < model.getNumChoices(state); action++) {
							row.put(xOffsetArr[k][state] + action, signum * reward.getValue());
						}
					});
				}
				i++;

				SolverProxyInterface.Comparator comp;
				if (mdp.cmpOperator.equals(Comparison.GEQ) || mdp.cmpOperator.equals(Comparison.LT)) {
					comp = Comparator.GE;
				} else {
					comp = Comparator.LE;
				}
				if (row.isEmpty()) { //apparently the LPSolverProxy cannot deal with empty rows
					row.put(0, 0.0);
				}
				solver.addRowFromMap(row, signum * mdp.bound, comp, "sum");
			}
		}
	}

	/**
	 * This method computes the MECs which satisfy the Fin- and Inf-conditions of a generalized Rabin-pair.
	 * This is done a bit inefficiently by computing for each Inf-condition inf the MECs, which contain
	 * some states of inf, and intersecting all these sets. If the ECC-Computation could handle multiple
	 * Infinite-conditions at once, then we would need to only compute the ECCs only once and not once
	 * for each Infinite-condition.
	 */
	private List<BitSet> computeMecsForPair(GenRabinPair acc) throws PrismException
	{
		ECComputer ecc = ECComputerDefault.createECComputer(null, model);

		BitSet notFinite = new BitSet();
		notFinite.set(0, model.getNumStates());
		acc.Finite.stream().forEach(i -> notFinite.set(i, false));
		ecc.computeMECStates(notFinite);
		List<BitSet> initialList = ecc.getMECStates();
		return acc.Infinite.stream().map(acceptance::transformToStateSet).map(set -> {
			try {
				ecc.computeMECStates(notFinite, set);
			} catch (Exception e) {
				// streams do not like unhandled exception
				System.err.println("the following exception occurred:");
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			return ecc.getMECStates();
		}).reduce(initialList, (list1, list2) -> {
			list2.retainAll(list1);
			return list2;
		});
	}
}
