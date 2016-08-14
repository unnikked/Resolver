package me.nicolamalizia.resolver;

public class Pair<T, E> {
	private final T left;
	private final E right;

	public Pair(T left, E right) {
		this.left = left;
		this.right = right;
	}

	public T getLeft() {
		return left;
	}

	public E getRight() {
		return right;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Pair)) return false;

		Pair<?, ?> pair = (Pair<?, ?>) o;

		if (getLeft() != null ? !getLeft().equals(pair.getLeft()) : pair.getLeft() != null) return false;
		return getRight() != null ? getRight().equals(pair.getRight()) : pair.getRight() == null;

	}

	@Override
	public int hashCode() {
		int result = getLeft() != null ? getLeft().hashCode() : 0;
		result = 31 * result + (getRight() != null ? getRight().hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Pair{" +
				"left=" + left +
				", right=" + right +
				'}';
	}
}