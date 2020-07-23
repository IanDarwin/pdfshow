package net.rejmi.pdfshow;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GPolyLineTest {

	GPolyLine target;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		target = new GPolyLine(123,  123);
	}

	@Test
	public void test() {
		target.addPoint(150,  151);
		System.out.println("Target is " + target);
		assertEquals(1, target.length());
		assertEquals(123, target.x);
		assertEquals(150, target.getX(0));
		assertEquals(151, target.getY(0));
		GPolyLine target2 = new GPolyLine(42, 42);
		assertEquals(150, target.getX(0)); // Checking isolation
		assertEquals(42, target2.x);
		assertEquals(0, target2.getX(0));
	}

}
