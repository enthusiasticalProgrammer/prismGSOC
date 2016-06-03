package strat;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import explicit.Distribution;
import explicit.MultiLongRun;
import explicit.TestMultiLongRun;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismUtils;

//TODO Christopher: add documentation
public class TestMultiLongRunStrategy
{
	TestMultiLongRun tmlr;

	@Before
	public void setUp() throws PrismLangException
	{
		tmlr = new TestMultiLongRun();
		tmlr.setUp();
	}

	/**
	 * This tests whether the transient part of example 2 in paper TODO: which
	 * works fine
	 * Nota Bene: if the numbering of the nodes changes, then the test fails.
	 *TODO Christopher: refactor it such that it works
	 * @throws PrismException 
	 */
	@Test
	public void testExample2Transient() throws PrismException
	{
		MultiLongRun ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP(false);
		ml11.solveDefault();
		MultiLongRunStrategy strat = ml11.getStrategy(false);

		//The numbers are slightly different to example 2 of paper TODO cite paper,
		// because the strategy in the paper is not memoryless, but ours is
		//The if-else is necessary, because sometimes the indices ar egiven the other way round
		if (strat.transientChoices[0].get(0) > 0.5) {
			assertEquals(2.0 / 3.0, strat.transientChoices[0].get(0), PrismUtils.epsilonDouble);
			assertEquals(1.0 / 3.0, strat.transientChoices[0].get(1), PrismUtils.epsilonDouble);
		} else {
			assertEquals(1.0 / 3.0, strat.transientChoices[0].get(0), PrismUtils.epsilonDouble);
			assertEquals(2.0 / 3.0, strat.transientChoices[0].get(1), PrismUtils.epsilonDouble);
		}

	}

	@Test
	public void testNaNNotOccurring() throws PrismException, InvalidStrategyStateException
	{
		MultiLongRun ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP(false);
		ml11.solveDefault();
		MultiLongRunStrategy strat = ml11.getStrategy(false);

		for (int strategy = 0; strategy < strat.recurrentChoices.length; strategy++) {
			for (int state = 0; state < tmlr.m1.getNumStates(); state++) {
				Distribution d = strat.recurrentChoices[strategy].getNextMove(1);

				for (int action = 0; action < tmlr.m1.getNumChoices(state); action++) {
					assertFalse(Double.isNaN(d.get(action)));
				}
			}
		}
	}

	@Test
	public void testRecurrentStrategiesAreNoBusybodiesMeddlingInThingsThatOughtNotToBeMeddledIn() throws PrismException, InvalidStrategyStateException
	{
		MultiLongRun ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP(false);
		ml11.solveDefault();
		MultiLongRunStrategy strat = ml11.getStrategy(false);

		for (int strategy = 0; strategy < strat.recurrentChoices.length; strategy++) {
			Distribution d = strat.recurrentChoices[strategy].getNextMove(1);

			for (int action = 0; action < tmlr.m1.getNumChoices(1); action++) {
				//In this state, we have to take the respective choice anyway, so
				//each strategy has to take it with full probability without making resistance
				assertTrue(d.get(action) > 0.99999);
			}
		}

	}

}
