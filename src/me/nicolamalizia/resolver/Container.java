package me.nicolamalizia.resolver;

import java.util.function.Function;

/**
 *
 */
public interface Container {

	/**
	 * Checks if the given abstract type has been bound.
	 *
	 * @param anAbstract abstract to check
	 * @return true if the abstract is bound
	 */
	boolean bounded(Class<?> anAbstract);

	/**
	 * Register a binding with the container.
	 *
	 * @param anAbstract abstract to bind
	 * @param aConcrete  implementation to bind
	 * @return true if the binding succeed
	 */
	boolean bind(Class<?> anAbstract, Class<?> aConcrete);

	/**
	 * Register a binding with the container choosing a specific constructor.
	 *
	 * @param anAbstract abstract to bind
	 * @param aConcrete  implementation to bind
	 * @return true if the binding succeed
	 */
	boolean bind(Class<?> anAbstract, Class<?> aConcrete, Class<?>... parameters);

	/**
	 * Register a binding with the container using a custom resolver
	 *
	 * @param anAbstract     abstract to bind
	 * @param customResolver custom resolver to bind
	 * @return true if the binding succeed
	 */
	boolean bind(Class<?> anAbstract, Function<Resolver, Object> customResolver);

	/**
	 * Register a singleton with the container.
	 *
	 * @param anAbstract abstract to bind
	 * @param aConcrete  singleton to bind
	 * @return true if the binding succeed
	 */
	boolean singleton(Class<?> anAbstract, Class<?> aConcrete);

	/**
	 * Register a singleton with the container using a custom resolver
	 *
	 * @param anAbstract     abstract to bind
	 * @param customResolver custom resolver to bind
	 * @return true if the binding succeed
	 */
	boolean singleton(Class<?> anAbstract, Function<Resolver, Object> customResolver);

	/**
	 * Resolve the given type from the container.
	 * This method resolves the first constructor declared
	 * into the class.
	 *
	 * @param aClass the type given
	 * @param <T>    type captured
	 * @return type instantiated
	 */
	<T> T resolve(Class<?> aClass);

	/**
	 * Resolve the given type from the container.
	 *
	 * @param aClass     abstract to bind
	 * @param parameters definition of the constructor
	 * @param <T>        type captured
	 * @return type instantiated
	 */
	<T> T resolve(Class<?> aClass, Class<?>... parameters);

	/**
	 * Call the given method on an object and inject its dependencies.
	 *
	 * @param instance instance which have the method to call
	 * @param method method to call
	 * @param params method parameters
	 * @return result of the called method
	 */
	Object call(Object instance, String method, Class<?>... params);

	/**
	 * Initialize a ContextualBinding builder
	 *
	 * @param aConcrete contextual concrete
	 * @return a contextual builder
	 */
	ContextualBinding when(Class<?> aConcrete);

	/**
	 *
	 * @param concrete
	 * @param needs
	 * @param aConcrete
	 * @return
	 */
	boolean addContextualBinding(Class<?> concrete, Class<?> needs, Class<?> aConcrete);

	/**
	 *
	 * @param concrete
	 * @param needs
	 * @param object
	 * @return
	 */
	boolean addContextualBinding(Class<?> concrete, Class<?> needs, Object object);
}
