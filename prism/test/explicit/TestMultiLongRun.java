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

package explicit;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import parser.ast.ExpressionFunc;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.type.TypeBool;
import prism.Filter;
import prism.Prism;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismSettings;
import prism.PrismUtils;
import simulator.ModulesFileModelGenerator;

public class TestMultiLongRun
{
	public MDP m1;
	public MDP m2;

	public MDPModelChecker mdp11;
	public MDPModelChecker mdp12;
	public MDPModelChecker mdp13;

	public MDPModelChecker mdp21;

	public ExpressionFunc e1;
	public ExpressionFunc e2;
	public ExpressionFunc e3;
	public ExpressionFunc e21;

	public ModulesFile modulesFile = null;
	public ModulesFile modulesFile2 = null;

	public PropertiesFile propertiesFile = null;
	public PropertiesFile propertiesFile2 = null;
	public PropertiesFile propertiesFile3 = null;
	public PropertiesFile propertiesFile21 = null;

	public PrismSettings defaultSettingsForSolvingMultiObjectives;

	public StateValues infeasible;

	public TestMultiLongRun() throws PrismLangException, PrismException
	{
		setUp();
		this.defaultSettingsForSolvingMultiObjectives = new PrismSettings();
		this.defaultSettingsForSolvingMultiObjectives.set(PrismSettings.PRISM_MDP_MULTI_SOLN_METHOD, "Linear programming");
	}

