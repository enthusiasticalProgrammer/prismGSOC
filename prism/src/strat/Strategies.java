//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* @aistis
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

/**
 * Class contains methods to work with strategies
 * 
 * @author aistis
 *
 *TODO: this class seems to be a bit unneccessary, maybe we can drop it
 */
public class Strategies
{
	public static final String FORMAT_STRING_MD_STRAT = "$MD.strat-v0.1";
	public static final String FORMAT_STRING_STEP_BOUNDED_STRAT = "$SB.strat-v0.1";
	public static final String FORMAT_STRING_BOUNDED_REW_STRAT = "$RB.strat-v0.1";
	public static final String FORMAT_STRING_EXACT_VALUE_MD_STRAT = "$EVMD.strat-v0.1";

	private Strategies()
	{
		throw new AssertionError("This class should not be initialised.");
	}

	/**
	 * Loads the strategy from the given file
	 * @param filename name/path of the file
	 * @return the generated strategy
	 * @throws IllegalArgumentException if the file format is not recognised or file is not found
	 */
	public static Strategy loadStrategyFromFile(String filename) throws IllegalArgumentException
	{
		return MultiLongRunStrategy.loadFromFile(filename);
		/*try {
			Scanner scan = new Scanner(new File(filename));
			try {
				String type = scan.nextLine();
				//TODO merge with prism-games
				if (type.equals(FORMAT_STRING_MD_STRAT)) {
					return new MemorylessDeterministicStrategy(scan);
				} else if (type.equals(FORMAT_STRING_STEP_BOUNDED_STRAT)) {
					return new StepBoundedDeterministicStrategy(scan);
				} else if (type.equals(FORMAT_STRING_BOUNDED_REW_STRAT)) {
					return new BoundedRewardDeterministicStrategy(scan);
				} else if (type.equals(FORMAT_STRING_EXACT_VALUE_MD_STRAT)) {
					return new ExactValueStrategy(scan);
				}
				
				//TODO type check
				throw new IllegalArgumentException("Format not supported");
			} finally {
				scan.close();
			}
		} catch (FileNotFoundException error) {
			throw new IllegalArgumentException("File not found.");
		}*/
	}

	public static void main(String[] args)
	{
		/*String fn = "md.adv";
		String fn2 = "md2.adv";
		Strategy mdstrat = new MemorylessDeterministicStrategy(new int[] { 1, 2, 4, 6, 2 });
		mdstrat.exportToFile(fn);
		Strategy mdstrat2 = Strategies.loadStrategyFromFile(fn);
		mdstrat2.exportToFile(fn2);
		mdstrat = mdstrat2 = null;
		
		int[][] choices = { { 30, 1, 28, 2 }, { 25, 1, 24, 2 } };
		int bound = 25;
		
		String sbfn = "sb.adv";
		String sbfn2 = "sb2.adv";
		Strategy sbstrat = new StepBoundedDeterministicStrategy(choices, bound);
		sbstrat.exportToFile(sbfn);
		Strategy sbstrat2 = Strategies.loadStrategyFromFile(sbfn);
		sbstrat2.exportToFile(sbfn2);
		
		String f0fn = "examples/f0.adv";
		String f0fn2 = "f02.adv";
		Strategy rbstrat2 = Strategies.loadStrategyFromFile(f0fn);
		rbstrat2.exportToFile(f0fn2);
		
		String exfn = "exact05.adv";
		String exfn2 = "exact052.adv";
		Strategy exstrat2 = Strategies.loadStrategyFromFile(exfn);
		exstrat2.exportToFile(exfn2);
		*/
	}

}
