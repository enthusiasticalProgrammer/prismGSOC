package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import parser.type.TypeBool;
import parser.type.TypeDouble;
import prism.Operator;
import prism.Point;
import prism.PrismException;
import solvers.SolverProxyInterface;
import strat.Strategy;

/**
 * This class contains functions used when solving
 * multi-objective mean-payoff (=long run, steady state)
 * problem for MDPs. It provides a LP encoding taken from
 * CKK15
 * (Unifying two views on Multiple Mean-Payoff Objectives in Markov Decision Processes).
 * 
 * Note that we use a bit different notation here and refer to y_{s,N} variables as
 * Z, not to confuse them with y_{s,a}.
 * @author vojfor+Christopher
 *
 *This class is abstract, because it can model either an MDP or a DTMC (which is computed
 *	by taking the product of an MDP and a MultiLongRunStrategy)
 */
public abstract class MultiLongRun<M extends NondetModel>
{
	protected Collection<BitSet> mecs;
	private final List<MDPConstraint> constraints;
	private final Collection<MDPExpectationConstraint> expConstraints;
	final Collection<MDPObjective> objectives;
	final protected M model;

	/**
	 * The instance providing access to the LP solver.
	 */
	final SolverProxyInterface solver;

	/**
	 * Number of continuous variables in the LP instance
	 */
	private int numRealLPVars;

	/**
	 * xOffset[i] is the solver's variable (column) for the first action of state i, i.e. for x_{i,0}
	 */
	private int[] xOffsetArr;

	/**
	 * yOffset[i] is the solver's variable (column) for the first action of state i, i.e. for y_{i,0}
	 */
	private int[] yOffsetArr;

	/**
	 * zIndex[i] is the z variable for the state i (i.e. y_i in LICS11 terminology). 
	 */
	private int[] zIndex;

	/**
	 * This stores whether we want to solve conjunctive-sat or joint-sat in CKK15 terminolology 
	 */
	private final boolean isConjunctiveSat;

	/**
	 * The default constructor.
	 * @param constraints: MLR-constraints of the form P[ R{x} >= bound [S]]
	 * @param objectives: MLR-objectives of the form R{x}max=?
	 * @param expConstraints: objectives of the form R{x} >= bound as expectation value
	 * @param method: the method of LP solving, either "Linear programming" or "Gurobi"
	 *        {@see PrismSettings.PrismSettings.PRISM_MDP_MULTI_SOLN_METHOD}
	 * @param method Method to use, should be a valid value for
	 * @param M: a nondeterministic model, it should be either an MDP or a DTMCFromMDPAndMDStrategy,
	 * 		because currently these are the only classes for which currently multi-objectives are used. 
	 * @param isConjunctiveSat: true if we are considering conjunctive-SAT, false if we are considering joint-SAT, in
	 *  		terminology of paper CKK15
	 * @throws PrismException 
	 */
	protected MultiLongRun(final Collection<MDPConstraint> constraints, final Collection<MDPObjective> objectives,
			final Collection<MDPExpectationConstraint> expConstraints, final String method, final M m, boolean isConjunctiveSat) throws PrismException
	{
		this.constraints = new ArrayList<>(constraints);
		this.objectives = new ArrayList<>(objectives);
		this.expConstraints = new ArrayList<>(expConstraints);
		this.isConjunctiveSat = isConjunctiveSat;
		this.model = m;
		this.mecs = computeMECs();
		computeOffsets();
		this.solver = AbstractLPStakeholder.initialiseSolver(numRealLPVars, method);

		if (getN() >= 30) {
			throw new IllegalArgumentException(
					"The problem you want to solve requires to solve an LP with 2^30>=one billion variables. This is more than we are supporting");
		}
	}

	/**
	 * computes the set of end components and stores it in {@see #mecs}
	 * @throws PrismException
	 */
	private List<BitSet> computeMECs() throws PrismException
	{
		ECComputer ecc = ECComputerDefault.createECComputer(null, model);
		ecc.computeMECStates();
		return ecc.getMECStates();
	}

