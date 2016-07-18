package me.nicolamalizia.resolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * IoC
 */
public final class Resolver implements Container {
	public static final Class<?>[] DEFAULT_CONSTRUCTOR = new Class<?>[]{};
	private static final Class<?> [] FIRST_CONSTRUCTOR = new Class<?>[]{};
	private final Map<Class<?>, Pair<Class<?>, Class<?>[]>> bindings = new ConcurrentHashMap<>();
	private final Map<Class<?>, Function<Resolver, Object>> customBindings = new ConcurrentHashMap<>();
	private final Map<Pair<Class<?>, Class<?>>, Class<?>> contextualBindings = new ConcurrentHashMap<>();
	private final Map<Pair<Class<?>, Class<?>>, Object> primitiveContextualBindings = new ConcurrentHashMap<>();

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolve(Class<?> aClass) {
		// if there is a binding definition we want to resolve
		// the associate implementation.
		if (isBinded(aClass)) {
			Pair<Class<?>, Class<?>[]> bind = bindings.get(aClass);

			Class<?> thatClass = bind.getLeft();
			Class<?>[] thatParameters = bind.getRight();

			return resolve(thatClass, thatParameters);
		}

		// if there is a binding defined by a custom resolver
		// we want to resolve by the custom resolver.
		if (isCustomBinded(aClass)) {
			return resolveCustomBinded(aClass);
		}

		// we cannot instantiate a not bound interface
		if (aClass.isInterface()) {
			throw new RuntimeException("Unbound interface " + aClass);
		}

		return resolve(aClass, FIRST_CONSTRUCTOR);
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveCustomBinded(Class<?> aClass) {
		return (T) customBindings.get(aClass).apply(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolve(Class<?> aClass, Class<?>... parameters) {
		try {
			if (parameters == FIRST_CONSTRUCTOR) {
				return resolveFirstConstructor(aClass);
			}
			return resolveConstructor(aClass, parameters);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
			throw new RuntimeException(e);
		}
	}

	private <T> T resolveFirstConstructor(Class<?> aClass) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		Constructor<?>[] declaredConstructors = aClass.getDeclaredConstructors();
		Constructor<?> firstConstructor = declaredConstructors[0];
		Object[] dependencies = resolveDependencies(aClass, firstConstructor);
		return (T) firstConstructor.newInstance(dependencies);
	}

	private <T> T resolveConstructor(Class<?> aClass, Class<?>[] parameters) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Constructor<?> constructor = aClass.getDeclaredConstructor(parameters);
		Object[] dependencies = resolveDependencies(aClass, constructor);
		return (T) constructor.newInstance(dependencies);
	}

	/**
	 * Resolves constructor parameters
	 * @param owner of the constructor
	 * @param constructor to be resolved
	 * @return resolved dependencies
	 */
	private Object[] resolveDependencies(Class<?> owner, Constructor<?> constructor) {
		List<Object> dependencies = new LinkedList<>();
		// Resolving each parameter of the constructor recursively.
		for (Class<?> aClass : constructor.getParameterTypes()) {
			if (isContextBinded(owner, aClass)) {
				dependencies.add(resolve(getContext(owner, aClass)));
				continue;
			}

			if (isPrimitiveContextBinded(owner, aClass)) {
				dependencies.add(getPrimitiveContext(owner, aClass));
				continue;
			}

			if (aClass.isPrimitive()) {
				throw new RuntimeException("Cannot resolve a Primitive Type " + aClass);
			}

			if (aClass.isArray()) {
				throw new RuntimeException("Cannot resolve an Array " + aClass);
			}

			if (aClass.isInterface()) {
				Pair<Class<?>, Class<?>[]> concrete;
				if ((concrete = bindings.get(aClass)) != null) {
					dependencies.add(resolve(concrete.getLeft(), concrete.getRight()));
					continue;
				}

				Function<Resolver, Object> resolver;
				if ((resolver = customBindings.get(aClass)) != null) {
					dependencies.add(resolver.apply(this));
					continue;
				}
				throw new RuntimeException("Unbound Interface " + aClass);
			}

			dependencies.add(resolve(aClass));
		}
		return dependencies.toArray();
	}

	/**
	 * Checks if the class is custom bindend
	 * @param aClass class to check
	 * @return true if the class is custom binded
	 */
	private boolean isCustomBinded(Class<?> aClass) {
		return customBindings.get(aClass) != null;
	}

	/**
	 * Checks if the class is bindend
	 * @param aClass class to check
	 * @return true if the class is custom binded
	 */
	private boolean isBinded(Class<?> aClass) {
		return bindings.get(aClass) != null;
	}

	/**
	 * Gets the implementation of the needed concrete
	 * @param concrete the concrete class
	 * @param needs the needed class
	 * @return the implementation needed
	 */
	private Class<?> getContext(Class<?> concrete, Class<?> needs) {
		Pair<Class<?>, Class<?>> implementation = new Pair<>(concrete, needs);
		return contextualBindings.get(implementation);
	}

	/**
	 * Checks if the concrete is context binded
	 * @param concrete the concreete class
	 * @param needs the needed class
	 * @return true if there exists an implementation needed
	 */
	private boolean isContextBinded(Class<?> concrete, Class<?> needs) {
		Pair<Class<?>, Class<?>> implementation = new Pair<>(concrete, needs);
		return contextualBindings.containsKey(implementation);
	}

	private Object getPrimitiveContext(Class<?> owner, Class<?> aClass) {
		Pair<Class<?>, Class<?>> key = new Pair<>(owner, aClass);
		return primitiveContextualBindings.get(key);
	}

	private boolean isPrimitiveContextBinded(Class<?> owner, Class<?> aClass) {
		Pair<Class<?>, Class<?>> key = new Pair<>(owner, aClass);
		return primitiveContextualBindings.containsKey(key);
	}

	@Override
	public boolean bounded(Class<?> anAbstract) {
		return false;
	}

	@Override
	public boolean bind(Class<?> anAbstract, Class<?> aConcrete) {
		return bind(anAbstract, aConcrete, FIRST_CONSTRUCTOR);
	}

	@Override
	public boolean bind(Class<?> anAbstract, Class<?> aConcrete, Class<?>... parameters) {
		Pair<Class<?>, Class<?>[]> bind = new Pair<>(aConcrete, parameters);
		return bindings.put(anAbstract, bind) != null;
	}

	@Override
	public boolean bind(Class<?> anAbstract, Function<Resolver, Object> customResolver) {
		return customBindings.put(anAbstract, customResolver) != null;
	}

	@Override
	public boolean singleton(Class<?> anAbstract, Class<?> aConcrete) {
		return false;
	}

	@Override
	public boolean singleton(Class<?> anAbstract, Function<Resolver, Object> customResolver) {
		return false;
	}

	public boolean addContextualBinding(Class<?> concrete, Class<?> needs, Class<?> aConcrete) {
		Pair<Class<?>, Class<?>> bind = new Pair<>(concrete, needs);
		return contextualBindings.put(bind, aConcrete) != null;
	}

	public boolean addContextualBinding(Class<?> concrete, Class<?> needs, Object object) {
		Pair<Class<?>, Class<?>> bind = new Pair<>(concrete, needs);
		return primitiveContextualBindings.put(bind, object) != null;
	}

	@Override
	public <T> T call(Class<?> aClass, String method, Class<?>... params) {
		return null;
	}

	public ContextualBinding when(Class<?> aConcrete) {
		return new ContextualBinding(this, aConcrete);
	}
}
