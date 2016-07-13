package rabinizerPRISMAdapter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;

import acceptance.AcceptanceGenRabinTransition;
import automata.DA;
import jltl2ba.SimpleLTL;
import prism.PrismException;

public class TestLTL2DA
{
	// X X a
	public static SimpleLTL formula1 = new SimpleLTL(SimpleLTL.LTLType.NEXT, new SimpleLTL(SimpleLTL.LTLType.NEXT, new SimpleLTL("a")));

	@Test
	public void TestNoExceptionOccurringForFormula1()
	{
		assertNotNull(LTL2DA.getDA(formula1));
	}

	@Test
	public void HasEdgesWithFirstPropositionNegated() throws PrismException
	{
		DA<BitSet, AcceptanceGenRabinTransition<BitSet>> da = LTL2DA.getDA(formula1);
		BitSet negatedA = new BitSet(1);
		negatedA.set(0, false);
		assertTrue(da.hasEdge(da.getStartState(), negatedA));
	}

	@Test
	public void EdgesDoNotEndInStateLessThanZero()
	{
		DA<BitSet, AcceptanceGenRabinTransition<BitSet>> da = LTL2DA.getDA(formula1);
		for (int i = 0; i < da.size(); i++) {
			Set<DA<BitSet, AcceptanceGenRabinTransition<BitSet>>.Edge> edgesFromState = da.getAllEdgesFrom(i);
			for (DA<BitSet, AcceptanceGenRabinTransition<BitSet>>.Edge edge : edgesFromState) {
				assertTrue(edge.dest >= 0);
			}
		}
	}

	@Test
	public void OneStateHasNotOutgoingEdgesForEachBitSet() throws PrismException
	{
		DA<BitSet, AcceptanceGenRabinTransition<BitSet>> da = LTL2DA.getDA(formula1);
		BitSet bs = new BitSet(1);
		bs.set(0, false);
		assertTrue(IntStream.range(0, da.size()).anyMatch(s -> !da.hasEdge(s, bs)));

	}
}
