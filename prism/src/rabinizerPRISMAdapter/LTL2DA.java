//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Christopher Ziegler <ga25suc@mytum.de>
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

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
import ltl.equivalence.EquivalenceClassFactory;
import ltl.visitors.RestrictToFGXU;
import omega_automaton.collections.valuationset.BDDValuationSetFactory;
import omega_automaton.collections.valuationset.ValuationSetFactory;
import rabinizer.automata.AbstractAutomatonFactory;
import rabinizer.automata.DTGRAFactory;
import rabinizer.automata.DTGRMAFactory;
import rabinizer.automata.Optimisation;
import rabinizer.automata.Product;
import rabinizer.frequencyLTL.MojmirOperatorVisitor;

/**
 * This class is the overall class to transform Rabinizer automata into DAs
 */
public class LTL2DA
{
	/**
	 * returns a DA recognising the specified LTL-formula computed by Rabinizer. This method
	 * should be only called by automata.LTL2DA, in order to keep Rabinizer and LTL2DSTAR
	 * with LTL2BA exchangeable during runtime.
	 * 
	 *    @param ltlFormula an LTL-formula specified by SimpleLTL
	 *    @return A DA with Edges specified as BitSet and generalised Rabin acceptance,
	 *    					which is transition-based.
	 */
	public static DA<BitSet, ? extends AcceptanceGenRabinTransition> getDA(SimpleLTL ltlFormula)
	{
		BiMap<String, Integer> aliases = jltl2baLTLToRabinizerLTLConverter.getAliasesFromSimpleLTL(ltlFormula);
		Formula inputFormula = jltl2baLTLToRabinizerLTLConverter.transformToRabinizerLTL(ltlFormula, aliases);
		inputFormula = inputFormula.accept(new RestrictToFGXU());
		if (ltlFormula.containsFrequencyG()) {
			inputFormula = inputFormula.accept(new MojmirOperatorVisitor());
		}

		Set<Optimisation> optimisations = ltlFormula.containsFrequencyG() ? EnumSet.of(Optimisation.COMPUTE_ACC_CONDITION) : EnumSet.allOf(Optimisation.class);
		EquivalenceClassFactory factory = ltl.equivalence.FactoryRegistry.createEquivalenceClassFactory(inputFormula);
		ValuationSetFactory valuationSetFactory = new BDDValuationSetFactory(aliases.values().size());

		AbstractAutomatonFactory<?, ?, ?> automataFactory;
		if (ltlFormula.containsFrequencyG()) {
			automataFactory = new DTGRMAFactory(inputFormula, factory, valuationSetFactory, optimisations);
		} else {
			automataFactory = new DTGRAFactory(inputFormula, factory, valuationSetFactory, optimisations);
		}

		Product<?> dtgra = automataFactory.constructAutomaton();
		dtgra.toHOA(new HOAConsumerPrint(System.out), aliases);

		return RabinizerToDA.getGenericDA(dtgra, aliases);
	}
}
