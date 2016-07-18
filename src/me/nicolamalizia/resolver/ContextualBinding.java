package me.nicolamalizia.resolver;

/**
 *
 */
final class ContextualBinding {
	private final Resolver container;

	private final Class<?> concrete;

	private Class<?> needs;

	ContextualBinding(Resolver container, Class<?> aConcrete) {
		this.container = container;
		this.concrete = aConcrete;
	}

	ContextualBinding needs(Class<?> anAbstract) {
		this.needs = anAbstract;
		return this;
	}

	void give(Class<?> implementation) {
		container.addContextualBinding(concrete, needs, implementation);
	}

	void give(Object object) {
		container.addContextualBinding(concrete, needs, object);
	}
}
