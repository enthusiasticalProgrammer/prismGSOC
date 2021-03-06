//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vincent Nimal <vincent.nimal@comlab.ox.ac.uk> (University of Oxford)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator.method;

import prism.PrismException;

/**
 * SimulationMethod class for the APMC ("approximate probabilistic model checking")
 * approach of Herault/Lassaigne/Magniette/Peyronnet (VMCAI'04).
 * Case where 'iterations' (number of samples) is unknown parameter.
 */
public class APMCiterations extends APMCMethod
{
	public APMCiterations(double confidence, double approximation)
	{
		this.confidence = confidence;
		this.approximation = approximation;
	}

	@Override
	public void computeMissingParameterBeforeSim() throws PrismException
	{
		double missing = Math.ceil(0.5 * Math.log(2.0 / confidence) / (approximation * approximation));
		if (missing < Integer.MAX_VALUE) {
			numSamples = (int) missing;
			missingParameterComputed = true;
		} else {
			throw new PrismException("Overflow in APMC method: required number of iterations is too high");
		}
	}

	@Override
	public Object getMissingParameter() throws PrismException
	{
		if (!missingParameterComputed)
			computeMissingParameterBeforeSim();
		return numSamples;
	}

	@Override
	public String getParametersString()
	{
		if (!missingParameterComputed)
			return "approximation=" + approximation + ", confidence=" + confidence + ", number of samples=" + "unknown";
		else
			return "approximation=" + approximation + ", confidence=" + confidence + ", number of samples=" + numSamples;
	}

	@Override
	public SimulationMethod clone()
	{
		APMCiterations m = new APMCiterations(confidence, approximation);
		m.numSamples = numSamples;
		m.missingParameterComputed = missingParameterComputed;
		m.prOp = prOp;
		m.theta = theta;
		return m;
	}
}
