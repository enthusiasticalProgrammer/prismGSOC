package strat;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import explicit.Distribution;
import explicit.Model;
import prism.PrismLog;

/**
 * This strategy is memoryless
 * It is currently only used as part of MultiLongRunStrategy, and therefore most of the
 * methods are not needed to be implemented
 */
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
	public void initialise(int state)
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
	public Model buildProduct(Model model)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public String getInfo()
	{
		return "EpsilonApproximationXiNStrategy";
	}

	@Override
	public void setInfo(String info)
	{
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public int getMemorySize()
	{
		return 0;
	}

	@Override
	public String getType()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Object getCurrentMemoryElement()
	{
		return null;
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
	public boolean equals(Object o)
	{
		if (o.getClass() != this.getClass()) {
			return false;
		}
		EpsilonApproximationXiNStrategy other = (EpsilonApproximationXiNStrategy) o;
		return choices.equals(other.choices);
	}

	@Override
	public void clear()
	{
		// nothing to do
	}
}
