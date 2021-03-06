/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.execution.engine.window;

import io.crate.metadata.ColumnIdent;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.contains;

public class RowNumberWindowFunctionTest extends AbstractWindowFunctionTest {

    @Test
    public void testRowNumberFunction() throws Exception {
        Object[] expected = new Object[] {1, 2, 3, 4};

        assertEvaluate("row_number() over(order by x)",
            contains(expected),
            Collections.singletonMap(new ColumnIdent("x"), 0),
            new Object[] {4},
            new Object[] {3},
            new Object[] {2},
            new Object[] {1}
        );
    }

    @Test
    public void testRowNumberOverPartitionedWindow() throws Exception {
        Object[] expected = new Object[]{1, 2, 3, 1, 2, 3, 1};
        assertEvaluate("row_number() over(partition by x>2)",
                       contains(expected),
                       Collections.singletonMap(new ColumnIdent("x"), 0),
                       new Object[]{1},
                       new Object[]{2},
                       new Object[]{2},
                       new Object[]{3},
                       new Object[]{4},
                       new Object[]{5},
                       new Object[]{null});
    }

    @Test
    public void testRowNumberOverPartitionedOrderedWindow() throws Exception {
        Object[] expected = new Object[]{1, 2, 3, 1, 2, 3, 1};
        assertEvaluate("row_number() over(partition by x>2 order by x)",
                       contains(expected),
                       Collections.singletonMap(new ColumnIdent("x"), 0),
                       new Object[]{1, 1},
                       new Object[]{2, 2},
                       new Object[]{2, 2},
                       new Object[]{3, 3},
                       new Object[]{4, 4},
                       new Object[]{5, 5},
                       new Object[]{null, null});
    }
}
