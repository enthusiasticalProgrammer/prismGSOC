package strat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import parser.State;
import prism.PrismException;
import prism.PrismLog;
import explicit.Distribution;
import explicit.MDPExplicit;
import explicit.MDPSimple;
import explicit.MDPSparse;
import explicit.Model;
import explicit.NondetModel;
import explicit.STPG;

@XmlRootElement
public class MultiLongRunStrategy implements Strategy, Serializable
{
	public static final long serialVersionUID = 0L;

	// strategy info
	protected String info = "No information available.";

	// storing last state
	protected transient int lastState;

	@XmlElementWrapper(name = "transientChoices")
	@XmlElement(name = "distribution")
	protected Distribution[] transientChoices;

	@XmlElementWrapper(name = "reccurentChoices")
	@XmlElement(name = "distribution")
	protected Distribution[] recurrentChoices;

	@XmlElementWrapper(name = "switchingProbabilities")
	/**For convenience, the first type is Integer, which stands for the state*/
	protected Map<Integer, Distribution> switchProb;
	
	/**-1 for transient and 0...2^N for epsilon_{N}*/
	private transient int strategy;
	private transient boolean isTransient; //represents the single bit of memory

	private MultiLongRunStrategy()
	{
		//for XML serialization by JAXB
	}

	/**
	 * Loads a strategy from a XML file
	 * @param filename
	 */
	public static MultiLongRunStrategy loadFromFile(String filename)
	{
		try {
			File file = new File(filename);
			//InputStream inputStream = new FileInputStream(file);
			JAXBContext jc = JAXBContext.newInstance(MultiLongRunStrategy.class);
			Unmarshaller u = jc.createUnmarshaller();
			return (MultiLongRunStrategy) u.unmarshal(file);
		} catch (JAXBException ex) {
			System.out.println("The following exception occurred during the loading of the Strategy.");
			ex.printStackTrace();
			return null;
		}
	}

	//TODO: the doc is garbage
	/**
	 * 
	 * Creates a multi-long run strategy
	 *
	 * @param minStrat minimising strategy
	 * @param minValues expected values for states for min strategy
	 * @param maxStrat maximising strategy
	 * @param maxValues expected value for states for max strategy
	 * @param targetValue value to be achieved by the strategy
	 * @param model the model to provide info about players and transitions
	 */
	public MultiLongRunStrategy(Distribution[] transChoices, Map<Integer,Distribution> switchProb, Distribution[] recChoices)
	{
		this.transientChoices = transChoices;
		this.switchProb = switchProb;
		this.recurrentChoices = recChoices;
	}

	/**
	 * 
	 * Creates a multi-long run strategy which switches memory elements
	 * as soon as recurrent distr is defined
	 *
	 * @param minStrat minimising strategy
	 * @param minValues expected values for states for min strategy
	 * @param maxStrat maximising strategy
	 * @param maxValues expected value for states for max strategy
	 * @param targetValue value to be achieved by the strategy
	 * @param model the model to provide info about players and transitions
	 */
	public MultiLongRunStrategy(Distribution[] transChoices, Distribution[] recChoices)
	{
		this.switchProb = null;
		this.transientChoices = transChoices;
		this.recurrentChoices = recChoices;
	}

	/**
	 * Creates a ExactValueStrategy.
	 *
	 * @param scan
	 */
	public MultiLongRunStrategy(Scanner scan)
	{
	}

	private int switchToRecurrent(int state)
	{
		if(switchProb.get(state)==null){
			//state not in MEC
			return -1;
		}
		double rand=Math.random();
		for(int i=0;i<switchProb.keySet().size();i++){
			rand -= switchProb.get(state).get(i);
			if(rand<=0.0){
				return i;
			}
		}
		return -1;
	}

	@Override
	public void init(int state) throws InvalidStrategyStateException
	{
		strategy=switchToRecurrent(state);
		System.out.println("init to " + isTransient);
		lastState = state;
	}

	@Override
	public void updateMemory(int action, int state) throws InvalidStrategyStateException
	{
		if (strategy==-1) {
			strategy = switchToRecurrent(state);
		}
		lastState = state;
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		return (isTransient) ? this.transientChoices[state] : this.recurrentChoices[state];
	}

	@Override
	public void reset()
	{
		lastState = -1;
	}

