/*
 * Copyright 2022 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * https://rcenvironment.de/
 */
 
package de.rcenvironment.core.utils.testing;

import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * 
 * Hamcrest Matchers for Collections.
 *
 * @author Alexander Weinert
 */
public final class CollectionMatchers {
    
    private CollectionMatchers() {}
    
    public static <T> Matcher<Collection<T>> isEmpty() {
        return new TypeSafeDiagnosingMatcher<Collection<T>>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText(" is an empty collection");
            }

            @Override
            protected boolean matchesSafely(Collection<T> arg0, Description arg1) {
                return arg0.isEmpty();
            }
        };
    }

    
    public static <T> Matcher<Collection<T>> contains(T expected) {
        return new TypeSafeDiagnosingMatcher<Collection<T>>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText(" contains ");
                arg0.appendValue(expected);
            }

            @Override
            protected boolean matchesSafely(Collection<T> arg0, Description arg1) {
                return arg0.contains(expected);
            }
        };
    }
    
    public static <T> Matcher<Collection<T>> size(int expected) {
        return new TypeSafeDiagnosingMatcher<Collection<T>>() {

            @Override
            public void describeTo(Description arg0) {
                arg0.appendText(" is of size ");
                arg0.appendValue(expected);
            }

            @Override
            protected boolean matchesSafely(Collection<T> arg0, Description arg1) {
                return arg0.size() == expected;
            }
        };
    }

}
