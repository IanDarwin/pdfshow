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
		assertTrue("ULT", new MockGObject(100, 100, -160, -200).contains(-50, -50));
	}
	@Test
	public void testContainsULF() {
		logger.fine("ContainmentTest.testContainsULF()");
		assertFalse("ULF", new MockGObject(100, 100, -160, -200).contains(-150, -150));
	}
	@Test
	public void testContainsURT() {
		logger.fine("ContainmentTest.testContainsURT()");
		assertTrue("URT", new MockGObject(100, 100, +160, -200).contains(+150, -50));
	}
	@Test
	public void testContainsURF() {
		logger.fine("ContainmentTest.testContainsURF()");
		assertFalse("URF", new MockGObject(100, 100, +160, -200).contains(+300, -150));
	}
	@Test
	public void testContainsLLT() {
		logger.fine("ContainmentTest.testContainsLLT()");
		assertTrue("LLT", new MockGObject(100, 100, -160, +200).contains(50, 150));
	}
	@Test
	public void testContainsLLF() {
		logger.fine("ContainmentTest.testContainsLLF()");
		assertFalse("LLF", new MockGObject(100, 100, -160, +200).contains(-300, 150));
	}
	@Test
	public void testContainsLRT() {
		logger.fine("ContainmentTest.testContainsLRT()");
		assertTrue("LRT", new MockGObject(100, 100, +160, +200).contains(150, 150));
	}
	@Test
	public void testContainsLRF() {
		logger.fine("ContainmentTest.testContainsLRF()");
		assertFalse("LRF", new MockGObject(100, 100, +160, +200).contains(300, 150));
	}
}
