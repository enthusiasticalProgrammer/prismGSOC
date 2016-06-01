package strat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import prism.PrismException;
import prism.PrismLog;
import explicit.Distribution;
import explicit.MDPSparse;
import explicit.Model;

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
	protected final Distribution[] transientChoices;

	@XmlElementWrapper(name = "reccurentChoices")
	@XmlElement(name = "distribution")
	protected final Strategy[] recurrentChoices;

	@XmlElementWrapper(name = "switchingProbabilities")
	/**The offset is the state*/
	protected final Distribution[] switchProb;

	/**-1 for transient and 0...2^N for epsilon_{N}*/
	private transient int strategy;

	private MultiLongRunStrategy()
	{
		this.switchProb = null;
		this.recurrentChoices = null;
		this.transientChoices = null;
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
	public MultiLongRunStrategy(Distribution[] transChoices, Distribution[] switchProb, XiNStrategy[] recChoices)
	{
		this.transientChoices = transChoices;
		this.switchProb = switchProb;
		this.recurrentChoices = recChoices;
	}

	//TODO check if this still works
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
	public MultiLongRunStrategy(Distribution[] transChoices, XiNStrategy[] recChoices)
	{
		this.switchProb = null;
		this.transientChoices = transChoices;
		this.recurrentChoices = recChoices;
	}

	private int switchToRecurrent(int state)
	{
		if (switchProb[state] == null) {
			//state not in MEC
			return -1;
		}
		double rand = Math.random();
		for (int i = 0; i < switchProb.length; i++) {
			rand -= switchProb[state].get(i);
			if (rand <= 0.0) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void init(int state) throws InvalidStrategyStateException
	{
		strategy = switchToRecurrent(state);
		System.out.println("init to " + isTransient());
		lastState = state;
	}

	@Override
	public void updateMemory(int action, int state)
	{
		if (strategy == -1) {
			strategy = switchToRecurrent(state);
		}
		lastState = state;
	}

	//TODO Christopher: add some documentation
	public void switchToApproximate()
	{
		for (int i = 0; i < recurrentChoices.length; i++) {
			if (recurrentChoices[i] instanceof XiNStrategy) {
				recurrentChoices[i] = ((XiNStrategy) recurrentChoices[i]).computeApproximation();
			}
		}
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		return (isTransient()) ? this.transientChoices[state] : this.recurrentChoices[strategy].getNextMove(state);
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
	public Model buildProduct(Model model)
	{
		System.out.println("in buildProduct");
		try {
			return buildProductFromMDPExplicit((MDPSparse) model);
		} catch (PrismException e) {
			// TODO Auto-generated catch block
			System.out.println("something bad happened in buildProduct");
			e.printStackTrace();
		}
		return null;
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

	/**
	 * I will use for now boolean/List<Integer/BigInteger>, because depending on the complexity of the problem it could
	 * be either boolean or BigInteger or Tuple(int,BigInteger).
	 * This might be a bit redundant, compare to Multigain- and other paper
	 */
	@Override
	public Object getCurrentMemoryElement()
	{
		if (isTransient()) {
			return true;
		} else {
			List<Object> result = new ArrayList<>();
			result.add(strategy);
			result.add(recurrentChoices[strategy].getCurrentMemoryElement());
			return result;
		}
	}

	private boolean isTransient()
	{
		return strategy == -1;
	}

	/**
	 * Has to be boolean or List<Object> where first element is int and second one is BigInteger
	 * Note that this could maybe be optimised a bit.
	 */
	@Override
	public void setMemory(Object memory) throws InvalidStrategyStateException
	{
		if (memory instanceof Boolean) {
			if ((boolean) memory) {
				this.strategy = -1;
				for (Strategy rec : recurrentChoices) {
					rec.reset();
				}
			}
		} else {
			if (memory instanceof List) {
				List list = (List) memory;
				if (list.size() != 2) {
					throw new InvalidStrategyStateException("Use List only if first element is int and second one BigInteger");
				}
				if (!(list.get(0) instanceof Integer))
					throw new InvalidStrategyStateException("Use List only if first element is int and second one BigInteger");

				if (!(list.get(1) instanceof BigInteger))
					throw new InvalidStrategyStateException("Use List only if first element is int and second one BigInteger");

				this.strategy = (int) list.get(0);
				for (Strategy rec : recurrentChoices) {
					rec.reset();
				}
				recurrentChoices[strategy].setMemory(list.get(1));
			}
		}
	}

	@Override
	public String getStateDescription()
	{
		StringBuilder s = new StringBuilder();
		if (switchProb != null) {
			s.append("Stochastic update strategy.\n");
			s.append("Memory size: 2 (transient/recurrent phase).\n");
			s.append("Current memory element: ");
			s.append((this.isTransient()) ? "transient." : "recurrent.");
		} else {
			s.append("Memoryless randomised strategyy.\n");
			s.append("Current state is ");
			s.append((this.isTransient()) ? "transient." : "recurrent.");
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
		strategy = -1;
		for (Strategy strategy : this.recurrentChoices) {
			strategy.initialise(s);
		}

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

	}

	public String toString()
	{
		String result = "";
		result += "transientChoices\nabc";
		for (int i = 0; i < transientChoices.length; i++) {
			result += i + "  " + transientChoices[i] + "\n";
		}
		result += "\n";
		result += "switchProbabilities\n";
		for (int i = 0; i < switchProb.length; i++) {
			result += i + "  " + switchProb[i] + "\n";
		}

		result += "\n";
		result += "recurrentChoices\n";
		for (int i = 0; i < recurrentChoices.length; i++) {
			result += i + "  " + recurrentChoices[i] + "\n";
		}
		return result;
	}
}
