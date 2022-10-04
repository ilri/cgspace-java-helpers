package io.github.ilri.cgspace.scripts;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.content.service.BundleService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 * @author Alan Orth for the International Livestock Research Institute
 * @version 6.1
 * @since 5.1
 */
public class FixJpgJpgThumbnails {
	//note: static members belong to the class itself, not any one instance
	public static ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	public static HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
	public static BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();

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
						case Constants.COLLECTION:
							process(context, itemService.findByCollection(context, (Collection) parent));
							break;
						case Constants.COMMUNITY:
							List<Collection> collections = ((Community) parent).getCollections();
							for (Collection collection : collections) {
								process(context, itemService.findAllByCollection(context, collection));
							}
							break;
						case Constants.SITE:
							process(context, itemService.findAll(context));
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

	private static void process(Context context, Iterator<Item> items) throws SQLException, IOException, AuthorizeException {
		while (items.hasNext()) {
			Item item = items.next();
			processItem(context, item);
			itemService.update(context, item);
		}
	}

	private static void processItem(Context context, Item item) throws SQLException, AuthorizeException, IOException {
		// Some bitstreams like Infographics are large JPGs and put in the ORIGINAL bundle on purpose so we shouldn't
		// swap them.
		List<MetadataValue> itemTypes = itemService.getMetadataByMetadataString(item, "dcterms.type");
		boolean itemHasInfographic = false;
		for (MetadataValue itemType: itemTypes) {
			if (itemType.getValue().equals("Infographic")) {
				itemHasInfographic = true;
			}
		}

		List<Bundle> thumbnailBundles = item.getBundles("THUMBNAIL");
		for (Bundle thumbnailBundle : thumbnailBundles) {
			List<Bitstream> thumbnailBundleBitstreams = thumbnailBundle.getBitstreams();
			for (Bitstream thumbnailBitstream : thumbnailBundleBitstreams) {
				String thumbnailName = thumbnailBitstream.getName();

				if (thumbnailName.toLowerCase().contains(".jpg.jpg")) {
					List<Bundle> originalBundles = item.getBundles("ORIGINAL");
					for (Bundle originalBundle : originalBundles) {
						List<Bitstream> originalBundleBitstreams = originalBundle.getBitstreams();

						for (Bitstream originalBitstream : originalBundleBitstreams) {
							String originalName = originalBitstream.getName();

							long originalBitstreamBytes = originalBitstream.getSize();

							/*
							- check if the original file name is the same as the thumbnail name minus the extra ".jpg"
							- check if the thumbnail description indicates it was automatically generated
							- check if the item has dc.type Infographic (JPG could be the "real" item!)
							- check if the original bitstream is less than ~100KiB
							    - Note: in my tests there were 4022 items with ".jpg.jpg" thumbnails totaling 394549249
							      bytes for an average of about 98KiB so ~100KiB seems like a good cut off
							*/
							if (
									originalName.equalsIgnoreCase(StringUtils.removeEndIgnoreCase(thumbnailName, ".jpg"))
									&& ("Generated Thumbnail".equals(thumbnailBitstream.getDescription()) || "IM Thumbnail".equals(thumbnailBitstream.getDescription()))
									&& !itemHasInfographic
									&& originalBitstreamBytes < 100000
							) {
								System.out.println(item.getHandle() + ": replacing " + thumbnailName + " with " + originalName);

								//add the original bitstream to the THUMBNAIL bundle
								bundleService.addBitstream(context, thumbnailBundle, originalBitstream);
								//remove the original bitstream from the ORIGINAL bundle
								originalBundle.removeBitstream(originalBitstream);
								//remove the JpgJpg bitstream from the THUMBNAIL bundle
								thumbnailBundle.removeBitstream(thumbnailBitstream);
							}
						}
					}
				}
			}
		}
	}
}
