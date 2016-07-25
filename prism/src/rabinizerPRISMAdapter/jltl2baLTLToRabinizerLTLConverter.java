package rabinizerPRISMAdapter;

import java.util.Collection;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import jltl2ba.APSet;
import jltl2ba.SimpleLTL;
import ltl.BooleanConstant;
import ltl.Conjunction;
import ltl.Disjunction;
import ltl.FOperator;
import ltl.Formula;
import ltl.GOperator;
import ltl.Literal;
import ltl.UOperator;
import ltl.XOperator;

public class jltl2baLTLToRabinizerLTLConverter
{
	public static Formula transformToRabinizerLTL(SimpleLTL simple, BiMap<String, Integer> aliases)
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

	public static BiMap<String, Integer> getAliasesFromSimpleLTL(SimpleLTL ltl)
	{
		APSet aps = ltl.getAPs();
		BiMap<String, Integer> result = HashBiMap.create();
		int i = 0;
		for (String ap : aps)
			result.put(ap, i++);
		return result;
	}
}
