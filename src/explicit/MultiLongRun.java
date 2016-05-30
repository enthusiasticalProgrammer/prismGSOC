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
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import parser.type.TypeBool;
import parser.type.TypeDouble;
import prism.Operator;
import prism.Point;
import prism.PrismException;
import solvers.LpSolverProxy;
import solvers.SolverProxyInterface;
import strat.MultiLongRunStrategy;

/**
 * This class contains functions used when solving
 * multi-objective mean-payoff (=long run, steady state)
 * problem for MDPs. It provides a LP encoding taken from
 * http://qav.cs.ox.ac.uk/bibitem.php?key=BBC+11 
 * (Two views on Multiple Mean-Payoff Objectives in Markov Decision Processes).
 * 
 * Note that we use a bit different notation here and refer to y_s variables as
 * Z, not to confuse them with y_{s,a}.
 * @author vojfor
 *
 */
//TODO @Christopher: extend this class and adjust documentation
//TODO no2: are actions and transitions confused here?
public class MultiLongRun
{
	private MDP mdp;
	private Collection<BitSet> mecs;
	private Collection<MDPConstraint> constraints;
	private Collection<MDPObjective> objectives;

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
	 * This helps maintaining BitSets over the MDPConstraints with non-trivial probability
	 */
	private Map<MDPConstraint, Integer> offsetForConstraints;

	/**
	 * xOffset[i] is the solver's variable (column) for the first action of state i, i.e. for x_{i,0}
	 */
	@XmlElement
	private int[] xOffsetArr;

	/**
	 * yOffset[i] is the solver's variable (column) for the first action of state i, i.e. for y_{i,0}
	 */
	private int[] yOffsetArr;

	private int[] sOffsetArr;

	private int[] tOffsetArr;

	private int epsilonVarIndex;

	/**
	 * zIndex[i] is the z variable for the state i (i.e. y_i in LICS11 terminology). 
	 */
	private int[] zIndex;

	/**
	 * qIndex[i] is the q variable for the state i (binary variable ensuring memorylessnes, not present in LICS11). 
	 */
	private int[] qIndex;

	@SuppressWarnings("unused")
	private MultiLongRun()
	{
		//neccessary for XML I/O
	}

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
	public MultiLongRun(MDP mdp, Collection<MDPConstraint> constraints, Collection<MDPObjective> objectives, String method) throws PrismException
	{
		this.mdp = mdp;
		this.constraints = new ArrayList<>(constraints);
		this.objectives = new ArrayList<>(objectives);
		this.method = method;
		this.mecs = computeMECs();
		offsetForConstraints = computeOffsetforConstraints();
		if (getN() >= 30) {
			throw new IllegalArgumentException(
					"The problem you want to solve requires to solve an LP with 2^30>=one billion variables. This is more than we are supporting");
		}
	}

	private Map<MDPConstraint, Integer> computeOffsetforConstraints()
	{
		int threshold = 0;
		Map<MDPConstraint, Integer> result = new HashMap<>();
		for (MDPConstraint constraint : constraints) {
			if (constraint.isProbabilistic()) {
				result.put(constraint, threshold++);
			}
		}
		return result;
	}

