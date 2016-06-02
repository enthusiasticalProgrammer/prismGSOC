package strat;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import explicit.Distribution;
import explicit.Model;
import prism.PrismException;
import prism.PrismLog;

//this strategy is memoryless
public class EpsilonApproximationXiNStrategy implements Strategy
{
	/**
	 * Map, instead of the more straightforward array, because it
	 * looks better upon outputting 
	 */
	@XmlElementWrapper(name = "Choices_as_state_mapped_to_Distribution_of_successors")
	@XmlElement(name = "distribution of successors:")
	private final Map<Integer, Distribution> choices;

	public EpsilonApproximationXiNStrategy(Distribution[] choices)
	{
		this.choices = new HashMap<>();
		for (int i = 0; i < choices.length; i++) {
			this.choices.put(i, choices[i]);
		}
	}

	@Override
	public void init(int state)
	{
		// nothing needs to be done
	}

	@Override
	public void updateMemory(int action, int state)
	{
		// nothing needs to be done
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		if (choices.get(state) == null) {
			throw new InvalidStrategyStateException("Xi_N-strategies are only defined for states in maximal end components.");
		}
		return choices.get(state);
	}

	@Override
	public void reset()
	{
		// nothing needs to be done here

	}

	@Override
	public void exportToFile(String file)
	{
		throw new UnsupportedOperationException("not yet implemented");

	}

	@Override
	public Model buildProduct(Model model) throws PrismException
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	//TODO Override
	@Override
	public String getInfo()
	{
		return "not yet implemented";
	}

	@Override
	public void setInfo(String info)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public int getMemorySize()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public String getType()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Object getCurrentMemoryElement()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void setMemory(Object memory)
	{
		//nothing to do here (because we are memory-less)
	}

	@Override
	public String getStateDescription()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public int getInitialStateOfTheProduct(int s)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void exportActions(PrismLog out)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void exportIndices(PrismLog out)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void exportInducedModel(PrismLog out)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void exportDotFile(PrismLog out)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void initialise(int s)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void update(Object action, int s)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Object getChoiceAction()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

}
