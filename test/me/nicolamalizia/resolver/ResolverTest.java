package me.nicolamalizia.resolver;

import org.junit.Test;

import java.util.LinkedList;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ResolverTest {
	@Test
	public void testImplementationBinding() {
		Container container = new Resolver();
		container.bind(I.class, A.class);
		assertEquals(A.class, container.resolve(I.class).getClass());
	}

	@Test
	public void testNestedClasses() {
		Container container = new Resolver();
		A a = container.resolve(A.class);
		assertEquals(A.class, a.getClass());
	}

	@Test
	public void testCustomResolver() {
		Container container = new Resolver();
		container.bind(I.class, (resolver -> new A(new B())));
		assertEquals(A.class, container.resolve(I.class).getClass());
	}

	@Test
	public void testCustomResolverCalled() {
		Container container = new Resolver();
		container.bind(I.class, (resolver -> resolver.resolve(A.class)));
		assertEquals(A.class, container.resolve(I.class).getClass());
	}

	@Test
	public void testCustomResolverAnonymousClass() {
		Container container = new Resolver();
		container.bind(Runnable.class, resolver -> new Runnable() {
			@Override
			public void run() {
				//
			}
		});
		assertTrue(container.resolve(Runnable.class).getClass().isAnonymousClass());
	}

	@Test
	public void testCustomResolverLambda() {
		Container container = new Resolver();
		container.bind(Function.class, resolver -> (Function<A, B>) a -> new B());
		assertTrue(container.resolve(Function.class).getClass().isSynthetic());
	}

	@Test
	public void testContextualBinding() {
		Container container = new Resolver();
		container.when(C.class)
				.needs(I.class)
				.give(A.class);
		C c = container.resolve(C.class);
		assertEquals(C.class, c.getClass());
		I i = c.getI();
		assertEquals(A.class, i.getClass());
	}

	@Test
	public void testContextualBindingForAPrimitiveValue() {
		Container container = new Resolver();
		int ten = 10;
		container.when(D.class)
				.needs(int.class)
				.give(ten);
		D d = container.resolve(D.class);
		assertEquals(D.class, d.getClass());
		int i = d.getInteger();
		assertEquals(ten, i);
	}

	@Test
	public void testNoConstructor() {
		Container container = new Resolver();
		assertEquals(E.class, container.resolve(E.class).getClass());
	}

	@Test
	public void testSpecificConstructor() {
		Container container = new Resolver();
		container.bind(F.class, F.class, A.class, LinkedList.class);
		assertEquals(F.class, container.resolve(F.class).getClass());
		assertEquals(F.class, container.resolve(F.class, LinkedList.class, A.class).getClass());
	}

	@Test
	public void testCallMethod() {
		Container container = new Resolver();
		container.when(A.class)
				.needs(boolean.class)
				.give(true);
		A instance = container.resolve(A.class);
		Object result = container.call(instance, "method", boolean.class);
		assertTrue((boolean) result);
	}
}

interface I {

}

class A implements I {
	public A(B b) {

	}

	boolean method(boolean bool) {
		return bool;
	}
}

class B {
	public B() {

	}
}

class C {
	private I i;

	public C(I i) {
		this.i = i;
	}

	public I getI() {
		return i;
	}
}

class D {
	private int integer;

	public D(int integer) {
		this.integer = integer;
	}

	public int getInteger() {
		return integer;
	}
}

class E {
}

class F {
	public F(LinkedList<A> linkedList, A a) {

	}

	public F(A a, LinkedList<A> linkedList) {

	}
}