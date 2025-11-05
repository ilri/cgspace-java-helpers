/*
 * Copyright (C) 2025 Alan Orth
 *
 * SPDX-License-Identifier: GPL-3.0-only
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
import org.dspace.content.MetadataValue;
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
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 * @author Alan Orth for the International Livestock Research Institute
 * @version 8.2.0
 * @since 8.2.0
 */
public class RemoveGeneratedThumbnails {
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
        List<Bundle> thumbnailBundles = item.getBundles("THUMBNAIL");
        for (Bundle thumbnailBundle : thumbnailBundles) {
            List<Bitstream> thumbnailBundleBitstreams = thumbnailBundle.getBitstreams();
            for (Bitstream thumbnailBitstream : thumbnailBundleBitstreams) {
                String thumbnailName = thumbnailBitstream.getName();
                String thumbnailDescription = thumbnailBitstream.getDescription();

                // There is no point continuing if the thumbnail's description is empty or null
                if (StringUtils.isEmpty(thumbnailDescription)) {
                    continue;
                }

                if (thumbnailName.toLowerCase().endsWith(".pdf.jpg")) {
                    List<Bundle> originalBundles = item.getBundles("ORIGINAL");
                    for (Bundle originalBundle : originalBundles) {
                        List<Bitstream> originalBundleBitstreams = originalBundle.getBitstreams();

                        for (Bitstream originalBitstream : originalBundleBitstreams) {
                            String originalName = originalBitstream.getName();

                            /*
                            - check if the original file name is the same as the thumbnail name minus the extra ".jpg"
                            - check if the thumbnail description indicates it was automatically generated
                            */
                            if (originalName.equalsIgnoreCase(
                                            StringUtils.removeEndIgnoreCase(thumbnailName, ".jpg"))
                                    && ("Generated Thumbnail".equals(thumbnailDescription)
                                            || "IM Thumbnail".equals(thumbnailDescription))) {
                                System.out.println(
                                        item.getHandle()
                                                + ": removing "
                                                + thumbnailName);

                                thumbnailBundle.removeBitstream(thumbnailBitstream);
                            }
                        }
                    }
                }
            }
        }
    }
}
