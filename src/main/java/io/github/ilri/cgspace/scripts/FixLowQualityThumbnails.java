/*
 * Copyright (C) 2022 Alan Orth
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.ilri.cgspace.scripts;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Fix low-quality thumbnails in a DSpace repository.
 *
 * <p>Search the DSpace repository for items containing bitstreams matching the following criteria:
 *
 * <ul>
 *   <li>If an item has an <code>IM Thumbnail</code> and a <code>Generated Thumbnail</code> in the
 *       <code>THUMBNAIL</code> bundle, remove the <code>Generated Thumbnail</code>.
 *   <li>If an item has a PDF bitstream and a JPEG bitstream with description "thumbnail" in the
 *       <code>ORIGINAL</code> bundle, remove the "thumbnail" bitstream in the ORIGINAL bundle.
 * </ul>
 *
 * <p>The general idea is that we should always prefer thumbnails generated from PDFs by ImageMagick
 * to manually uploaded JPEGs because ImageMagick Thumbnails can be regenerated with higher quality,
 * resolution, etc. Furthermore, if there are JPEG bitstreams in the ORIGINAL bundle DSpace will
 * automatically create ".jpg.jpg" thumbnails from them in the THUMBNAIL bundle so we should remove
 * those as well!
 *
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 * @author Alan Orth for the International Livestock Research Institute
 * @version 6.1
 * @since 6.1
 * @see FixJpgJpgThumbnails
 */
public class FixLowQualityThumbnails {
    // note: static members belong to the class itself, not any one instance
    public static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    public static HandleService handleService =
            HandleServiceFactory.getInstance().getHandleService();
    public static BundleService bundleService =
            ContentServiceFactory.getInstance().getBundleService();

