package strat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import prism.PrismException;
import prism.PrismLog;
import prism.PrismUtils;
import explicit.Distribution;
import explicit.MDPSimple;
import explicit.MDPSparse;
import explicit.Model;
import parser.State;

@XmlRootElement
public class MultiLongRunStrategy implements Strategy, Serializable
{
	public static final long serialVersionUID = 0L;

	// strategy info
	protected String info = "No information available.";

	@XmlElementWrapper(name = "transientChoices")
	@XmlElement(name = "distribution", nillable = true)
	protected final Distribution[] transientChoices;

	/**
	 * type is used, because xml is not happy about interfaces
	 */
	@XmlElementWrapper(name = "recurrentChoices")
	@XmlElement(name = "One_recurrent_strategy")
	protected final EpsilonApproximationXiNStrategy[] recurrentChoices;

	@XmlElementWrapper(name = "switchingProbabilities")
	@XmlElement(name = "distribution", nillable = true)
	/**The offset is the state*/
	protected final Distribution[] switchProb;

	/**-1 for transient and 0...2^N for epsilon_{N}*/
	private transient int strategy;

	/**
	 * This constructior is important for xml-I/O, do not remove it
	 * even it does not seem to be called
	 */
	@SuppressWarnings("unused")
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
		//TODO Christopher: adjust it
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
		this.recurrentChoices = new EpsilonApproximationXiNStrategy[recChoices.length];
		for (int i = 0; i < recChoices.length; i++) {
			recurrentChoices[i] = recChoices[i].computeApproximation();
		}
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
		this(transChoices, null, recChoices);
	}

	private void setRecurrency(int state)
	{
		if (strategy != -1)
			return;
		if (switchProb[state] == null) {
			//state not in MEC
			strategy = -1;
		} else {
			double rand = Math.random();
			for (int i = 0; i < switchProb.length; i++) {
				rand -= switchProb[state].get(i);
				if (rand <= 0.0) {
					strategy = i;
					return;
				}
			}
			strategy = -1;
		}
	}

	@Override
	public void init(int state)
	{
		strategy = -1;
		setRecurrency(state);
		System.out.println("init to " + isTransient());
	}

	@Override
	public void updateMemory(int action, int state)
	{
		setRecurrency(state);
		System.out.println("in updateMemory,strategy: " + strategy + " state: " + state);
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		System.out.println("in getNextMove, strategy: " + strategy);
		return (isTransient()) ? this.transientChoices[state] : this.recurrentChoices[strategy].getNextMove(state);
	}

	@Override
	public void reset()
	{
		//nothing to do here
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

	//TODO verify under strategy is not working (but however output with strategy does)
	public Model buildProductFromMDPExplicit(MDPSparse model) throws PrismException
	{
		// construct a new STPG of size three times the original model
		MDPSimple mdp = new MDPSimple((recurrentChoices.length + 1) * model.getNumStates());

		List<State> oldStates = model.getStatesList();

		// creating helper states
		State[] strategyStates = new State[recurrentChoices.length + 1];
		for (int i = 0; i < strategyStates.length; i++) {
			strategyStates[i] = new State(1);
			strategyStates[i].setValue(0, i);
		}

		// creating product state list
		List<State> newStates = new ArrayList<State>(mdp.getNumStates());
		for (State oldState : oldStates) {
			for (State strategyState : strategyStates)
				newStates.add(new State(oldState, strategyState));
		}

		// setting the states list to STPG
		mdp.setStatesList(newStates);

		//take care of the transitions from the transient states
		for (int oldState = 0; oldState < oldStates.size(); oldState++) {
			System.out.println("somewhere in buildProduct");
			//add transient-Choices
			if (transientChoices[oldState] != null) {
				System.out.println("trans");
				Distribution newTransientChoice = new Distribution();
				for (int action = 0; action < model.getNumChoices(oldState); action++) {
					System.out.println("in action");
					Iterator<Map.Entry<Integer, Double>> iterator = model.getTransitionsIterator(oldState, action);
					while (iterator.hasNext()) {
						Map.Entry<Integer, Double> entry = iterator.next();
						newTransientChoice.add(entry.getKey() * (recurrentChoices.length + 1), transientChoices[oldState].get(action) * entry.getValue());
					}
				}
				System.out.println("oldState:" + oldState);
				System.out.println("newTransientChoice:" + newTransientChoice);

				Distribution newTransientChoiceWithSwitch = new Distribution();

				Iterator<Map.Entry<Integer, Double>> switchIterator = newTransientChoice.iterator();
				while (switchIterator.hasNext()) {
					Map.Entry<Integer, Double> entry = switchIterator.next();
					for (int strategy = 0; strategy < recurrentChoices.length; strategy++) {
						if (switchProb[entry.getKey() / (recurrentChoices.length + 1)] != null) {
							newTransientChoiceWithSwitch.add(entry.getKey() + strategy + 1,
									entry.getValue() * switchProb[entry.getKey() / (recurrentChoices.length + 1)].get(strategy));

							System.out.println("entry: " + entry);
							System.out.println("strategy: " + strategy);
							System.out.println("switch here: " + switchProb[entry.getKey() / (recurrentChoices.length + 1)].get(strategy));

						}
					}
				}

				System.out.println("oldState:" + oldState);
				System.out.println("newTransientChoiceWithSwitch:" + newTransientChoiceWithSwitch);

				//add transitions that are not switching the strategy (and give them the remaining probability)
				for (int oldTargetState = 0; oldTargetState < model.getNumStates(); oldTargetState++) {
					double sum = 0.0;
					for (int i = 0; i <= recurrentChoices.length;i++){
						 sum += newTransientChoiceWithSwitch.get(oldTargetState * (recurrentChoices.length + 1) + i);
						 System.out.println("sum: "+sum);
						 System.out.println("i:"+i);
						 System.out.println("oldTargetState:"+oldTargetState);
					}
						;
					System.out.println("sum: "+sum);
					if (sum + PrismUtils.epsilonDouble <= newTransientChoice.get(oldTargetState * (recurrentChoices.length + 1))) {
						newTransientChoiceWithSwitch.add(oldTargetState * (recurrentChoices.length + 1),
								newTransientChoice.get(oldTargetState * (recurrentChoices.length + 1)) - sum);
					}
				}
				System.out.println("oldState:" + oldState);
				System.out.println("newTransientChoiceWithSwitch after adding:" + newTransientChoiceWithSwitch);
				int errorCode = mdp.addChoice(oldState * (recurrentChoices.length + 1), newTransientChoiceWithSwitch);
				if (errorCode == -1) {
					throw new RuntimeException("something in buildProduct went wrong");
				}
			}

		}

		//TODO Christopher: does not work
		//take care of the transitions from the recurrent states
		for (int recurrentStrategy = 0; recurrentStrategy < recurrentChoices.length; recurrentStrategy++) {
			for (int oldState = 0; oldState < oldStates.size(); oldState++) {
				Distribution newRecurrentChoice = new Distribution();
				for (int action = 0; action < model.getNumChoices(oldState); action++) {
					Iterator<Map.Entry<Integer, Double>> iterator = model.getTransitionsIterator(oldState, action);
					while (iterator.hasNext()) {
						Map.Entry<Integer, Double> entry = iterator.next();
						try {
							newRecurrentChoice.add(entry.getKey() * (recurrentChoices.length + 1)+recurrentStrategy+1,
									recurrentChoices[recurrentStrategy].getNextMove(oldState).get(action) * entry.getValue());
						} catch (InvalidStrategyStateException e) {
							continue; //in this case, the outgoing transitions from the respective
										//states do not matter, because they are unreachable
						}
					}
				}

				mdp.addChoice(oldState * (recurrentChoices.length + 1) + recurrentStrategy+1, newRecurrentChoice);
			}
		}

		mdp.addInitialState(0);
		System.out.println("mdp:" + mdp.toString());
		return mdp;
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
		//TODO overwrite
		return "Stochastic update strategy.";
	}

	/**
	 * Integer is used for now. Note that an enum in the size [-1...2^N-1]
	 * would suffice (N: defined in paper CKK15] for denoting, which
	 * recurrent strategy is used (or -1 if the transient strategy is yet used),
	 * but N is not fixed).
	 */
	@Override
	public Integer getCurrentMemoryElement()
	{
		return strategy;
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
		if (memory instanceof Integer) {
			int mem = (Integer) memory;
			if (mem < -1 || mem >= recurrentChoices.length) {
				throw new IllegalArgumentException("only values from -1 to " + recurrentChoices.length + " are allowed");
			}
			this.strategy = (int) mem;
		} else {
			throw new IllegalArgumentException("Integer is required as argument, current type: " + memory.getClass());
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
		result += "transientChoices\n";
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
		if (strategy > -2) {
			throw new IllegalArgumentException();
		}
		return result;
	}
}
