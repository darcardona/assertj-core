/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Copyright 2012-2016 the original author or authors.
 */
package org.assertj.core.api;

import static java.util.Arrays.stream;
import static org.assertj.core.api.filter.Filters.filter;
import static org.assertj.core.extractor.Extractors.byName;
import static org.assertj.core.extractor.Extractors.resultOf;
import static org.assertj.core.util.Arrays.isArray;
import static org.assertj.core.util.IterableUtil.toArray;
import static org.assertj.core.util.Lists.newArrayList;
import static org.assertj.core.util.Preconditions.checkNotNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.assertj.core.api.filter.FilterOperator;
import org.assertj.core.api.filter.Filters;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.condition.Not;
import org.assertj.core.data.Index;
import org.assertj.core.description.Description;
import org.assertj.core.groups.FieldsOrPropertiesExtractor;
import org.assertj.core.groups.Tuple;
import org.assertj.core.internal.CommonErrors;
import org.assertj.core.internal.ComparatorBasedComparisonStrategy;
import org.assertj.core.internal.TypeComparators;
import org.assertj.core.internal.FieldByFieldComparator;
import org.assertj.core.internal.IgnoringFieldsComparator;
import org.assertj.core.internal.Iterables;
import org.assertj.core.internal.ObjectArrayElementComparisonStrategy;
import org.assertj.core.internal.ObjectArrays;
import org.assertj.core.internal.Objects;
import org.assertj.core.internal.OnFieldsComparator;
import org.assertj.core.internal.RecursiveFieldByFieldComparator;
import org.assertj.core.presentation.PredicateDescription;
import org.assertj.core.util.IterableUtil;
import org.assertj.core.util.VisibleForTesting;
import org.assertj.core.util.introspection.IntrospectionError;

/**
 * Assertion methods for arrays of objects.
 * <p>
 * To create an instance of this class, invoke <code>{@link Assertions#assertThat(T[])}</code>.
 * </p>
 *
 * @param <T> the type of elements of the "actual" value.
 *
 * @author Yvonne Wang
 * @author Alex Ruiz
 * @author Joel Costigliola
 * @author Nicolas François
 * @author Mikhail Mazursky
 * @author Mateusz Haligowski
 * @author Lovro Pandzic
 */
