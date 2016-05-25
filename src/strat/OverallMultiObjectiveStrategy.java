package strat;

import java.util.Collection;

import explicit.Distribution;
import explicit.Model;
import prism.PrismException;
import prism.PrismLog;

//TODO Christopher: add Documentation
public class OverallMultiObjectiveStrategy implements Strategy
{
	Collection<zetaNStrategy> zetaNStrategies;
	
	zetaNStrategy currentlyActiveStrategy;

	@Override
	public void init(int state) throws InvalidStrategyStateException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void updateMemory(int action, int state) throws InvalidStrategyStateException
	{
		//TODO Auto-generated method stub

	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void exportToFile(String file)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Model buildProduct(Model model) throws PrismException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInfo()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setInfo(String info)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public int getMemorySize()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getCurrentMemoryElement()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMemory(Object memory) throws InvalidStrategyStateException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getStateDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInitialStateOfTheProduct(int s)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void exportActions(PrismLog out)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void exportIndices(PrismLog out)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void exportInducedModel(PrismLog out)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void exportDotFile(PrismLog out)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void initialise(int s)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void update(Object action, int s)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Object getChoiceAction()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear()
	{
		// TODO Auto-generated method stub

	}

}
