package explicit;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

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
import prism.Result;
import simulator.ModulesFileModelGenerator;

//TODO Christopher: some parts belong to a utility-class (or at least to MDPModelChecker)
public class TestMultiLongRun
{
	public MDP m1;
	public MDP m2; //currently not used, TODO Christopher: use it or lose it
	public MDPModelChecker mdp11;
	public MDPModelChecker mdp12;
	public MDPModelChecker mdp13;

	public ExpressionFunc e1;
	public ExpressionFunc e2;
	public ExpressionFunc e3;

	StateValues infeasible;

	@Before
	public void setUp() throws PrismLangException
	{

		ModulesFile modulesFile = null;
		PropertiesFile propertiesFile = null;
		PropertiesFile propertiesFile2 = null;
		PropertiesFile propertiesFile3 = null;

		try {
			PrismLog mainLog = new PrismFileLog("stdout");
			Prism prism = new Prism(mainLog);
			modulesFile = prism.parseModelFile(new File("test/testInputs/CKK15Examples/CKK15Model.nm"));
			modulesFile.setUndefinedConstants(null);
			propertiesFile = prism.parsePropertiesFile(modulesFile, new File("test/testInputs/CKK15Examples/CKK15PropertyFile1.props"));
			propertiesFile.setUndefinedConstants(null);
			PrismExplicit pe = new PrismExplicit(prism.getMainLog(), prism.getSettings());

			propertiesFile2 = prism.parsePropertiesFile(modulesFile, new File("test/testInputs/CKK15Examples/CKK15PropertyFile2.props"));
			propertiesFile2.setUndefinedConstants(null);

			propertiesFile3 = prism.parsePropertiesFile(modulesFile, new File("test/testInputs/CKK15Examples/CKK15PropertyFile3.props"));
			propertiesFile3.setUndefinedConstants(null);

			m1 = (MDP) pe.buildModel(modulesFile, new ModulesFileModelGenerator(modulesFile, prism));
			e1 = (ExpressionFunc) propertiesFile.getProperty(0);

			e2 = (ExpressionFunc) propertiesFile2.getProperty(0);
			e3 = (ExpressionFunc) propertiesFile3.getProperty(0);

		} catch (PrismException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			mdp11 = new MDPModelChecker(null);
			mdp11.currentFilter = new Filter(Filter.FilterOperator.STATE, 0);
			mdp11.setModulesFileAndPropertiesFile(modulesFile, propertiesFile);
			mdp11.setSettings(new PrismSettings());

			mdp12 = new MDPModelChecker(null);
			mdp12.currentFilter = new Filter(Filter.FilterOperator.STATE, 0);
			mdp12.setModulesFileAndPropertiesFile(modulesFile, propertiesFile2);
			mdp12.setSettings(new PrismSettings());

			mdp13 = new MDPModelChecker(null);
			mdp13.currentFilter = new Filter(Filter.FilterOperator.STATE, 0);
			mdp13.setModulesFileAndPropertiesFile(modulesFile, propertiesFile3);
			mdp13.setSettings(new PrismSettings());
		} catch (PrismException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		infeasible = new StateValues(TypeBool.getInstance(), m1);
		infeasible.setBooleanValue(m1.getFirstInitialState(), false);
	}

	/**
	 * To check that no exception occurs 
	 * @throws PrismException 
	 */
	@Test
	public void isFeasible() throws PrismException
	{
		StateValues sv = mdp12.checkExpressionMultiObjective(m1, e2);
		assertNotEquals(infeasible, sv);
	}

	/**
	 * To check that no exception occurs 
	 * @throws PrismException 
	 */
	@Test
	public void isFeasible2() throws PrismException
	{
		StateValues sv = mdp13.checkExpressionMultiObjective(m1, e3);
		assertNotEquals(infeasible, sv);
	}

	/** 
	 * @throws PrismException 
	 */
	@Test
	public void testTwoObjectives() throws PrismException
	{
		MultiLongRun ml12 = mdp12.createMultiLongRun(m1, e2);
		assertEquals(2, ml12.objectives.size());
	}

	/** 
	 * @throws PrismException 
	 */
	@Test
	public void testGetN() throws PrismException
	{
		MultiLongRun ml12 = mdp12.createMultiLongRun(m1, e2);
		assertEquals(2, ml12.getN());
	}

	/** 
	 * this tests, whether the x-variables corresponding to states, which are not in
	 * an MEC are less than 0
	 */
	@Test
	public void testGetVarXNegative() throws PrismException
	{
		MultiLongRun ml13 = mdp13.createMultiLongRun(m1, e3);
		ml13.computeOffsets(mdp13.isMemoryLess(e3));
		assertTrue(ml13.getVarX(0, 0, 1) == -1);
	}

	/**
	 * This test checks some values for LP-variables given in CKK15, example 4
	 * @throws PrismException 
	 */
	@Test
	public void testValuesOfExample4() throws PrismException
	{
		MultiLongRun ml11 = mdp11.createMultiLongRun(m1, e1);
		ml11.createMultiLongRunLP(false);
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
	 * @throws PrismException 
	 */
	@Test
	public void testValuesOfExample5() throws PrismException
	{
		MultiLongRun ml11 = mdp11.createMultiLongRun(m1, e1);
		ml11.createMultiLongRunLP(false);
		ml11.solveDefault();

		double[] lpResult = ml11.solver.getVariableValues();

		//test y_u, y_v [u=1,v=2], note that y_state is denoted as Z in the implementation
		assertEquals(0.2, lpResult[ml11.getVarZ(1, 1)], PrismUtils.epsilonDouble);
		assertEquals(0.2, lpResult[ml11.getVarZ(2, 2)], PrismUtils.epsilonDouble);
		assertEquals(0.6, lpResult[ml11.getVarZ(2, 3)], PrismUtils.epsilonDouble);

	}
}
