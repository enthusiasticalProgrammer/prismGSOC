package explicit;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import prism.PrismException;
import strat.MultiLongRunStrategy;
import strat.XiNStrategy;

public class MultiLongRunMDP extends MultiLongRun
{
	private final MDP mdp;

	public MultiLongRunMDP(MDP mdp, Collection<MDPConstraint> constraints, Collection<MDPObjective> objectives,
			Collection<MDPExpectationConstraint> expConstraints, String method) throws PrismException
	{
		super(constraints, objectives, expConstraints, method);
		this.mdp = mdp;
		this.mecs = computeMECs();
	}

	@Override
	protected int getNumChoicesOfModel(int state)
	{
		return mdp.getNumChoices(state);
	}

	@Override
	protected Model getModel()
	{
		return mdp;
	}

	@Override
	protected Iterator<Entry<Integer, Double>> getTransitionIteratorOfModel(int state, int action)
	{
		return mdp.getTransitionsIterator(state, action);
	}

	@Override
	protected int prepareStateForReward(int state)
	{
		return state;
	}
	
	@Override
	public MultiLongRunStrategy getStrategy()
	{
		double[] lpResult;
		lpResult = solver.getVariableValues(); //computeStrategy actually just added some constraints, which were already there

		int numStates = getNumStatesOfModel();
		Distribution[] transientDistribution = new Distribution[numStates];
		Distribution[] switchProbability = new Distribution[numStates];

		for (int state = 0; state < numStates; state++) {
			double transientSum = 0.0;
			for (int j = 0; j < getNumChoicesOfModel(state); j++) {
				transientSum += lpResult[getVarY(state, j)];
			}
			transientDistribution[state] = getTransientDistributionAt(state, transientSum, lpResult);
		}

		for (BitSet mec : mecs) {
			Distribution mecDistribution = this.getSwitchProbabilityAt(mec);
			for (int state = mec.nextSetBit(0); state >= 0; state = mec.nextSetBit(state + 1)) {
				switchProbability[state] = mecDistribution;
			}
		}

		try {
			if (!solver.getBoolResult()) {
				//LP is infeasible => no strategy exists
				return null;
			}
		} catch (PrismException e) {
			return null;
		}
		return new MultiLongRunStrategy(transientDistribution, switchProbability, getReccurrentDistribution());
	}

	//indirection: N (as number)
	private Distribution getSwitchProbabilityAt(BitSet mec)
	{
		double[] inBetweenResult = new double[1 << getN()];
		for (int state = mec.nextSetBit(0); state >= 0; state = mec.nextSetBit(state + 1)) {
			for (int choice = 0; choice < getNumChoicesOfModel(state); choice++) {
				for (int N = 0; N < 1 << getN(); N++) {
					inBetweenResult[N] += solver.getVariableValues()[getVarX(state, choice, N)];
				}
			}
		}
		double sumForNormalisation=0.0;
		for(int i=0;i<inBetweenResult.length;sumForNormalisation+=inBetweenResult[i++]);
		Distribution result = new Distribution();
		for (int i = 0; i < inBetweenResult.length; i++) {
			result.add(i, inBetweenResult[i]/sumForNormalisation);
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

	private Distribution getTransientDistributionAt(int state, double transientSum, double[] resSt)
	{
		if (transientSum > 0.0) {
			Distribution result = new Distribution();

			for (int j = 0; j < getNumChoicesOfModel(state); j++) {
				result.add(j, resSt[getVarY(state, j)] / transientSum);
			}
			return result;
		}
		return null;
	}
}
