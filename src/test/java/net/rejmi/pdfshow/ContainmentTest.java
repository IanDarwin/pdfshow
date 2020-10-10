package net.rejmi.pdfshow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics;

import org.junit.Test;

public class ContainmentTest {

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
		assertTrue ("ULT", Containment.contains(new MockGObject(100, 100, -160, -200), -50, -50));
	}
	@Test
	public void testContainsULF() {
		assertFalse("ULF", Containment.contains(new MockGObject(100, 100, -160, -200), -150, -150));
	}
	@Test
	public void testContainsURT() {
		assertTrue("URT", Containment.contains(new MockGObject(100, 100, +160, -200), +150, -50));
	}
	@Test
	public void testContainsURF() {
		assertFalse("URF", Containment.contains(new MockGObject(100, 100, +160, -200), +300, -150));
	}
	@Test
	public void testContainsLLT() {
		assertTrue("LLT", Containment.contains(new MockGObject(100, 100, -160, +200), -150, 150));
	}
	@Test
	public void testContainsLLF() {
		assertFalse("LLF", Containment.contains(new MockGObject(100, 100, -160, +200), -300, 150));
	}
	@Test
	public void testContainsLRT() {
		assertTrue("LRT", Containment.contains(new MockGObject(100, 100, +160, +200), 150, 150));
	}
	@Test
	public void testContainsLRF() {
		assertFalse("LRF", Containment.contains(new MockGObject(100, 100, +160, +200), 300, 150));
	}
}
