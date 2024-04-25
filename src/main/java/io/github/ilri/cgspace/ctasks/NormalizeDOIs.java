/*
 * Copyright (C) 2024 Alan Orth
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */

package io.github.ilri.cgspace.ctasks;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Suspendable;

import java.io.IOException;
import java.util.List;

/**
 * Attempt to normalize DOIs by stripping whitespace, lower casing, and
 * converting to <code>https://doi.org</code> format. The reason is that DOIs are case
 * insensitive and must be unique, which we can only guarantee if they are
 * normalized to the same format.
 *
 * See: <a href="https://www.crossref.org/documentation/member-setup/constructing-your-dois/">https://www.crossref.org/documentation/member-setup/constructing-your-dois/</a>
 *
 * TODO: set curation to failed if invalid DOI submitted (and configure to reject in workflow)
 * TODO: allow operation on communities and collections (currently only works on items)
 *
 * @author Alan Orth for the International Livestock Research Institute
 * @version 7.6.1.1
 * @since 7.6.1.1
 */
@Suspendable
public class NormalizeDOIs extends AbstractCurationTask {
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso.getType() == Constants.ITEM) {
            Item item = (Item) dso;
            String result;

            // Keep track of whether we change metadata, and how many
            boolean metadataChanged = false;
            int count = 0;

            // Hard coding the metadata field for now since I can't figure out how to read the taskProperty
            List<MetadataValue> itemDOIs = itemService.getMetadataByMetadataString(item, "cg.identifier.doi");

            // skip items that don't have DOIs
            if (itemDOIs.isEmpty()) {
                setResult("No DOIs, skipping");
                return Curator.CURATE_SKIP;
            } else {
                for (MetadataValue itemDOI : itemDOIs) {
                    String newDOI = getNormalizedDOI(itemDOI);

                    // Check if the normalized DOI is different than the original
                    if (!newDOI.equals(itemDOI.getValue())) {
                        itemDOI.setValue(newDOI);
                        metadataChanged = true;
                        count++;
                    }
                }
            }
            if (metadataChanged) {
                result = "Normalized " + count + " DOI(s)";
            } else {
                result = "All DOIs already normalized";
            }
            setResult(result);

            return Curator.CURATE_SUCCESS;
        } else {
            setResult("Object skipped");
            return Curator.CURATE_SKIP;
        }
    }

    private static String getNormalizedDOI(MetadataValue itemDOI) {
        // 1. Convert to lowercase
        String newDOI = itemDOI.getValue().toLowerCase();
        // 2. Strip leading and trailing whitespace
        newDOI = newDOI.strip();
        // 3. Convert to HTTPS
        newDOI = newDOI.replace("http://", "https://");
        // 4. Prefer doi.org to dx.doi.org
        newDOI = newDOI.replace("dx.doi.org", "doi.org");
        // 5. Replace values like doi: 10.11648/j.jps.20140201.14
        newDOI = newDOI.replaceAll("^doi: 10\\.", "https://doi.org/10.");
        // 6. Replace values like 10.3390/foods12010115
        newDOI = newDOI.replaceAll("^10\\.", "https://doi.org/10.");

        return newDOI;
    }
}
