package minispring.container;

@FunctionalInterface
public interface ObjectFactory<T> {
    T getObject();
}
