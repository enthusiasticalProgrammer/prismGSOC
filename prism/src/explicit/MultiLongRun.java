package explicit;

import java.lang.reflect.InvocationTargetException;
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
import solvers.LpSolverProxy;
import solvers.SolverProxyInterface;
import strat.Strategy;

/**
 * This class contains functions used when solving
 * multi-objective mean-payoff (=long run, steady state)
 * problem for MDPs. It provides a LP encoding taken from
 * http://qav.cs.ox.ac.uk/bibitem.php?key=BBC+11 
 * (Two views on Multiple Mean-Payoff Objectives in Markov Decision Processes).
 * 
 * Note that we use a bit different notation here and refer to y_s variables as
 * Z, not to confuse them with y_{s,a}.
 * @author vojfor+Christopher
 *
 *This class is abstract, because it can model either an MDP or a DTMC (which is computed
 *	by taking the product of an MDP and a MultiLongRunStrategy)
 */
public abstract class MultiLongRun
{
	protected Collection<BitSet> mecs;
	private final List<MDPConstraint> constraints;
	private final Collection<MDPExpectationConstraint> expConstraints;
	final Collection<MDPObjective> objectives;

	/**
	 * The instance providing access to the LP solver.
	 */
	SolverProxyInterface solver;

	private boolean initialised = false;

	/**
	 * Number of continuous variables in the LP instance
	 */
	private int numRealLPVars;

	/**
	 * Number of binary in the LP instance
	 */
	private int numBinaryLPVars;

	/**
	 * LP solver to be used
	 */
	private String method;

	/**
	 * xOffset[i] is the solver's variable (column) for the first action of state i, i.e. for x_{i,0}
	 */
	private int[] xOffsetArr;

	/**
	 * yOffset[i] is the solver's variable (column) for the first action of state i, i.e. for y_{i,0}
	 */
	private int[] yOffsetArr;

	private int epsilonVarIndex;

	/**
	 * zIndex[i] is the z variable for the state i (i.e. y_i in LICS11 terminology). 
	 */
	private int[] zIndex;

	/**
	 * The default constructor.
	 * @param mdp The MDP to work with
	 * @param rewards Rewards for the MDP
	 * @param operators The operators for the rewards, instances of {@see prism.Operator}
	 * @param bounds Lower/upper bounds for the rewards, if operators are >= or <=
	 * @param method Method to use, should be a valid value for
	 *        {@see PrismSettings.PrismSettings.PRISM_MDP_MULTI_SOLN_METHOD}
	 * @throws PrismException 
	 */
	public MultiLongRun(Collection<MDPConstraint> constraints, Collection<MDPObjective> objectives, Collection<MDPExpectationConstraint> expConstraints,
			String method) throws PrismException
	{
		this.constraints = new ArrayList<>(constraints);
		this.objectives = new ArrayList<>(objectives);
		this.method = method;
		this.expConstraints = expConstraints;
		if (getN() >= 30) {
			throw new IllegalArgumentException(
					"The problem you want to solve requires to solve an LP with 2^30>=one billion variables. This is more than we are supporting");
		}
	}

	/**
	 * Creates a new solver instance, based on the argument {@see #method}.
	 * @throws PrismException If the jar file providing access to the required LP solver is not found.
	 */
	private void initialiseSolver() throws PrismException
	{
		try { //below Class.forName throws exception if the required jar is not present
			if (method.equals("Linear programming")) {
				//create new solver
				solver = new LpSolverProxy(this.numRealLPVars, this.numBinaryLPVars);
			} else if (method.equals("Gurobi")) {
				Class<?> cl = Class.forName("solvers.GurobiProxy");
				solver = (SolverProxyInterface) cl.getConstructor(int.class, int.class).newInstance(this.numRealLPVars, this.numBinaryLPVars);
			} else
				throw new UnsupportedOperationException("The given method for solving LP programs is not supported: " + method);
		} catch (ClassNotFoundException ex) {
			throw new PrismException("Cannot load the class required for LP solving. Was gurobi.jar file present in compilation time and is it present now?");
		} catch (NoClassDefFoundError e) {
			throw new PrismException(
					"Cannot load the class required for LP solving, it seems that gurobi.jar file is missing. Is GUROBI_HOME variable set properly?");
		} catch (InvocationTargetException e) {
			String append = "";
			if (e.getCause() != null) {
				append = "The message of parent exception is: " + e.getCause().getMessage();
			}

			throw new PrismException(
					"Problem when initialising an LP solver. " + "InvocationTargetException was thrown" + "\n Message: " + e.getMessage() + "\n" + append);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			throw new PrismException("Problem when initialising an LP solver. "
					+ "It appears that the JAR file is present, but there is some problem, because the exception of type " + e.getClass().toString()
					+ " was thrown. Message: " + e.getMessage());
		}
	}

