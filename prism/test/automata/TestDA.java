package automata;

import static org.junit.Assert.*;

import java.util.BitSet;

import org.junit.Test;

import jltl2ba.SimpleLTL;

public class TestDA
{

	@Test
	public void testMakeComplete_GaRabinizer()
	{
		SimpleLTL formulaGa = new SimpleLTL("a");
		formulaGa = new SimpleLTL(SimpleLTL.LTLType.GLOBALLY, formulaGa);
		DA<BitSet, ?> da = rabinizerPRISMAdapter.LTL2DA.getDA(formulaGa);
		da.complete();
		assertEquals(2, da.size());
		for (int state = 0; state < 2; state++) {
			assertEquals(2, da.getNumEdges(state));
		}
	}
}