	/**
	 * The LICS11 paper considers variables y_{s,a}, y_s and x_{s,a}. The solvers mostly
	 * access variables just by numbers, starting from 0 or so. We use
	 * {@see #getVarY(int, int)}, {@see #getVarZ(int)} and {@see #getVarX(int, int)}
	 * to get, for a variable y_{s,a}, y_s and x_{s,a}, respectively, in the LICS11 sense,
	 * a corresponding variable (i.e. column) in the linear program.
	 * 
	 * This method does all the required initialisations that are required for the
	 * above mentioned methods to work correctly.
	 */
	void computeOffsets()
	{
		this.xOffsetArr = new int[model.getNumStates()];
		int current = 0;
		for (int state = 0; state < model.getNumStates(); state++) {
			if (isMECState(state)) {
				xOffsetArr[state] = current;
				current += model.getNumChoices(state) * (1 << getN());
			} else {
				xOffsetArr[state] = Integer.MIN_VALUE; //so that when used in array, we get exception
			}
		}

		this.yOffsetArr = new int[model.getNumStates()];
		for (int i = 0; i < model.getNumStates(); i++) {
			yOffsetArr[i] = current;
			current += model.getNumChoices(i);
		}

		this.zIndex = new int[model.getNumStates()];

		for (int i = 0; i < model.getNumStates(); i++) {
			zIndex[i] = (isMECState(i)) ? current : Integer.MIN_VALUE;
			current += (1 << getN());
		}

		this.numRealLPVars = current;
	}

	/**
	 * Returns true if the state given is in some MEC.
	 */
	public boolean isMECState(int state)
	{
		return mecs.stream().anyMatch(mec -> mec.get(state));
	}

	/**
	 * This method returns the MEC of the state. If state is in no MEC, it returns null.
	 */
	public BitSet getMecOf(int state)
	{
		return mecs.stream().filter(mec -> mec.get(state)).reduce(null, (a, b) -> (a == null ? b : a));
	}

	/**
	 * Names all variables, useful for debugging.
	 * @throws PrismException
	 */
	private void nameLPVars() throws PrismException
	{
		int current = 0;

		for (int i = 0; i < model.getNumStates(); i++) {
			if (isMECState(i)) {
				for (int j = 0; j < model.getNumChoices(i); j++) {
					for (int n = 0; n < 1 << getN(); n++) {
						String name = "x" + i + "c" + j + "N" + n;
						solver.setVarName(current + j, name);
						solver.setVarBounds(current + j, 0.0, 1.0);
						current++;
					}
				}
			}
		}

		for (int i = 0; i < model.getNumStates(); i++) {
			for (int j = 0; j < model.getNumChoices(i); j++) {
				for (int n = 0; n < 1 << getN(); n++) {
					String name = "y" + i + "c" + j;
					solver.setVarName(current + j, name);
					solver.setVarBounds(current + j, 0.0, Double.MAX_VALUE);
				}
			}
			current += model.getNumChoices(i) * (1 << getN());
		}

		for (int i = 0; i < model.getNumStates(); i++) {
			if (isMECState(i)) {
				String name = "z" + i;
				solver.setVarName(current, name);
				solver.setVarBounds(current++, 0.0, Double.MAX_VALUE);
			}
		}
	}

	/**
	 * Adds a row to the linear program, saying
	 * "all switching probabilities z must sum to 1". See CKK15 (equation no 2) for details
	 * @throws PrismException
	 */
	private void setZSumToOne() throws PrismException
	{
		//NOTE: there is an error in the LICS11 paper, we need to sum over MEC states only.
		Map<Integer, Double> row = new HashMap<>();

		for (BitSet b : this.mecs) {
			for (int i = 0; i < b.length(); i++) {
				for (int n = 0; n < 1 << getN(); n++) {
					if (b.get(i)) {
						int index = getVarZ(i, n);
						row.put(index, 1.0);
					}
				}
			}
		}

		solver.addRowFromMap(row, 1.0, SolverProxyInterface.Comparator.EQ, "sum");
	}

	/**
	 * Returns a Map giving a left hand side for the equation for a reward.
	 * (i,d) in the hashmap says that variable i is multiplied by d. If key i is not
	 * present, it means 0.
	 * @param constraint
	 * @return
	 */
	private Map<Integer, Double> getRowForReward(MDPItem constraint)
	{
		Map<Integer, Double> row = new HashMap<>();
		for (int state = 0; state < model.getNumStates(); state++) {
			if (isMECState(state)) {
				for (int action = 0; action < model.getNumChoices(state); action++) {
					for (int n = 0; n < 1 << getN(); n++) {
						int index = getVarX(state, action, n);
						double val = 0;
						if (row.containsKey(index))
							val = row.get(index);
						val += constraint.reward.getStateReward(prepareStateForReward(state));
						val += constraint.reward.getTransitionReward(prepareStateForReward(state), action);
						row.put(index, val);
					}
				}
			}
		}

		return row;
	}

