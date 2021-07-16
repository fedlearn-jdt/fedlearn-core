/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.type.data;

import java.io.Serializable;
import java.util.Objects;


/**
 * <p>A convenience class to represent Tuple .</p>
 */
public class StringTuple2 implements Serializable {

    /**
     * first of this <code>Pair</code>.
     */
    private String first;
    /**
     * second of this this <code>Pair</code>.
     */
    private String second;


    public StringTuple2() {
    }

    /**
     * Creates a new pair
     *
     * @param first  The key for this pair
     * @param second The value to use for this pair
     */
    public StringTuple2(@NamedArg("first") String first, @NamedArg("second") String second) {
        this.first = first;
        this.second = second;
    }


    /**
     * Gets the first of this pair.
     *
     * @return first of this pair
     */
    public String getFirst() {
        return first;
    }

    /**
     * Gets the value for this pair.
     *
     * @return value for this pair
     */
    public String getSecond() {
        return second;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    /**
     * <p><code>String</code> representation of this
     * <code>Pair</code>.</p>
     *
     * <p>The default name/value delimiter '=' is always used.</p>
     *
     * @return <code>String</code> representation of this <code>Pair</code>
     */
    @Override
    public String toString() {
        return first + "=" + second;
    }

    /**
     * <p>Generate a hash code for this <code>Pair</code>.</p>
     *
     * <p>The hash code is calculated using both the name and
     * the value of the <code>Pair</code>.</p>
     *
     * @return hash code for this <code>Pair</code>
     */
    @Override
    public int hashCode() {
        // name's hashCode is multiplied by an arbitrary prime number (13)
        // in order to make sure there is a difference in the hashCode between
        // these two parameters:
        //  name: a  value: aa
        //  name: aa value: a
        return first.hashCode() * 13 + (second == null ? 0 : second.hashCode());
    }

    /**
     * <p>Test this <code>Pair</code> for equality with another
     * <code>Object</code>.</p>
     *
     * <p>If the <code>Object</code> to be tested is not a
     * <code>Pair</code> or is <code>null</code>, then this method
     * returns <code>false</code>.</p>
     *
     * <p>Two <code>Pair</code>s are considered equal if and only if
     * both the names and values are equal.</p>
     *
     * @param o the <code>Object</code> to test for
     *          equality with this <code>Pair</code>
     * @return <code>true</code> if the given <code>Object</code> is
     * equal to this <code>Pair</code> else <code>false</code>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof StringTuple2) {
            StringTuple2 pair = (StringTuple2) o;
            if (!Objects.equals(first, pair.first)) {
                return false;
            }
            if (!Objects.equals(second, pair.second)) {
                return false;
            }
            return true;
        }
        return false;
    }

}
