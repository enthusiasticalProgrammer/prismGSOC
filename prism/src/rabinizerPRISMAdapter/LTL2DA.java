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
import rabinizer.automata.ProductRabinizer;
import rabinizer.exec.CLIParser;

public class LTL2DA
{
	public static DA<BitSet, AcceptanceGenRabinTransition<BitSet>> getDA(SimpleLTL ltlFormula)
	{
		BiMap<String, Integer> aliases = jltl2baLTLToRabinizerLTLConverter.getAliasesFromSimpleLTL(ltlFormula);
		Formula inputFormula = jltl2baLTLToRabinizerLTLConverter.transformToRabinizerLTL(ltlFormula, aliases);

		Set<Optimisation> optimisations = EnumSet.allOf(Optimisation.class);
		optimisations.remove(Optimisation.SLAVE_SUSPENSION);

		Automaton<?, ?> automaton = rabinizer.exec.Main.computeAutomaton(inputFormula, CLIParser.AutomatonType.TGR, Simplifier.Strategy.AGGRESSIVELY,
				ltl.equivalence.FactoryRegistry.Backend.BDD, optimisations, aliases);
		automaton.toHOA(new HOAConsumerPrint(System.out), aliases);

		if (automaton instanceof ProductRabinizer) {
			return RabinizerToDA.getDAFromRabinizer((ProductRabinizer) automaton, aliases);
		}
		throw new RuntimeException(
				"Unfortunately the resulting automaton obtained by Rabinizer had a wrong (or unknown) type. Therefore we cannot build a DA out of it. Something in the rabinizer-prism-interface does probably not work.");
	}
}