	/**
	 * Adds a row to the linear program saying that reward of item must have
	 * at least/most required value (given in the constructor to this class)
	 * In the paper CKK15 it is equation no 5
	 * @return
	 */
	private void setEqnForExpectationConstraints() throws PrismException
	{
		for (MDPExpectationConstraint item : expConstraints) {
			SolverProxyInterface.Comparator op;
			if (item.operator == Operator.R_GE) {
				op = SolverProxyInterface.Comparator.GE;
			} else if (item.operator == Operator.R_LE) {
				op = SolverProxyInterface.Comparator.LE;
			} else {
				throw new AssertionError("Should never occur");
			}

			Map<Integer, Double> row = getRowForReward(item);
			double bound = item.getBound();

			solver.addRowFromMap(row, bound, op, "r" + item);
		}
	}

	/**
	 * Adds an equation to the linear program saying that for the mec bmaxEndComponent,
	 * the switching probabilities in sum must be equal to the x variables in sum.
	 * See the CKK15 paper (equation no 3) for details.
	 * @param maxEndComponent
	 * @throws PrismException
	 */
	private void setEqnForMECLink(BitSet maxEndComponent) throws PrismException
	{
		for (int n = 0; n < 1 << getN(); n++) {
			Map<Integer, Double> row = new HashMap<>();

			for (int state = maxEndComponent.nextSetBit(0); state >= 0; state = maxEndComponent.nextSetBit(state + 1)) {

				//X
				for (int action = 0; action < model.getNumChoices(state); action++) {
					row.put(getVarX(state, action, n), 1.0);
				}

				//Z
				row.put(getVarZ(state, n), -1.0);
			}
			solver.addRowFromMap(row, 0, SolverProxyInterface.Comparator.EQ, "m" + maxEndComponent);
		}

	}

	/**
	 * These are the variables x_{state,action,N} from the paper.
	 * @param state
	 * @param action
	 * @param threshold
	 * @return the corresponding variable number or -1 if either the using method is
	 */
	public int getVarX(int state, int action, int threshold)
	{
		if (xOffsetArr[state] < 0)
			return -1;
		return xOffsetArr[state] + action * (1 << getN()) + threshold;
	}

	/**
	 * These are the variables y_{state,action} from the paper.
	 * @param state
	 * @param action
	 * @return
	 */
	int getVarY(int state, int action)
	{
		return yOffsetArr[state] + action;
	}

	/**
	 * These are the variables y_{state,N} from the paper.
	 * @param state
	 * @param threshold
	 * @return
	 */
	int getVarZ(int state, int threshold)
	{
		int result = zIndex[state] + threshold;
		return result;
	}

	/**
	 * Adds all rows to the LP program that give requirements
	 * on the steady-state distribution (via x variables). This method
	 * correspond to equation 4 of CKK15.
	 * @throws PrismException
	 */
	private void setXConstraints() throws PrismException
	{
		Map<Integer, Map<Integer, Double>> map = new HashMap<>();
		for (int state = 0; state < model.getNumStates(); state++) {
			if (isMECState(state))
				map.put(state, new HashMap<Integer, Double>());
		}

		//outflow
		for (int state = 0; state < model.getNumStates(); state++) {
			if (!isMECState(state))
				continue;

			for (int i = 0; i < model.getNumChoices(state); i++) {
				for (int n = 0; n < 1 << getN(); n++) {
					int index = getVarX(state, i, n);
					map.get(state).put(index, -1.0);
				}
			}
		}

		//inflow
		for (int preState = 0; preState < model.getNumStates(); preState++) {
			if (!isMECState(preState))
				continue;

			for (int action = 0; action < model.getNumChoices(preState); action++) {
				for (int n = 0; n < 1 << getN(); n++) {
					int index = getVarX(preState, action, n);
					Iterator<Entry<Integer, Double>> it = getTransitionIteratorOfModel(preState, action);
					while (it.hasNext()) {
						Entry<Integer, Double> en = it.next();

						Map<Integer, Double> row = map.get(en.getKey());
						assert (row != null); //we created mec rows just aboved

						if (!isMECState(en.getKey())) {
							//This action is forbidden
							row.remove(index);
							break;
						}

						double val = row.getOrDefault(index, 0.0);

						if (val + en.getValue() != 0)
							row.put(index, val + en.getValue());
						else if (row.containsKey(index))
							row.remove(index);
					}
				}
			}
		}

		//fill in
		for (Entry<Integer, Map<Integer, Double>> entry : map.entrySet()) {
			solver.addRowFromMap(entry.getValue(), 0, SolverProxyInterface.Comparator.EQ, "x" + entry.getKey());
		}
	}

