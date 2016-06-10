package explicit;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;

import prism.PrismException;
import strat.MDStrategyArray;
import strat.Strategy;

public class MultiLongRunDTMC extends MultiLongRun
{
	private final @NonNull DTMCProductMLRStrategyAndMDP dtmc;

	public MultiLongRunDTMC(@NonNull DTMCProductMLRStrategyAndMDP dtmc,@NonNull Collection<@NonNull MDPConstraint> constraints, @NonNull Collection<@NonNull MDPObjective> objectives,
			@NonNull Collection<@NonNull MDPExpectationConstraint> expConstraints,@NonNull String method) throws PrismException
	{
		super(constraints, objectives, expConstraints, method, new ArtificialNondetModelFromModel(dtmc));
		this.dtmc = dtmc;
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
	public Strategy getStrategy()
	{
		int[] choices=new int[dtmc.getNumStates()];
		for(int i=0;i<choices.length;choices[i++]=-2)
			;
		return new MDStrategyArray(new ArtificialNondetModelFromModel(dtmc),choices);
	}
	

}
