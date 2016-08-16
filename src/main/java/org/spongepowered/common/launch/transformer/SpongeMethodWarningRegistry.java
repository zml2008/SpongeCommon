/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.launch.transformer;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpongeMethodWarningRegistry {

    protected static List<TransformData> warningMethods = new ArrayList<>();

    public static void registerMethodWarning(Set<String> whitelistedClasses, String method, String warningMessage) {
        warningMethods.add(new TransformData(Sets.newHashSet(whitelistedClasses), method, warningMessage));
    }

    protected static class TransformData {
        protected Set<String> whitelistedClasses;
        protected String owner;
        protected String method;
        protected String warningMessage;

        public TransformData(Set<String> whitelistedClasses, String method, String warningMessage) {
            this.whitelistedClasses = whitelistedClasses;
            this.warningMessage = warningMessage;

            String[] ownerSplit = method.split("\\.");
            this.owner = ownerSplit[0];
            this.method = ownerSplit[1];

            this.whitelistedClasses.add(this.owner);
        }
    }

    private SpongeMethodWarningRegistry() {

    }

}