	/**
	 * Creates a new solver instance, based on the argument {@see #method}.
	 * @throws PrismException If the jar file providing access to the required LP solver is not found.
	 */
	//TODO This belongs to some extend into the solver
	private void initialiseSolver(boolean memoryless) throws PrismException
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
	private List<BitSet> computeMECs() throws PrismException
	{
		ECComputer ecc = ECComputerDefault.createECComputer(null, mdp);
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
	private void computeOffsets(boolean memoryless)
	{
		this.xOffsetArr = new int[mdp.getNumStates()];
		int current = 0;
		for (int i = 0; i < mdp.getNumStates(); i++) {
			boolean isInMEC = false;
			for (BitSet b : this.mecs) {
				if (b.get(i)) {
					isInMEC = true;
					break;
				}
			}
			if (isInMEC) {
				xOffsetArr[i] = current;
				current += mdp.getNumChoices(i) * (1 << getN());
			} else {
				xOffsetArr[i] = Integer.MIN_VALUE; //so that when used in array, we get exception
			}
		}

		this.yOffsetArr = new int[mdp.getNumStates()];
		for (int i = 0; i < mdp.getNumStates(); i++) {
			yOffsetArr[i] = current;
			current += mdp.getNumChoices(i);
		}

		this.zIndex = new int[mdp.getNumStates()];

		for (int i = 0; i < mdp.getNumStates(); i++) {
			zIndex[i] = (isMECState(i)) ? current += (1 << getN()) : Integer.MIN_VALUE;
		}

		if (!memoryless) {
			this.numRealLPVars = current;
			return; //end here if we don't do memoryless strategy, ow we do MILP
		}

		//position of the epsilon variable
		this.epsilonVarIndex = current++;
		this.numRealLPVars = current;

		//binary variables

		this.sOffsetArr = new int[mdp.getNumStates()];
		for (int i = 0; i < mdp.getNumStates(); i++) {
			if (isMECState(i)) {
				sOffsetArr[i] = current;
				current += mdp.getNumChoices(i);
			} else {
				sOffsetArr[i] = Integer.MIN_VALUE; //so that when used in array, we get exception
			}
		}

		this.tOffsetArr = new int[mdp.getNumStates()];
		for (int i = 0; i < mdp.getNumStates(); i++) {
			if (isMECState(i)) {
				tOffsetArr[i] = current;
				current += mdp.getNumChoices(i);
			} else {
				tOffsetArr[i] = Integer.MIN_VALUE; //so that when used in array, we get exception
			}
		}

		this.qIndex = new int[mdp.getNumStates()];

		for (int i = 0; i < mdp.getNumStates(); i++) {
			qIndex[i] = (isMECState(i)) ? current++ : Integer.MIN_VALUE;
		}

		this.numBinaryLPVars = current - this.numRealLPVars;
	}

	/**
	 * Returns true if the state given is in some MEC.
	 */
	private boolean isMECState(int state)
	{
		return mecs.stream().anyMatch(mec -> mec.get(state));
	}

	/**
	 * Names all variables, useful for debugging.
	 * @throws PrismException
	 */
	private void nameLPVars(boolean memoryless) throws PrismException
	{
		int current = 0;

		for (int i = 0; i < mdp.getNumStates(); i++) {
			if (isMECState(i)) {
				for (int j = 0; j < mdp.getNumChoices(i); j++) {
					for (int n = 0; n < 1 << getN(); n++) {
						String name = "x" + i + "c" + j + "N" + n;
						solver.setVarName(current + j, name);
						solver.setVarBounds(current + j, 0.0, 1.0);
					}
				}
				current += mdp.getNumChoices(i) * (1 << getN());
			}
		}

		for (int i = 0; i < mdp.getNumStates(); i++) {
			for (int j = 0; j < mdp.getNumChoices(i); j++) {
				for (int n = 0; n < 1 << getN(); n++) {
					String name = "y" + i + "c" + j;
					solver.setVarName(current + j, name);
					solver.setVarBounds(current + j, 0.0, Double.MAX_VALUE);
				}
			}
			current += mdp.getNumChoices(i) * (1 << getN());
		}

		for (int i = 0; i < mdp.getNumStates(); i++) {
			if (isMECState(i)) {
				String name = "z" + i;
				solver.setVarName(current, name);
				solver.setVarBounds(current++, 0.0, Double.MAX_VALUE);
			}
		}

		if (!memoryless)
			return; //there are no more variables

		solver.setVarName(current++, "eps");

		for (int i = 0; i < mdp.getNumStates(); i++) {
			if (isMECState(i)) {
				for (int j = 0; j < mdp.getNumChoices(i); j++) {
					String name = "s" + i + "c" + j;
					solver.setVarName(current + j, name);
					solver.setVarBounds(current + j, 0.0, 1.0);
				}
				current += mdp.getNumChoices(i);
			}
		}

		for (int i = 0; i < mdp.getNumStates(); i++) {
			if (isMECState(i)) {
				for (int j = 0; j < mdp.getNumChoices(i); j++) {
					String name = "t" + i + "c" + j;
					solver.setVarName(current + j, name);
					solver.setVarBounds(current + j, 0.0, Double.MAX_VALUE);
				}
				current += mdp.getNumChoices(i);
			}
		}

		for (int i = 0; i < mdp.getNumStates(); i++) {
			if (isMECState(i)) {
				String name = "q" + i;
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

		solver.addRowFromMap(row, 1.0, SolverProxyInterface.EQ, "sum");
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
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (isMECState(state)) {
				for (int i = 0; i < mdp.getNumChoices(state); i++) {
					for (int n = 0; n < 1 << getN(); n++) {
						int index = getVarX(state, i, n);
						double val = 0;
						if (row.containsKey(index))
							val = row.get(index);
						val += constraint.reward.getStateReward(state);
						val += constraint.reward.getTransitionReward(state, i);
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
	 * @param item
	 * @return
	 */
	private void setEqnForConstraint(MDPConstraint item) throws PrismException
	{
		int op = 0;
		if (item.operator == Operator.R_GE) {
			op = SolverProxyInterface.GE;
		} else if (item.operator == Operator.R_LE) {
			op = SolverProxyInterface.LE;
		} else {
			throw new AssertionError("Should never occur");
		}

		HashMap<Integer, Double> row = getRowForReward(item);
		double bound = item.getBound();

		solver.addRowFromMap(row, bound, op, "r" + item);
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
		HashMap<Integer, Double> row = new HashMap<Integer, Double>();

		for (int state = maxEndComponent.nextSetBit(0); state >= 0; state = maxEndComponent.nextSetBit(state + 1)) {
			for (int n = 0; n < 1 << getN(); n++) {
				//X
				for (int i = 0; i < mdp.getNumChoices(state); i++) {
					int index = getVarX(state, i, n);
					row.put(index, 1.0);
				}

				//Z
				int index = getVarZ(state, n);
				row.put(index, -1.0);
			}
		}

		solver.addRowFromMap(row, 0, SolverProxyInterface.EQ, "m" + maxEndComponent);
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

		return xOffsetArr[state] + action * (1 << getN()) + threshold;
	}

	/**
	 * These are the variables x_{state,action} from the paper. See {@see #computeOffsets()} for more details.
	 * @param state
	 * @param action
	 * @param threshold
	 * @return
	 */
	private int getVarY(int state, int action)
	{
		return yOffsetArr[state] + action;
	}

	/**
	 * These are the variables y_state from the paper. See {@see #computeOffsets()} for more details.
	 * @param state
	 * @return
	 */
	private int getVarZ(int state, int threshold)
	{
		int result = zIndex[state] + threshold;
		return result;
	}

	private int getConstraintThreshold(Set<MDPConstraint> x)
	{
		int result = 0;
		for (MDPConstraint constraint : x) {
			if (offsetForConstraints.get(constraint) == null) {
				throw new IllegalArgumentException("An input MDPConstraint is not a probabilistic");
			} else {
				result += (1 << offsetForConstraints.get(constraint));
			}
		}
		return result;
	}

	private int getVarS(int state, int action)
	{
		return sOffsetArr[state] + action;
	}

	private int getVarT(int state, int action)
	{
		return tOffsetArr[state] + action;
	}

	private int getVarQ(int state)
	{
		return qIndex[state];
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
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (isMECState(state))
				map.put(state, new HashMap<Integer, Double>());
		}

		//outflow
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (!isMECState(state))
				continue;

			for (int i = 0; i < mdp.getNumChoices(state); i++) {
				for (int n = 0; n < 1 << getN(); n++) {
					int index = getVarX(state, i, n);
					map.get(state).put(index, -1.0);
				}
			}
		}

		//inflow
		for (int preState = 0; preState < mdp.getNumStates(); preState++) {
			if (!isMECState(preState))
				continue;

			for (int i = 0; i < mdp.getNumChoices(preState); i++) {
				for (int n = 0; n < 1 << getN(); n++) {
					int index = getVarX(preState, i, n);
					Iterator<Entry<Integer, Double>> it = mdp.getTransitionsIterator(preState, i);
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
						//System.out.println("just added " + val + "+" + en.getValue()));
					}
				}
			}
		}

		//fill in
		for (int state : map.keySet()) {
			solver.addRowFromMap(map.get(state), 0, SolverProxyInterface.EQ, "x" + state);
		}
	}

	private void setZXLink() throws PrismException
	{
		HashMap<Integer, HashMap<Integer, Double>> map = new HashMap<Integer, HashMap<Integer, Double>>();
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (isMECState(state))
				map.put(state, new HashMap<Integer, Double>());
		}

		//outflow
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (!isMECState(state))
				continue;
			for (int n = 0; n < 1 << getN(); n++) {
				map.get(state).put(getVarZ(state, n), 1.0);
				for (int i = 0; i < mdp.getNumChoices(state); i++) {
					int index = getVarX(state, i, n);
					map.get(state).put(index, -1.0);
				}
			}
		}

		//fill in
		for (int state : map.keySet()) {
			solver.addRowFromMap(map.get(state), 0, SolverProxyInterface.EQ, "zx" + state);
		}
	}

	private void setSConstraint(int state, int choice) throws PrismException
	{
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		map.put(getVarY(state, choice), 1.0);
		map.put(getVarS(state, choice), 1.0);
		solver.addRowFromMap(map, 1.0, SolverProxyInterface.LE, "s" + state + "c" + choice);
	}

	private void setTConstraint(int state, int choice) throws PrismException
	{
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		for (int n = 0; n < 1 << getN(); n++) {
			map.put(getVarX(state, choice, n), 1.0);
		}
		map.put(getVarT(state, choice), -1.0);
		map.put(getVarEpsilon(), -1.0);
		solver.addRowFromMap(map, -1.0, SolverProxyInterface.GE, "t" + state + "c" + choice);
	}

	private void setQConstraint(int state) throws PrismException
	{
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();

		for (int i = 0; i < mdp.getNumChoices(state); i++) {
			for (int n = 0; n < 1 << getN(); n++) {
				int index = getVarX(state, i, n);

				map.put(index, 1.0);
			}
		}
		map.put(getVarQ(state), 1.0);

		solver.addRowFromMap(map, 1.0, SolverProxyInterface.LE, "q" + state);
	}

	private void setSTQConstraintLink(int state, int choice) throws PrismException
	{
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();

		map.put(getVarS(state, choice), 1.0);
		map.put(getVarT(state, choice), 1.0);
		map.put(getVarQ(state), 1.0);

		solver.addRowFromMap(map, 1.0, SolverProxyInterface.GE, "stq" + state);
	}

	/**
	 * For every state s: "q >= sum_a y_{s,a}" and "q + sum_a x_{s,a} <=1".
	 * This ensures that either sum_a y_{s,a} or sum_a x_{s,a} is nonzero.
	 * @throws PrismException
	 */
	private void setSTQConstraints() throws PrismException
	{
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (!isMECState(state))
				continue;

			for (int i = 0; i < mdp.getNumChoices(state); i++) {
				setSConstraint(state, i);
				setTConstraint(state, i);
				setSTQConstraintLink(state, i);
			}

			setQConstraint(state);
		}
	}

	/**
	 * Adds all rows to the LP program that give requirements
	 * on the MEC reaching probability (via y variables)
	 * @throws PrismException
	 */
	private void setYConstraints() throws PrismException
	{
		HashMap<Integer, Double>[] map = (HashMap<Integer, Double>[]) new HashMap[mdp.getNumStates()];
		for (int state = 0; state < mdp.getNumStates(); state++) {
			map[state] = new HashMap<Integer, Double>();
		}

		int initialState = mdp.getInitialStates().iterator().next();

		for (int state = 0; state < mdp.getNumStates(); state++) {

			//outflow y
			for (int i = 0; i < mdp.getNumChoices(state); i++) {
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
		for (int preState = 0; preState < mdp.getNumStates(); preState++) {
			for (int i = 0; i < mdp.getNumChoices(preState); i++) {
				int index = getVarY(preState, i);
				Iterator<Entry<Integer, Double>> it = mdp.getTransitionsIterator(preState, i);
				while (it.hasNext()) {
					Entry<Integer, Double> en = it.next();
					double val = 0;
					if (map[en.getKey()].containsKey(index))
						val += map[en.getKey()].get(index);

					if (val + en.getValue() != 0)
						map[en.getKey()].put(index, val + en.getValue());
					else if (map[en.getKey()].containsKey(index))
						map[en.getKey()].remove(index);
					//System.out.println("just added " + val + "+" + en.getValue()));
				}
			}
		}

		//fill in

		for (int state = 0; state < mdp.getNumStates(); state++) {
			solver.addRowFromMap(map[state], (initialState == state) ? -1.0 : 0, SolverProxyInterface.EQ, "y" + state);
		}
	}

	/**
	 * Initialises the solver and creates a new instance of
	 * multi-longrun LP, for the parameters given in constructor.
	 * Objective function is not set at all, no matter if it is required.
	 * @throws PrismException
	 */
	public void createMultiLongRunLP(boolean memoryless) throws PrismException
	{
		if (!initialised) {
			System.out.println("Finished computing end components.");
			computeOffsets(memoryless);
			initialised = true;
		}

		double solverStartTime = System.currentTimeMillis();

		initialiseSolver(memoryless);

		nameLPVars(memoryless);

		//Transient flow
		setYConstraints();
		//Recurrent flow
		setXConstraints();
		//Ensuring everything reaches an end-component
		setZSumToOne();

		if (memoryless) {//add binary contstraints ensuring memorylessness
			setZXLink();
			setSTQConstraints();
		} else {
			//Linking the two kinds of flow
			for (BitSet b : mecs) {
				setEqnForMECLink(b);
			}
		}

		//Reward bounds
		//TODO Christopher: put for-loop in subroutine
		for (MDPConstraint constraint : constraints) {
			setEqnForConstraint(constraint);
		}

		setCommitmentForSatisfaction();

		setSatisfactionForNontrivialProbability();

		double time = (System.currentTimeMillis() - solverStartTime) / 1000;
		System.out.println("LP problem construction finished in " + time + " s.");
	}

	//Equation number 7
	private void setSatisfactionForNontrivialProbability() throws PrismException
	{

		for (int i = 0; i < getN(); i++) {
			MDPConstraint constraint = this.getConstraintNonTrivialProbabilityConstraints().get(i);
			HashMap<Integer, Double> map = new HashMap<Integer, Double>();

			for (int n = 0; n < 1 << getN(); n++) {
				if ((n & (1 << i)) != 0) {
					for (int state = 0; state < mdp.getNumStates(); state++) {
						for (int act = 0; act < mdp.getNumChoices(state); act++) {
							map.put(getVarX(state, act, n), 1.0);
						}
					}
				}
			}
			solver.addRowFromMap(map, constraint.getProbability(), SolverProxyInterface.GE, "satisfaction for i: " + i);
		}
	}

	//Equation number 6
	private void setCommitmentForSatisfaction() throws PrismException
	{
		List<MDPConstraint> nonTrivialProbabilities = this.getConstraintNonTrivialProbabilityConstraints();

		for (BitSet maxEndComponent : mecs) {
			for (int n = 0; n < 1 << getN(); n++) {
				for (int i = 0; i < getN(); i++) {
					BitSet b = new BitSet(n);
					if (b.get(i)) {
						addSingleCommitmentToSatisfaction(maxEndComponent, n, nonTrivialProbabilities.get(i));
					}
				}
			}
		}
	}

	private void addSingleCommitmentToSatisfaction(BitSet maxEndComponent, int n, MDPConstraint mdpConstraint) throws PrismException
	{
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (!isMECState(state))
				continue;
			for (int act = 0; act < mdp.getNumChoices(state); act++) {
				double value = mdpConstraint.reward.getStateReward(state) + mdpConstraint.reward.getTransitionReward(state, act) - mdpConstraint.getBound();
				map.put(getVarX(state, act, n), value);
			}
		}
		solver.addRowFromMap(map, 0.0, SolverProxyInterface.GE, "commitment,component: " + maxEndComponent + " n:" + n + "i: " + mdpConstraint);
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
		assert (this.objectives.size() < 2);

		//Reward bounds
		for (MDPObjective objective : objectives) {
			HashMap<Integer, Double> row = getRowForReward(objective);
			solver.setObjFunct(row, objective.operator == Operator.R_MAX);
		}

		double solverStartTime = System.currentTimeMillis();

		solver.solve();
		double time = (System.currentTimeMillis() - solverStartTime) / 1000;
		System.out.println("LP solving took " + time + " s.");

		if (this.objectives.size() == 0) {//We should return bool type
			StateValues sv = new StateValues(TypeBool.getInstance(), mdp);
			sv.setBooleanValue(mdp.getFirstInitialState(), solver.getBoolResult());
			return sv;
		} else {
			StateValues sv = new StateValues(TypeDouble.getInstance(), mdp);
			sv.setDoubleValue(mdp.getFirstInitialState(), solver.getDoubleResult());
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

		StateValues sv = new StateValues(TypeBool.getInstance(), mdp);
		sv.setBooleanValue(mdp.getFirstInitialState(), value);
		return sv;
	}

	/**
	 * Returns the strategy for the last call to solveDefault(), or null if it was never called before,
	 * or if the strategy did not exist.
	 * @return
	 */
	//TODO Christopher: adjust it
	public MultiLongRunStrategy getStrategy(boolean memoryless) throws PrismException
	{
		double[] resultVariables = solver.getVariableValues();
		double[] lpResult;
		lpResult = solver.getVariableValues(); //computeStrategy actually just added some constraints, which were already there

		int numStates = this.mdp.getNumStates();
		Distribution[] transientDistribution = new Distribution[numStates];
		Distribution[] switchProbability = new Distribution[numStates];

		for (int state = 0; state < numStates; state++) {
			double transientSum = 0.0;
			double recurrentSum = 0.0;
			for (int j = 0; j < this.mdp.getNumChoices(state); j++) {
				transientSum += lpResult[getVarY(state, j)];
				if (isMECState(state)) {
					for (int n = 0; n < 1 << getN(); n++) {
						recurrentSum += resultVariables[getVarX(state, j, n)];
					}
				}
			}

			transientDistribution[state] = getTransientDistributionAt(state, memoryless, recurrentSum, transientSum, lpResult);
		}

		for (BitSet mec : mecs) {
			Distribution mecDistribution = this.getSwitchProbabilityAt(mec);
			for (int state = mec.nextSetBit(0); state >= 0; state = mec.nextSetBit(state + 1)) {
				switchProbability[state] = mecDistribution;
			}
		}

		//TODO sometimes return null?
		if (memoryless)
			return new MultiLongRunStrategy(transientDistribution, getReccurrentDistribution());
		else
			return new MultiLongRunStrategy(transientDistribution, switchProbability, getReccurrentDistribution());
	}

	//indirection: N (as number)
	private Distribution getSwitchProbabilityAt(BitSet mec)
	{
		double[] inBetweenResult = new double[1 << getN()];
		for (int state = mec.nextSetBit(0); state >= 0; state = mec.nextSetBit(state + 1)) {
			for (int choice = 0; choice < mdp.getNumChoices(state); choice++) {
				for (int N = 0; N < 1 << getN(); N++) {
					inBetweenResult[N] += solver.getVariableValues()[getVarX(state, choice, N)];
				}
			}
		}
		for (int i = 0; i < inBetweenResult.length; i++) {
			System.out.println(i + " " + inBetweenResult[i]);
		}
		Distribution result = new Distribution();
		for (int i = 0; i < inBetweenResult.length; i++) {
			result.add(i, inBetweenResult[i]);
		}
		return result;
	}

	private XiNStrategy[] getReccurrentDistribution()
	{
		XiNStrategy[] strategies = new XiNStrategy[1 << getN()];
		for (int i = 0; i < 1 << getN(); i++) {
			strategies[i] = new XiNStrategy(solver, mdp, this, i);
		}

		return strategies;
	}

	private Distribution getTransientDistributionAt(int state, boolean memoryless, double recurrentSum, double transientSum, double[] resSt)
	{
		if (transientSum > 0.0 && (!memoryless || !isMECState(state) || recurrentSum == 0.0)) {
			Distribution result = new Distribution();

			for (int j = 0; j < this.mdp.getNumChoices(state); j++) {
				result.add(j, resSt[getVarY(state, j)] / transientSum);
			}
			return result;
		}
		return null;
	}

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
						"Only maximising rewards in Pareto curves are currently supported (note: you can multiply your rewards by 1 and change min to max"); //TODO min
			}
		}

		solver.setObjFunct(weightedMap, true);

		int r = solver.solve();
		double[] resultVars = solver.getVariableValues();

		Point p = new Point(2);

		if (r == lpsolve.LpSolve.INFEASIBLE) {//TODO handle it better
			throw new PrismException("the LP seems infeasible ");
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
			throw new PrismException("Unexpected result of LP solving: " + r);
		}
		return p;
	}

	/**
	 * This returns the number n from the paper "Unifying Two Views on Multiple Mean-Payoff
	 * Objectives in Markov Decision Processes" aka the number of satisfaction bound with a
	 * non-trivial probability
	 */
	private int getN()
	{
		int result = 0;
		for (MDPConstraint constraint : constraints) {
			if (constraint.isProbabilistic()) {
				result++;
			}
		}
		return result;
	}
}