	/**
	 * computes the set of end components and stores it in {@see #mecs}
	 * @throws PrismException
	 */
	protected List<BitSet> computeMECs() throws PrismException
	{
		Model model = getModel();
		if (!(model instanceof NondetModel || model instanceof MDP)) {
			model = new ArtificialNondetModelFromModel(model);
		}
		ECComputer ecc = ECComputerDefault.createECComputer(null, (NondetModel) model);
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
	void computeOffsets() throws PrismException
	{
		this.xOffsetArr = new int[getNumStatesOfModel()];
		int current = 0;
		for (int i = 0; i < getNumStatesOfModel(); i++) {
			boolean isInMEC = false;
			for (BitSet b : this.mecs) {
				if (b.get(i)) {
					isInMEC = true;
					break;
				}
			}
			if (isInMEC) {
				xOffsetArr[i] = current;
				current += getNumChoicesOfModel(i) * (1 << getN());
			} else {
				xOffsetArr[i] = Integer.MIN_VALUE; //so that when used in array, we get exception
			}
		}

		this.yOffsetArr = new int[getNumStatesOfModel()];
		for (int i = 0; i < getNumStatesOfModel(); i++) {
			yOffsetArr[i] = current;
			current += getNumChoicesOfModel(i);
		}

		this.zIndex = new int[getNumStatesOfModel()];

		for (int i = 0; i < getNumStatesOfModel(); i++) {
			zIndex[i] = (isMECState(i)) ? current += (1 << getN()) : Integer.MIN_VALUE;
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
	 * Names all variables, useful for debugging.
	 * @throws PrismException
	 */
	void nameLPVars() throws PrismException
	{
		int current = 0;

		for (int i = 0; i < getNumStatesOfModel(); i++) {
			if (isMECState(i)) {
				for (int j = 0; j < getNumChoicesOfModel(i); j++) {
					for (int n = 0; n < 1 << getN(); n++) {
						String name = "x" + i + "c" + j + "N" + n;
						solver.setVarName(current + j, name);
						solver.setVarBounds(current + j, 0.0, 1.0);
						current++;
					}
				}
			}
		}

		for (int i = 0; i < getNumStatesOfModel(); i++) {
			for (int j = 0; j < getNumChoicesOfModel(i); j++) {
				for (int n = 0; n < 1 << getN(); n++) {
					String name = "y" + i + "c" + j;
					solver.setVarName(current + j, name);
					solver.setVarBounds(current + j, 0.0, Double.MAX_VALUE);
				}
			}
			current += getNumChoicesOfModel(i) * (1 << getN());
		}

		for (int i = 0; i < getNumStatesOfModel(); i++) {
			if (isMECState(i)) {
				String name = "z" + i;
				solver.setVarName(current, name);
				solver.setVarBounds(current++, 0.0, Double.MAX_VALUE);
			}
		}
	}

	/**
	 * Adds a row to the linear program, saying
	 * "all switching probabilities z must sum to 1". See LICS11 paper (equation no 2) for details
	 * @throws PrismException
	 */
	private void setZSumToOne() throws PrismException
	{
		//NOTE: there is an error in the LICS11 paper, we need to sum over MEC states only.
		HashMap<Integer, Double> row = new HashMap<Integer, Double>();

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
	 * Returns a hashmap giving a left hand side for the equation for a reward {@code idx}.
	 * (i,d) in the hashmap says that variable i is multiplied by d. If key i is not
	 * present, it means 0.
	 * @param constraint
	 * @return
	 */
	private HashMap<Integer, Double> getRowForReward(MDPItem constraint)
	{
		HashMap<Integer, Double> row = new HashMap<Integer, Double>();
		for (int state = 0; state < getNumStatesOfModel(); state++) {
			if (isMECState(state)) {
				for (int action = 0; action < getNumChoicesOfModel(state); action++) {
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
	 * In the paper it is equation no 5
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

			HashMap<Integer, Double> row = getRowForReward(item);
			double bound = item.getBound();

			solver.addRowFromMap(row, bound, op, "r" + item);
		}
	}

	/**
	 * Adds an equation to the linear program saying that for the mec bs,
	 * the switching probabilities in sum must be equal to the x variables in sum.
	 * See the LICS 11 paper (equation no 3) for details.
	 * @param maxEndComponent
	 * @throws PrismException
	 */
	private void setEqnForMECLink(BitSet maxEndComponent) throws PrismException
	{
		for (int n = 0; n < 1 << getN(); n++) {
			HashMap<Integer, Double> row = new HashMap<Integer, Double>();

			for (int state = maxEndComponent.nextSetBit(0); state >= 0; state = maxEndComponent.nextSetBit(state + 1)) {

				//X
				for (int action = 0; action < getNumChoicesOfModel(state); action++) {
					row.put(getVarX(state, action, n), 1.0);
				}

				//Z
				row.put(getVarZ(state, n), -1.0);
			}
			solver.addRowFromMap(row, 0, SolverProxyInterface.Comparator.EQ, "m" + maxEndComponent);
		}

	}

	/**
	 * These are the variables y_{state,action} from the paper. See {@see #computeOffsets()} for more details.
	 * @param state
	 * @param action
	 * @param threshold
	 * @return
	 */
	public int getVarX(int state, int action, int threshold)
	{
		if (xOffsetArr[state] < 0)
			return -1;
		return xOffsetArr[state] + action * (1 << getN()) + threshold;
	}

	/**
	 * These are the variables y_{state,action} from the paper. See {@see #computeOffsets()} for more details.
	 * @param state
	 * @param action
	 * @param threshold
	 * @return
	 */
	int getVarY(int state, int action)
	{
		return yOffsetArr[state] + action;
	}

	/**
	 * These are the variables y_state from the paper. See {@see #computeOffsets()} for more details.
	 * @param state
	 * @return
	 */
	int getVarZ(int state, int threshold)
	{
		int result = zIndex[state] + threshold;
		return result;
	}

	private int getVarEpsilon()
	{
		return this.epsilonVarIndex;
	}

	/**
	 * Adds all rows to the LP program that give requirements
	 * on the steady-state distribution (via x variables), equation no 4
	 * @throws PrismException
	 */
	private void setXConstraints() throws PrismException
	{
		HashMap<Integer, HashMap<Integer, Double>> map = new HashMap<Integer, HashMap<Integer, Double>>();
		for (int state = 0; state < getNumStatesOfModel(); state++) {
			if (isMECState(state))
				map.put(state, new HashMap<Integer, Double>());
		}

		//outflow
		for (int state = 0; state < getNumStatesOfModel(); state++) {
			if (!isMECState(state))
				continue;

			for (int i = 0; i < getNumChoicesOfModel(state); i++) {
				for (int n = 0; n < 1 << getN(); n++) {
					int index = getVarX(state, i, n);
					map.get(state).put(index, -1.0);
				}
			}
		}

		//inflow
		for (int preState = 0; preState < getNumStatesOfModel(); preState++) {
			if (!isMECState(preState))
				continue;

			for (int action = 0; action < getNumChoicesOfModel(preState); action++) {
				for (int n = 0; n < 1 << getN(); n++) {
					int index = getVarX(preState, action, n);
					Iterator<Entry<Integer, Double>> it = getTransitionIteratorOfModel(preState, action);
					while (it.hasNext()) {
						Entry<Integer, Double> en = it.next();

						if (!isMECState(en.getKey()))
							continue;

						Map<Integer, Double> row = map.get(en.getKey());
						assert (row != null); //we created mec rows just aboved

						double val = 0;
						if (row.containsKey(index))
							val += map.get(en.getKey()).get(index);

						if (val + en.getValue() != 0)
							row.put(index, val + en.getValue());
						else if (row.containsKey(index))
							row.remove(index);
					}
				}
			}
		}

		//fill in
		for (int state : map.keySet()) {
			solver.addRowFromMap(map.get(state), 0, SolverProxyInterface.Comparator.EQ, "x" + state);
		}
	}

	/**
	 * Adds all rows to the LP program that give requirements
	 * on the MEC reaching probability (via y variables)
	 * @throws PrismException
	 */
	private void setYConstraints() throws PrismException
	{
		HashMap<Integer, Double>[] map = (HashMap<Integer, Double>[]) new HashMap[getNumStatesOfModel()];
		for (int state = 0; state < getNumStatesOfModel(); state++) {
			map[state] = new HashMap<Integer, Double>();
		}

		int initialState = getInitialStateOfModel();

		for (int state = 0; state < getNumStatesOfModel(); state++) {

			//outflow y
			for (int i = 0; i < getNumChoicesOfModel(state); i++) {
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
		for (int preState = 0; preState < getNumStatesOfModel(); preState++) {
			for (int i = 0; i < getNumChoicesOfModel(preState); i++) {
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

		for (int state = 0; state < getNumStatesOfModel(); state++) {
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
		if (!initialised) {
			computeOffsets();
			initialised = true;
		}

		initialiseSolver();

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

	//Equation number 7
	private void setSatisfactionForNontrivialProbability() throws PrismException
	{

		for (int i = 0; i < getN(); i++) {
			MDPConstraint constraint = this.getConstraintNonTrivialProbabilityConstraints().get(i);
			HashMap<Integer, Double> map = new HashMap<Integer, Double>();

			for (int n = 0; n < 1 << getN(); n++) {
				if ((n & (1 << i)) != 0) {
					for (int state = 0; state < getNumStatesOfModel(); state++) {
						for (int act = 0; act < getNumChoicesOfModel(state); act++) {
							if (isMECState(state)) {
								map.put(getVarX(state, act, n), 1.0);
							}
						}
					}
				}
			}
			solver.addRowFromMap(map, constraint.getProbability(), SolverProxyInterface.Comparator.GE, "satisfaction for i: " + i);
		}
	}

	//Equation number 6
	private void setCommitmentForSatisfaction() throws PrismException
	{
		for (BitSet maxEndComponent : mecs) {
			for (int n = 0; n < 1 << getN(); n++) {
				for (int i = 0; i < getN(); i++) {
					if ((n & (1 << i)) != 0) {
						addSingleCommitmentToSatisfaction(maxEndComponent, n, i);
					}
				}
			}
		}
	}

	private void addSingleCommitmentToSatisfaction(BitSet maxEndComponent, int n, int i) throws PrismException
	{
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		for (int state = 0; state < getNumStatesOfModel(); state++) {
			if (maxEndComponent.get(state)) {
				for (int act = 0; act < getNumChoicesOfModel(state); act++) {
					double value = constraints.get(i).reward.getStateReward(prepareStateForReward(state)) + constraints.get(i).reward.getTransitionReward(prepareStateForReward(state), act)
							- constraints.get(i).getBound();
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
			HashMap<Integer, Double> row = getRowForReward(objective);
			solver.setObjFunct(row, objective.operator == Operator.R_MAX);
		}
		solver.solve();

		if (this.objectives.size() == 0) {//We should return bool type
			StateValues sv = new StateValues(TypeBool.getInstance(), getModel());
			sv.setBooleanValue(getInitialStateOfModel(), solver.getBoolResult());
			return sv;
		} else {
			StateValues sv = new StateValues(TypeDouble.getInstance(), getModel());
			sv.setDoubleValue(getInitialStateOfModel(), solver.getDoubleResult());
			return sv;
		}
	}

	/**
	 * Solves the memoryless multiobjective problem for constraint only, or numerical (i.e. no Pareto) 
	 * @return
	 * @throws PrismException
	 */
	public StateValues solveMemoryless() throws PrismException
	{
		if (!this.objectives.isEmpty()) {
			throw new UnsupportedOperationException("Memoryless problem cannot be solved for numerical objectives (Rmin/Rmax)");
		}

		HashMap<Integer, Double> epsObj = new HashMap<Integer, Double>();
		epsObj.put(getVarEpsilon(), 1.0);
		solver.setObjFunct(epsObj, true);

		double solverStartTime = System.currentTimeMillis();

		boolean value = solver.solveIsPositive();
		double time = (System.currentTimeMillis() - solverStartTime) / 1000;
		System.out.println("LP solving took " + time + " s.");

		StateValues sv = new StateValues(TypeBool.getInstance(), getModel());
		sv.setBooleanValue(getInitialStateOfModel(), value);
		return sv;
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
		HashMap<Integer, Double> weightedMap = new HashMap<Integer, Double>();
		//Reward bounds
		int numCount = 0;
		ArrayList<MDPObjective> numIndices = new ArrayList<>();
		for (MDPObjective objective : objectives) {
			if (objective.operator == Operator.R_MAX) {
				HashMap<Integer, Double> map = getRowForReward(objective);
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
			new Point(2);

			for (int i = 0; i < weights.getDimension(); i++) {
				double res = 0;
				HashMap<Integer, Double> rewardEqn = getRowForReward(numIndices.get(i));
				for (int j = 0; j < resultVars.length; j++) {
					if (rewardEqn.containsKey(j)) {
						res += rewardEqn.get(j) * resultVars[j];
					}
				}
				p.setCoord(i, res);
			}
		} else {
			System.out.println("this.resut: "+r);
			throw new PrismException("Unexpected result of LP solving: " + r);
		}
		return p;
	}

	/**
	 * This returns the number n from the paper "Unifying Two Views on Multiple Mean-Payoff
	 * Objectives in Markov Decision Processes" aka the number of satisfaction bound with a
	 * non-trivial probability
	 */
	int getN()
	{
		int result = 0;
		for (MDPConstraint constraint : constraints) {
			if (constraint.isProbabilistic()) {
				result++;
			}
		}
		return result;
	}
	
	protected final int getNumStatesOfModel(){
		return getModel().getNumStates();
	}
	
	protected abstract int getNumChoicesOfModel(int state);
	
	protected abstract Model getModel();
	
	protected int getInitialStateOfModel(){
		return getModel().getFirstInitialState();
	}
	
	protected abstract Iterator<Entry<Integer, Double>> getTransitionIteratorOfModel(int state, int action);
	
	/**
	 * if our model is a product of MDP and strategy, then we have to slightly prepare the state-
	 * number, because otherwise the rewards are not anymore working
	 */
	protected abstract int prepareStateForReward(int state);
	
}
