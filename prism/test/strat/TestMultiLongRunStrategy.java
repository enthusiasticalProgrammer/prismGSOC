package strat;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import explicit.DTMCModelChecker;
import explicit.Distribution;
import explicit.Model;
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
	public void setUp() throws PrismLangException, PrismException
	{
		tmlr = new TestMultiLongRun();
		tmlr.setUp();
	}

	/**
	 * This tests whether the transient part of example 2 in paper CKK15
	 * works fine
	 * Nota Bene: if the numbering of the nodes changes, then the test fails.
	 *TODO Christopher: refactor it such that it works
	 * @throws PrismException 
	 * @throws InvalidStrategyStateException 
	 */
	@Test
	public void testExample2Transient() throws PrismException, InvalidStrategyStateException
	{
		MultiLongRun<?> ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();
		MultiLongRunStrategy strat = (MultiLongRunStrategy) ml11.getStrategy();

		//The numbers are slightly different to example 2 of paper TODO cite paper,
		// because the strategy in the paper is not memoryless, but ours is
		//The if-else is necessary, because sometimes the indices ar egiven the other way round
		if (strat.transientChoices[0].get(0) > 0.5) {
			assertEquals(2.0 / 3.0, strat.transientChoices[0].get(0), PrismUtils.epsilonDouble);
			assertEquals(1.0 / 3.0, strat.transientChoices[0].get(1), PrismUtils.epsilonDouble);
		} else {
			assertEquals(1.0 / 3.0, strat.getNextMove(0).get(0), PrismUtils.epsilonDouble);
			assertEquals(2.0 / 3.0, strat.getNextMove(0).get(1), PrismUtils.epsilonDouble);
		}

	}

	@Test
	public void testNaNNotOccurring() throws PrismException, InvalidStrategyStateException
	{
		MultiLongRun<?> ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();
		MultiLongRunStrategy strat = (MultiLongRunStrategy) ml11.getStrategy();

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
		MultiLongRun<?> ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();
		MultiLongRunStrategy strat = (MultiLongRunStrategy) ml11.getStrategy();

		for (int strategy = 0; strategy < strat.recurrentChoices.length; strategy++) {
			Distribution d = strat.recurrentChoices[strategy].getNextMove(1);

			for (int action = 0; action < tmlr.m1.getNumChoices(1); action++) {
				//In this state, we have to take the respective choice anyway, so
				//each strategy has to take it with full probability without making resistance
				assertEquals(1.0, d.get(action), PrismUtils.epsilonDouble);
			}
		}
	}

	@Test
	public void testStrategyIsAlwaysDefined() throws PrismException, InvalidStrategyStateException
	{
		MultiLongRun<?> ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();
		MultiLongRunStrategy strat = (MultiLongRunStrategy) ml11.getStrategy();

		for (int i = 0; i < 1000; i++) {//updateMemory is nondeterministic, therefore the loop
			strat.initialise(0);
			strat.updateMemory(0, 2); //action does not matter, important is that we are in state 1
			Distribution d = strat.getNextMove(2);
			assertEquals(1.0, d.sum(), PrismUtils.epsilonDouble);
		}
	}

	@Test
	public void testIfProductIsFeasible() throws PrismException
	{
		MultiLongRun<?> ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();
		MultiLongRunStrategy strat = (MultiLongRunStrategy) ml11.getStrategy();

		Model m = strat.buildProduct(tmlr.m1);
		DTMCModelChecker mc = new DTMCModelChecker(null);
		mc.buildInitialDistribution(m);
		mc.setModulesFileAndPropertiesFile(tmlr.modulesFile, tmlr.propertiesFile);
		mc.setSettings(tmlr.defaultSettingsForSolvingMultiObjectives);
		assertNotEquals(tmlr.infeasible, mc.check(m, tmlr.e1));
	}

	/**
	 * This test basically goes over a strategy solving Example15 from paper CKK15 and checks all
	 * the distributions. 
	 */
	@Test
	public void testSecondModelFromPaper() throws PrismException, InvalidStrategyStateException
	{
		MultiLongRun<?> ml21 = tmlr.mdp21.createMultiLongRun(tmlr.m2, tmlr.e21);
		ml21.createMultiLongRunLP();
		ml21.solveDefault();
		MultiLongRunStrategy strat = (MultiLongRunStrategy) ml21.getStrategy();

		strat.initialise(0);
		Distribution d = strat.getNextMove(0);
		assertEquals(0.1, d.get(0), PrismUtils.epsilonDouble);
		assertEquals(0.9, d.get(1), PrismUtils.epsilonDouble);

		strat.updateMemory(0, 0);
		d = strat.getNextMove(0);
		assertEquals(1.0, d.get(0), PrismUtils.epsilonDouble);
		assertEquals(Collections.singleton(0), d.getSupport());

		strat.initialise(0);
		strat.updateMemory(1, 1);
		d = strat.getNextMove(1);
		assertEquals(1.0, d.get(0), PrismUtils.epsilonDouble);
		assertEquals(Collections.singleton(0), d.getSupport());

		strat.initialise(0);
		strat.updateMemory(1, 2);
		d = strat.getNextMove(2);
		assertEquals(1.0, d.get(0), PrismUtils.epsilonDouble);
		assertEquals(Collections.singleton(0), d.getSupport());
	}
}
