package userinterface;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestGUIPrism
{

	@Test
	public void testGetIconFromImageNotNull()
	{
		assertNotEquals(null, GUIPrism.getIconFromImage("splash.png"));
		assertNotEquals(null, GUIPrism.getIconFromImage("smallPrism.png"));

	}
}