	/**
	 * Adds all rows to the LP program that give requirements
	 * on the MEC reaching probability (via y variables)
	 * @throws PrismException
	 */
	private void setYConstraints() throws PrismException
	{
		Map<Integer, Double>[] map = new HashMap[model.getNumStates()];
		for (int state = 0; state < model.getNumStates(); state++) {
			map[state] = new HashMap<>();
		}

		int initialState = model.getFirstInitialState();

		for (int state = 0; state < model.getNumStates(); state++) {

			//outflow y
			for (int i = 0; i < model.getNumChoices(state); i++) {
				int index = getVarY(state, i);
				map[state].put(index, -1.0);
			}

			//outflow z
			if (isMECState(state)) {
				for (int n = 0; n < 1 << getN(); n++) {
					int idx = getVarZ(state, n);
					map[state].put(idx, -1.0);
				}
			}
		}

		//inflow
		for (int preState = 0; preState < model.getNumStates(); preState++) {
			for (int i = 0; i < model.getNumChoices(preState); i++) {
				int index = getVarY(preState, i);
				Iterator<Entry<Integer, Double>> it = getTransitionIteratorOfModel(preState, i);
				while (it.hasNext()) {
					Entry<Integer, Double> en = it.next();
					double val = 0;
					if (map[en.getKey()].containsKey(index))
						val += map[en.getKey()].get(index);

					if (val + en.getValue() != 0)
						map[en.getKey()].put(index, val + en.getValue());
					else if (map[en.getKey()].containsKey(index))
						map[en.getKey()].remove(index);
				}
			}
		}

		//fill in

		for (int state = 0; state < model.getNumStates(); state++) {
			solver.addRowFromMap(map[state], (initialState == state) ? -1.0 : 0, SolverProxyInterface.Comparator.EQ, "y" + state);
		}
	}

	/**
	 * Initialises the solver and creates a new instance of
	 * multi-longrun LP, for the parameters given in constructor.
	 * Objective function is not set at all, no matter if it is required.
	 * @throws PrismException
	 */
	public void createMultiLongRunLP() throws PrismException
	{
		nameLPVars();

		//Transient flow
		setYConstraints();
		//Recurrent flow
		setXConstraints();
		//Ensuring everything reaches an end-component
		setZSumToOne();

		//Linking the two kinds of flow
		for (BitSet b : mecs) {
			setEqnForMECLink(b);
		}

		//Reward bounds
		setEqnForExpectationConstraints();

		setCommitmentForSatisfaction();

		setSatisfactionForNontrivialProbability();

	}

	/**
	 * This method corresponds to equation 7 of paper CKK15 
	 */
	private void setSatisfactionForNontrivialProbability() throws PrismException
	{
		for (int i = 0; i < getNBackend(); i++) {
			MDPConstraint constraint = this.getConstraintNonTrivialProbabilityConstraints().get(i);
			Map<Integer, Double> map = new HashMap<>();

			for (int n = (isConjunctiveSat ? 0 : (1 << getN()) - 1); n < 1 << getN(); n++) {
				if ((n & (1 << i)) != 0) {
					for (int state = 0; state < model.getNumStates(); state++) {
						if (isMECState(state)) {
							for (int act = 0; act < model.getNumChoices(state); act++) {
								map.put(getVarX(state, act, n), 1.0);
							}
						}
					}
				}
			}
			solver.addRowFromMap(map, constraint.getProbability(), SolverProxyInterface.Comparator.GE, "satisfaction for i: " + i);
		}
	}

	/**
	 * This method corresponds to equation 6 of paper CKK15 
	 */
	private void setCommitmentForSatisfaction() throws PrismException
	{
		for (BitSet maxEndComponent : mecs) {
			for (int n = (isConjunctiveSat ? 0 : (1 << getN()) - 1); n < 1 << getN(); n++) {
				for (int i = 0; i < getNBackend(); i++) {
					if ((n & (1 << i)) != 0) {
						addSingleCommitmentToSatisfaction(maxEndComponent, n, i);
					}
				}
			}
		}
	}

	private void addSingleCommitmentToSatisfaction(BitSet maxEndComponent, int n, int i) throws PrismException
	{
		Map<Integer, Double> map = new HashMap<>();
		for (int state = 0; state < model.getNumStates(); state++) {
			if (maxEndComponent.get(state)) {
				for (int act = 0; act < model.getNumChoices(state); act++) {
					double value = constraints.get(i).reward.getStateReward(prepareStateForReward(state))
							+ constraints.get(i).reward.getTransitionReward(prepareStateForReward(state), act) - constraints.get(i).getBound();
					map.put(getVarX(state, act, n), value);
				}
			}
		}
		solver.addRowFromMap(map, 0.0, SolverProxyInterface.Comparator.GE, "commitment,component: " + maxEndComponent + " n:" + n + "i: " + i);
	}

