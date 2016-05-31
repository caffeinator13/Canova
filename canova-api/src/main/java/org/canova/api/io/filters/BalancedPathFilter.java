/*
 *
 *  *
 *  *  * Copyright 2016 Skymind,Inc.
 *  *  *
 *  *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *    you may not use this file except in compliance with the License.
 *  *  *    You may obtain a copy of the License at
 *  *  *
 *  *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  *    Unless required by applicable law or agreed to in writing, software
 *  *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *    See the License for the specific language governing permissions and
 *  *  *    limitations under the License.
 *  *
 *
 */
package org.canova.api.io.filters;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.canova.api.io.labels.PathLabelGenerator;
import org.canova.api.writable.Writable;

/**
 * Randomizes the order of paths in an array and removes paths randomly
 * to have the same number of paths for each label. Further interlaces the paths
 * on output based on their labels, to obtain easily optimal batches for training.
 *
 * @author saudet
 */
public class BalancedPathFilter extends RandomPathFilter {

    protected PathLabelGenerator labelGenerator;
    protected int maxLabels = 0, maxPathsPerLabel = 0;

    /** Calls {@code this(random, extensions, labelGenerator, 0, 0, 0)}. */
    public BalancedPathFilter(Random random, String[] extensions, PathLabelGenerator labelGenerator) {
        this(random, extensions, labelGenerator, 0, 0, 0);
    }

    /**
     * Constructs an instance of the PathFilter.
     *
     * @param random           object to use
     * @param extensions       of files to keep
     * @param labelGenerator   to obtain labels from paths
     * @param maxPaths         max number of paths to return (0 == unlimited)
     * @param maxLabels        max number of labels to return (0 == unlimited)
     * @param maxPathsPerLabel max number of paths per labels to return (0 == unlimited)
     */
    public BalancedPathFilter(Random random, String[] extensions, PathLabelGenerator labelGenerator,
            int maxPaths, int maxLabels, int maxPathsPerLabel) {
        super(random, extensions, maxPaths);
        this.labelGenerator = labelGenerator;
        this.maxLabels = maxLabels;
        this.maxPathsPerLabel = maxPathsPerLabel;
    }

    @Override
    public URI[] filter(URI[] paths) {
        paths = super.filter(paths);

        Map<Writable, List<URI>> labelPaths  = new LinkedHashMap<Writable, List<URI>>();
        for (int i = 0; i < paths.length; i++) {
            URI path = paths[i];
            Writable label = labelGenerator.getLabelForPath(path);
            List<URI> pathList = labelPaths.get(label);
            if (pathList == null) {
                if (maxLabels > 0 && labelPaths.size() >= maxLabels) {
                    continue;
                }
                labelPaths.put(label, pathList = new ArrayList<URI>());
            }
            pathList.add(path);
        }

        int minCount = Integer.MAX_VALUE;
        for (List<URI> pathList : labelPaths.values()) {
            if (minCount > pathList.size()) {
                minCount = pathList.size();
            }
        }
        if (maxPathsPerLabel > 0 && minCount > maxPathsPerLabel) {
            minCount = maxPathsPerLabel;
        }

        ArrayList<URI> newpaths = new ArrayList<URI>();
        for (int i = 0; i < minCount; i++) {
            for (List<URI> p : labelPaths.values()) {
                newpaths.add(p.get(i));
            }
        }
        return newpaths.toArray(new URI[newpaths.size()]);
    }
}
