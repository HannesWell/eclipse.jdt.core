/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Joerg Kubitz    - refactoring
 *******************************************************************************/

package org.eclipse.jdt.internal.core.util;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.jdt.internal.core.JavaElement;

/** Utility to provide deduplication by best effort. **/
public final class DeduplicationUtil {
	private DeduplicationUtil() {
	}

	private static final Map<Object, Reference<Object>> objectCache = new WeakHashMap<>(20 * 4 / 3 + 1);
	private static final Map<String, Reference<String>> stringSymbols = new WeakHashMap<>(20 * 4 / 3 + 1);
	private static final WeakHashSetOfCharArray charArraySymbols = new WeakHashSetOfCharArray();

	private static <T> T add(T element, Map<T, Reference<T>> cache) {
		cache.computeIfAbsent(element, WeakReference::new);
		T referent = cache.computeIfAbsent(element, WeakReference::new).get();
		if (referent != null) {
			return referent;
		}
		cache.put(element, new WeakReference<>(element));
		return element;
	}

	@SuppressWarnings("unchecked")
	public static <T> T internObject(T obj) {
		if (obj == null) {
			return null;
		}
		synchronized (objectCache) {
			return (T) add(obj, objectCache);
		}
	}

	public static char[] intern(char[] array) {
		synchronized (charArraySymbols) {
			return charArraySymbols.add(array);
		}
	}

	public static String toString(char[] array) {
		return intern(new String(array));
	}

	/*
	 * Used as a replacement for String#intern() that could prevent garbage collection of strings on some VMs.
	 */
	public static String intern(String s) {
		if (s == null) {
			return null;
		}
		synchronized (stringSymbols) {
			return add(s, stringSymbols);
		}
	}

	public static String[] intern(String[] a) {
		if (a.length == 0) {
			return JavaElement.NO_STRINGS;
		}
		synchronized (stringSymbols) {
			for (int j = 0; j < a.length; j++) {
				a[j] = a[j] == null ? null : add(a[j], stringSymbols);
			}
			return a;
		}
	}

	/** interns the elements and the list as whole **/
	public static List<String> intern(List<String> a) {
		if (a.size() == 0) {
			return List.of();
		}
		synchronized (objectCache) {
			Reference<Object> existing = objectCache.get(a);
			if (existing != null && existing.get() instanceof List l) {
				@SuppressWarnings("unchecked")
				List<String> existingList = l;
				return existingList;
			}
		}

		List<String> result = new ArrayList<>(a.size());
		synchronized (stringSymbols) {
			for (String s:a) {
				result.add(s == null ? null : add(s, stringSymbols));
			}
		}
		return internObject(List.copyOf(result));
	}
}
