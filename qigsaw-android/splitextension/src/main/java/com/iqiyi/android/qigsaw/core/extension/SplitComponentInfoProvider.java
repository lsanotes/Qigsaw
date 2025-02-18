/*
 * MIT License
 *
 * Copyright (c) 2019-present, iQIYI, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.iqiyi.android.qigsaw.core.extension;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SplitComponentInfoProvider {

    private final Set<String> splitNames;

    SplitComponentInfoProvider(@NonNull Set<String> splitNames) {
        this.splitNames = splitNames;
    }

    /**
     * Gets split's application name by split name.
     * Qigsaw-Gradle-Plugin would write split application name in Class ComponentInfo.
     *
     * @param splitName name of split.
     * @return application name of split.
     */
    String getSplitApplicationName(String splitName) {
        return ComponentInfoManager.getSplitApplication(splitName);
    }

    @NonNull
    List<String> getSplitActivities() {
        List<String> activities = new ArrayList<>();
        for (String splitName : splitNames) {
            String[] result = ComponentInfoManager.getSplitActivities(splitName);
            if (result != null && result.length > 0) {
                activities.addAll(Arrays.asList(result));
            }
        }
        return activities;
    }

    @NonNull
    List<String> getSplitServices() {
        List<String> services = new ArrayList<>();
        for (String splitName : splitNames) {
            String[] result = ComponentInfoManager.getSplitServices(splitName);
            if (result != null && result.length > 0) {
                services.addAll(Arrays.asList(result));
            }
        }
        return services;
    }

    @NonNull
    List<String> getSplitReceivers() {
        List<String> receivers = new ArrayList<>();
        for (String splitName : splitNames) {
            String[] result = ComponentInfoManager.getSplitReceivers(splitName);
            if (result != null && result.length > 0) {
                receivers.addAll(Arrays.asList(result));
            }
        }
        return receivers;
    }

    /**
     * Gets all splits' provider names
     * Qigsaw-Gradle-Plugin would write split provider names in Class ComponentInfo
     *
     * @return a map of provider names.
     */
    @NonNull
    Map<String, List<String>> getSplitProviders() {
        Map<String, List<String>> providerMap = new HashMap<>();
        for (String splitName : splitNames) {
            String[] result = ComponentInfoManager.getSplitProviders(splitName);
            if (result != null && result.length > 0) {
                List<String> providers = new ArrayList<>(Arrays.asList(result));
                providerMap.put(splitName, providers);
            }
        }
        return providerMap;
    }

}
