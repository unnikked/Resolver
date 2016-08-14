package me.nicolamalizia.resolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * IoC
 */
public final class Resolver implements Container {
	public static final Class<?>[] DEFAULT_CONSTRUCTOR = new Class<?>[]{};
	private static final Class<?>[] FIRST_DECLARED = new Class<?>[]{};
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

		return resolve(aClass, FIRST_DECLARED);
	}

	@SuppressWarnings("unchecked")
	private <T> T resolveCustomBinded(Class<?> aClass) {
		return (T) customBindings.get(aClass).apply(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolve(Class<?> aClass, Class<?>... parameters) {
		try {
			if (parameters == FIRST_DECLARED) {
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
	 *
	 * @param owner       of the constructor
	 * @param executable to be resolved
	 * @return resolved dependencies
	 */
	private Object[] resolveDependencies(Class<?> owner, Executable executable) {
		List<Object> dependencies = new LinkedList<>();
		// Resolving each parameter of the constructor recursively.
		for (Class<?> aClass : executable.getParameterTypes()) {
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
					final Class<?>[] right = concrete.getRight();
					dependencies.add(resolve(concrete.getLeft(), right));
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
	 *
	 * @param aClass class to check
	 * @return true if the class is custom binded
	 */
	private boolean isCustomBinded(Class<?> aClass) {
		return customBindings.get(aClass) != null;
	}

	/**
	 * Checks if the class is bindend
	 *
	 * @param aClass class to check
	 * @return true if the class is custom binded
	 */
	private boolean isBinded(Class<?> aClass) {
		return bindings.get(aClass) != null;
	}

	/**
	 * Gets the implementation of the needed concrete
	 *
	 * @param concrete the concrete class
	 * @param needs    the needed class
	 * @return the implementation needed
	 */
	private Class<?> getContext(Class<?> concrete, Class<?> needs) {
		Pair<Class<?>, Class<?>> implementation = new Pair<>(concrete, needs);
		return contextualBindings.get(implementation);
	}

	/**
	 * Checks if the concrete is context binded
	 *
	 * @param concrete the concrete class
	 * @param needs    the needed class
	 * @return true if there exists an implementation needed
	 */
	private boolean isContextBinded(Class<?> concrete, Class<?> needs) {
		Pair<Class<?>, Class<?>> implementation = new Pair<>(concrete, needs);
		return contextualBindings.containsKey(implementation);
	}

	/**
	 * Gets the value of a primitive type contextually binded
	 * @param owner the owner type
	 * @param aClass the primitive type
	 * @return value of a primitive type
	 */
	private Object getPrimitiveContext(Class<?> owner, Class<?> aClass) {
		return primitiveContextualBindings.get(new Pair<>(owner, aClass));
	}

	/**
	 * Checks if the value of a primitive type is contextually binded
	 * @param owner the owner type
	 * @param aClass the primitive type
	 * @return value of a primitive type
	 */
	private boolean isPrimitiveContextBinded(Class<?> owner, Class<?> aClass) {
		return primitiveContextualBindings.containsKey(new Pair<>(owner, aClass));
	}

	/**
	 * Checks if the given abstract type has been bound.
	 *
	 * @param anAbstract abstract to check
	 * @return true if the abstract is bound
	 */
	@Override
	public boolean bounded(Class<?> anAbstract) {
		return false;
	}

	/**
	 * Register a binding with the container.
	 *
	 * @param anAbstract abstract to bind
	 * @param aConcrete  implementation to bind
	 * @return true if the binding succeed
	 */
	@Override
	public boolean bind(Class<?> anAbstract, Class<?> aConcrete) {
		return bind(anAbstract, aConcrete, FIRST_DECLARED);
	}

	/**
	 * Register a binding with the container choosing a specific constructor.
	 *
	 * @param anAbstract abstract to bind
	 * @param aConcrete  implementation to bind
	 * @return true if the binding succeed
	 */
	@Override
	public boolean bind(Class<?> anAbstract, Class<?> aConcrete, Class<?>... parameters) {
		Pair<Class<?>, Class<?>[]> bind = new Pair<>(aConcrete, parameters);
		return bindings.put(anAbstract, bind) != null;
	}

	/**
	 * Register a binding with the container using a custom resolver
	 *
	 * @param anAbstract     abstract to bind
	 * @param customResolver custom resolver to bind
	 * @return true if the binding succeed
	 */
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
	public Object call(Object instance, String method, Class<?>... params) {
		try {
			Method declaredMethod = instance.getClass().getDeclaredMethod(method, params);
			Object[] dependencies = resolveDependencies(instance.getClass(), declaredMethod);
			return declaredMethod.invoke(instance, dependencies);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public ContextualBinding when(Class<?> aConcrete) {
		return new ContextualBinding(this, aConcrete);
	}
}