    public static void main(String[] args) {
        String parentHandle = null;
        if (args.length >= 1) {
            parentHandle = args[0];
        }

        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();

            if (StringUtils.isBlank(parentHandle)) {
                process(context, itemService.findAll(context));
            } else {
                DSpaceObject parent = handleService.resolveToObject(context, parentHandle);
                if (parent != null) {
                    switch (parent.getType()) {
                        case Constants.SITE:
                            process(context, itemService.findAll(context));
                            context.commit();
                            break;
                        case Constants.COMMUNITY:
                            List<Collection> collections = ((Community) parent).getCollections();
                            for (Collection collection : collections) {
                                process(
                                        context,
                                        itemService.findAllByCollection(context, collection));
                            }
                            context.commit();
                            break;
                        case Constants.COLLECTION:
                            process(
                                    context,
                                    itemService.findByCollection(context, (Collection) parent));
                            context.commit();
                            break;
                        case Constants.ITEM:
                            processItem(context, (Item) parent);
                            context.commit();
                            break;
                    }
                }
            }
        } catch (SQLException | AuthorizeException | IOException e) {
            e.printStackTrace(System.err);
        } finally {
            if (context != null && context.isValid()) {
                context.abort();
            }
        }
    }

    private static void process(Context context, Iterator<Item> items)
            throws SQLException, IOException, AuthorizeException {
        while (items.hasNext()) {
            Item item = items.next();
            processItem(context, item);
            itemService.update(context, item);
        }
    }

    private static void processItem(Context context, Item item)
            throws SQLException, AuthorizeException, IOException {
        // Set some state for the item before we iterate over the THUMBNAIL bundle
        boolean itemHasImThumbnail = false;

        // Iterate over the THUMBNAIL bundle to first identify if this item has an "IM Thumbnail"
        List<Bundle> thumbnailBundles = item.getBundles("THUMBNAIL");
        for (Bundle thumbnailBundle : thumbnailBundles) {
            List<Bitstream> thumbnailBundleBitstreams = thumbnailBundle.getBitstreams();
            for (Bitstream thumbnailBitstream : thumbnailBundleBitstreams) {
                String thumbnailDescription = thumbnailBitstream.getDescription();

                if (StringUtils.isEmpty(thumbnailDescription)) {
                    continue;
                }

                // Check if this item has a bitstream in the THUMBNAIL bundle with description "IM
                // Thumbnail", but only if we haven't already seen one in another iteration for this
                // bundle.
                if (!itemHasImThumbnail && "IM Thumbnail".equals(thumbnailDescription)) {
                    itemHasImThumbnail = true;
                }
            }

            // If this item has an IM Thumbnail we can be reasonably sure that there is a PDF
            // in the ORIGINAL bundle and we don't need any other thumbnails.
            if (itemHasImThumbnail) {
                // Iterate over the bitstreams in the THUMBNAIL bundle again.
                for (Bitstream thumbnailBitstream : thumbnailBundleBitstreams) {
                    String thumbnailName = thumbnailBitstream.getName();
                    String thumbnailDescription = thumbnailBitstream.getDescription();

                    if (StringUtils.isEmpty(thumbnailDescription)) {
                        continue;
                    }

                    // If this item has a "Generated Thumbnail" we can remove it, because those
                    // typically come from other JPEGs in the ORIGINAL bundle and we would prefer
                    // the IM Thumbnail generated from a PDF anyway. The DSpace-generated descri-
                    // ption will *always* be "Generated Thumbnail".
                    if ("Generated Thumbnail".equals(thumbnailDescription)) {
                        System.out.print("\u001b[33m");
                        System.out.println("Deleting (" + item.getHandle() + "):");
                        System.out.println("> Name: »" + thumbnailName + "«");
                        System.out.println("> Description: »" + thumbnailDescription + "«");
                        System.out.print("\u001b[0m");

                        // Remove the "Generated Thumbnail" bitstream from the THUMBNAIL bundle
                        thumbnailBundle.removeBitstream(thumbnailBitstream);

                        // If this item has a bitstream with the word "thumbnail" in it then we can
                        // remove it because we already know this item has an IM Thumbnail and we
                        // prefer that one.
                    } else if (thumbnailDescription.toLowerCase().contains("thumbnail")
                            && !"IM Thumbnail".equals(thumbnailDescription)) {
                        System.out.print("\u001b[33m");
                        System.out.println("Deleting (" + item.getHandle() + "):");
                        System.out.println("> Name: »" + thumbnailName + "«");
                        System.out.println("> Description: »" + thumbnailDescription + "«");
                        System.out.print("\u001b[0m");

                        // Remove the "thumbnail" bitstream from the THUMBNAIL bundle
                        thumbnailBundle.removeBitstream(thumbnailBitstream);

                        // Otherwise skip it because it might be something uploaded manually, like
                        // a thumbnail for a journal or a limited access item.
                    } else {
                        System.out.print("\u001b[34m");
                        System.out.println("Skipping (" + item.getHandle() + "):");
                        System.out.println("> Name: »" + thumbnailName + "«");
                        System.out.println("> Description: »" + thumbnailDescription + "«");
                        System.out.print("\u001b[0m");
                    }

                    // Print a blank line
                    System.out.println();
                }
            }
        }

        // Set some state before we iterate over the ORIGINAL bundle
        boolean itemHasOriginalPdfBitstream = false;
        boolean itemHasOriginalJpegBitstream = false;

        // Iterate over the ORIGINAL bundle to delete manually-uploaded JPEG
        // bitstreams labeled "Thumbnail" whenever we have a PDF because they
        // don't belong in the ORIGINAL bundle and DSpace will automatically
        // create a better thumbnail from the PDF anyway.
        List<Bundle> originalBundles = item.getBundles("ORIGINAL");
        for (Bundle originalBundle : originalBundles) {
            List<Bitstream> originalBundleBitstreams = originalBundle.getBitstreams();
            for (Bitstream originalBitstream : originalBundleBitstreams) {
                String originalFormat = originalBitstream.getFormat(context).getMIMEType();

                // Check if this item has a PDF bitstream in the ORIGINAL bundle,
                // but only if we haven't already seen one in another iteration
                // for this bundle. DSpace will return "format application/pdf"
                // for the MIME type.
                if (!itemHasOriginalPdfBitstream && originalFormat.contains("application/pdf")) {
                    itemHasOriginalPdfBitstream = true;
                }

                // Check if this item has a JPEG bitstream in the ORIGINAL bundle,
                // but only if we haven't already seen one in another iteration
                // for this bundle. DSpace will return "format image/jpeg" for
                // the MIME type.
                if (!itemHasOriginalJpegBitstream && originalFormat.contains("image/jpeg")) {
                    itemHasOriginalJpegBitstream = true;
                }
            }

            // Check if we found a PDF *and* a JPEG in this item's ORIGINAL
            // bundle.
            if (itemHasOriginalPdfBitstream && itemHasOriginalJpegBitstream) {
                // Yes! Now iterate over the bitstreams in the ORIGINAL bundle
                // again to see if the JPEG is a manually uploaded "Thumbnail"
                for (Bitstream originalBitstream : originalBundleBitstreams) {
                    String originalName = originalBitstream.getName();
                    String originalDescription = originalBitstream.getDescription();
                    String originalFormat = originalBitstream.getFormat(context).getMIMEType();

                    if (StringUtils.isEmpty(originalDescription)) {
                        continue;
                    }

                    /*
                    - check if the bitstream is a JPEG based on its MIME Type
                    - check if the bitstream's name or description is "Thumbnail"
                    */
                    if (originalFormat.toLowerCase().contains("image/jpeg")
                            && (originalName.toLowerCase().contains("thumbnail")
                                    || originalDescription.toLowerCase().contains("thumbnail"))) {
                        System.out.print("\u001b[33m");
                        System.out.println("Removing (" + item.getHandle() + "):");
                        System.out.println("> Name: »" + originalName + "«");
                        System.out.println("> Description: »" + originalDescription + "«");
                        System.out.print("\u001b[0m");

                        // Remove the original bitstream from the ORIGINAL bundle
                        originalBundle.removeBitstream(originalBitstream);

                    } else {
                        System.out.print("\u001b[34m");
                        System.out.println("Skipping (" + item.getHandle() + "):");
                        System.out.println("> Name: »" + originalName + "«");
                        System.out.println("> Description: »" + originalDescription + "«");
                        System.out.print("\u001b[0m");
                    }

                    // Print a blank line
                    System.out.println();
                }
            }
        }
    }
}
