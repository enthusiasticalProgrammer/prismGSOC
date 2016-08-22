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

package strat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import explicit.DTMC;
import explicit.DTMCProductMLRStrategyAndMDP;
import explicit.Distribution;
import explicit.MDPSparse;
import explicit.Model;
import prism.PrismLog;

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
	private transient int strategy = -1;

	private transient int lastState = -1;

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
	 * 
	 * @param filename the corresponding filename
	 * @return the MultiLongRunStrategy described by the file
	 */
	public static MultiLongRunStrategy loadFromFile(String filename)
	{
		try {
			File file = new File(filename);
			JAXBContext jc = JAXBContext.newInstance(MultiLongRunStrategy.class);
			Unmarshaller u = jc.createUnmarshaller();
			return (MultiLongRunStrategy) u.unmarshal(file);
		} catch (JAXBException ex) {
			System.out.println("The following exception occurred during the loading of the Strategy.");
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * Creates a multi-long run strategy
	 *
	 * @param transChoices
	 * 			The transient choices.
	 * @param switchProb 
	 * 			The probability of switching in a state to certain recurrent Strategies.
	 * @param recChoices The recurrent strategies
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

	@Override
	public void initialise(int state)
	{
		lastState = state;
		strategy = -1;
	}

	@Override
	public void updateMemory(int action, int state)
	{

		if (!isTransient()) {
			return; //Nothing needs to be updated
		}
		Distribution probabilityToHaveSwitchedToStrategy = new Distribution();
		if (this.transientChoices[lastState] != null) {
			probabilityToHaveSwitchedToStrategy.add(-1, this.transientChoices[lastState].get(action));
		}
		switchProb[lastState].forEach(entry -> {
			try {
				probabilityToHaveSwitchedToStrategy.add(entry.getKey(), entry.getValue() * recurrentChoices[entry.getKey()].getNextMove(lastState).get(action));
			} catch (Exception e) {
				throw new RuntimeException();
			}
		});
		double random = Math.random();
		for (int strat : probabilityToHaveSwitchedToStrategy.getSupport()) {
			if (random < probabilityToHaveSwitchedToStrategy.get(strat) / probabilityToHaveSwitchedToStrategy.sum()) {
				this.strategy = strat;
				lastState = state;
				return;
			}
			random = random - probabilityToHaveSwitchedToStrategy.get(strat) / probabilityToHaveSwitchedToStrategy.sum();
		}
		lastState = state;
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		if (isTransient()) {
			Distribution result = new Distribution();
			Distribution switchProbability = this.switchProb[state].deepCopy();
			switchProbability.forEach(entry -> {
				try {
					recurrentChoices[entry.getKey()].getNextMove(state).forEach(recurrentEntry -> {
						result.add(recurrentEntry.getKey(), recurrentEntry.getValue() * entry.getValue());
					});
				} catch (InvalidStrategyStateException e) {
					throw new RuntimeException(e);
				}
			});
			if (transientChoices[state] != null) {
				this.transientChoices[state].forEach(entry -> result.add(entry.getKey(), entry.getValue()));
			}
			result.normalise();
			return result;
		}
		return this.recurrentChoices[strategy].getNextMove(state);
	}

	@Override
	public void reset()
	{
		this.strategy = -1;
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
		} catch (JAXBException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Model buildProduct(Model model)
	{
		if (model == null) {
			throw new NullPointerException();
		}
		return buildProductFromMDPExplicit((MDPSparse) model);
	}

	public DTMC buildProductFromMDPExplicit(MDPSparse model)
	{
		return new DTMCProductMLRStrategyAndMDP(model, this);
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
	public void setMemory(Object memory)
	{
		if (memory instanceof Integer) {
			int mem = (Integer) memory;
			if (mem < -1 || mem >= recurrentChoices.length) {
				throw new IllegalArgumentException("only values from -1 to " + recurrentChoices.length + " are allowed");
			}
			this.strategy = mem;
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
		throw new UnsupportedOperationException("You can use exportToFileInstead");

	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder("");
		builder.append("transientChoices\n");
		for (int i = 0; i < transientChoices.length; i++) {
			builder.append(i);
			builder.append("  ");
			builder.append(transientChoices[i]);
			builder.append("\n");
		}
		builder.append("\n");
		builder.append("switchProbabilities\n");
		for (int i = 0; i < switchProb.length; i++) {
			builder.append(i);
			builder.append("  ");
			builder.append(switchProb[i]);
			builder.append("\n");
		}

		builder.append("\n");
		builder.append("recurrentChoices\n");
		for (int i = 0; i < recurrentChoices.length; i++) {
			builder.append(i);
			builder.append("  ");
			builder.append(recurrentChoices[i]);
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * This method is important for building the product
	 */
	public int getNumberOfDifferentStrategies()
	{
		return recurrentChoices.length + 1;
	}

	/**
	 * This method returns the Distribution of a strategy in a state.
	 * strategy number 0 is transient, strategy at 1..N+1 is recurrentStrategy[x-1] 
	 * @throws InvalidStrategyStateException if strategy is not defined in this state
	 * @throws IllegalArgumentException, if strategy-number is not valid
	 * @return the transition-Distribution, and not the action-Distribution
	 */
	public Distribution getDistributionOfStrategy(int state, int strategy) throws InvalidStrategyStateException
	{
		if (strategy < 0 || strategy > recurrentChoices.length) {
			throw new IllegalArgumentException("wrong strategy-number");
		}

		if (strategy == 0)
			return transientChoices[state];
		return recurrentChoices[strategy - 1].getNextMove(state);
	}

	public Distribution getSwitchProbability(int i)
	{
		return switchProb[i];
	}

	@Override
	public void clear()
	{
		//nothing to do
	}

	/**
	 * Note that we do not compare the current state/strategy, because they are frequently changing 
	 */
	@Override
	public boolean equals(Object o)
	{
		if (o == null) {
			return false;
		}
		if (o instanceof MultiLongRunStrategy) {
			MultiLongRunStrategy that = (MultiLongRunStrategy) o;
			return Arrays.equals(this.recurrentChoices, that.recurrentChoices) && Arrays.equals(this.switchProb, that.switchProb)
					&& Arrays.equals(this.transientChoices, that.transientChoices);
		}
		return false;
	}
}
