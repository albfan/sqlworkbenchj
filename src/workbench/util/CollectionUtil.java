/*
 * CollectionUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility functions for Collection handling.
 *
 * @author Thomas Kellerer
 */
public class CollectionUtil
{
  public static void clear(Collection c)
  {
    if (c != null)
    {
      c.clear();
    }
  }
  
  public static boolean isNonEmpty(Collection c)
  {
    return (c != null && c.size() > 0);
  }

  public static boolean isNonEmpty(Map map)
  {
    return (map != null && map.size() > 0);
  }

  public static boolean isEmpty(Collection c)
  {
    return (c == null || c.isEmpty());
  }

  public static boolean isEmpty(Map m)
  {
    return (m == null || m.isEmpty());
  }

  public static <E> Set<E> treeSet(E... add)
  {
    Set<E> result = new TreeSet<>();
    if (add != null)
    {
      result.addAll(Arrays.asList(add));
    }
    return result;
  }

  public static <E> Set<E> treeSet(Set<E> base, E... add)
  {
    Set<E> result = new TreeSet<>();
    result.addAll(base);
    if (add != null)
    {
      result.addAll(Arrays.asList(add));
    }
    return result;
  }

  public static Set<String> caseInsensitiveSet()
  {
    return new TreeSet<>(CaseInsensitiveComparator.INSTANCE);
  }

  public static Set<String> unmodifiableSet(Set<String> base, String... add)
  {
    return Collections.unmodifiableSet(caseInsensitiveSet(base, add));
  }

  public static Set<String> unmodifiableSet(String... a)
  {
    return Collections.unmodifiableSet(CollectionUtil.caseInsensitiveSet(a));
  }

  public static Set<String> caseInsensitiveSet(String... a)
  {
    Set<String> result = caseInsensitiveSet();
    result.addAll(Arrays.asList(a));
    return result;
  }

  public static Set<String> caseInsensitiveSet(Collection<String> elements)
  {
    Set<String> result = caseInsensitiveSet();
    if (elements != null)
    {
      result.addAll(elements);
    }
    return result;
  }

  public static Set<String> caseInsensitiveSet(Set<String> base, String... a)
  {
    Set<String> result = caseInsensitiveSet();
    result.addAll(base);
    result.addAll(Arrays.asList(a));
    return result;
  }

  public static <E> List<E> arrayList(List<E> source)
  {
    return new ArrayList<>(source);
  }

  public static <E> List<E> sizedArrayList(int capacity)
  {
    return new ArrayList<>(capacity);
  }

  public static <E> List<E> arrayList()
  {
    return new ArrayList<>();
  }

  /**
   * Create an ArrayList from the given elements. The returned list
   * can be changed (in constrast to Arrays.asList() where a non-modifieable list
   * is returned)
   */
  public static <E> List<E> arrayList(E... a)
  {
    ArrayList<E> result = new ArrayList<>(a.length);
    result.addAll(Arrays.asList(a));
    return result;
  }

  public static <E> List<E> readOnlyList(E... a)
  {
    return Collections.unmodifiableList(arrayList(a));
  }

  /**
   * Remove the given element from the array.
   *
   * The check is done using String.equalsIgnoreCase() so it's case in-sensitive.
   *
   * @param array   the array
   * @param remove  the element to remove
   * @return a copy of the original array without the element.
   */
  public static String[] removeElement(String[] array, String remove)
  {
    if (array == null) return array;
    if (remove == null) return array;

    List<String> elements = new ArrayList<>(array.length);
    for (String s : array)
    {
      if (s.equalsIgnoreCase(remove)) continue;
      elements.add(s);
    }
    return elements.toArray(new String[0]);
  }

  public static void replaceElement(String[] array, String oldName, String newName)
  {
    if (array == null || oldName == null || newName == null) return;
    for (int i=0; i < array.length; i++)
    {
      if (oldName.equals(array[i]))
      {
        array[i] = newName;
        return;
      }
    }
  }

  public static boolean containsAny(Set<String> haystack, Set<String> needles)
  {
    if (needles.stream().anyMatch((needle) -> (haystack.contains(needle)))) return true;
    return false;
  }
}
