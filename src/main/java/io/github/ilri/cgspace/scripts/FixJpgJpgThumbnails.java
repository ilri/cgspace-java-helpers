package io.github.ilri.cgspace.scripts;

import org.apache.commons.lang.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 * @author Alan Orth for the International Livestock Research Institute
 * @version 5.3
 * @since 5.1
 */
public class FixJpgJpgThumbnails {

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
				process(context, Item.findAll(context));
			} else {
				DSpaceObject parent = HandleManager.resolveToObject(context, parentHandle);
				if (parent != null) {
					switch (parent.getType()) {
						case Constants.COLLECTION:
							process(context, ((Collection) parent).getAllItems()); // getAllItems because we want to work on non-archived ones as well
							break;
						case Constants.COMMUNITY:
							Collection[] collections = ((Community) parent).getCollections();
							for (Collection collection : collections) {
								process(context, collection.getAllItems()); // getAllItems because we want to work on non-archived ones as well
							}
							break;
						case Constants.SITE:
							process(context, Item.findAll(context));
							break;
						case Constants.ITEM:
							processItem((Item) parent);
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

	private static void process(Context context, ItemIterator items) throws SQLException, IOException, AuthorizeException {
		while (items.hasNext()) {
			Item item = items.next();
			processItem(item);
			context.commit();
			item.decache();
		}
	}

	private static void processItem(Item item) throws SQLException, AuthorizeException, IOException {
		// Some bitstreams like Infographics are large JPGs and put in the ORIGINAL bundle on purpose so we shouldn't
		// swap them.
		Metadatum[] itemTypes = item.getMetadataByMetadataString("dc.type");
		Boolean itemHasInfographic = false;
		for (Metadatum itemType: itemTypes) {
			if (itemType.value.equals("Infographic")) {
				itemHasInfographic = true;
			}
		}

		Bundle[] thumbnailBundles = item.getBundles("THUMBNAIL");
		for (Bundle thumbnailBundle : thumbnailBundles) {
			Bitstream[] thumbnailBundleBitstreams = thumbnailBundle.getBitstreams();
			for (Bitstream thumbnailBitstream : thumbnailBundleBitstreams) {
				String thumbnailName = thumbnailBitstream.getName();

				if (thumbnailName.toLowerCase().contains(".jpg.jpg")) {
					Bundle[] originalBundles = item.getBundles("ORIGINAL");
					for (Bundle originalBundle : originalBundles) {
						Bitstream[] originalBundleBitstreams = originalBundle.getBitstreams();

						for (Bitstream originalBitstream : originalBundleBitstreams) {
							String originalName = originalBitstream.getName();

							Long originalBitstreamBytes = originalBitstream.getSize();

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
								thumbnailBundle.addBitstream(originalBitstream);
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
