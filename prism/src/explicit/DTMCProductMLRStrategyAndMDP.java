package explicit;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import explicit.rewards.MCRewards;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.ModelType;
import prism.Pair;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import strat.InvalidStrategyStateException;
import strat.MultiLongRunStrategy;

/**
 * This class simulates a product between an MDP and a MultiLongRunStrategy,
 * which satisfies a multi-objective property. The product is computed implicitely.
 * 
 */
public class DTMCProductMLRStrategyAndMDP implements DTMC
{
	private final MDP mdp;
	private final MultiLongRunStrategy mlrs;

	public DTMCProductMLRStrategyAndMDP(MDP mdp, MultiLongRunStrategy mlrs)
	{
		this.mdp = mdp;
		this.mlrs = mlrs;
	}

	public MDP getMDP()
	{
		return mdp;
	}

	@Override
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	@Override
	public int getNumStates()
	{
		return mdp.getNumStates() * mlrs.getNumberOfDifferentStrategies();
	}

	public int getNumStrategies()
	{
		return mlrs.getNumberOfDifferentStrategies();
	}

	@Override
	public int getNumInitialStates()
	{
		return 1;
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		Collection<Integer> c = new ArrayList<>();
		c.add(0);
		return c;
	}

	@Override
	public int getFirstInitialState()
	{
		return 0;
	}

	@Override
	public boolean isInitialState(int i)
	{
		return i == 0;
	}

	@Override
	public int getNumDeadlockStates()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterable<Integer> getDeadlockStates()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getFirstDeadlockState()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDeadlockState(int i)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public List<State> getStatesList()
	{
		List<State> statelist = mdp.getStatesList();
		List<State> result = new ArrayList<>();
		for (State mdpState : statelist) {
			for (int strategy = 0; strategy < mlrs.getNumberOfDifferentStrategies(); strategy++) {
				State strategyState = new State(1);
				strategyState.setValue(0, strategy);
				result.add(new State(mdpState, strategyState));
			}
		}
		return result;
	}

	@Override
	public VarList getVarList()
	{
		return mdp.getVarList();
	}

	@Override
	public Values getConstantValues()
	{
		return mdp.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		return null;
	}

	@Override
	public Set<String> getLabels()
	{
		return null;
	}

	@Override
	public boolean hasLabel(String name)
	{
		return false;
	}

	/**
	 * Some transititons are traversed with probability 0.0
	 */
	@Override
	public int getNumTransitions()
	{
		return this.getNumStates() * getNumStates();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int state)
	{
		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < this.getNumStates(); i++) {
			if (isSuccessor(state, i))
				result.add(i);
		}
		return result.iterator();
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		Iterator<Entry<Integer, Double>> iterator = this.getTransitionsIterator(s1);
		for (; iterator.hasNext();) {
			Entry<Integer, Double> entry = iterator.next();
			if (entry.getKey() == s2 && entry.getValue() != 0.0)
				return true;
		}
		return false;
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		for (int i = 0; i < this.getNumStates(); i++) {
			if (!isSuccessor(s, i) && set.get(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		for (int i = 0; i < this.getNumStates(); i++) {
			if (isSuccessor(s, i) && set.get(i)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog log)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark, boolean showStates)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportStates(int exportType, VarList varList, PrismLog log) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String infoString()
	{
		return "Product of MDP " + mdp.infoString() + ", and strategy " + mlrs.getInfo();
	}

	@Override
	public String infoStringTable()
	{
		return "Product of MDP " + mdp.infoStringTable() + ", and strategy " + mlrs.getInfo();
	}

	@Override
	public boolean hasStoredPredecessorRelation()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PredecessorRelation getPredecessorRelation(PrismComponent parent, boolean storeIfNew)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearPredecessorRelation()
	{
		// nothing to do, since we do not have a predecessorRelation anyway
	}

	@Override
	public int getNumTransitions(int state)
	{
		int result = 0;
		Iterator<Entry<Integer, Double>> iter = getTransitionsIterator(state);
		for (; iter.hasNext(); iter.next()) {
			result++;
		}
		return result;
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int state)
	{
		int mdpState = state / mlrs.getNumberOfDifferentStrategies();
		int strategyState = state % mlrs.getNumberOfDifferentStrategies();

		Distribution oneMove;
		try {
			oneMove = mlrs.getDistributionOfStrategy(mdpState, strategyState);
		} catch (InvalidStrategyStateException e) {
			return new ArrayList<Entry<Integer, Double>>().iterator();
		}
		if(oneMove==null){
			return new ArrayList<Entry<Integer, Double>>().iterator();
		}
		Distribution afterIndexAdaption = new Distribution();
		oneMove.forEach(entry -> afterIndexAdaption.set(entry.getKey() * mlrs.getNumberOfDifferentStrategies(), entry.getValue()));
		if (strategyState != 0) {
			return afterIndexAdaption.iterator();
		} else { //transient state --> we have to check some switching
			Distribution overallResult = new Distribution();
			for (int stateValue : afterIndexAdaption.getSupport()) {
				Distribution switchDistribution = mlrs.getSwitchProbability(stateValue / mlrs.getNumberOfDifferentStrategies());
				if (switchDistribution != null) {
					for (int stateValue2 : switchDistribution.getSupport()) {
						overallResult.set(stateValue + stateValue2, afterIndexAdaption.get(stateValue) * switchDistribution.get(stateValue2));
					}
				}
			}
			return overallResult.iterator();
		}
	}

	@Override
	public Iterator<Entry<Integer, Pair<Double, Object>>> getTransitionsAndActionsIterator(int s)
	{
		return null; //no actions, I guess
	}

	@Override
	public void prob0step(BitSet subset, BitSet u, BitSet result)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void prob1step(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void mvMult(double[] vect, double[] result, BitSet subset, boolean complement)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double mvMultSingle(int s, double[] vect)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double mvMultGS(double[] vect, BitSet subset, boolean complement, boolean absolute)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double mvMultJacSingle(int s, double[] vect)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void mvMultRew(double[] vect, MCRewards mcRewards, double[] result, BitSet subset, boolean complement)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public double mvMultRewSingle(int s, double[] vect, MCRewards mcRewards)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void vmMult(double[] vect, double[] result)
	{
		throw new UnsupportedOperationException();
	}
}
