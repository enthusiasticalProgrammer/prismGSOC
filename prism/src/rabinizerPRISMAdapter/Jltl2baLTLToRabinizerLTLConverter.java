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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import jltl2ba.APSet;
import jltl2ba.SimpleLTL;
import ltl.BooleanConstant;
import ltl.Conjunction;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.FrequencyG;
import ltl.GOperator;
import ltl.Literal;
import ltl.UOperator;
import ltl.XOperator;

class Jltl2baLTLToRabinizerLTLConverter
{
	static Formula transformToRabinizerLTL(SimpleLTL simple, BiMap<String, Integer> aliases)
	{
		switch (simple.kind) {
		case AND:
			return Conjunction.create(transformToRabinizerLTL(simple.left, aliases), transformToRabinizerLTL(simple.right, aliases));
		case AP:
			return new Literal(aliases.get(simple.ap));
		case EQUIV:
			Formula l = transformToRabinizerLTL(simple.left, aliases);
			Formula r = transformToRabinizerLTL(simple.right, aliases);
			return Disjunction.create(Conjunction.create(l, r), Conjunction.create(l.not(), r.not()));
		case FALSE:
			return BooleanConstant.FALSE;
		case FINALLY:
			return FOperator.create(transformToRabinizerLTL(simple.left, aliases));
		case GLOBALLY:
			return GOperator.create(transformToRabinizerLTL(simple.left, aliases));
		case FREQ_G:
			System.out.println("in freqG-case,simple: " + simple);
			FrequencyG.Comparison comp = null;
			double bound = simple.bound;
			boolean isLimInf = simple.isLimInf;
			Formula f = transformToRabinizerLTL(simple.left, aliases);
			switch (simple.cmp) {
			case LEQ:
				bound = 1 - bound;
				comp = FrequencyG.Comparison.GT;
				f = f.not();
				break;

			case LT:
				bound = 1 - bound;
				comp = FrequencyG.Comparison.GEQ;
				f = f.not();
				break;

			case GEQ:
				comp = FrequencyG.Comparison.GEQ;
				break;

			case GT:
				comp = FrequencyG.Comparison.GT;
				break;
			default:
				throw new AssertionError("Should never happen unless comparison is changed.");
			}
			FrequencyG result = new FrequencyG(f, bound, comp, isLimInf ? FrequencyG.Limes.INF : FrequencyG.Limes.SUP);
			return result;
		case IMPLIES:
			return Disjunction.create(transformToRabinizerLTL(simple.left, aliases).not(), transformToRabinizerLTL(simple.right, aliases));
		case NEXT:
			return XOperator.create(transformToRabinizerLTL(simple.left, aliases));
		case NOT:
			return transformToRabinizerLTL(simple.left, aliases).not();
		case OR:
			return Disjunction.create(transformToRabinizerLTL(simple.left, aliases), transformToRabinizerLTL(simple.right, aliases));
		case RELEASE:
			return UOperator.create(transformToRabinizerLTL(simple.left, aliases).not(), transformToRabinizerLTL(simple.right, aliases).not()).not();
		case TRUE:
			return BooleanConstant.TRUE;
		case UNTIL:
			return UOperator.create(transformToRabinizerLTL(simple.left, aliases), transformToRabinizerLTL(simple.right, aliases));
		default:
			throw new AssertionError();
		}
	}

	static BiMap<String, Integer> getAliasesFromSimpleLTL(SimpleLTL ltl)
	{
		APSet aps = ltl.getAPs();
		BiMap<String, Integer> result = HashBiMap.create();
		int i = 0;
		for (String ap : aps)
			result.put(ap, i++);
		return result;
	}
}