	private List<MDPConstraint> getConstraintNonTrivialProbabilityConstraints()
	{
		List<MDPConstraint> result = new ArrayList<>();
		for (MDPConstraint i : this.constraints) {
			if (i.isProbabilistic()) {
				result.add(i);
			}
		}
		return result;
	}

	/**
	 * Solves the multiobjective problem for constraint only, or numerical (i.e. no Pareto) 
	 * @return
	 * @throws PrismException
	 */
	public StateValues solveDefault() throws PrismException
	{

		//Reward bounds
		for (MDPObjective objective : objectives) {
			Map<Integer, Double> row = getRowForReward(objective);
			solver.setObjFunct(row, objective.operator == Operator.R_MAX);
		}
		solver.solve();

		if (this.objectives.size() == 0) {//We should return bool type
			StateValues sv = new StateValues(TypeBool.getInstance(), model);
			sv.setBooleanValue(model.getFirstInitialState(), solver.getBoolResult());
			return sv;
		} else {
			StateValues sv = new StateValues(TypeDouble.getInstance(), model);
			sv.setDoubleValue(model.getFirstInitialState(), solver.getDoubleResult());
			return sv;
		}
	}

	/**
	 * Returns the strategy for the last call to solveDefault(), or null if it was never called before,
	 * or if the strategy did not exist.
	 * @return
	 * @throws PrismException 
	 */
	public abstract Strategy getStrategy();

	/**
	 * For the given 2D weights, get a point p on that is maximal for these weights, i.e.
	 * there is no p' with p.weights<p'.weights.
	 * @param weights
	 * @return
	 * @throws PrismException
	 */
	public Point solveMulti(Point weights) throws PrismException
	{
		if (weights.getDimension() != 2) {
			throw new UnsupportedOperationException("MultiLongRun can only create 2D pareto curve.");
		}
		Map<Integer, Double> weightedMap = new HashMap<>();
		//Reward bounds
		int numCount = 0;
		ArrayList<MDPObjective> numIndices = new ArrayList<>();
		for (MDPObjective objective : objectives) {
			if (objective.operator == Operator.R_MAX) {
				Map<Integer, Double> map = getRowForReward(objective);
				for (Entry<Integer, Double> e : map.entrySet()) {
					double val = 0;
					if (weightedMap.containsKey(e.getKey()))
						val = weightedMap.get(e.getKey());
					weightedMap.put(e.getKey(), val + (weights.getCoord(numCount) * e.getValue()));
				}
				numCount++;
				numIndices.add(objective);
			} else {
				throw new PrismException(
						"Only maximising rewards in Pareto curves are currently supported (note: you can multiply your rewards by -1 and change min to max");
			}
		}

		solver.setObjFunct(weightedMap, true);

		int r = solver.solve();
		double[] resultVars = solver.getVariableValues();

		Point p = new Point(2);

		if (r == lpsolve.LpSolve.INFEASIBLE) {
			return null;
		} else if (r == lpsolve.LpSolve.OPTIMAL) {
			for (int i = 0; i < weights.getDimension(); i++) {
				double res = 0;
				Map<Integer, Double> rewardEqn = getRowForReward(numIndices.get(i));
				for (int j = 0; j < resultVars.length; j++) {
					if (rewardEqn.containsKey(j)) {
						res += rewardEqn.get(j) * resultVars[j];
					}
				}
				p.setCoord(i, res);
			}
		} else {
			throw new PrismException("Unexpected result of LP solving: " + r);
		}
		return p;
	}

	/**
	 * This returns the number n from the paper CKK15, aka the number of satisfaction bound with a
	 * non-trivial probability. However, for convenience, it returns one (or zero) if we compute a multi-joint-conjunctive.
	 */
	int getN()
	{
		int result = getNBackend();
		if (!isConjunctiveSat) { //if we are using joint-SAT
			return result == 0 ? 0 : 1;
		}
		return result;
	}

	/**
	 * This returns the number n from the paper CKK15, aka the number of satisfaction bound with a
	 * non-trivial probability, ignoring whether we use conjunctive-SAT or joint-SAT. in Most cases you
	 * should use getN and not this method.
	 */
	private int getNBackend()
	{
		int result = 0;
		for (MDPConstraint constraint : constraints) {
			if (constraint.isProbabilistic()) {
				result++;
			}
		}
		return result;
	}

	protected abstract Iterator<Entry<Integer, Double>> getTransitionIteratorOfModel(int state, int action);

	/**
	 * if our model is a product of MDP and strategy, then we have to slightly prepare the state-
	 * number by sometimes adjusting some offsets
	 */
	protected abstract int prepareStateForReward(int state);

}
