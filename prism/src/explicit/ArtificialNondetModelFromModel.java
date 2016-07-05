package explicit;

import java.io.File;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import parser.State;
import parser.Values;
import parser.VarList;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismLog;
import strat.MDStrategy;

/**
 * Basically this class tries to take a model and it emulates it as nondeterministic model,
 * it est it supports the nondeterministic choices etc. in a way such that
 * there are choices, but only one.
 */
public class ArtificialNondetModelFromModel implements NondetModel
{
	private final Model model;

	public ArtificialNondetModelFromModel(Model m)
	{
		if (m instanceof NondetModel) {
			throw new IllegalArgumentException("You need not use this wrapper class, because you already have a nondeterministic model.");
		}
		model = m;
	}

	@Override
	public ModelType getModelType()
	{
		return model.getModelType();
	}

	@Override
	public int getNumStates()
	{
		return model.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return model.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return model.getInitialStates();
	}

	@Override
	public int getFirstInitialState()
	{
		return model.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(int i)
	{
		return model.isInitialState(i);
	}

	@Override
	public int getNumDeadlockStates()
	{
		return model.getNumDeadlockStates();
	}

	@Override
	public Iterable<Integer> getDeadlockStates()
	{
		return model.getDeadlockStates();
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		return model.getDeadlockStatesList();
	}

	@Override
	public int getFirstDeadlockState()
	{
		return model.getFirstDeadlockState();
	}

	@Override
	public boolean isDeadlockState(int i)
	{
		return model.isDeadlockState(i);
	}

	@Override
	public List<State> getStatesList()
	{
		return model.getStatesList();
	}

	@Override
	public VarList getVarList()
	{
		return model.getVarList();
	}

	@Override
	public Values getConstantValues()
	{
		return model.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		return model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return model.getLabels();
	}

	@Override
	public boolean hasLabel(String name)
	{
		return model.hasLabel(name);
	}

	@Override
	public int getNumTransitions()
	{
		return model.getNumTransitions();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int s)
	{
		return model.getSuccessorsIterator(s);
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		return model.isSuccessor(s1, s2);
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		return model.allSuccessorsInSet(s, set);
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		return model.someSuccessorsInSet(s, set);
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		model.findDeadlocks(fix);
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		model.checkForDeadlocks();
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		model.checkForDeadlocks(except);
	}

	@Override
	public void exportToPrismExplicit(String baseFilename) throws PrismException
	{
		model.exportToPrismExplicit(baseFilename);
	}

	@Override
	public void exportToPrismExplicitTra(String filename) throws PrismException
	{
		model.exportToPrismExplicitTra(filename);
	}

	@Override
	public void exportToPrismExplicitTra(File file) throws PrismException
	{
		model.exportToPrismExplicitTra(file);
	}

	@Override
	public void exportToPrismExplicitTra(PrismLog log)
	{
		model.exportToPrismExplicitTra(log);
	}

	@Override
	public void exportToDotFile(String filename) throws PrismException
	{
		model.exportToDotFile(filename);
	}

	@Override
	public void exportToDotFile(String filename, BitSet mark) throws PrismException
	{
		model.exportToDotFile(filename, mark);
	}

	@Override
	public void exportToDotFile(PrismLog out)
	{
		model.exportToDotFile(out);
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark)
	{
		model.exportToDotFile(out, mark);
	}

	@Override
	public void exportToDotFile(PrismLog out, BitSet mark, boolean showStates)
	{
		model.exportToDotFile(out, mark, showStates);
	}

	@Override
	public void exportToPrismLanguage(String filename) throws PrismException
	{
		model.exportToPrismLanguage(filename);
	}

	@Override
	public void exportStates(int exportType, VarList varList, PrismLog log) throws PrismException
	{
		model.exportStates(exportType, varList, log);
	}

	@Override
	public String infoString()
	{
		return model.infoString();
	}

	@Override
	public String infoStringTable()
	{
		return model.infoStringTable();
	}

	@Override
	public boolean hasStoredPredecessorRelation()
	{
		return model.hasStoredPredecessorRelation();
	}

	@Override
	public PredecessorRelation getPredecessorRelation(PrismComponent parent, boolean storeIfNew)
	{
		return model.getPredecessorRelation(parent, storeIfNew);
	}

	@Override
	public void clearPredecessorRelation()
	{
		model.clearPredecessorRelation();
	}

	@Override
	public int getNumChoices(int s)
	{
		return 1;
	}

	@Override
	public int getMaxNumChoices()
	{
		return 1;
	}

	@Override
	public int getNumChoices()
	{
		return 1;
	}

	@Override
	public Object getAction(int s, int i)
	{
		return null;
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return false;
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		Iterator<Integer> successors = model.getSuccessorsIterator(s);
		int result = 0;
		for (; successors.hasNext(); successors.next()) {
			result++;
		}
		return result;
	}

	@Override
	public boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		Iterator<Integer> successors = model.getSuccessorsIterator(s);
		while (successors.hasNext()) {
			Integer succ = successors.next();
			if (!set.get(succ)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(int s, int i, BitSet set)
	{
		Iterator<Integer> successors = model.getSuccessorsIterator(s);
		while (successors.hasNext()) {
			Integer succ = successors.next();
			if (set.get(succ)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int s, int i)
	{
		if (i != 0) {
			throw new IllegalArgumentException();
		}
		return model.getSuccessorsIterator(s);
	}

	@Override
	public Model constructInducedModel(MDStrategy strat)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportToDotFileWithStrat(PrismLog out, BitSet mark, int[] strat)
	{
		throw new UnsupportedOperationException();

	}
}