public abstract class AbstractObjectArrayAssert<S extends AbstractObjectArrayAssert<S, T>, T> extends
    AbstractAssert<S, T[]> implements IndexedObjectEnumerableAssert<AbstractObjectArrayAssert<S, T>, T>,
    ArraySortedAssert<AbstractObjectArrayAssert<S, T>, T> {

  @VisibleForTesting
  ObjectArrays arrays = ObjectArrays.instance();
  @VisibleForTesting
  Iterables iterables = Iterables.instance();

  private Map<String, Comparator<?>> comparatorsForElementPropertyOrFieldNames = new HashMap<>();
  private TypeComparators comparatorsForElementPropertyOrFieldTypes = new TypeComparators();

  public AbstractObjectArrayAssert(T[] actual, Class<?> selfType) {
    super(actual, selfType);
  }
  
  @Override
  public S as(Description description) {
    return super.as(description);
  }
  
  @Override
  public S as(String description, Object... args) {
    return super.as(description, args);
  }

  /**
   * {@inheritDoc}
   *
   * @throws AssertionError {@inheritDoc}
   */
  @Override
  public void isNullOrEmpty() {
    arrays.assertNullOrEmpty(info, actual);
  }

  /**
   * {@inheritDoc}
   *
   * @throws AssertionError {@inheritDoc}
   */
  @Override
  public void isEmpty() {
    arrays.assertEmpty(info, actual);
  }

  /**
   * {@inheritDoc}
   *
   * @throws AssertionError {@inheritDoc}
   */
  @Override
  public S isNotEmpty() {
    arrays.assertNotEmpty(info, actual);
    return myself;
  }

  /**
   * {@inheritDoc}
   *
   * @throws AssertionError {@inheritDoc}
   */
  @Override
  public S hasSize(int expected) {
    arrays.assertHasSize(info, actual, expected);
    return myself;
  }

  /**
   * Verifies that the actual array has the same size as the given array.
   * <p>
   * Parameter is declared as Object to accept both {@code Object[]} and primitive arrays (e.g. {@code int[]}).
   * <p>
   * Example:
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   * int[] fourFiveSix = {4, 5, 6};
   *
   * // assertions will pass
   * assertThat(oneTwoThree).hasSameSizeAs(fourFiveSix);</code></pre>
   *
   * @param array the array to compare size with actual group.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the array parameter is {@code null} or is not a true array.
   * @throws AssertionError if actual group and given array don't have the same size.
   */
  @Override
  public S hasSameSizeAs(Object other) {
    // same implementation as in AbstractArrayAssert, but can't inherit from it due to generics problem ...
    arrays.assertHasSameSizeAs(info, actual, other);
    return myself;
  }

  /**
   * Verifies that the actual group has the same size as the given {@link Iterable}.
   * <p>
   * Example:
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   * Iterable&lt;Ring&gt; elvesRings = newArrayList(vilya, nenya, narya);
   *
   * // assertions will pass
   * assertThat(oneTwoThree).hasSameSizeAs(elvesRings);</code></pre>
   *
   * @param other the {@code Iterable} to compare size with actual group.
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the other {@code Iterable} is {@code null}.
   * @throws AssertionError if actual group and given {@code Iterable} don't have the same size.
   */
  @Override
  public S hasSameSizeAs(Iterable<?> other) {
    arrays.assertHasSameSizeAs(info, actual, other);
    return myself;
  }

  /**
   * Verifies that the actual group contains the given values, in any order.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   *
   * // assertions will pass
   * assertThat(abc).contains("b", "a");
   * assertThat(abc).contains("b", "a", "b");
   *
   * // assertion will fail
   * assertThat(abc).contains("d");</code></pre>
   *
   * @param values the given values.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws IllegalArgumentException if the given argument is an empty array.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not contain the given values.
   */
  @Override
  public S contains(@SuppressWarnings("unchecked") T... values) {
    arrays.assertContains(info, actual, values);
    return myself;
  }

  /**
   * Verifies that the actual group contains only the given values and nothing else, <b>in any order</b>.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   *
   * // assertion will pass
   * assertThat(abc).containsOnly("c", "b", "a");
   *
   * // assertion will fail because "c" is missing
   * assertThat(abc).containsOnly("a", "b");</code></pre>
   *
   * @param values the given values.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws IllegalArgumentException if the given argument is an empty array.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not contain the given values, i.e. the actual group contains some
   *           or none of the given values, or the actual group contains more values than the given ones.
   */
  @Override
  public S containsOnly(@SuppressWarnings("unchecked") T... values) {
    arrays.assertContainsOnly(info, actual, values);
    return myself;
  }

  /**
   * Same semantic as {@link #containsOnly(Object[])} : verifies that actual contains all elements of the given
   * {@code Iterable} and nothing else, <b>in any order</b>.
   * <p>
   * Example :
   * <pre><code class='java'> Ring[] rings = {nenya, vilya};
   *
   * // assertion will pass
   * assertThat(rings).containsOnlyElementsOf(newArrayList(nenya, vilya));
   * assertThat(rings).containsOnlyElementsOf(newArrayList(nenya, nenya, vilya, vilya));
   *
   * // assertion will fail as actual does not contain narya
   * assertThat(rings).containsOnlyElementsOf(newArrayList(nenya, vilya, narya));
   * // assertion will fail as actual contains nenya
   * assertThat(rings).containsOnlyElementsOf(newArrayList(vilya));</code></pre>
   *
   * @param iterable the given {@code Iterable} we will get elements from.
   */
  @Override
  public S containsOnlyElementsOf(Iterable<? extends T> iterable) {
    return containsOnly(toArray(iterable));
  }

  /**
   * Verifies that the unique element of the array satifies the given assertions expressed as a {@link Consumer},
   * if it does not, only the first error is reported, use {@link SoftAssertions} to get all the errors.
   * <p>
   * Example:
   * <pre><code class='java'> Jedi[] jedis = array(new Jedi("Yoda", "red"));
   *
   * // assertions will pass
   *
   * assertThat(jedis).hasOnlyOneElementSatisfying(yoda -> assertThat(yoda.getName()).startsWith("Y"));
   *
   * assertThat(jedis).hasOnlyOneElementSatisfying(yoda -> {
   *   assertThat(yoda.getName()).isEqualTo("Yoda");
   *   assertThat(yoda.getLightSaberColor()).isEqualTo("red");
   * });
   *
   * // assertions will fail
   *
   * assertThat(jedis).hasOnlyOneElementSatisfying(yoda -> assertThat(yoda.getName()).startsWith("Vad"));
   *
   * // fail as one the assertions is not satisfied
   * assertThat(jedis).hasOnlyOneElementSatisfying(yoda -> {
   *   assertThat(yoda.getName()).isEqualTo("Yoda");
   *   assertThat(yoda.getLightSaberColor()).isEqualTo("purple");
   * });
   *
   * // fail but only report the first error
   * assertThat(jedis).hasOnlyOneElementSatisfying(yoda -> {
   *   assertThat(yoda.getName()).isEqualTo("Luke");
   *   assertThat(yoda.getLightSaberColor()).isEqualTo("green");
   * });
   *
   * // fail and reports the errors thanks to Soft assertions
   * assertThat(jedis).hasOnlyOneElementSatisfying(yoda -> {
   *   SoftAssertions softly = new SoftAssertions();
   *   softly.assertThat(yoda.getName()).isEqualTo("Luke");
   *   softly.assertThat(yoda.getLightSaberColor()).isEqualTo("green");
   *   softly.assertAll();
   * });
   *
   * // even if the assertion is correct, there are too many jedis !
   * jedis = array(new Jedi("Yoda", "red"), new Jedi("Luke", "green"));
   * assertThat(jedis).hasOnlyOneElementSatisfying(yoda -> assertThat(yoda.getName()).startsWith("Yo"));</code></pre>
   *
   * @param elementAssertions the assertions to perform on the unique element.
   * @throws AssertionError if the array does not have a unique element.
   * @throws AssertionError if the array's unique element does not satifies the given assertions.
   *
   * @since 3.5.0
   */
  public void hasOnlyOneElementSatisfying(Consumer<T> elementAssertions) {
    arrays.assertHasSize(info, actual, 1);
    elementAssertions.accept(actual[0]);
  }

  /**
   * An alias of {@link #containsOnlyElementsOf(Iterable)} : verifies that actual contains all elements of the
   * given {@code Iterable} and nothing else, <b>in any order</b>.
   * </p>
   * Example:
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertions will pass:
   * assertThat(elvesRings).hasSameElementsAs(newArrayList(nenya, narya, vilya));
   * assertThat(elvesRings).hasSameElementsAs(newArrayList(nenya, narya, vilya, nenya));
   *
   * // assertions will fail:
   * assertThat(elvesRings).hasSameElementsAs(newArrayList(nenya, narya));
   * assertThat(elvesRings).hasSameElementsAs(newArrayList(nenya, narya, vilya, oneRing));</code></pre>
   *
   * @param iterable the {@code Iterable} whose elements we expect to be present
   * @return this assertion object
   * @throws AssertionError if the actual group is {@code null}
   * @throws NullPointerException if the given {@code Iterable} is {@code null}
   * @throws AssertionError if the actual {@code Iterable} does not have the same elements, in any order, as the given
   *           {@code Iterable}
   */
  @Override
  public S hasSameElementsAs(Iterable<? extends T> iterable) {
    return containsOnlyElementsOf(iterable);
  }

  /**
   * Verifies that the actual group contains the given values only once.
   * <p>
   * Examples :
   * <pre><code class='java'> // array if a factory method to create arrays.
   *
   * // assertions will pass
   * assertThat(array(&quot;winter&quot;, &quot;is&quot;, &quot;coming&quot;)).containsOnlyOnce(&quot;winter&quot;);
   * assertThat(array(&quot;winter&quot;, &quot;is&quot;, &quot;coming&quot;)).containsOnlyOnce(&quot;coming&quot;, &quot;winter&quot;);
   *
   * // assertions will fail
   * assertThat(array(&quot;winter&quot;, &quot;is&quot;, &quot;coming&quot;)).containsOnlyOnce(&quot;Lannister&quot;);
   * assertThat(array(&quot;Arya&quot;, &quot;Stark&quot;, &quot;daughter&quot;, &quot;of&quot;, &quot;Ned&quot;, &quot;Stark&quot;)).containsOnlyOnce(&quot;Stark&quot;);
   * assertThat(array(&quot;Arya&quot;, &quot;Stark&quot;, &quot;daughter&quot;, &quot;of&quot;, &quot;Ned&quot;, &quot;Stark&quot;)).containsOnlyOnce(&quot;Stark&quot;, &quot;Lannister&quot;, &quot;Arya&quot;);</code></pre>
   *
   * @param values the given values.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws IllegalArgumentException if the given argument is an empty array.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not contain the given values, i.e. the actual group contains some
   *           or none of the given values, or the actual group contains more than once these values.
   */
  @Override
  public S containsOnlyOnce(@SuppressWarnings("unchecked") T... values) {
    arrays.assertContainsOnlyOnce(info, actual, values);
    return myself;
  }

  /**
   * Verifies that the actual array contains only the given values and nothing else, <b>in order</b>.<br>
   * <p>
   * Example :
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertion will pass
   * assertThat(elvesRings).containsExactly(vilya, nenya, narya);
   *
   * // assertion will fail as actual and expected order differ
   * assertThat(elvesRings).containsExactly(nenya, vilya, narya);</code></pre>
   *
   * @param values the given values.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not contain the given values with same order, i.e. the actual group
   *           contains some or none of the given values, or the actual group contains more values than the given ones
   *           or values are the same but the order is not.
   */
  @Override
  public S containsExactly(@SuppressWarnings("unchecked") T... values) {
    arrays.assertContainsExactly(info, actual, values);
    return myself;
  }

  /**
   * Verifies that the actual array contains exactly the given values and nothing else, <b>in any order</b>.<br>
   *
   * <p>
   * Example :
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya, vilya};
   *
   * // assertion will pass
   * assertThat(elvesRings).containsExactlyInAnyOrder(vilya, vilya, nenya, narya);
   *
   * // assertion will fail as vilya exists twice in elvesRings
   * assertThat(elvesRings).containsExactlyInAnyOrder(nenya, vilya, narya);</code></pre>
   *
   * @param values the given values.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws AssertionError if the actual array is {@code null}.
   * @throws AssertionError if the actual arrray does not contain the given values, i.e. the actual array
   *           contains some or none of the given values, or the actual group contains more values than the given ones.
   */
  @Override
  public S containsExactlyInAnyOrder(@SuppressWarnings("unchecked") T... values) {
    arrays.assertContainsExactlyInAnyOrder(info, actual, values);
    return myself;
  }

  /**
   * Same as {@link #containsExactly(Object...)} but handle the {@link Iterable} to array conversion : verifies that
   * actual contains all elements of the given {@code Iterable} and nothing else <b>in the same order</b>.
   * <p>
   * Example :
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertion will pass
   * assertThat(elvesRings).containsExactlyElementsOf(newLinkedList(vilya, nenya, narya));
   *
   * // assertion will fail as actual and expected order differ
   * assertThat(elvesRings).containsExactlyElementsOf(newLinkedList(nenya, vilya, narya));</code></pre>
   *
   * @param iterable the given {@code Iterable} we will get elements from.
   */
  @Override
  public S containsExactlyElementsOf(Iterable<? extends T> iterable) {
    return containsExactly(toArray(iterable));
  }

  /**
   * Verifies that the actual array contains the given sequence in the correct order and <b>without extra value between the sequence values</b>.
   * <p>
   * Use {@link #containsSubsequence(Object...)} to allow values between the expected sequence values.
   * <p>
   * Example:
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertion will pass
   * assertThat(elvesRings).containsSequence(vilya, nenya);
   * assertThat(elvesRings).containsSequence(nenya, narya);
   *
   * // assertions will fail, the elements order is correct but there is a value between them (nenya)
   * assertThat(elvesRings).containsSequence(vilya, narya);
   * assertThat(elvesRings).containsSequence(nenya, vilya);</code></pre>
   *
   * @param sequence the sequence of objects to look for.
   * @return this assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the given array is {@code null}.
   * @throws AssertionError if the actual group does not contain the given sequence.
   */
  @Override
  public S containsSequence(@SuppressWarnings("unchecked") T... sequence) {
    arrays.assertContainsSequence(info, actual, sequence);
    return myself;
  }

  /**
   * Verifies that the actual array contains the given subsequence in the correct order (possibly with other values between them).
   * <p>
   * Example:
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertions will pass
   * assertThat(elvesRings).containsSubsequence(vilya, nenya);
   * assertThat(elvesRings).containsSubsequence(vilya, narya);
   *
   * // assertion will fail
   * assertThat(elvesRings).containsSubsequence(nenya, vilya);</code></pre>
   *
   * @param sequence the sequence of objects to look for.
   * @return this assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the given array is {@code null}.
   * @throws AssertionError if the actual group does not contain the given subsequence.
   */
  @Override
  public S containsSubsequence(@SuppressWarnings("unchecked") T... subsequence) {
    arrays.assertContainsSubsequence(info, actual, subsequence);
    return myself;
  }

  /**
   * Verifies that the actual array contains the given object at the given index.
   * <p>
   * Example:
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertions will pass
   * assertThat(elvesRings).contains(vilya, atIndex(0));
   * assertThat(elvesRings).contains(nenya, atIndex(1));
   * assertThat(elvesRings).contains(narya, atIndex(2));
   *
   * // assertions will fail
   * assertThat(elvesRings).contains(vilya, atIndex(1));
   * assertThat(elvesRings).contains(nenya, atIndex(2));
   * assertThat(elvesRings).contains(narya, atIndex(0));</code></pre>
   *
   * @param value the object to look for.
   * @param index the index where the object should be stored in the actual group.
   * @return this assertion object.
   * @throws AssertionError if the actual group is {@code null} or empty.
   * @throws NullPointerException if the given {@code Index} is {@code null}.
   * @throws IndexOutOfBoundsException if the value of the given {@code Index} is equal to or greater than the size of the actual
   *           group.
   * @throws AssertionError if the actual group does not contain the given object at the given index.
   */
  @Override
  public S contains(T value, Index index) {
    arrays.assertContains(info, actual, value, index);
    return myself;
  }

  /**
   * Verifies that the actual array does not contain the given object at the given index.
   * <p>
   * Example:
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertions will pass
   * assertThat(elvesRings).contains(vilya, atIndex(1));
   * assertThat(elvesRings).contains(nenya, atIndex(2));
   * assertThat(elvesRings).contains(narya, atIndex(0));
   *
   * // assertions will fail
   * assertThat(elvesRings).contains(vilya, atIndex(0));
   * assertThat(elvesRings).contains(nenya, atIndex(1));
   * assertThat(elvesRings).contains(narya, atIndex(2));</code></pre>
   *
   * @param value the object to look for.
   * @param index the index where the object should not be stored in the actual group.
   * @return this assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws NullPointerException if the given {@code Index} is {@code null}.
   * @throws AssertionError if the actual group contains the given object at the given index.
   */
  public S doesNotContain(T value, Index index) {
    arrays.assertDoesNotContain(info, actual, value, index);
    return myself;
  }

  /**
   * Verifies that the actual array does not contain the given values.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   *
   * // assertion will pass
   * assertThat(abc).doesNotContain("d", "e");
   *
   * // assertions will fail
   * assertThat(abc).doesNotContain("a");
   * assertThat(abc).doesNotContain("a", "b", "c");
   * assertThat(abc).doesNotContain("a", "x");</code></pre>
   *
   * @param values the given values.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws IllegalArgumentException if the given argument is an empty array.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group contains any of the given values.
   */
  @Override
  public S doesNotContain(@SuppressWarnings("unchecked") T... values) {
    arrays.assertDoesNotContain(info, actual, values);
    return myself;
  }

  /**
   * Verifies that the actual array does not contain any elements of the given {@link Iterable} (i.e. none).
   * <p>
   * Example:
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   *
   * // assertion will pass
   * assertThat(actual).doesNotContainAnyElementsOf(newArrayList("d", "e"));
   *
   * // assertions will fail
   * assertThat(actual).doesNotContainAnyElementsOf(newArrayList("a", "b"));
   * assertThat(actual).doesNotContainAnyElementsOf(newArrayList("d", "e", "a"));</code></pre>
   *
   * @param iterable the {@link Iterable} whose elements must not be in the actual group.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws IllegalArgumentException if the given argument is an empty iterable.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group contains some elements of the given {@link Iterable}.
   */
  @Override
  public S doesNotContainAnyElementsOf(Iterable<? extends T> iterable) {
    arrays.assertDoesNotContainAnyElementsOf(info, actual, iterable);
    return myself;
  }

  /**
   * Verifies that the actual array does not contain duplicates.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   * String[] lotsOfAs = {"a", "a", "a"};
   *
   * // assertion will pass
   * assertThat(abc).doesNotHaveDuplicates();
   *
   * // assertion will fail
   * assertThat(lotsOfAs).doesNotHaveDuplicates();</code></pre>
   *
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group contains duplicates.
   */
  @Override
  public S doesNotHaveDuplicates() {
    arrays.assertDoesNotHaveDuplicates(info, actual);
    return myself;
  }

  /**
   * Verifies that the actual array starts with the given sequence of objects, without any other objects between them.
   * Similar to <code>{@link #containsSequence(Object...)}</code>, but it also verifies that the first element in the
   * sequence is also the first element of the actual array.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   *
   * // assertion will pass
   * assertThat(abc).startsWith("a", "b");
   *
   * // assertion will fail
   * assertThat(abc).startsWith("c");</code></pre>
   *
   * @param sequence the sequence of objects to look for.
   * @return this assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws IllegalArgumentException if the given argument is an empty array.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not start with the given sequence of objects.
   */
  @Override
  public S startsWith(@SuppressWarnings("unchecked") T... sequence) {
    arrays.assertStartsWith(info, actual, sequence);
    return myself;
  }

  /**
   * Verifies that the actual array ends with the given sequence of objects, without any other objects between them.
   * Similar to <code>{@link #containsSequence(Object...)}</code>, but it also verifies that the last element in the
   * sequence is also last element of the actual array.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   *
   * // assertion will pass
   * assertThat(abc).endsWith("b", "c");
   *
   * // assertion will fail
   * assertThat(abc).endsWith("a");</code></pre>
   *
   * @param sequence the sequence of objects to look for.
   * @return this assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws IllegalArgumentException if the given argument is an empty array.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not end with the given sequence of objects.
   */
  @Override
  public S endsWith(@SuppressWarnings("unchecked") T... sequence) {
    arrays.assertEndsWith(info, actual, sequence);
    return myself;
  }

  /**
   * Verifies that all elements of actual are present in the given {@code Iterable}.
   * <p>
   * Example:
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   * List&lt;Ring&gt; ringsOfPower = newArrayList(oneRing, vilya, nenya, narya, dwarfRing, manRing);
   *
   * // assertion will pass:
   * assertThat(elvesRings).isSubsetOf(ringsOfPower);
   *
   * // assertion will fail:
   * assertThat(elvesRings).isSubsetOf(newArrayList(nenya, narya));</code></pre>
   *
   * @param values the {@code Iterable} that should contain all actual elements.
   * @return this assertion object.
   * @throws AssertionError if the actual {@code Iterable} is {@code null}.
   * @throws NullPointerException if the given {@code Iterable} is {@code null}.
   * @throws AssertionError if the actual {@code Iterable} is not subset of set {@code Iterable}.
   */
  @Override
  public S isSubsetOf(Iterable<? extends T> values) {
    arrays.assertIsSubsetOf(info, actual, values);
    return myself;
  }

  /**
   * Verifies that all elements of actual are present in the given values.
   * <p>
   * Example:
   * <pre><code class='java'> Ring[] elvesRings = {vilya, nenya, narya};
   *
   * // assertions will pass:
   * assertThat(elvesRings).isSubsetOf(vilya, nenya, narya);
   * assertThat(elvesRings).isSubsetOf(vilya, nenya, narya, dwarfRing);
   *
   * // assertions will fail:
   * assertThat(elvesRings).isSubsetOf(vilya, nenya);
   * assertThat(elvesRings).isSubsetOf(vilya, nenya, dwarfRing);</code></pre>
   *
   * @param values the values that should be used for checking the elements of actual.
   * @return this assertion object.
   * @throws AssertionError if the actual {@code Iterable} is {@code null}.
   * @throws AssertionError if the actual {@code Iterable} is not subset of the given values.
   */
  @Override
  public S isSubsetOf(@SuppressWarnings("unchecked") T... values) {
    arrays.assertIsSubsetOf(info, actual, Arrays.asList(values));
    return myself;
  }

  /**
   * Verifies that the actual array contains at least a null element.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   * String[] abNull = {"a", "b", null};
   *
   * // assertion will pass
   * assertThat(abNull).containsNull();
   *
   * // assertion will fail
   * assertThat(abc).containsNull();</code></pre>
   *
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not contain a null element.
   */
  @Override
  public S containsNull() {
    arrays.assertContainsNull(info, actual);
    return myself;
  }

  /**
   * Verifies that the actual array does not contain null elements.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   * String[] abNull = {"a", "b", null};
   *
   * // assertion will pass
   * assertThat(abc).doesNotContainNull();
   *
   * // assertion will fail
   * assertThat(abNull).doesNotContainNull();</code></pre>
   *
   * @return {@code this} assertion object.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group contains a null element.
   */
  @Override
  public S doesNotContainNull() {
    arrays.assertDoesNotContainNull(info, actual);
    return myself;
  }

  /**
   * Verifies that each element value satisfies the given condition
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc  = {"a", "b", "c"};
   * String[] abcc = {"a", "b", "cc"};
   *
   * Condition&lt;String&gt; singleCharacterString
   *      = new Condition&lt;&gt;(s -> s.length() == 1, "single character String");
   *
   * // assertion will pass
   * assertThat(abc).are(singleCharacterString);
   *
   * // assertion will fail
   * assertThat(abcc).are(singleCharacterString);</code></pre>
   *
   * @param condition the given condition.
   * @return {@code this} object.
   * @throws NullPointerException if the given condition is {@code null}.
   * @throws AssertionError if an element cannot be cast to T.
   * @throws AssertionError if one or more elements don't satisfy the given condition.
   */
  @Override
  public S are(Condition<? super T> condition) {
    arrays.assertAre(info, actual, condition);
    return myself;
  }

  /**
   * Verifies that each element value does not satisfy the given condition
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc  = {"a", "b", "c"};
   * String[] abcc = {"a", "b", "cc"};
   *
   * Condition&lt;String&gt; moreThanOneCharacter =
   *     = new Condition&lt;&gt;(s -> s.length() > 1, "more than one character");
   *
   * // assertion will pass
   * assertThat(abc).areNot(moreThanOneCharacter);
   *
   * // assertion will fail
   * assertThat(abcc).areNot(moreThanOneCharacter);</code></pre>
   *
   * @param condition the given condition.
   * @return {@code this} object.
   * @throws NullPointerException if the given condition is {@code null}.
   * @throws AssertionError if an element cannot be cast to T.
   * @throws AssertionError if one or more elements satisfy the given condition.
   */
  @Override
  public S areNot(Condition<? super T> condition) {
    arrays.assertAreNot(info, actual, condition);
    return myself;
  }

  /**
   * Verifies that all elements satisfy the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc  = {"a", "b", "c"};
   * String[] abcc = {"a", "b", "cc"};
   *
   * Condition&lt;String&gt; onlyOneCharacter =
   *     = new Condition&lt;&gt;(s -> s.length() == 1, "only one character");
   *
   * // assertion will pass
   * assertThat(abc).have(onlyOneCharacter);
   *
   * // assertion will fail
   * assertThat(abcc).have(onlyOneCharacter);</code></pre>
   *
   * @param condition the given condition.
   * @return {@code this} object.
   * @throws NullPointerException if the given condition is {@code null}.
   * @throws AssertionError if an element cannot be cast to T.
   * @throws AssertionError if one or more elements do not satisfy the given condition.
   */
  @Override
  public S have(Condition<? super T> condition) {
    arrays.assertHave(info, actual, condition);
    return myself;
  }

  /**
   * Verifies that all elements don't satisfy the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc  = {"a", "b", "c"};
   * String[] abcc = {"a", "b", "cc"};
   *
   * Condition&lt;String&gt; moreThanOneCharacter =
   *     = new Condition&lt;&gt;(s -> s.length() > 1, "more than one character");
   *
   * // assertion will pass
   * assertThat(abc).doNotHave(moreThanOneCharacter);
   *
   * // assertion will fail
   * assertThat(abcc).doNotHave(moreThanOneCharacter);</code></pre>
   *
   * @param condition the given condition.
   * @return {@code this} object.
   * @throws NullPointerException if the given condition is {@code null}.
   * @throws AssertionError if an element cannot be cast to T.
   * @throws AssertionError if one or more elements satisfy the given condition.
   */
  @Override
  public S doNotHave(Condition<? super T> condition) {
    arrays.assertDoNotHave(info, actual, condition);
    return myself;
  }

  /**
   * Verifies that there is <b>at least</b> <i>n</i> elements in the actual group satisfying the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   *
   * Condition&lt;Integer&gt; oddNumber = new Condition&lt;&gt;(value % 2 == 1, "odd number");
   *
   * // assertion will pass
   * oneTwoThree.areAtLeast(2, oddNumber);
   *
   * // assertion will fail
   * oneTwoThree.areAtLeast(3, oddNumber);</code></pre>
   *
   * @param n the minimum number of times the condition should be verified.
   * @param condition the given condition.
   * @return {@code this} object.
   * @throws NullPointerException if the given condition is {@code null}.
   * @throws AssertionError if an element can not be cast to T.
   * @throws AssertionError if the number of elements satisfying the given condition is &lt; n.
   */
  @Override
  public S areAtLeast(int times, Condition<? super T> condition) {
    arrays.assertAreAtLeast(info, actual, times, condition);
    return myself;
  }

  /**
   * Verifies that there is <b>at least <i>one</i></b> element in the actual array satisfying the given condition.
   * <p>
   * This method is an alias for {@code areAtLeast(1, condition)}.
   * <p>
   * Example:
   * <pre><code class='java'> // jedi is a Condition&lt;String&gt;
   * assertThat(new String[]{"Luke", "Solo", "Leia"}).areAtLeastOne(jedi);</code></pre>
   *
   * @see #haveAtLeast(int, Condition)
   */
  @Override
  public S areAtLeastOne(Condition<? super T> condition) {
    areAtLeast(1, condition);
    return myself;
  }

  /**
   * Verifies that there is <b>at most</b> <i>n</i> elements in the actual group satisfying the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   *
   * Condition&lt;Integer&gt; oddNumber = new Condition&lt;&gt;(value % 2 == 1, "odd number");
   *
   * // assertions will pass
   * oneTwoThree.areAtMost(2, oddNumber);
   * oneTwoThree.areAtMost(3, oddNumber);
   *
   * // assertion will fail
   * oneTwoThree.areAtMost(1, oddNumber);</code></pre>
   *
   * @param n the number of times the condition should be at most verified.
   * @param condition the given condition.
   * @return {@code this} object.
   * @throws NullPointerException if the given condition is {@code null}.
   * @throws AssertionError if an element cannot be cast to T.
   * @throws AssertionError if the number of elements satisfying the given condition is &gt; n.
   */
  @Override
  public S areAtMost(int times, Condition<? super T> condition) {
    arrays.assertAreAtMost(info, actual, times, condition);
    return myself;
  }

  /**
   * Verifies that there is <b>exactly</b> <i>n</i> elements in the actual group satisfying the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   *
   * Condition&lt;Integer&gt; oddNumber = new Condition&lt;&gt;(value % 2 == 1, "odd number");
   *
   * // assertion will pass
   * oneTwoThree.areExactly(2, oddNumber);
   *
   * // assertions will fail
   * oneTwoThree.areExactly(1, oddNumber);
   * oneTwoThree.areExactly(3, oddNumber);</code></pre>
   *
   * @param n the exact number of times the condition should be verified.
   * @param condition the given condition.
   * @return {@code this} object.
   * @throws NullPointerException if the given condition is {@code null}.
   * @throws AssertionError if an element cannot be cast to T.
   * @throws AssertionError if the number of elements satisfying the given condition is &ne; n.
   */
  @Override
  public S areExactly(int times, Condition<? super T> condition) {
    arrays.assertAreExactly(info, actual, times, condition);
    return myself;
  }

  /**
   * Verifies that there is <b>at least <i>one</i></b> element in the actual group satisfying the given condition.
   * <p>
   * This method is an alias for {@code haveAtLeast(1, condition)}.
   * <p>
   * Example:
   * <pre><code class='java'> BasketBallPlayer[] bullsPlayers = {butler, rose};
   *
   * // potentialMvp is a Condition&lt;BasketBallPlayer&gt;
   * assertThat(bullsPlayers).haveAtLeastOne(potentialMvp);</code></pre>
   *
   * @see #haveAtLeast(int, Condition)
   */
  @Override
  public S haveAtLeastOne(Condition<? super T> condition) {
    return haveAtLeast(1, condition);
  }

  /**
   * Verifies that there is <b>at least <i>n</i></b> elements in the actual group satisfying the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   *
   * Condition&lt;Integer&gt; oddNumber = new Condition&lt;&gt;(value % 2 == 1, "odd number");
   *
   * // assertion will pass
   * oneTwoThree.haveAtLeast(2, oddNumber);
   *
   * // assertion will fail
   * oneTwoThree.haveAtLeast(3, oddNumber);</code></pre>
   *
   * This method is an alias for {@link #areAtLeast(int, Condition)}.
   */
  @Override
  public S haveAtLeast(int times, Condition<? super T> condition) {
    arrays.assertHaveAtLeast(info, actual, times, condition);
    return myself;
  }

  /**
   * Verifies that there is <b>at most</b> <i>n</i> elements in the actual group satisfying the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   *
   * Condition&lt;Integer&gt; oddNumber = new Condition&lt;&gt;(value % 2 == 1, "odd number");
   *
   * // assertions will pass
   * oneTwoThree.haveAtMost(2, oddNumber);
   * oneTwoThree.haveAtMost(3, oddNumber);
   *
   * // assertion will fail
   * oneTwoThree.haveAtMost(1, oddNumber);</code></pre>
   *
   * This method is an alias {@link #areAtMost(int, Condition)}.
   */
  @Override
  public S haveAtMost(int times, Condition<? super T> condition) {
    arrays.assertHaveAtMost(info, actual, times, condition);
    return myself;
  }

  /**
   * Verifies that there is <b>exactly</b> <i>n</i> elements in the actual group satisfying the given condition.
   * <p>
   * Example :
   * <pre><code class='java'> int[] oneTwoThree = {1, 2, 3};
   *
   * Condition&lt;Integer&gt; oddNumber = new Condition&lt;&gt;(value % 2 == 1, "odd number");
   *
   * // assertion will pass
   * oneTwoThree.haveExactly(2, oddNumber);
   *
   * // assertions will fail
   * oneTwoThree.haveExactly(1, oddNumber);
   * oneTwoThree.haveExactly(3, oddNumber);</code></pre>
   *
   * This method is an alias {@link #areExactly(int, Condition)}.
   */
  @Override
  public S haveExactly(int times, Condition<? super T> condition) {
    arrays.assertHaveExactly(info, actual, times, condition);
    return myself;
  }

  /** {@inheritDoc} */
  @Override
  public S hasAtLeastOneElementOfType(Class<?> type) {
    arrays.assertHasAtLeastOneElementOfType(info, actual, type);
    return myself;
  }

  /** {@inheritDoc} */
  @Override
  public S hasOnlyElementsOfType(Class<?> type) {
    arrays.assertHasOnlyElementsOfType(info, actual, type);
    return myself;
  }

  /** {@inheritDoc} */
  @Override
  public S isSorted() {
    arrays.assertIsSorted(info, actual);
    return myself;
  }

  /** {@inheritDoc} */
  @Override
  public S isSortedAccordingTo(Comparator<? super T> comparator) {
    arrays.assertIsSortedAccordingToComparator(info, actual, comparator);
    return myself;
  }

  /**
   * Verifies that the actual array contains all the elements of given {@code Iterable}, in any order.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc = {"a", "b", "c"};
   * String[] bc = {"b", "c"};
   *
   * // assertion will pass
   * assertThat(abc).containsAll(bc);</code></pre>
   *
   * @param iterable the given {@code Iterable} we will get elements from.
   * @return {@code this} assertion object.
   * @throws NullPointerException if the given argument is {@code null}.
   * @throws AssertionError if the actual group is {@code null}.
   * @throws AssertionError if the actual group does not contain all the elements of given {@code Iterable}.
   */
  @Override
  public S containsAll(Iterable<? extends T> iterable) {
    arrays.assertContainsAll(info, actual, iterable);
    return myself;
  }

  /**
   * Use given custom comparator instead of relying on actual type A <code>equals</code> method to compare group
   * elements for
   * incoming assertion checks.
   * <p>
   * Custom comparator is bound to assertion instance, meaning that if a new assertion is created, it will use default
   * comparison strategy.
   * <p>
   * Examples :
   * <pre><code class='java'> // compares invoices by payee
   * assertThat(invoiceArray).usingComparator(invoicePayeeComparator).isEqualTo(expectedinvoiceArray).
   *
   * // compares invoices by date, doesNotHaveDuplicates and contains both use the given invoice date comparator
   * assertThat(invoiceArray).usingComparator(invoiceDateComparator).doesNotHaveDuplicates().contains(may2010Invoice)
   *
   * // as assertThat(invoiceArray) creates a new assertion, it falls back to standard comparison strategy
   * // based on Invoice's equal method to compare invoiceArray elements to lowestInvoice.
   * assertThat(invoiceArray).contains(lowestInvoice).
   *
   * // standard comparison : the fellowshipOfTheRing includes Gandalf but not Sauron (believe me) ...
   * assertThat(fellowshipOfTheRing).contains(gandalf)
   *                                .doesNotContain(sauron);
   *
   * // ... but if we compare only races, Sauron is in fellowshipOfTheRing because he's a Maia like Gandalf.
   * assertThat(fellowshipOfTheRing).usingElementComparator(raceComparator)
   *                                .contains(sauron);</code></pre>
   *
   * @param customComparator the comparator to use for incoming assertion checks.
   * @throws NullPointerException if the given comparator is {@code null}.
   * @return {@code this} assertion object.
   */
  public S usingElementComparator(Comparator<? super T> elementComparator) {
    this.arrays = new ObjectArrays(new ComparatorBasedComparisonStrategy(elementComparator));
    // to have the same semantics on base assertions like isEqualTo, we need to use an iterable comparator comparing
    // elements with elementComparator parameter
    objects = new Objects(new ObjectArrayElementComparisonStrategy<>(elementComparator));
    return myself;
  }

  /** {@inheritDoc} */
  @Override
  public S usingDefaultElementComparator() {
    this.arrays = ObjectArrays.instance();
    return myself;
  }

  /**
   * Allows to set a comparator to compare properties or fields of elements with the given names.
   * A typical usage is for comparing fields of numeric type at a given precision.
   * <p>
   * To be used, comparators need to be specified by this method <b>before</b> calling any of:
   * <ul>
   * <li>{@link #usingFieldByFieldElementComparator}</li>
   * <li>{@link #usingElementComparatorOnFields}</li>
   * <li>{@link #usingElementComparatorIgnoringFields}</li>
   * <li>{@link #usingRecursiveFieldByFieldElementComparator}</li>
   * </ul>
   * <p>
   * Comparators specified by this method have precedence over comparators specified by
   * {@link #usingComparatorForElementFieldsWithType(Comparator, Class) usingComparatorForElementFieldsWithType}.
   * <p>
   * Example:
   * <p>
   * <pre><code class='java'> public class TolkienCharacter {
   *   private String name;
   *   private double height;
   *   // constructor omitted
   * }
   *
   * TolkienCharacter frodo = new TolkienCharacter(&quot;Frodo&quot;, 1.2);
   * TolkienCharacter tallerFrodo = new TolkienCharacter(&quot;Frodo&quot;, 1.3);
   * TolkienCharacter reallyTallFrodo = new TolkienCharacter(&quot;Frodo&quot;, 1.9);
   *
   * Comparator&lt;Double&gt; closeEnough = new Comparator&lt;Double&gt;() {
   *   double precision = 0.5;
   *   public int compare(Double d1, Double d2) {
   *     return Math.abs(d1 - d2) <= precision ? 0 : 1;
   *   }
   * };
   *
   * TolkienCharacter[] hobbits = new TolkienCharacter[] {frodo};
   *
   * // assertions will pass
   * assertThat(hobbits).usingComparatorForElementFieldsWithNames(closeEnough, &quot;height&quot;)
   *                    .usingFieldByFieldElementComparator()
   *                    .contains(tallerFrodo);
   *
   * assertThat(hobbits).usingComparatorForElementFieldsWithNames(closeEnough, &quot;height&quot;)
   *                    .usingElementComparatorOnFields(&quot;height&quot;)
   *                    .contains(tallerFrodo);
   *
   * assertThat(hobbits).usingComparatorForElementFieldsWithNames(closeEnough, &quot;height&quot;)
   *                    .usingElementComparatorIgnoringFields(&quot;name&quot;)
   *                    .contains(tallerFrodo);
   *
   * assertThat(hobbits).usingComparatorForElementFieldsWithNames(closeEnough, &quot;height&quot;)
   *                    .usingRecursiveFieldByFieldElementComparator()
   *                    .contains(tallerFrodo);
   *
   * // assertion will fail
   * assertThat(hobbits).usingComparatorForElementFieldsWithNames(closeEnough, &quot;height&quot;)
   *                    .usingFieldByFieldElementComparator()
   *                    .containsExactly(reallyTallFrodo);</code></pre>
   * </p>
   *
   * @param comparator the {@link java.util.Comparator} to use
   * @param elementPropertyOrFieldNames the names of the properties and/or fields of the elements the comparator should be used for
   * @return {@code this} assertions object
   * @since 2.5.0 / 3.5.0
   */
  public <C> S usingComparatorForElementFieldsWithNames(Comparator<C> comparator,
                                                        String... elementPropertyOrFieldNames) {
    for (String elementPropertyOrField : elementPropertyOrFieldNames) {
      comparatorsForElementPropertyOrFieldNames.put(elementPropertyOrField, comparator);
    }
    return myself;
  }

  /**
   * Allows to set a specific comparator to compare properties or fields of elements with the given type.
   * A typical usage is for comparing fields of numeric type at a given precision.
   * <p>
   * To be used, comparators need to be specified by this method <b>before</b> calling any of:
   * <ul>
   * <li>{@link #usingFieldByFieldElementComparator}</li>
   * <li>{@link #usingElementComparatorOnFields}</li>
   * <li>{@link #usingElementComparatorIgnoringFields}</li>
   * <li>{@link #usingRecursiveFieldByFieldElementComparator}</li>
   * </ul>
   * <p>
   * Comparators specified by {@link #usingComparatorForElementFieldsWithNames(Comparator, String...) usingComparatorForElementFieldsWithNames}
   * have precedence over comparators specified by this method.
   * <p>
   * Example:
   * <pre><code class='java'> public class TolkienCharacter {
   *   private String name;
   *   private double height;
   *   // constructor omitted
   * }
   * TolkienCharacter frodo = new TolkienCharacter(&quot;Frodo&quot;, 1.2);
   * TolkienCharacter tallerFrodo = new TolkienCharacter(&quot;Frodo&quot;, 1.3);
   * TolkienCharacter reallyTallFrodo = new TolkienCharacter(&quot;Frodo&quot;, 1.9);
   *
   * Comparator&lt;Double&gt; closeEnough = new Comparator&lt;Double&gt;() {
   *   double precision = 0.5;
   *   public int compare(Double d1, Double d2) {
   *     return Math.abs(d1 - d2) <= precision ? 0 : 1;
   *   }
   * };
   *
   * TolkienCharacter[] hobbits = new TolkienCharacter[] {frodo};
   *
   * // assertions will pass
   * assertThat(hobbits).usingComparatorForElementFieldsWithType(closeEnough, Double.class)
   *                    .usingFieldByFieldElementComparator()
   *                    .contains(tallerFrodo);
   *
   * assertThat(hobbits).usingComparatorForElementFieldsWithType(closeEnough, Double.class)
   *                    .usingElementComparatorOnFields(&quot;height&quot;)
   *                    .contains(tallerFrodo);
   *
   * assertThat(hobbits).usingComparatorForElementFieldsWithType(closeEnough, Double.class)
   *                    .usingElementComparatorIgnoringFields(&quot;name&quot;)
   *                    .contains(tallerFrodo);
   *
   * assertThat(hobbits).usingComparatorForElementFieldsWithType(closeEnough, Double.class)
   *                    .usingRecursiveFieldByFieldElementComparator()
   *                    .contains(tallerFrodo);
   *
   * // assertion will fail
   * assertThat(hobbits).usingComparatorForElementFieldsWithType(closeEnough, Double.class)
   *                    .usingFieldByFieldElementComparator()
   *                    .contains(reallyTallFrodo);</code></pre>
   * </p>
   * If multiple compatible comparators have been registered for a given {@code type}, the closest in the inheritance 
   * chain to the given {@code type} is chosen in the following order:
   * <ol>
   * <li>The comparator for the exact given {@code type}</li>
   * <li>The comparator of a superclass of the given {@code type}</li>
   * <li>The comparator of an interface implemented by the given {@code type}</li>
   * </ol>
   *
   * @param comparator the {@link java.util.Comparator} to use
   * @param type the {@link java.lang.Class} of the type of the element fields the comparator should be used for
   * @return {@code this} assertions object
   * @since 2.5.0 / 3.5.0
   */
  public <C> S usingComparatorForElementFieldsWithType(Comparator<C> comparator, Class<C> type) {
    comparatorsForElementPropertyOrFieldTypes.put(type, comparator);
    return myself;
  }

  /**
   * Use field/property by field/property comparison (including inherited fields/properties) instead of relying on
   * actual type A <code>equals</code> method to compare group elements for incoming assertion checks. Private fields
   * are included but this can be disabled using {@link Assertions#setAllowExtractingPrivateFields(boolean)}.
   * <p>
   * This can be handy if <code>equals</code> method of the objects to compare does not suit you.
   * <p>
   * You can specify a custom comparator per name or type of element field with
   * {@link #usingComparatorForElementFieldsWithNames(Comparator, String...)}
   * and {@link #usingComparatorForElementFieldsWithType(Comparator, Class)}.
   * <p>
   * Note that the comparison is <b>not</b> recursive, if one of the fields/properties is an Object, it will be compared
   * to the other field/property using its <code>equals</code> method.
   * </p>
   * Example:
   * <pre><code class='java'> TolkienCharacter frodo = new TolkienCharacter("Frodo", 33, HOBBIT);
   * TolkienCharacter frodoClone = new TolkienCharacter("Frodo", 33, HOBBIT);
   *
   * // Fail if equals has not been overridden in TolkienCharacter as equals default implementation only compares references
   * assertThat(array(frodo)).contains(frodoClone);
   *
   * // frodo and frodoClone are equals when doing a field by field comparison.
   * assertThat(array(frodo)).usingFieldByFieldElementComparator().contains(frodoClone);</code></pre>
   *
   * @return {@code this} assertion object.
   */
  public S usingFieldByFieldElementComparator() {
    return usingElementComparator(new FieldByFieldComparator(comparatorsForElementPropertyOrFieldNames,
                                                             comparatorsForElementPropertyOrFieldTypes));
  }

  /**
   * Use a recursive field/property by field/property comparison (including inherited fields/properties)
   * instead of relying on actual type A <code>equals</code> method to compare group elements for incoming
   * assertion checks. This can be useful if actual's {@code equals} implementation does not suit you.
   * <p>
   * The recursive property/field comparison is <b>not</b> applied on fields having a custom {@code equals}
   * implementation, i.e. the overriden {@code equals} method will be used instead of a field/property by field/property comparison.
   * <p>
   * You can specify a custom comparator per (nested) name or type of element field with
   * {@link #usingComparatorForElementFieldsWithNames(Comparator, String...) usingComparatorForElementFieldsWithNames}
   * and {@link #usingComparatorForElementFieldsWithType(Comparator, Class) usingComparatorForElementFieldsWithType}.
   * <p>
   * The recursive comparison handles cycles.
   * <p>
   * The objects to compare can be of different types but must have the same properties/fields. For example if actual object has a
   * {@code name} String field, the other object must also have one.
   * <p>
   * If an object has a field and a property with the same name, the property value will be used over the field.
   * <p>
   * Example:
   *
   * <pre><code class='java'> TolkienCharacter frodo = new TolkienCharacter("Frodo", 33, HOBBIT);
   * TolkienCharacter pippin = new TolkienCharacter("Pippin", 28, HOBBIT);
   * frodo.setFriend(pippin);
   * pippin.setFriend(frodo);
   *
   * TolkienCharacter frodoClone = new TolkienCharacter("Frodo", 33, HOBBIT);
   * TolkienCharacter pippinClone = new TolkienCharacter("Pippin", 28, HOBBIT);
   * frodoClone.setFriend(pippinClone);
   * pippinClone.setFriend(frodoClone);
   *
   * TolkienCharacter[] hobbits = new TolkienCharacter[] {frodo, pippin};
   *
   * // fails if equals has not been overridden in TolkienCharacter as it would compares object references
   * assertThat(hobbits).contains(frodoClone, pippinClone);
   *
   * // frodo/frodoClone and pippin/pippinClone are equals when doing a recursive property/field by property/field comparison
   * assertThat(hobbits).usingRecursiveFieldByFieldElementComparator()
   *                    .contains(frodoClone, pippinClone);</code></pre>
   *
   * @return {@code this} assertion object.
   * @since 2.5.0 / 3.5.0
   */
  public S usingRecursiveFieldByFieldElementComparator() {
    return usingElementComparator(new RecursiveFieldByFieldComparator(comparatorsForElementPropertyOrFieldNames,
                                                                      comparatorsForElementPropertyOrFieldTypes));
  }

  /**
   * Use field/property by field/property comparison on the <b>given fields/properties only</b> (including inherited
   * fields/properties)instead of relying on actual type A <code>equals</code> method to compare group elements for
   * incoming assertion checks. Private fields are included but this can be disabled using
   * {@link Assertions#setAllowExtractingPrivateFields(boolean)}.
   * <p>
   * This can be handy if <code>equals</code> method of the objects to compare does not suit you.
   * <p>
   * You can specify a custom comparator per name or type of element field with
   * {@link #usingComparatorForElementFieldsWithNames(Comparator, String...)}
   * and {@link #usingComparatorForElementFieldsWithType(Comparator, Class)}.
   * <p>
   * Note that the comparison is <b>not</b> recursive, if one of the fields/properties is an Object, it will be compared
   * to the other field/property using its <code>equals</code> method.
   * </p>
   * Example:
   * <pre><code class='java'> TolkienCharacter frodo = new TolkienCharacter("Frodo", 33, HOBBIT);
   * TolkienCharacter sam = new TolkienCharacter("Sam", 38, HOBBIT);
   *
   * // frodo and sam both are hobbits, so they are equals when comparing only race
   * assertThat(array(frodo)).usingElementComparatorOnFields("race").contains(sam); // OK
   *
   * // ... but not when comparing both name and race
   * assertThat(array(frodo)).usingElementComparatorOnFields("name", "race").contains(sam); // FAIL</code></pre>
   *
   * @return {@code this} assertion object.
   */
  public S usingElementComparatorOnFields(String... fields) {
    return usingElementComparator(new OnFieldsComparator(comparatorsForElementPropertyOrFieldNames,
                                                         comparatorsForElementPropertyOrFieldTypes, fields));
  }

  /**
   * Use field/property by field/property on all fields/properties <b>except</b> the given ones (including inherited
   * fields/properties)instead of relying on actual type A <code>equals</code> method to compare group elements for
   * incoming assertion checks. Private fields are included but this can be disabled using
   * {@link Assertions#setAllowExtractingPrivateFields(boolean)}.
   * <p>
   * This can be handy if <code>equals</code> method of the objects to compare does not suit you.
   * <p>
   * You can specify a custom comparator per name or type of element field with
   * {@link #usingComparatorForElementFieldsWithNames(Comparator, String...)}
   * and {@link #usingComparatorForElementFieldsWithType(Comparator, Class)}.
   * <p>
   * Note that the comparison is <b>not</b> recursive, if one of the fields/properties is an Object, it will be compared
   * to the other field/property using its <code>equals</code> method.
   * </p>
   * Example:
   * <pre><code class='java'> TolkienCharacter frodo = new TolkienCharacter("Frodo", 33, HOBBIT);
   * TolkienCharacter sam = new TolkienCharacter("Sam", 38, HOBBIT);
   *
   * // frodo and sam both are hobbits, so they are equals when comparing only race (i.e. ignoring all other fields)
   * assertThat(array(frodo)).usingElementComparatorIgnoringFields("name", "age").contains(sam); // OK
   *
   * // ... but not when comparing both name and race
   * assertThat(array(frodo)).usingElementComparatorIgnoringFields("age").contains(sam); // FAIL</code></pre>
   *
   * @return {@code this} assertion object.
   */
  public S usingElementComparatorIgnoringFields(String... fields) {
    return usingElementComparator(new IgnoringFieldsComparator(comparatorsForElementPropertyOrFieldNames,
                                                               comparatorsForElementPropertyOrFieldTypes, fields));
  }

  /**
   * Extract the values of given field or property from the array's elements under test into a new array, this new array
   * becoming the array under test.
   * <p>
   * It allows you to test a field/property of the array's elements instead of testing the elements themselves, it can
   * be sometimes much less work !
   * <p>
   * Let's take an example to make things clearer :
   * <pre><code class='java'> // Build a array of TolkienCharacter, a TolkienCharacter has a name (String) and a Race (a class)
   * // they can be public field or properties, both works when extracting their values.
   * TolkienCharacter[] fellowshipOfTheRing = new TolkienCharacter[] {
   *   new TolkienCharacter(&quot;Frodo&quot;, 33, HOBBIT),
   *   new TolkienCharacter(&quot;Sam&quot;, 38, HOBBIT),
   *   new TolkienCharacter(&quot;Gandalf&quot;, 2020, MAIA),
   *   new TolkienCharacter(&quot;Legolas&quot;, 1000, ELF),
   *   new TolkienCharacter(&quot;Pippin&quot;, 28, HOBBIT),
   *   new TolkienCharacter(&quot;Gimli&quot;, 139, DWARF),
   *   new TolkienCharacter(&quot;Aragorn&quot;, 87, MAN,
   *   new TolkienCharacter(&quot;Boromir&quot;, 37, MAN)
   * };
   *
   * // let's verify the names of TolkienCharacter in fellowshipOfTheRing :
   *
   * assertThat(fellowshipOfTheRing).extracting(&quot;name&quot;)
   *           .contains(&quot;Boromir&quot;, &quot;Gandalf&quot;, &quot;Frodo&quot;)
   *           .doesNotContain(&quot;Sauron&quot;, &quot;Elrond&quot;);
   *
   * // you can also extract nested field/property like the name of Race :
   *
   * assertThat(fellowshipOfTheRing).extracting(&quot;race.name&quot;)
   *                                .contains(&quot;Hobbit&quot;, &quot;Elf&quot;)
   *                                .doesNotContain(&quot;Orc&quot;);</code></pre>
   *
   * A property with the given name is looked for first, if it does not exist then a field with the given name
   * is looked for.
   * <p>
   * Note that the order of extracted field/property values is consistent with the array order.
   *
   * @param fieldOrProperty the field/property to extract from the array under test
   * @return a new assertion object whose object under test is the array of extracted field/property values.
   * @throws IntrospectionError if no field or property exists with the given name
   */
  public ObjectArrayAssert<Object> extracting(String fieldOrProperty) {
    Object[] values = FieldsOrPropertiesExtractor.extract(actual, byName(fieldOrProperty));
    return new ObjectArrayAssert<>(values);
  }

  /**
   * Extract the values of given field or property from the array's elements under test into a new array, this new array
   * becoming the array under test with type.
   * <p>
   * It allows you to test a field/property of the array's elements instead of testing the elements themselves, it can
   * be sometimes much less work !
   * <p>
   * Let's take an example to make things clearer :
   * <pre><code class='java'> // Build an array of TolkienCharacter, a TolkienCharacter has a name (String) and a Race (a class)
   * // they can be public field or properties, both works when extracting their values.
   * TolkienCharacter[] fellowshipOfTheRing = new TolkienCharacter[] {
   *   new TolkienCharacter(&quot;Frodo&quot;, 33, HOBBIT),
   *   new TolkienCharacter(&quot;Sam&quot;, 38, HOBBIT),
   *   new TolkienCharacter(&quot;Gandalf&quot;, 2020, MAIA),
   *   new TolkienCharacter(&quot;Legolas&quot;, 1000, ELF),
   *   new TolkienCharacter(&quot;Pippin&quot;, 28, HOBBIT),
   *   new TolkienCharacter(&quot;Gimli&quot;, 139, DWARF),
   *   new TolkienCharacter(&quot;Aragorn&quot;, 87, MAN,
   *   new TolkienCharacter(&quot;Boromir&quot;, 37, MAN)
   * };
   *
   * // let's verify the names of TolkienCharacter in fellowshipOfTheRing :
   *
   * assertThat(fellowshipOfTheRing).extracting(&quot;name&quot;, String.class)
   *           .contains(&quot;Boromir&quot;, &quot;Gandalf&quot;, &quot;Frodo&quot;)
   *           .doesNotContain(&quot;Sauron&quot;, &quot;Elrond&quot;);
   *
   * // you can also extract nested field/property like the name of Race :
   *
   * assertThat(fellowshipOfTheRing).extracting(&quot;race.name&quot;, String.class)
   *                                .contains(&quot;Hobbit&quot;, &quot;Elf&quot;)
   *                                .doesNotContain(&quot;Orc&quot;);</code></pre>
   *
   * A property with the given name is looked for first, if it does not exist then a field with the given name
   * is looked for.
   * <p>
   * Note that the order of extracted field/property values is consistent with the order of the array under test.
   *
   * @param fieldOrProperty the field/property to extract from the array under test
   * @param extractingType type to return
   * @return a new assertion object whose object under test is the array of extracted field/property values.
   * @throws IntrospectionError if no field or property exists with the given name
   */
  public <P> ObjectArrayAssert<P> extracting(String fieldOrProperty, Class<P> extractingType) {
    @SuppressWarnings("unchecked")
    P[] values = (P[]) FieldsOrPropertiesExtractor.extract(actual, byName(fieldOrProperty));
    return new ObjectArrayAssert<>(values);
  }

  /**
   * Extract the values of given fields/properties from the array's elements under test into a new array composed of
   * Tuple (a simple data structure), this new array becoming the array under test.
   * <p>
   * It allows you to test fields/properties of the the array's elements instead of testing the elements themselves, it
   * can be sometimes much less work !
   * <p>
   * The Tuple data corresponds to the extracted values of the given fields/properties, for instance if you ask to
   * extract "id", "name" and "email" then each Tuple data will be composed of id, name and email extracted from the
   * element of the initial array (the Tuple's data order is the same as the given fields/properties order).
   * <p>
   * Let's take an example to make things clearer :
   * <pre><code class='java'> // Build an array of TolkienCharacter, a TolkienCharacter has a name (String) and a Race (a class)
   * // they can be public field or properties, both works when extracting their values.
   * TolkienCharacter[] fellowshipOfTheRing = new TolkienCharacter[] {
   *   new TolkienCharacter(&quot;Frodo&quot;, 33, HOBBIT),
   *   new TolkienCharacter(&quot;Sam&quot;, 38, HOBBIT),
   *   new TolkienCharacter(&quot;Gandalf&quot;, 2020, MAIA),
   *   new TolkienCharacter(&quot;Legolas&quot;, 1000, ELF),
   *   new TolkienCharacter(&quot;Pippin&quot;, 28, HOBBIT),
   *   new TolkienCharacter(&quot;Gimli&quot;, 139, DWARF),
   *   new TolkienCharacter(&quot;Aragorn&quot;, 87, MAN,
   *   new TolkienCharacter(&quot;Boromir&quot;, 37, MAN)
   * };
   *
   * // let's verify 'name' and 'age' of some TolkienCharacter in fellowshipOfTheRing :
   *
   * assertThat(fellowshipOfTheRing).extracting(&quot;name&quot;, &quot;age&quot;)
   *                                .contains(tuple(&quot;Boromir&quot;, 37),
   *                                          tuple(&quot;Sam&quot;, 38),
   *                                          tuple(&quot;Legolas&quot;, 1000));
   *
   *
   * // extract 'name', 'age' and Race name values.
   *
   * assertThat(fellowshipOfTheRing).extracting(&quot;name&quot;, &quot;age&quot;, &quot;race.name&quot;)
   *                                .contains(tuple(&quot;Boromir&quot;, 37, &quot;Man&quot;),
   *                                          tuple(&quot;Sam&quot;, 38, &quot;Hobbit&quot;),
   *                                          tuple(&quot;Legolas&quot;, 1000, &quot;Elf&quot;));</code></pre>
   *
   * A property with the given name is looked for first, if it does not exist the a field with the given name is
   * looked for.
   * <p>
   * Note that the order of extracted property/field values is consistent with the iteration order of the array under
   * test.
   *
   * @param propertiesOrFields the properties/fields to extract from the initial array under test
   * @return a new assertion object whose object under test is the list of Tuple with extracted properties/fields values
   *         as data.
   * @throws IntrospectionError if one of the given name does not match a field or property in one of the initial
   *         Iterable's element.
   */
  public ObjectArrayAssert<Tuple> extracting(String... propertiesOrFields) {
    Object[] values = FieldsOrPropertiesExtractor.extract(actual, byName(propertiesOrFields));
    Tuple[] result = Arrays.copyOf(values, values.length, Tuple[].class);

    return new ObjectArrayAssert<>(result);
  }

  /**
   * Extract the values from the array's elements by applying an extracting function on them. The returned
   * array becomes a new object under test.
   * <p>
   * It allows to test values from the elements in safer way than by using {@link #extracting(String)}, as it
   * doesn't utilize introspection.
   * <p>
   * Let's take a look an example:
   * <pre><code class='java'> // Build a list of TolkienCharacter, a TolkienCharacter has a name, and age and a Race (a specific class)
   * // they can be public field or properties, both can be extracted.
   * TolkienCharacter[] fellowshipOfTheRing = new TolkienCharacter[] {
   *   new TolkienCharacter(&quot;Frodo&quot;, 33, HOBBIT),
   *   new TolkienCharacter(&quot;Sam&quot;, 38, HOBBIT),
   *   new TolkienCharacter(&quot;Gandalf&quot;, 2020, MAIA),
   *   new TolkienCharacter(&quot;Legolas&quot;, 1000, ELF),
   *   new TolkienCharacter(&quot;Pippin&quot;, 28, HOBBIT),
   *   new TolkienCharacter(&quot;Gimli&quot;, 139, DWARF),
   *   new TolkienCharacter(&quot;Aragorn&quot;, 87, MAN,
   *   new TolkienCharacter(&quot;Boromir&quot;, 37, MAN)
   * };
   *
   * // fellowship has hobbitses, right, my presioussss?
   * assertThat(fellowshipOfTheRing).extracting(TolkienCharacter::getRace).contains(HOBBIT);</code></pre>
   *
   * Note that the order of extracted property/field values is consistent with the iteration order of the Iterable under
   * test, for example if it's a {@link HashSet}, you won't be able to make any assumptions on the extracted values
   * order.
   *
   * @param extractor the object transforming input object to desired one
   * @return a new assertion object whose object under test is the list of values extracted
   */
  public <U> ObjectArrayAssert<U> extracting(Extractor<? super T, U> extractor) {
    U[] extracted = FieldsOrPropertiesExtractor.extract(actual, extractor);

    return new ObjectArrayAssert<>(extracted);
  }

  /**
   * Use the given {@link Function}s to extract the values from the array's elements into a new array
   * composed of {@link Tuple}s (a simple data structure containing the extracted values), this new array becoming the
   * object under test.
   * <p>
   * It allows you to test values from the array's elements instead of testing the elements themselves, which sometimes can be
   * much less work!
   * <p>
   * The {@link Tuple} data corresponds to the extracted values from the arrays's elements, for instance if you pass functions
   * extracting "id", "name" and "email" values then each {@link Tuple}'s data will be composed of an id, a name and an email
   * extracted from the element of the initial array (the Tuple's data order is the same as the given functions
   * order).
   * <p>
   * Let's take a look at an example to make things clearer :
   * <pre><code class='java'> // Build an array of TolkienCharacter, a TolkienCharacter has a name (String) and a Race (a class)
   * // they can be public field or properties, both works when extracting their values.
   * TolkienCharacter[] fellowshipOfTheRing = new TolkienCharacter[] {
   *   new TolkienCharacter(&quot;Frodo&quot;, 33, HOBBIT),
   *   new TolkienCharacter(&quot;Sam&quot;, 38, HOBBIT),
   *   new TolkienCharacter(&quot;Gandalf&quot;, 2020, MAIA),
   *   new TolkienCharacter(&quot;Legolas&quot;, 1000, ELF),
   *   new TolkienCharacter(&quot;Pippin&quot;, 28, HOBBIT),
   *   new TolkienCharacter(&quot;Gimli&quot;, 139, DWARF),
   *   new TolkienCharacter(&quot;Aragorn&quot;, 87, MAN,
   *   new TolkienCharacter(&quot;Boromir&quot;, 37, MAN)
   * };
   *
   * // let's verify 'name', 'age' and Race of some TolkienCharacter in fellowshipOfTheRing :
   * assertThat(fellowshipOfTheRing).extracting(TolkienCharacter::getName,
   *                                            character -> character.getAge(),
   *                                            TolkienCharacter::getRace)
   *                                .containsOnly(tuple(&quot;Frodo&quot;, 33, HOBBIT),
   *                                              tuple(&quot;Sam&quot;, 38, HOBBIT),
   *                                              tuple(&quot;Gandalf&quot;, 2020, MAIA),
   *                                              tuple(&quot;Legolas&quot;, 1000, ELF),
   *                                              tuple(&quot;Pippin&quot;, 28, HOBBIT),
   *                                              tuple(&quot;Gimli&quot;, 139, DWARF),
   *                                              tuple(&quot;Aragorn&quot;, 87, MAN),
   *                                              tuple(&quot;Boromir&quot;, 37, MAN));</code></pre>
   * You can use lambda expression or a method reference to extract the expected values.
   * <p>
   * Use {@link Tuple#tuple(Object...)} to initialize the expected values.
   * <p>
   * Note that the order of the extracted tuples list is consistent with the iteration order of the array under test,
   * for example if it's a {@link HashSet}, you won't be able to make any assumptions on the extracted tuples order.
   *
   * @param extractors the extractor functions to extract a value from an element of the array under test.
   * @return a new assertion object whose object under test is the array of Tuples containing the extracted values.
   */
  @SafeVarargs
  public final ObjectArrayAssert<Tuple> extracting(Function<T, ?>... extractors) {
    // combine all extractors into one function
    Function<T, Tuple> tupleExtractor = objectToExtractValueFrom -> {
      Tuple tuple = new Tuple();
      for (Function<T, ?> extractor : extractors) {
        // extract value one by one
        tuple.addData(extractor.apply(objectToExtractValueFrom));
      }
      return tuple;
    };
    Tuple[] tuples = stream(actual).map(tupleExtractor).toArray(size -> new Tuple[size]);
    return new ObjectArrayAssert<Tuple>(tuples);
  }

  /**
   * Extract the Iterable values from arrays elements under test by applying an Iterable extracting function on them
   * and concatenating the result lists into an array which becomes the new object under test.
   * <p>
   * It allows testing the results of extracting values that are represented by Iterables.
   * <p>
   * For example:
   * <pre><code class='java'> CartoonCharacter bart = new CartoonCharacter("Bart Simpson");
   * CartoonCharacter lisa = new CartoonCharacter("Lisa Simpson");
   * CartoonCharacter maggie = new CartoonCharacter("Maggie Simpson");
   * CartoonCharacter homer = new CartoonCharacter("Homer Simpson");
   * homer.addChildren(bart, lisa, maggie);
   *
   * CartoonCharacter pebbles = new CartoonCharacter("Pebbles Flintstone");
   * CartoonCharacter fred = new CartoonCharacter("Fred Flintstone");
   * fred.getChildren().add(pebbles);
   *
   * CartoonCharacter[] parents = new CartoonCharacter[] { homer, fred };
   * // check children
   * assertThat(parents).flatExtracting(CartoonCharacter::getChildren)
   *                    .containsOnly(bart, lisa, maggie, pebbles);</code></pre>
   *
   * The order of extracted values is consisted with both the order of the collection itself, as well as the extracted
   * collections.
   *
   * @param extractor the object transforming input object to an Iterable of desired ones
   * @return a new assertion object whose object under test is the list of values extracted
   */
  public <U, C extends Collection<U>> ObjectArrayAssert<U> flatExtracting(Extractor<? super T, C> extractor) {
    final List<C> extractedValues = FieldsOrPropertiesExtractor.extract(Arrays.asList(actual), extractor);

    final List<U> result = newArrayList();
    for (C e : extractedValues) {
      result.addAll(e);
    }

    return new ObjectArrayAssert<>(IterableUtil.toArray(result));
  }

  /**
   * Extract from array's elements the Iterable/Array values corresponding to the given property/field name and
   * concatenate them into a single array becoming the new object under test.
   * <p>
   * It allows testing the elements of extracting values that are represented by iterables or arrays.
   * <p>
   * For example:
   * <pre><code class='java'> CartoonCharacter bart = new CartoonCharacter("Bart Simpson");
   * CartoonCharacter lisa = new CartoonCharacter("Lisa Simpson");
   * CartoonCharacter maggie = new CartoonCharacter("Maggie Simpson");
   * CartoonCharacter homer = new CartoonCharacter("Homer Simpson");
   * homer.addChildren(bart, lisa, maggie);
   *
   * CartoonCharacter pebbles = new CartoonCharacter("Pebbles Flintstone");
   * CartoonCharacter fred = new CartoonCharacter("Fred Flintstone");
   * fred.getChildren().add(pebbles);
   *
   * CartoonCharacter[] parents = new CartoonCharacter[] { homer, fred };
   * // check children
   * assertThat(parents).flatExtracting("children")
   *                    .containsOnly(bart, lisa, maggie, pebbles);</code></pre>
   *
   * The order of extracted values is consisted with both the order of the collection itself, as well as the extracted
   * collections.
   *
   * @param propertyName the object transforming input object to an Iterable of desired ones
   * @return a new assertion object whose object under test is the list of values extracted
   * @throws IllegalArgumentException if one of the extracted property value was not an array or an iterable.
   */
  public ObjectArrayAssert<Object> flatExtracting(String propertyName) {
    List<Object> extractedValues = newArrayList();
    List<?> extractedGroups = FieldsOrPropertiesExtractor.extract(Arrays.asList(actual), byName(propertyName));
    for (Object group : extractedGroups) {
      // expecting group to be an iterable or an array
      if (isArray(group)) {
        int size = Array.getLength(group);
        for (int i = 0; i < size; i++) {
          extractedValues.add(Array.get(group, i));
        }
      } else if (group instanceof Iterable) {
        Iterable<?> iterable = (Iterable<?>) group;
        for (Object value : iterable) {
          extractedValues.add(value);
        }
      } else {
        CommonErrors.wrongElementTypeForFlatExtracting(group);
      }
    }
    return new ObjectArrayAssert<>(extractedValues.toArray());
  }

  /**
   * Extract the result of given method invocation from the array's elements under test into a new array, this new array
   * becoming the array under test.
   * <p>
   * It allows you to test a method results of the array's elements instead of testing the elements themselves, it can be
   * sometimes much less work!
   * <p>
   * It is especially useful for classes that does not conform to Java Bean's getter specification (i.e. public String
   * toString() or public String status() instead of public String getStatus()).
   * <p>
   * Let's take an example to make things clearer :
   * <pre><code class='java'> // Build a array of WesterosHouse, a WesterosHouse has a method: public String sayTheWords()
   * WesterosHouse[] greatHousesOfWesteros = new WesterosHouse[] { new WesterosHouse(&quot;Stark&quot;, &quot;Winter is Coming&quot;),
   *     new WesterosHouse(&quot;Lannister&quot;, &quot;Hear Me Roar!&quot;), new WesterosHouse(&quot;Greyjoy&quot;, &quot;We Do Not Sow&quot;),
   *     new WesterosHouse(&quot;Baratheon&quot;, &quot;Our is the Fury&quot;), new WesterosHouse(&quot;Martell&quot;, &quot;Unbowed, Unbent, Unbroken&quot;),
   *     new WesterosHouse(&quot;Tyrell&quot;, &quot;Growing Strong&quot;) };
   *
   * // let's verify the words of the great houses of Westeros:
   * assertThat(greatHousesOfWesteros).extractingResultOf(&quot;sayTheWords&quot;)
   *                                  .contains(&quot;Winter is Coming&quot;, &quot;We Do Not Sow&quot;, &quot;Hear Me Roar&quot;)
   *                                  .doesNotContain(&quot;Lannisters always pay their debts&quot;);</code></pre>
   *
   * <p>
   * Following requirements have to be met to extract method results:
   * <ul>
   * <li>method has to be public,</li>
   * <li>method cannot accept any arguments,</li>
   * <li>method cannot return void.</li>
   * </ul>
   * <p>
   * Note that the order of extracted values is consistent with the order of the array under test.
   *
   * @param method the name of the method which result is to be extracted from the array under test
   * @return a new assertion object whose object under test is the array of extracted values.
   * @throws IllegalArgumentException if no method exists with the given name, or method is not public, or method does
   *           return void, or method accepts arguments.
   */
  public ObjectArrayAssert<Object> extractingResultOf(String method) {
    Object[] values = FieldsOrPropertiesExtractor.extract(actual, resultOf(method));
    return new ObjectArrayAssert<>(values);
  }

  /**
   * Extract the result of given method invocation from the array's elements under test into a new array, this new array
   * becoming the array under test.
   * <p>
   * It allows you to test a method results of the array's elements instead of testing the elements themselves, it can be
   * sometimes much less work!
   * <p>
   * It is especially useful for classes that does not conform to Java Bean's getter specification (i.e. public String
   * toString() or public String status() instead of public String getStatus()).
   * <p>
   * Let's take an example to make things clearer :
   * <pre><code class='java'> // Build a array of WesterosHouse, a WesterosHouse has a method: public String sayTheWords()
   * WesterosHouse[] greatHousesOfWesteros = new WesterosHouse[] { new WesterosHouse(&quot;Stark&quot;, &quot;Winter is Coming&quot;),
   *     new WesterosHouse(&quot;Lannister&quot;, &quot;Hear Me Roar!&quot;), new WesterosHouse(&quot;Greyjoy&quot;, &quot;We Do Not Sow&quot;),
   *     new WesterosHouse(&quot;Baratheon&quot;, &quot;Our is the Fury&quot;), new WesterosHouse(&quot;Martell&quot;, &quot;Unbowed, Unbent, Unbroken&quot;),
   *     new WesterosHouse(&quot;Tyrell&quot;, &quot;Growing Strong&quot;) };
   *
   * // let's verify the words of the great houses of Westeros:
   * assertThat(greatHousesOfWesteros).extractingResultOf(&quot;sayTheWords&quot;, String.class)
   *                                  .contains(&quot;Winter is Coming&quot;, &quot;We Do Not Sow&quot;, &quot;Hear Me Roar&quot;)
   *                                  .doesNotContain(&quot;Lannisters always pay their debts&quot;);</code></pre>
   *
   * <p>
   * Following requirements have to be met to extract method results:
   * <ul>
   * <li>method has to be public,</li>
   * <li>method can not accept any arguments,</li>
   * <li>method can not return void.</li>
   * </ul>
   * <p>
   * Note that the order of extracted values is consistent with the order of the array under test.
   *
   * @param method the name of the method which result is to be extracted from the array under test
   * @param extractingType type to return
   * @return a new assertion object whose object under test is the array of extracted values.
   * @throws IllegalArgumentException if no method exists with the given name, or method is not public, or method does
   *           return void, or method accepts arguments.
   */
  public <P> ObjectArrayAssert<P> extractingResultOf(String method, Class<P> extractingType) {
    @SuppressWarnings("unchecked")
    P[] values = (P[]) FieldsOrPropertiesExtractor.extract(actual, resultOf(method));
    return new ObjectArrayAssert<>(values);
  }

  /**
   * Enable hexadecimal object representation of Iterable elements instead of standard java representation in error
   * messages.
   * <p>
   * It can be useful to better understand what the error was with a more meaningful error message.
   * <p>
   * Example
   * <pre><code class='java'> assertThat(new Byte[] { 0x10, 0x20 }).inHexadecimal().contains(new Byte[] { 0x30 });</code></pre>
   *
   * With standard error message:
   * <pre><code class='java'> Expecting:
   *  <[16, 32]>
   * to contain:
   *  <[48]>
   * but could not find:
   *  <[48]></code></pre>
   *
   * With Hexadecimal error message:
   * <pre><code class='java'> Expecting:
   *  <[0x10, 0x20]>
   * to contain:
   *  <[0x30]>
   * but could not find:
   *  <[0x30]></code></pre>
   *
   * @return {@code this} assertion object.
   */
  @Override
  public S inHexadecimal() {
    return super.inHexadecimal();
  }

  @Override
  public S inBinary() {
    return super.inBinary();
  }

  /**
   * Filter the array under test keeping only elements having a property or field equal to {@code expectedValue}, the
   * property/field is specified by {@code propertyOrFieldName} parameter.
   * <p>
   * The filter first tries to get the value from a property (named {@code propertyOrFieldName}), if no such property
   * exists it tries to read the value from a field. Reading private fields is supported by default, this can be
   * globally disabled by calling {@link Assertions#setAllowExtractingPrivateFields(boolean)
   * Assertions.setAllowExtractingPrivateFields(false)}.
   * <p>
   * When reading <b>nested</b> property/field, if an intermediate value is null the whole nested property/field is
   * considered to be null, thus reading "address.street.name" value will return null if "street" value is null.
   * <p>
   *
   * As an example, let's check all employees 800 years old (yes, special employees):
   * <pre><code class='java'> Employee yoda   = new Employee(1L, new Name("Yoda"), 800);
   * Employee obiwan = new Employee(2L, new Name("Obiwan"), 800);
   * Employee luke   = new Employee(3L, new Name("Luke", "Skywalker"), 26);
   * Employee noname = new Employee(4L, null, 50);
   *
   * Employee[] employees = new Employee[] { yoda, luke, obiwan, noname };
   *
   * assertThat(employees).filteredOn("age", 800)
   *                      .containsOnly(yoda, obiwan);</code></pre>
   *
   * Nested properties/fields are supported:
   * <pre><code class='java'> // Name is bean class with 'first' and 'last' String properties
   *
   * // name is null for noname => it does not match the filter on "name.first"
   * assertThat(employees).filteredOn("name.first", "Luke")
   *                      .containsOnly(luke);
   *
   * assertThat(employees).filteredOn("name.last", "Vader")
   *                      .isEmpty();</code></pre>
   * <p>
   * If you want to filter on null value, use {@link #filteredOnNull(String)} as Java will resolve the call to
   * {@link #filteredOn(String, FilterOperator)} instead of this method.
   * <p>
   * An {@link IntrospectionError} is thrown if the given propertyOrFieldName can't be found in one of the array
   * elements.
   * <p>
   * You can chain filters:
   * <pre><code class='java'> // fellowshipOfTheRing is an array of TolkienCharacter having race and name fields
   * // 'not' filter is statically imported from Assertions.not
   *
   * assertThat(fellowshipOfTheRing).filteredOn("race.name", "Man")
   *                                .filteredOn("name", not("Boromir"))
   *                                .containsOnly(aragorn);</code></pre>
   * If you need more complex filter, use {@link #filteredOn(Condition)} and provide a {@link Condition} to specify the
   * filter to apply.
   *
   * @param propertyOrFieldName the name of the property or field to read
   * @param expectedValue the value to compare element's property or field with
   * @return a new assertion object with the filtered array under test
   * @throws IllegalArgumentException if the given propertyOrFieldName is {@code null} or empty.
   * @throws IntrospectionError if the given propertyOrFieldName can't be found in one of the array elements.
   */
  @SuppressWarnings("unchecked")
  public S filteredOn(String propertyOrFieldName, Object expectedValue) {
    Iterable<? extends T> filteredIterable = filter(actual).with(propertyOrFieldName, expectedValue).get();
    return (S) new ObjectArrayAssert<>(toArray(filteredIterable));
  }

  /**
   * Filter the array under test keeping only elements whose property or field specified by {@code propertyOrFieldName}
   * is null.
   * <p>
   * exists it tries to read the value from a field. Reading private fields is supported by default, this can be
   * globally disabled by calling {@link Assertions#setAllowExtractingPrivateFields(boolean)
   * Assertions.setAllowExtractingPrivateFields(false)}.
   * <p>
   * When reading <b>nested</b> property/field, if an intermediate value is null the whole nested property/field is
   * considered to be null, thus reading "address.street.name" value will return null if "street" value is null.
   * <p>
   * As an example, let's check all employees 800 years old (yes, special employees):
   * <pre><code class='java'> Employee yoda   = new Employee(1L, new Name("Yoda"), 800);
   * Employee obiwan = new Employee(2L, new Name("Obiwan"), 800);
   * Employee luke   = new Employee(3L, new Name("Luke", "Skywalker"), 26);
   * Employee noname = new Employee(4L, null, 50);
   *
   * Employee[] employees = new Employee[] { yoda, luke, obiwan, noname };
   *
   * assertThat(employees).filteredOnNull("name")
   *                      .containsOnly(noname);</code></pre>
   *
   * Nested properties/fields are supported:
   * <pre><code class='java'> // Name is bean class with 'first' and 'last' String properties
   *
   * assertThat(employees).filteredOnNull("name.last")
   *                      .containsOnly(yoda, obiwan, noname);</code></pre>
   *
   * An {@link IntrospectionError} is thrown if the given propertyOrFieldName can't be found in one of the array
   * elements.
   * <p>
   * If you need more complex filter, use {@link #filteredOn(Condition)} and provide a {@link Condition} to specify the
   * filter to apply.
   *
   * @param propertyOrFieldName the name of the property or field to read
   * @return a new assertion object with the filtered array under test
   * @throws IntrospectionError if the given propertyOrFieldName can't be found in one of the array elements.
   */
  public S filteredOnNull(String propertyOrFieldName) {
    // need to cast nulll to Object otherwise it calls :
    // filteredOn(String propertyOrFieldName, FilterOperation<?> filterOperation)
    return filteredOn(propertyOrFieldName, (Object) null);
  }

  /**
   * Filter the array under test keeping only elements having a property or field matching the filter expressed with
   * the {@link FilterOperator}, the property/field is specified by {@code propertyOrFieldName} parameter.
   * <p>
   * The existing filters are :
   * <ul>
   * <li> {@link Assertions#not(Object) not(Object)}</li>
   * <li> {@link Assertions#in(Object...) in(Object...)}</li>
   * <li> {@link Assertions#notIn(Object...) notIn(Object...)}</li>
   * </ul>
   * <p>
   * Whatever filter is applied, it first tries to get the value from a property (named {@code propertyOrFieldName}), if
   * no such property exists it tries to read the value from a field. Reading private fields is supported by default,
   * this can be globally disabled by calling {@link Assertions#setAllowExtractingPrivateFields(boolean)
   * Assertions.setAllowExtractingPrivateFields(false)}.
   * <p>
   * When reading <b>nested</b> property/field, if an intermediate value is null the whole nested property/field is
   * considered to be null, thus reading "address.street.name" value will return null if "street" value is null.
   * <p>
   *
   * As an example, let's check stuff on some special employees :
   * <pre><code class='java'> Employee yoda   = new Employee(1L, new Name("Yoda"), 800);
   * Employee obiwan = new Employee(2L, new Name("Obiwan"), 800);
   * Employee luke   = new Employee(3L, new Name("Luke", "Skywalker"), 26);
   *
   * Employee[] employees = new Employee[] { yoda, luke, obiwan, noname };
   *
   * // 'not' filter is statically imported from Assertions.not
   * assertThat(employees).filteredOn("age", not(800))
   *                      .containsOnly(luke);
   *
   * // 'in' filter is statically imported from Assertions.in
   * // Name is bean class with 'first' and 'last' String properties
   * assertThat(employees).filteredOn("name.first", in("Yoda", "Luke"))
   *                      .containsOnly(yoda, luke);
   *
   * // 'notIn' filter is statically imported from Assertions.notIn
   * assertThat(employees).filteredOn("name.first", notIn("Yoda", "Luke"))
   *                      .containsOnly(obiwan);</code></pre>
   *
   * An {@link IntrospectionError} is thrown if the given propertyOrFieldName can't be found in one of the array
   * elements.
   * <p>
   * Note that combining filter operators is not supported, thus the following code is not correct:
   * <pre><code class='java'> // Combining filter operators like not(in(800)) is NOT supported
   * // -&gt; throws UnsupportedOperationException
   * assertThat(employees).filteredOn("age", not(in(800)))
   *                      .contains(luke);</code></pre>
   * <p>
   * You can chain filters:
   * <pre><code class='java'> // fellowshipOfTheRing is an array of TolkienCharacter having race and name fields
   * // 'not' filter is statically imported from Assertions.not
   *
   * assertThat(fellowshipOfTheRing).filteredOn("race.name", "Man")
   *                                .filteredOn("name", not("Boromir"))
   *                                .containsOnly(aragorn);</code></pre>
   *
   * If you need more complex filter, use {@link #filteredOn(Condition)} and provide a {@link Condition} to specify the
   * filter to apply.
   *
   * @param propertyOrFieldName the name of the property or field to read
   * @param filterOperator the filter operator to apply
   * @return a new assertion object with the filtered array under test
   * @throws IllegalArgumentException if the given propertyOrFieldName is {@code null} or empty.
   */
  @SuppressWarnings("unchecked")
  public S filteredOn(String propertyOrFieldName, FilterOperator<?> filterOperator) {
    checkNotNull(filterOperator);
    Filters<? extends T> filter = filter(actual).with(propertyOrFieldName);
    filterOperator.applyOn(filter);
    return (S) new ObjectArrayAssert<>(toArray(filter.get()));
  }

  /**
   * Filter the array under test keeping only elements matching the given {@link Condition}.
   * <p>
   * Let's check old employees whose age > 100:
   * <pre><code class='java'> Employee yoda   = new Employee(1L, new Name("Yoda"), 800);
   * Employee obiwan = new Employee(2L, new Name("Obiwan"), 800);
   * Employee luke   = new Employee(3L, new Name("Luke", "Skywalker"), 26);
   * Employee noname = new Employee(4L, null, 50);
   *
   * Employee[] employees = new Employee[] { yoda, luke, obiwan, noname };
   *
   * // old employee condition, "old employees" describes the condition in error message
   * // you just have to implement 'matches' method
   * Condition&lt;Employee&gt; oldEmployees = new Condition&lt;Employee&gt;("old employees") {
   *       {@literal @}Override
   *       public boolean matches(Employee employee) {
   *         return employee.getAge() > 100;
   *       }
   *     };
   *   }
   * assertThat(employees).filteredOn(oldEmployees)
   *                      .containsOnly(yoda, obiwan);</code></pre>
   *
   * You can combine {@link Condition} with condition operator like {@link Not}:
   * <pre><code class='java'> // 'not' filter is statically imported from Assertions.not
   * assertThat(employees).filteredOn(not(oldEmployees))
   *                      .contains(luke, noname);</code></pre>
   *
   * @param condition the filter condition / predicate
   * @return a new assertion object with the filtered array under test
   * @throws IllegalArgumentException if the given condition is {@code null}.
   */
  @SuppressWarnings("unchecked")
  public S filteredOn(Condition<? super T> condition) {
    Iterable<? extends T> filteredIterable = filter(actual).being(condition).get();
    return (S) new ObjectArrayAssert<>(toArray(filteredIterable));
  }

  /**
   * Filter the iterable under test keeping only elements matching the given {@link Predicate}.
   * <p>
   * Example : check old employees whose age > 100:
   *
   * <pre><code class='java'> Employee yoda   = new Employee(1L, new Name("Yoda"), 800);
   * Employee obiwan = new Employee(2L, new Name("Obiwan"), 800);
   * Employee luke   = new Employee(3L, new Name("Luke", "Skywalker"), 26);
   *
   * Employee[] employees = new Employee[] { yoda, luke, obiwan };
   *
   * assertThat(employees).filteredOn(employee -> employee.getAge() > 100)
   *                      .containsOnly(yoda, obiwan);</code></pre>
   *
   * @param predicate the filter predicate
   * @return a new assertion object with the filtered array under test
   * @throws IllegalArgumentException if the given predicate is {@code null}.
   */
  @SuppressWarnings("unchecked")
  public S filteredOn(Predicate<? super T> predicate) {
    if (predicate == null) throw new IllegalArgumentException("The filter predicate should not be null");
    return (S) new ObjectArrayAssert<>(stream(actual).filter(predicate).toArray());
  }

  /**
   * Verifies that all elements match the given {@link Predicate}.
   * <p>
   * Example :
   * <pre><code class='java'> String[] abc  = new String[] {"a", "b", "c"};
   * String[] abcc  = new String[] {"a", "b", "cc"};
   *
   * // assertion will pass
   * assertThat(abc).allMatch(s -&gt; s.length() == 1);
   *
   * // assertion will fail
   * assertThat(abcc).allMatch(s -&gt; s.length() == 1);</code></pre>
   *
   * Note that you can achieve the same result with {@link #are(Condition) are(Condition)} or {@link #have(Condition) have(Condition)}.
   *
   * @param predicate the given {@link Predicate}.
   * @return {@code this} object.
   * @throws NullPointerException if the given predicate is {@code null}.
   * @throws AssertionError if an element cannot be cast to T.
   * @throws AssertionError if one or more elements don't satisfy the given predicate.
   */
  @Override
  public S allMatch(Predicate<? super T> condition) {
    iterables.assertAllMatch(info, newArrayList(actual), condition, PredicateDescription.GIVEN);
    return myself;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public S allMatch(Predicate<? super T> predicate, String predicateDescription) {
    iterables.assertAllMatch(info, newArrayList(actual), predicate, new PredicateDescription(predicateDescription));
    return myself;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public S allSatisfy(Consumer<? super T> requirements) {
    iterables.assertAllSatisfy(info, newArrayList(actual), requirements);
    return myself;
  }

}
