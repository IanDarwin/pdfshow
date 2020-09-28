package net.rejmi.pdfshow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics;

import org.junit.Test;

public class ContainmentTest {

	// Because GObject is abstract.
	class TestGObject extends GObject{
		TestGObject(int x, int y, int width, int height) {
			super(x, y, width, height);
		}

		@Override
		void render(Graphics g) {
			throw new IllegalStateException("render() called on mock GObject");
		}
	};

	@Test
	public void testContainsULT() {
		System.out.println("ContainmentTest.testContainsULT()");
		assertTrue ("ULT", Containment.contains(new TestGObject(100, 100, -160, -200), -50, -150));
	}
	@Test
	public void testContainsULF() {
		System.out.println("ContainmentTest.testContainsULF()");
		assertFalse("ULF", Containment.contains(new TestGObject(100, 100, -160, -200), -250, -150));
	}
	@Test
	public void testContainsURT() {
		assertTrue("URT", Containment.contains(new TestGObject(100, 100, +160, -200), +150, -150));
	}
	@Test
	public void testContainsURF() {
		assertFalse("URF", Containment.contains(new TestGObject(100, 100, +160, -200), +250, -150));
	}
	@Test
	public void testContainsLLT() {
		assertTrue("LLT", Containment.contains(new TestGObject(100, 100, -160, +200), -150, 150));
	}
	@Test
	public void testContainsLLF() {
		assertFalse("LLF", Containment.contains(new TestGObject(100, 100, -160, +200), -250, 150));
	}
	@Test
	public void testContainsLRT() {
		assertTrue("LRT", Containment.contains(new TestGObject(100, 100, +160, +200), 150, 150));
	}
	@Test
	public void testContainsLRF() {
		assertFalse("LRF", Containment.contains(new TestGObject(100, 100, +160, +200), 300, 150));
	}
}
