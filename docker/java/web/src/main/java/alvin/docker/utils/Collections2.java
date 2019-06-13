package alvin.docker.utils;

import lombok.val;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

public final class Collections2 {
    private static final Random RANDOM = new Random();

    private Collections2() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    public static <T> Optional<T> first(Collection<T> collection) {
        if (isEmpty(collection)) {
            return Optional.empty();
        }
        return Optional.of(collection.iterator().next());
    }

    public static <T> Optional<T> first(List<T> list) {
        if (isEmpty(list)) {
            return Optional.empty();
        }
        return Optional.of(list.get(0));
    }

    @SafeVarargs
    public static <T> T[] array(T... elements) {
        return elements;
    }

    @SafeVarargs
    public static <T> List<T> list(T... elements) {
        if (isEmpty(elements)) {
            return emptyList();
        }
        if (elements.length == 1) {
            return singletonList(elements[0]);
        }
        return Arrays.asList(elements);
    }

    public static <T> List<T> list(Collection<T> collection) {
        if (isEmpty(collection)) {
            return emptyList();
        }
        if (collection.size() == 1) {
            return singletonList(collection.iterator().next());
        }
        return list((Iterable<T>) collection);
    }

    public static <T> List<T> list(Iterator<T> iter) {
        return list(() -> iter);
    }

    public static <T> List<T> list(Iterable<T> iterable) {
        if (iterable instanceof List) {
            return (List<T>) iterable;
        }
        return newArrayList(iterable);
    }

    @SafeVarargs
    public static <T> Set<T> set(T... elements) {
        return isEmpty(elements) ? emptySet() : newHashSet(elements);
    }

    public static <T> Set<T> set(Collection<T> collection) {
        if (isEmpty(collection)) {
            return emptySet();
        }
        return set((Iterable<T>) collection);
    }

    public static <T> Set<T> set(Iterator<T> iter) {
        return set(() -> iter);
    }

    public static <T> Set<T> set(Iterable<T> iter) {
        if (iter instanceof Set) {
            return (Set<T>) iter;
        }
        return newLinkedHashSet(iter);
    }

    @SafeVarargs
    public static <T> Stream<T> stream(T... elements) {
        return Arrays.stream(elements);
    }

    public static <T> Stream<T> stream(Iterator<T> iter) {
        if (iter == null) {
            return Stream.empty();
        }
        return stream(() -> iter);
    }

    public static <T> Stream<T> stream(Iterable<T> iter) {
        if (iter == null) {
            return Stream.empty();
        }
        return StreamSupport.stream(iter.spliterator(), false);
    }

    public static <T, R> Set<R> toSet(Stream<T> stream, Function<? super T, ? extends R> mapper) {
        return stream.map(mapper).collect(Collectors.toSet());
    }

    public static <T, R> Set<R> toSet(Iterable<T> iterable, Function<? super T, ? extends R> mapper) {
        return toSet(stream(iterable), mapper);
    }

    public static <T, R> Set<R> toSet(Iterator<T> iterator, Function<? super T, ? extends R> mapper) {
        return toSet(stream(() -> iterator), mapper);
    }

    public static <K, R, C> Map<K, R> toMap(Stream<C> stream, Function<? super C, K> keyMapper,
                                            Function<? super C, R> valueMapper) {
        return stream.collect(Collectors.toMap(keyMapper, valueMapper, (o, n) -> n));
    }

    public static <K, R> Map<K, R> toMap(Stream<R> stream, Function<? super R, K> keyMapper) {
        return toMap(stream, keyMapper, Function.identity());
    }

    public static <K, R, C> Map<K, R> toMap(Iterable<C> iterable, Function<? super C, K> keyMapper,
                                            Function<? super C, R> valueMapper) {
        return toMap(stream(iterable), keyMapper, valueMapper);
    }

    public static <K, R> Map<K, R> toMap(Iterable<R> iterable, Function<? super R, K> keyMapper) {
        return toMap(stream(iterable), keyMapper);
    }

    public static <K, R, C> Map<K, R> toMap(Iterator<C> iterator, Function<? super C, K> keyMapper,
                                            Function<? super C, R> valueMapper) {
        return toMap(stream(iterator), keyMapper, valueMapper);
    }

    public static <K, R> Map<K, R> toMap(Iterator<R> iterator, Function<? super R, K> keyMapper) {
        return toMap(stream(iterator), keyMapper);
    }

    public static <T, R> List<T> toList(Stream<R> stream, Function<? super R, T> mapper) {
        return stream.map(mapper).collect(Collectors.toList());
    }

    public static <T, R> List<T> toList(Iterable<R> iterable, Function<? super R, T> mapper) {
        return toList(stream(iterable), mapper);
    }

    public static <T, R> List<T> flatList(Iterable<R> iterable, Function<? super R, Stream<? extends T>> mapper) {
        return stream(iterable).flatMap(mapper).collect(Collectors.toList());
    }

    public static <T, R> Set<T> flatSet(Iterable<R> iterable, Function<? super R, Stream<? extends T>> mapper) {
        return stream(iterable).flatMap(mapper).collect(Collectors.toSet());
    }

    public static <T, R> List<T> toList(Iterator<R> iterator, Function<? super R, T> mapper) {
        return toList(stream(iterator), mapper);
    }

    public static <K, V> Map<K, List<V>> groupBy(Stream<V> stream, Function<? super V, ? extends K> classifier) {
        return stream.collect(Collectors.groupingBy(classifier));
    }

    public static <K, V> Map<K, List<V>> groupBy(Iterable<V> iterable, Function<? super V, ? extends K> classifier) {
        return groupBy(stream(iterable), classifier);
    }

    public static <K, V> Map<K, V> combineMap(Map<K, V> left, Map<K, V> right) {
        val map = left == null ? new HashMap<K, V>() : new HashMap<>(left);
        if (right != null) {
            map.putAll(right);
        }
        return map;
    }

    public static <T> Set<T> combineSet(Collection<T> left, Collection<T> right) {
        val set = left == null ? new HashSet<T>() : new HashSet<>(left);
        if (right != null) {
            set.addAll(right);
        }
        return set;
    }

    public static <T> List<T> filter(Stream<T> stream, Predicate<? super T> predicate) {
        return stream.filter(predicate).collect(Collectors.toList());
    }

    public static <T> List<T> filter(Iterable<T> iterable, Predicate<? super T> predicate) {
        return filter(stream(iterable), predicate);
    }

    public static boolean isEmpty(Page<?> page) {
        return page == null || page.isEmpty();
    }

    public static boolean isNotEmpty(Page<?> page) {
        return !isEmpty(page);
    }

    public static <T> List<T> disorder(Collection<T> collection, int frequency) {
        val list = new ArrayList<>(collection);
        for (int i = 0; i < frequency; i++) {
            val a = RANDOM.nextInt(list.size());
            val b = RANDOM.nextInt(list.size());
            if (a != b) {
                val v = list.get(a);
                list.set(a, list.get(b));
                list.set(b, v);
            }
        }
        return list;
    }

    public static <T> List<T> disorder(Iterable<T> collection, int frequency) {
        return disorder(list(collection), frequency);
    }

    public static <T> Set<T> conact(Set<T> set, T... items) {
        val newSet = new LinkedHashSet<>(set);
        if (items != null) {
            newSet.addAll(Arrays.asList(items));
        }
        return newSet;
    }
}
