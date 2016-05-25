package strat;

import java.math.BigInteger;

import explicit.Distribution;
import explicit.Model;
import prism.PrismException;
import prism.PrismLog;

//TODO @Christopher: add Documentation
public class zetaNStrategy implements Strategy
{
	BigInteger countSteps;
	
	/**Corresponds to j in paper*/
	int phase;
	
	BigInteger stepsUntilNextPhase;
	int state; 

	@Override
	public void init(int state) throws InvalidStrategyStateException
	{
		this.state=state;
		this.phase=0;
		this.countSteps=BigInteger.valueOf(0);
		this.stepsUntilNextPhase=n(0);
	}

	@Override
	public void updateMemory(int action, int state) throws InvalidStrategyStateException
	{
		this.state=state;
		if(stepsUntilNextPhase.equals(BigInteger.ZERO)){
			phase++;
			stepsUntilNextPhase=n(phase);
		}else{
			stepsUntilNextPhase=stepsUntilNextPhase.subtract(BigInteger.ONE);
		}
		countSteps=countSteps.add(BigInteger.ONE);
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		//TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportToFile(String file)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Model buildProduct(Model model) throws PrismException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getInfo()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setInfo(String info)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMemorySize()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getCurrentMemoryElement()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMemory(Object memory) throws InvalidStrategyStateException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getStateDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInitialStateOfTheProduct(int s)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void exportActions(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportIndices(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportInducedModel(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void exportDotFile(PrismLog out)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialise(int s)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(Object action, int s)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object getChoiceAction()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear()
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * returns n_j from paper TODO: cite
	 * TODO: almost instantly overflows --> fix with Math.BigInt 
	 */
	private BigInteger n(long j){
		if(j<0)
			throw new IllegalArgumentException();
		if(j==0){
			return kappa(j+1);
		}
		BigInteger result= kappa(j+1).min( n(j-1)).multiply(BigInteger.valueOf(2L));
		
		for(long i=0;i<j;i++){
			result=result.multiply(BigInteger.valueOf(2L));
		}
		return result;
	}
	
	/**
	 * TODO howto compute it
	 */
	private BigInteger kappa(long j){
		if(j<0)
			throw new IllegalArgumentException();
		return BigInteger.valueOf(10L);
	}
	
	/**
	 * TODO howto compute it
	 * */
	private double M(){
		return phase*10.0;
	}
}