	@Override
	public void exportToFile(String file)
	{
		try {
			JAXBContext context = JAXBContext.newInstance(MultiLongRunStrategy.class);

			Marshaller m = context.createMarshaller();
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			FileWriter out = new FileWriter(new File(file));
			m.marshal(this, out);
			out.close();
		} catch (JAXBException ex) { //TODO do something more clever
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Model buildProduct(Model model) throws PrismException
	{
		return buildProductFromMDPExplicit((MDPSparse) model);
	}

	//TODO find out what this method does and adjust it
	public Model buildProductFromMDPExplicit(MDPSparse model) throws PrismException
	{
/*
		// construct a new STPG of size three times the original model
		MDPSimple mdp = new MDPSimple(3 * model.getNumStates());

		int n = mdp.getNumStates();

		List<State> oldStates = model.getStatesList();

		// creating helper states
		State stateInit = new State(1), stateTran = new State(1), stateRec = new State(1);
		stateInit.setValue(0, 0); // state where memory is not yet initialised
		stateTran.setValue(0, 1); // state where target is minimum elem
		stateRec.setValue(0, 2); // state where target is maximum element

		// creating product state list
		List<State> newStates = new ArrayList<State>(n);
		for (int i = 0; i < oldStates.size(); i++) {
			newStates.add(new State(oldStates.get(i), stateInit));
			newStates.add(new State(oldStates.get(i), stateTran));
			newStates.add(new State(oldStates.get(i), stateRec));
		}

		// setting the states list to STPG
		mdp.setStatesList(newStates);

		// adding choices for the product STPG
		// initial distributions
		int indx;
		Distribution distr;

		distr = new Distribution();
		if (this.switchProb.get(0) != 1)
			distr.add(1, 1 - this.switchProb[0]);
		if (this.switchProb.get(0) != 0)
			distr.add(2, this.switchProb[0]);
		mdp.addChoice(0, distr);

		for (int i = 1; i < oldStates.size(); i++) {
			indx = 3 * i;

			//Add self-loop only
			distr = new Distribution();
			distr.add(indx, 1.0);
			mdp.addChoice(indx, distr);

		}

		// all other states
		for (int i = 0; i < oldStates.size(); i++) {
			int tranIndx = 3 * i + 1;
			int recIndx = 3 * i + 2;

			Distribution distrTranState = new Distribution();
			Distribution distrRecState = new Distribution();

			Distribution choicesTran = this.transientChoices[i];
			Distribution choicesRec = this.recurrentChoices[i];

			//recurrent states
			if (choicesRec != null) { //MEC state
				for (Entry<Integer, Double> choiceEntry : choicesRec) {
					Iterator<Entry<Integer, Double>> iterator = model.getTransitionsIterator(i, choiceEntry.getKey());
					while (iterator.hasNext()) {
						Entry<Integer, Double> transitionEntry = iterator.next();
						distrRecState.add(transitionEntry.getKey(), choiceEntry.getValue() * transitionEntry.getValue());
					}
				}

				mdp.addChoice(recIndx, distrRecState);
			}

			//transient states, switching to recurrent
			if (choicesRec != null) { //MEC state
				for (Entry<Integer, Double> choiceEntry : choicesRec) {
					Iterator<Entry<Integer, Double>> iterator = model.getTransitionsIterator(i, choiceEntry.getKey());
					while (iterator.hasNext()) {
						Entry<Integer, Double> transitionEntry = iterator.next();
						distrTranState.add(transitionEntry.getKey(), switchProb[i] * choiceEntry.getValue() * transitionEntry.getValue());
					}
				}
			}

			//transitent states, not switching
			for (Entry<Integer, Double> choiceEntry : choicesTran) {
				Iterator<Entry<Integer, Double>> iterator = model.getTransitionsIterator(i, choiceEntry.getKey());
				while (iterator.hasNext()) {
					Entry<Integer, Double> transitionEntry = iterator.next();
					distrTranState.add(transitionEntry.getKey(), (1 - switchProb[i]) * choiceEntry.getValue() * transitionEntry.getValue());
				}
			}

			mdp.addChoice(tranIndx, distrTranState);
		}

		// setting initial state for the game
		mdp.addInitialState(0);

		return mdp;*/throw new UnsupportedOperationException();
	}

	@Override
	public String getInfo()
	{
		return info;
	}

	@Override
	public void setInfo(String info)
	{
		this.info = info;
	}

	@Override
	public int getMemorySize()
	{
		return (switchProb == null) ? 0 : 2;
	}

	@Override
	public String getType()
	{
		return "Stochastic update strategy.";
	}

	@Override
	public Object getCurrentMemoryElement()
	{
		return isTransient;
	}

	@Override
	public void setMemory(Object memory) throws InvalidStrategyStateException
	{
		if (memory instanceof Boolean) {
			this.isTransient = (boolean) memory;
		} else
			throw new InvalidStrategyStateException("Memory element has to be a boolean.");
	}

	@Override
	public String getStateDescription()
	{
		StringBuilder s = new StringBuilder();
		if (switchProb != null) {
			s.append("Stochastic update strategy.\n");
			s.append("Memory size: 2 (transient/recurrent phase).\n");
			s.append("Current memory element: ");
			s.append((this.isTransient) ? "transient." : "recurrent.");
		} else {
			s.append("Memoryless randomised strategyy.\n");
			s.append("Current state is ");
			s.append((this.isTransient) ? "transient." : "recurrent.");
		}
		return s.toString();
	}

	@Override
	public int getInitialStateOfTheProduct(int s)
	{
		throw new UnsupportedOperationException();
	}

	public void export(PrismLog out)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void exportActions(PrismLog out)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void initialise(int s)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void update(Object action, int s)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public Object getChoiceAction()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void exportIndices(PrismLog out)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void exportInducedModel(PrismLog out)
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public void exportDotFile(PrismLog out)
	{
		throw new UnsupportedOperationException();

	};
}
