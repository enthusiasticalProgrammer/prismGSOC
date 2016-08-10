package strat;

import java.math.BigInteger;

import explicit.Distribution;
import explicit.MDP;
import explicit.Model;
import explicit.MultiLongRun;
import prism.PrismLog;
import prism.PrismUtils;
import solvers.SolverProxyInterface;

/**
 * this is more or less an in-between state of a strategy, because the fact that it outputs the correct
 * strategy means that it is virtually indescribable. Therefore this gets never outputted directly,
 * but we can output an approximation of it.
 * We also ommitted some the computation of kappa and n (from the paper CKK15),
 * because it is irrelevant
 */
public class XiNStrategy implements Strategy
{

	/**Corresponds to j in paper*/
	private BigInteger phase;

	/**Corresponds to N in paper (with trivial mapping [1..n] <--> 2^[1..n])*/
	private int N;

	private double[] solverVariables;

	private MultiLongRun<?> mlr;

	private final MDP mdp;

	public XiNStrategy(SolverProxyInterface solver, MDP mdp, MultiLongRun<?> mlr, int n)
	{
		this.solverVariables = solver.getVariableValues();
		this.mlr = mlr;
		this.N = n;
		this.mdp = mdp;
		phase = BigInteger.ZERO;
	}

	public EpsilonApproximationXiNStrategy computeApproximation()
	{
		/**The 1000000 is important to distinguish between (numerical) rounding errors and desired switching of strategy states*/
		setPhaseForEpsilon(PrismUtils.epsilonDouble * 1000000.0);
		Distribution[] result = new Distribution[mdp.getNumStates()];
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (mlr.isMECState(state)) {
				try {
					result[state] = getNextMove(state);
				} catch (InvalidStrategyStateException e) {
					e.printStackTrace();
					throw new RuntimeException("This exception should never be thrown, because we actually check that it cannot occur.");
				}
			} else {
				result[state] = null;
			}
		}
		return new EpsilonApproximationXiNStrategy(result);

	}

	@Override
	public void initialise(int state)
	{
		this.phase = BigInteger.ZERO;
	}

	@Override
	public void updateMemory(int action, int state)
	{
		//Nothing to do.
		//technically one needs to adjust the phase sometimes, but
		//it is not necessary, because due to readability we only return
		//an approximated version of the strategy
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		if (!mlr.isMECState(state)) {
			throw new InvalidStrategyStateException("a Xi_N strategy can only be computed for states in maximal end components");
		}
		Distribution result = new Distribution();
		for (int action = 0; action < mdp.getNumChoices(state); action++) {
			if (mdp.allSuccessorsInSet(state, action, mlr.getMecOf(state))) {
				double numerator = solverVariables[mlr.getVarX(state, action, N)] + xprime(state);
				double denominator = sumOfPerturbedFrequencies(state);
				result.add(action, numerator / denominator);
			}
		}
		return result;
	}

	@Override
	public void reset()
	{
		phase = BigInteger.ZERO;
	}

	@Override
	public void exportToFile(String file)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Model buildProduct(Model model)
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
	public Object getCurrentMemoryElement()
	{
		return null;
	}

	@Override
	public void setMemory(Object memory)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getStateDescription()
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

	/**
	 * the most important thing is: it gets larger and larger
	 * */
	private double getM()
	{
		return (phase.add(BigInteger.ONE)).doubleValue() * 1000.0;
	}

	/**
	 * sets the phase large enough s.t. the strategy is at least as good as an epsilon-strategy
	 * note that this does not interfere badly with other parts of XiNStrategy, because enlarging phase
	 * makes the strategy better and not worse
	 */
	private void setPhaseForEpsilon(double epsilon)
	{
		while (!isEpsilonApproximation(epsilon)) {
			phase = phase.add(BigInteger.ONE);//because phase can be zero
			phase = phase.multiply(BigInteger.TEN);
		}
	}

	/**
	 * Check if in the current state, the strategy is an epsilon-approximation
	 */
	private boolean isEpsilonApproximation(double epsilon)
	{
		double sumXPrime = 0.0;
		double sumX = 0.0;
		for (int state = 0; state < mdp.getNumStates(); state++) {
			if (mlr.isMECState(state)) {
				for (int action = 0; action < mdp.getNumChoices(state); action++) {
					sumX = sumX + solverVariables[mlr.getVarX(state, action, N)];
					sumXPrime += xprime(state);
				}
			}
		}
		//epsilonDouble is important in case sumX is zero
		return sumXPrime * (1.0 - epsilon) <= sumX * epsilon + PrismUtils.epsilonDouble;
	}

	/**
	 * x'_{a} from paper, here: as function a ---> x'_{a}, dependent also on the state, on M and on
	 * the phase
	 */
	private double xprime(int state)
	{
		return (mdp.getNumChoices(state)) / getM();
	}

	private double sumOfPerturbedFrequencies(int state)
	{
		double sum = 0.0;
		for (int action = 0; action < mdp.getNumChoices(state); action++) {
			if (mlr.isMECState(state)) {
				sum = sum + solverVariables[mlr.getVarX(state, action, N)];
			}
			sum = sum + xprime(state);
		}
		return sum;
	}

	@Override
	public String getInfo()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void setInfo(String info)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear()
	{
		//Nothing to do
	}

	@Override
	public String toString()
	{
		return computeApproximation().toString();
	}

}
