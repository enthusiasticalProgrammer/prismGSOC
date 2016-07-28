package rabinizerPRISMAdapter;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.BiMap;

import acceptance.AcceptanceGenRabinTransition;
import automata.DA;
import jhoafparser.consumer.HOAConsumerPrint;
import jltl2ba.SimpleLTL;
import ltl.Formula;
import ltl.simplifier.Simplifier;
import omega_automaton.Automaton;
import rabinizer.automata.Optimisation;
import rabinizer.automata.Product;
import rabinizer.exec.CLIParser;

public class LTL2DA
{
	/**
	 * returns a DA recognising the specified LTL-formula computed by Rabinizer. This method
	 * should be only called by automata.LTL2DA, in order to keep Rabinizer and LTL2DSTAR
	 * with LTL2BA exchangeable during runtime.
	 * 
	 *    @param an LTL-formula specified by SimpleLTL
	 *    @return A DA with Edges specified as BitSet and generalised Rabin acceptance,
	 *    					which is transition-based.
	 */
	public static DA<BitSet, ? extends AcceptanceGenRabinTransition> getDA(SimpleLTL ltlFormula)
	{
		rabinizer.exec.Main.verbose = false;
		rabinizer.exec.Main.silent = true;

		BiMap<String, Integer> aliases = jltl2baLTLToRabinizerLTLConverter.getAliasesFromSimpleLTL(ltlFormula);
		Formula inputFormula = jltl2baLTLToRabinizerLTLConverter.transformToRabinizerLTL(ltlFormula, aliases);
		Set<Optimisation> optimisations = ltlFormula.containsFrequencyG() ? EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION) : EnumSet.allOf(Optimisation.class);
		Automaton<?, ?> automaton = rabinizer.exec.Main.computeAutomaton(inputFormula,
				ltlFormula.containsFrequencyG() ? CLIParser.AutomatonType.MDP : CLIParser.AutomatonType.TGR, Simplifier.Strategy.AGGRESSIVELY,
				ltl.equivalence.FactoryRegistry.Backend.BDD, optimisations, aliases);
		automaton.toHOA(new HOAConsumerPrint(System.out), aliases);

		if (automaton instanceof Product<?>) {
			return RabinizerToDA.getGenericDA((Product<?>) automaton, aliases);
		}
		throw new RuntimeException(
				"Unfortunately the resulting automaton obtained by Rabinizer had a wrong (or unknown) type. Therefore we cannot build a DA out of it. Something in the rabinizer-prism-interface does probably not work.");
	}
}
