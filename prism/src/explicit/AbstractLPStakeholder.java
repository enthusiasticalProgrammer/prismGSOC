package explicit;

import java.lang.reflect.InvocationTargetException;

import prism.PrismException;
import solvers.LpSolverProxy;
import solvers.SolverProxyInterface;

public class AbstractLPStakeholder
{

	/**
	 * Creates a new solver instance, based on the argument {@see #method}.
	 * @throws PrismException If the jar file providing access to the required LP solver is not found.
	 */
	protected static SolverProxyInterface initialiseSolver(int numRealLPVars, String method) throws PrismException
	{
		SolverProxyInterface result = null;
		try { //below Class.forName throws exception if the required jar is not present
			if (method.equals("Linear programming")) {
				//create new solver
				result = new LpSolverProxy(numRealLPVars, 0);
			} else if (method.equals("Gurobi")) {
				Class<?> cl = Class.forName("solvers.GurobiProxy");
				result = (SolverProxyInterface) cl.getConstructor(int.class, int.class).newInstance(numRealLPVars, 0);
			} else
				throw new UnsupportedOperationException("The given method for solving LP programs is not supported: " + method);
		} catch (ClassNotFoundException ex) {
			throw new PrismException("Cannot load the class required for LP solving. Was gurobi.jar file present in compilation time and is it present now?");
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new PrismException(
					"Cannot load the class required for LP solving, it seems that gurobi.jar file is missing. Is GUROBI_HOME variable set properly?");
		} catch (InvocationTargetException e) {
			String append = "";
			if (e.getCause() != null) {
				append = "The message of parent exception is: " + e.getCause().getMessage();
			}

			throw new PrismException(
					"Problem when initialising an LP solver. " + "InvocationTargetException was thrown" + "\n Message: " + e.getMessage() + "\n" + append);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			throw new PrismException("Problem when initialising an LP solver. "
					+ "It appears that the JAR file is present, but there is some problem, because the exception of type " + e.getClass().toString()
					+ " was thrown. Message: " + e.getMessage());
		}
		if (result != null) {
			return result;
		} else {
			throw new NullPointerException("Unfortunately the LP-solver initialised to null.");
		}
	}

}
