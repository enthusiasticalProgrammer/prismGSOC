package explicit;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import prism.PrismException;
import strat.MDStrategyArray;
import strat.Strategy;

public class MultiLongRunDTMC extends MultiLongRun
{
	private final DTMCProductMLRStrategyAndMDP dtmc;

	public MultiLongRunDTMC(DTMCProductMLRStrategyAndMDP dtmc, Collection<MDPConstraint> constraints, Collection<MDPObjective> objectives,
			Collection<MDPExpectationConstraint> expConstraints, String method) throws PrismException
	{
		super(constraints, objectives, expConstraints, method);
		this.dtmc = dtmc;
		this.mecs = computeMECs();
	}

	@Override
	protected int getNumChoicesOfModel(int state)
	{
		return 1; //a dtmc has always one choice
	}

	@Override
	protected Model getModel()
	{
		return dtmc;
	}

	@Override
	protected Iterator<Entry<Integer, Double>> getTransitionIteratorOfModel(int state, int action)
	{
		if(action!=0){
			throw new IllegalArgumentException("in an MC, we only have one action");
		}
		
		return dtmc.getTransitionsIterator(state);
	}

	@Override
	protected int prepareStateForReward(int state)
	{
		return state/dtmc.getNumStrategies();
	}

	@Override
	public Strategy getStrategy(boolean memoryless)
	{
		int[] choices=new int[dtmc.getNumStates()];
		for(int i=0;i<choices.length;choices[i++]=-2);
		return new MDStrategyArray(new ArtificialNondetModelFromModel(dtmc),choices);
	}
	

}
