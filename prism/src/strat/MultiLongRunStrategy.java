package strat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import prism.PrismException;
import prism.PrismLog;
import explicit.DTMC;
import explicit.DTMCProductMLRStrategyAndMDP;
import explicit.Distribution;
import explicit.MDPSparse;
import explicit.Model;

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
	}

	@Override
	public void updateMemory(int action, int state)
	{
		setRecurrency(state);
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
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
	public DTMC buildProductFromMDPExplicit(MDPSparse model) throws PrismException
	{
		return new DTMCProductMLRStrategyAndMDP(model,this);
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
	public void exportActions(PrismLog out)
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
	
	/**
	 * This method is important for building the product
	 */
	public int getNumberOfDifferentStrategies(){
		return recurrentChoices.length+1;
	}
	/**
	 * This method returns the Distribution of a strategy in a state.
	 * strategy number 0 is transient, strategy at 1..N+1 is recurrentStrategy[x-1] 
	 * @throws InvalidStrategyStateException if strategy is not defined in this state
	 * @throws IllegalArgumentException, if strategy-number is not valid
	 */
	public Distribution getDistributionOfStrategy(int state, int strategy) throws InvalidStrategyStateException{
		if(strategy<0 || strategy>recurrentChoices.length){
			throw new IllegalArgumentException("wrong strategy-number");
		}
		if(strategy==0){
			return transientChoices[state]; 
		}
		return recurrentChoices[strategy-1].getNextMove(state);
	}

	public Distribution getSwitchProbability(int i)
	{
		return switchProb[i];
	}
}