	@Before
	public void setUp() throws PrismLangException
	{

		try {
			PrismLog mainLog = new PrismFileLog("/dev/null");
			Prism prism = new Prism(mainLog);
			prism.setMDPMultiSolnMethod(Prism.MDP_MULTI_LP); //nothing else is currently supported (besides maybe Gurobi)
			modulesFile = prism.parseModelFile(new File("test/testInputs/CKK15Examples/CKK15Model.nm"));
			modulesFile.setUndefinedConstants(null);

			modulesFile2 = prism.parseModelFile(new File("test/testInputs/CKK15Examples/CKK15MiniModel.nm"));
			modulesFile2.setUndefinedConstants(null);

			propertiesFile = prism.parsePropertiesFile(modulesFile, new File("test/testInputs/CKK15Examples/CKK15PropertyFile1.props"));
			propertiesFile.setUndefinedConstants(null);
			PrismExplicit pe = new PrismExplicit(prism.getMainLog(), prism.getSettings());
			pe.setSettings(prism.getSettings());

			propertiesFile2 = prism.parsePropertiesFile(modulesFile, new File("test/testInputs/CKK15Examples/CKK15PropertyFile2.props"));
			propertiesFile2.setUndefinedConstants(null);

			propertiesFile3 = prism.parsePropertiesFile(modulesFile, new File("test/testInputs/CKK15Examples/CKK15PropertyFile3.props"));
			propertiesFile3.setUndefinedConstants(null);

			propertiesFile21 = prism.parsePropertiesFile(modulesFile2, new File("test/testInputs/CKK15Examples/CKK15MiniModel.nm.props"));
			propertiesFile21.setUndefinedConstants(null);

			m1 = (MDP) pe.buildModel(modulesFile, new ModulesFileModelGenerator(modulesFile, prism));
			m2 = (MDP) pe.buildModel(modulesFile2, new ModulesFileModelGenerator(modulesFile2, prism));

			e1 = (ExpressionFunc) propertiesFile.getProperty(0);
			e2 = (ExpressionFunc) propertiesFile2.getProperty(0);
			e3 = (ExpressionFunc) propertiesFile3.getProperty(0);

			e21 = (ExpressionFunc) propertiesFile21.getProperty(0);
		} catch (PrismException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			mdp11 = new MDPModelChecker(null);
			mdp11.currentFilter = new Filter(Filter.FilterOperator.STATE, 0);
			mdp11.setModulesFileAndPropertiesFile(modulesFile, propertiesFile);
			mdp11.setSettings(defaultSettingsForSolvingMultiObjectives);

			mdp12 = new MDPModelChecker(null);
			mdp12.currentFilter = new Filter(Filter.FilterOperator.STATE, 0);
			mdp12.setModulesFileAndPropertiesFile(modulesFile, propertiesFile2);
			mdp12.setSettings(defaultSettingsForSolvingMultiObjectives);

			mdp13 = new MDPModelChecker(null);
			mdp13.currentFilter = new Filter(Filter.FilterOperator.STATE, 0);
			mdp13.setModulesFileAndPropertiesFile(modulesFile, propertiesFile3);
			mdp13.setSettings(defaultSettingsForSolvingMultiObjectives);

			mdp21 = new MDPModelChecker(null);
			mdp21.currentFilter = new Filter(Filter.FilterOperator.STATE, 0);
			mdp21.setModulesFileAndPropertiesFile(modulesFile2, propertiesFile21);
			mdp21.setSettings(defaultSettingsForSolvingMultiObjectives);
		} catch (PrismException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		infeasible = new StateValues(TypeBool.getInstance(), m1);
		infeasible.setBooleanValue(m1.getFirstInitialState(), false);
	}

	@Test
	public void modelIsNotNull() throws PrismException
	{
		MultiLongRun<?> ml11 = mdp11.createMultiLongRun(m1, e1);
		assertNotNull(ml11.model);
	}

	/**
	 * We check here that no exception occurs, and if the MDP with the property is feasible
	 * as it should be
	 * 
	 * @throws PrismException because it may be thrown especially in the LP-solving (not expected)
	 */
	@Test
	public void isFeasible() throws PrismException
	{
		StateValues sv = mdp12.checkExpressionMultiObjective(m1, e2);
		assertNotEquals(infeasible, sv);
	}

	/**
	 * We check here that no exception occurs, and if the MDP with the property is feasible
	 * as it should be
	 * 
	 * @throws PrismException because it may be thrown especially in the LP-solving (not expected)
	 */
	@Test
	public void isFeasible2() throws PrismException
	{
		StateValues sv = mdp13.checkExpressionMultiObjective(m1, e3);
		assertNotEquals(infeasible, sv);
	}

	@Test
	public void isFeasibleWithNoConstraints() throws PrismException
	{
		MultiLongRun<MDP> mlr = mdp12.getMultiLongRunMDP(m1, new HashSet<MDPConstraint>(), new HashSet<MDPObjective>(), new HashSet<MDPExpectationConstraint>(),
				"Linear programming", true);
		mlr.createMultiLongRunLP();
		mlr.solveDefault();
		assertNotNull(mlr.getStrategy());

	}

	/** 
	 * In this method we check that createMultiLongRun can deal with two objectives
	 * 
	 * @throws PrismException may be thrown in the LP-solving (not expected)
	 */
	@Test
	public void testTwoObjectives() throws PrismException
	{
		MultiLongRun<?> ml12 = mdp12.createMultiLongRun(m1, e2);
		assertEquals(2, ml12.objectives.size());
	}

	/** 
	 * @throws PrismException may be thrown in the LP-solving (not expected)
	 */
	@Test
	public void testGetN() throws PrismException
	{
		MultiLongRun<?> ml12 = mdp12.createMultiLongRun(m1, e2);
		assertEquals(2, ml12.getN());
	}

	/** 
	 * this tests, whether the x-variables corresponding to states, which are not in
	 * an MEC are less than 0
	 */
	@Test
	public void testGetVarXNegative() throws PrismException
	{
		MultiLongRun<?> ml13 = mdp13.createMultiLongRun(m1, e3);
		assertEquals(-1, ml13.getVarX(0, 0, 1));
	}

	/**
	 * This test checks some values for LP-variables given in CKK15, example 4
	 * 
	 * @throws PrismException may be thrown during the LP solving (not expected)
	 */
	@Test
	public void testValuesOfExample4() throws PrismException
	{
		MultiLongRun<?> ml11 = mdp11.createMultiLongRun(m1, e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();

		double[] lpResult = ml11.solver.getVariableValues();

		//test x_a for each N
		assertEquals(0.0, lpResult[ml11.getVarX(1, 0, 0)], PrismUtils.epsilonDouble);
		assertEquals(0.2, lpResult[ml11.getVarX(1, 0, 1)], PrismUtils.epsilonDouble);
		assertEquals(0.0, lpResult[ml11.getVarX(1, 0, 2)], PrismUtils.epsilonDouble);
		assertEquals(0.0, lpResult[ml11.getVarX(1, 0, 3)], PrismUtils.epsilonDouble);

		//test x_d for each N
		assertEquals(0.0, lpResult[ml11.getVarX(3, 0, 0)], PrismUtils.epsilonDouble);
		assertEquals(0.0, lpResult[ml11.getVarX(3, 0, 1)], PrismUtils.epsilonDouble);
		assertEquals(0.2, lpResult[ml11.getVarX(3, 0, 2)], PrismUtils.epsilonDouble);
		assertEquals(0.3, lpResult[ml11.getVarX(3, 0, 3)], PrismUtils.epsilonDouble);
	}

	/**
	 * This test checks some values for LP-variables given in CKK15, example 5
	 * 
	 * @throws PrismException may be thrown during the LP-solving (not expected)
	 */
	@Test
	public void testValuesOfExample5() throws PrismException
	{
		MultiLongRun<?> ml11 = mdp11.createMultiLongRun(m1, e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();

		double[] lpResult = ml11.solver.getVariableValues();

		//test y_u, y_v [u=1,v=2], note that y_state is denoted as Z in the implementation
		assertEquals(0.2, lpResult[ml11.getVarZ(1, 1)], PrismUtils.epsilonDouble);
		assertEquals(0.2, lpResult[ml11.getVarZ(2, 2)], PrismUtils.epsilonDouble);
		assertEquals(0.6, lpResult[ml11.getVarZ(2, 3)], PrismUtils.epsilonDouble);

	}
}
