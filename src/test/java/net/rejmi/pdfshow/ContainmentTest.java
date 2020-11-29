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
		System.out.println("ContainmentTest.testContainsULT()");
		assertTrue("ULT", Containment.contains(new MockGObject(100, 100, -160, -200), -50, -50));
	}
	@Test
	public void testContainsULF() {
		System.out.println("ContainmentTest.testContainsULF()");
		assertFalse("ULF", Containment.contains(new MockGObject(100, 100, -160, -200), -150, -150));
	}
	@Test
	public void testContainsURT() {
		System.out.println("ContainmentTest.testContainsURT()");
		assertTrue("URT", Containment.contains(new MockGObject(100, 100, +160, -200), +150, -50));
	}
	@Test
	public void testContainsURF() {
		System.out.println("ContainmentTest.testContainsURF()");
		assertFalse("URF", Containment.contains(new MockGObject(100, 100, +160, -200), +300, -150));
	}
	@Test
	public void testContainsLLT() {
		System.out.println("ContainmentTest.testContainsLLT()");
		assertTrue("LLT", Containment.contains(new MockGObject(100, 100, -160, +200), 50, 150));
	}
	@Test
	public void testContainsLLF() {
		System.out.println("ContainmentTest.testContainsLLF()");
		assertFalse("LLF", Containment.contains(new MockGObject(100, 100, -160, +200), -300, 150));
	}
	@Test
	public void testContainsLRT() {
		System.out.println("ContainmentTest.testContainsLRT()");
		assertTrue("LRT", Containment.contains(new MockGObject(100, 100, +160, +200), 150, 150));
	}
	@Test
	public void testContainsLRF() {
		System.out.println("ContainmentTest.testContainsLRF()");
		assertFalse("LRF", Containment.contains(new MockGObject(100, 100, +160, +200), 300, 150));
	}
}
