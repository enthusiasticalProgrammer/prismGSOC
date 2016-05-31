package userinterface;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

public class TestGUIPrism
{

	@Ignore("Some environment variables need adjustment before it works")
	@Test
	public void testGetIconFromImageNotNull()
	{
		assertNotEquals(null, GUIPrism.getIconFromImage("splash.png"));
		assertNotEquals(null, GUIPrism.getIconFromImage("smallPrism.png"));

	}
}