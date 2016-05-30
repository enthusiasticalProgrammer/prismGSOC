package strat;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlElement;

import explicit.Distribution;
import explicit.MDP;
import explicit.Model;
import explicit.MultiLongRun;
import prism.PrismException;
import prism.PrismLog;
import solvers.SolverProxyInterface;

//TODO @Christopher: add Documentation
public class XiNStrategy implements Strategy
{
	private transient BigInteger countSteps;

	/**Corresponds to j in paper*/
	private transient int phase;

	/**Corresponds to N in paper (with trivial mapping [1..n] <--> 2^[1..n])*/
	@XmlElement
	private int N;

	/**theoretically it is redundant, but it is convenient in the implementation*/
	private transient BigInteger stepsUntilNextPhase;

	@XmlElement
	private double[] solverVariables;

	/**
	 * This is needed because the xml-I/O does not like to process the MDP, because it is an
	 * interface.
	 */
	@XmlElement
	private Map<Integer, Integer> numChoices;

	@XmlElement
	private MultiLongRun mlr;

	private XiNStrategy()
	{
		//needed for XML I/O
	}

	public XiNStrategy(SolverProxyInterface solver, MDP mdp, MultiLongRun mlr, int n)
	{
		this.solverVariables = solver.getVariableValues();
		this.mlr = mlr;
		this.N = n;

		numChoices = new HashMap<>();
		for (int state = 0; state < mdp.getNumStates(); state++) {
			numChoices.put(state, mdp.getNumChoices(state));
		}
	}

	@Override
	public void init(int state) throws InvalidStrategyStateException
	{
		this.phase = 0;
		this.countSteps = BigInteger.valueOf(0);
		this.stepsUntilNextPhase = n(0);
	}

	@Override
	public void updateMemory(int action, int state) throws InvalidStrategyStateException
	{
		if (stepsUntilNextPhase.equals(BigInteger.ZERO)) {
			phase++;
			stepsUntilNextPhase = n(phase);
		} else {
			stepsUntilNextPhase = stepsUntilNextPhase.subtract(BigInteger.ONE);
		}
		countSteps = countSteps.add(BigInteger.ONE);
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		Distribution result = new Distribution();
		for (int action = 0; action < numChoices.get(state); action++) {
			double numerator = solverVariables[mlr.getVarX(state, action, N)] + xprime(state, action);
			double denominator = sumOfPerturbedFrequencies(state);
			result.add(action, numerator / denominator);
		}
		return result;
	}

	@Override
	public void reset()
	{
		phase = 0;
		countSteps = BigInteger.ZERO;
		stepsUntilNextPhase = n(0);
	}

	@Override
	public void exportToFile(String file)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Model buildProduct(Model model) throws PrismException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getMemorySize()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getType()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public BigInteger getCurrentMemoryElement()
	{
		return this.countSteps;
	}

	@Override
	public void setMemory(Object memory) throws InvalidStrategyStateException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getStateDescription()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInitialStateOfTheProduct(int s)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportActions(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportIndices(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportInducedModel(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportDotFile(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void initialise(int s)
	{
		this.phase = 0;
		this.countSteps = BigInteger.ZERO;
		this.stepsUntilNextPhase = n(0);
	}

	@Override
	public void update(Object action, int s)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getChoiceAction()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * returns n_j from paper TODO: cite
	 * TODO: almost instantly overflows --> fix with Math.BigInt 
	 */
	private BigInteger n(long j)
	{
		if (j < 0)
			throw new IllegalArgumentException();
		if (j == 0) {
			return kappa(j + 1);
		}
		BigInteger result = kappa(j + 1).min(n(j - 1)).multiply(BigInteger.valueOf(2L));

		for (long i = 0; i < j; i++) {
			result = result.multiply(BigInteger.valueOf(2L));
		}
		return result;
	}

	/**
	 * TODO howto compute it
	 */
	private BigInteger kappa(long j)
	{
		if (j < 0)
			throw new IllegalArgumentException();
		return BigInteger.valueOf(10L);
	}

	/**
	 * TODO howto compute it
	 * */
	private double M()
	{
		return (phase + 1) * 10.0;
	}

	/**
	 * x'_{a} from paper, here: as function a ---> x'_{a}, dependent also on the state, on M and on
	 * the phase
	 */
	private double xprime(int state, int action)
	{
		return ((double) numChoices.get(state)) / M();
	}

	private double sumOfPerturbedFrequencies(int state)
	{
		double sum = 0.0;
		for (int action = 0; action < numChoices.get(state); action++) {
			sum = sum + solverVariables[mlr.getVarX(state, action, N)];
			sum = sum + xprime(state, action);
		}
		return sum;
	}

	@Override
	public String getInfo()
	{
		return "";
	}

	@Override
	public void setInfo(String info)
	{
	}
}
