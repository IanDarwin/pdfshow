package net.rejmi.pdfshow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

public class ContainmentTest {
	
	@BeforeClass
	public static void init() {
		LoggerSetup.init();
	}
	
	Logger logger = Logger.getLogger("pdfshow");

	// Because GObject is abstract.
	class MockGObject extends GObject{
		MockGObject(int x, int y, int width, int height) {
			super(x, y, width, height);
		}

		@Override
		void render(Graphics g) {
			throw new IllegalStateException("render() called on MockGObject");
		}
	};

	@Test
	public void testContainsULT() {
		logger.fine("ContainmentTest.testContainsULT()");
		assertTrue("ULT", Containment.contains(new MockGObject(100, 100, -160, -200), -50, -50));
	}
	@Test
	public void testContainsULF() {
		logger.fine("ContainmentTest.testContainsULF()");
		assertFalse("ULF", Containment.contains(new MockGObject(100, 100, -160, -200), -150, -150));
	}
	@Test
	public void testContainsURT() {
		logger.fine("ContainmentTest.testContainsURT()");
		assertTrue("URT", Containment.contains(new MockGObject(100, 100, +160, -200), +150, -50));
	}
	@Test
	public void testContainsURF() {
		logger.fine("ContainmentTest.testContainsURF()");
		assertFalse("URF", Containment.contains(new MockGObject(100, 100, +160, -200), +300, -150));
	}
	@Test
	public void testContainsLLT() {
		logger.fine("ContainmentTest.testContainsLLT()");
		assertTrue("LLT", Containment.contains(new MockGObject(100, 100, -160, +200), 50, 150));
	}
	@Test
	public void testContainsLLF() {
		logger.fine("ContainmentTest.testContainsLLF()");
		assertFalse("LLF", Containment.contains(new MockGObject(100, 100, -160, +200), -300, 150));
	}
	@Test
	public void testContainsLRT() {
		logger.fine("ContainmentTest.testContainsLRT()");
		assertTrue("LRT", Containment.contains(new MockGObject(100, 100, +160, +200), 150, 150));
	}
	@Test
	public void testContainsLRF() {
		logger.fine("ContainmentTest.testContainsLRF()");
		assertFalse("LRF", Containment.contains(new MockGObject(100, 100, +160, +200), 300, 150));
	}
}
