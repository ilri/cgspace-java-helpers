/*
    DSpace Curation Tasks
    Copyright (C) 2020  Alan Orth

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package org.cgiar.cgspace.ctasks;

import com.google.gson.Gson;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CountryCodeTagger extends AbstractCurationTask
{
    private int status = Curator.CURATE_UNSET;
    private String result = null;

    private static String isocodesJsonPath;
    private static String cgspaceCountriesJsonPath;
    private static String iso3166Field;
    private static String iso3166Alpha2Field;
    private static boolean forceupdate;

    private List<String> results = new ArrayList<String>();

    private static Logger log = Logger.getLogger(CountryCodeTagger.class);

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        // Load configuration
        isocodesJsonPath = "/org/cgiar/cgspace/ctasks/iso_3166-1.json";
        cgspaceCountriesJsonPath = "/org/cgiar/cgspace/ctasks/cgspace-countries.json";
        iso3166Field = taskProperty("iso3166.field");
        iso3166Alpha2Field = taskProperty("iso3166-alpha2.field");
        forceupdate = taskBooleanProperty("forceupdate", false);

		if (dso.getType() == Constants.ITEM)
        {
            Item item = (Item)dso;
            String itemHandle = item.getHandle();

            Metadatum[] itemCountries = item.getMetadataByMetadataString(iso3166Field);

            // skip items that don't have country metadata
            if (itemCountries.length == 0) {
                result = itemHandle + ": no countries, skipping.";
                status = Curator.CURATE_SKIP;
            } else {
                Gson gson = new Gson();

                // TODO: convert to try: https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(isocodesJsonPath)));
                ISO3166CountriesVocabulary isocodesCountriesJson = gson.fromJson(reader, ISO3166CountriesVocabulary.class);
                reader.close();

                reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(cgspaceCountriesJsonPath)));
                CGSpaceCountriesVocabulary cgspaceCountriesJson = gson.fromJson(reader, CGSpaceCountriesVocabulary.class);
                reader.close();

                //System.out.println(itemHandle + ": " + itemCountries.length + " countries possibly need tagging");

                // split the alpha2 country code field into schema, element, and qualifier so we can use it with item.addMetadata()
                String[] iso3166Alpha2FieldParts = iso3166Alpha2Field.split("\\.");

                if (forceupdate) {
                    item.clearMetadata(iso3166Alpha2FieldParts[0], iso3166Alpha2FieldParts[1], iso3166Alpha2FieldParts[2], Item.ANY);
                }

                // check the item's country codes, if any
                Metadatum[] itemAlpha2CountryCodes = item.getMetadataByMetadataString(iso3166Alpha2Field);

                if (itemAlpha2CountryCodes.length == 0) {
                    //System.out.println(itemHandle + ": Should add codes for " + itemCountries.length + " countries.");

                    Integer addedCodeCount = 0;
                    for (Metadatum itemCountry : itemCountries) {
                        //check ISO 3166-1 countries
                        for (CountriesVocabulary.Country country : isocodesCountriesJson.countries) {
                            if (itemCountry.value.equalsIgnoreCase(country.getName()) || itemCountry.value.equalsIgnoreCase(country.get_official_name()) || itemCountry.value.equalsIgnoreCase(country.get_common_name())) {
                                System.out.println(itemHandle + ": adding country code " + country.getAlpha_2());

                                try {
                                    item.addMetadata(iso3166Alpha2FieldParts[0], iso3166Alpha2FieldParts[1], iso3166Alpha2FieldParts[2], "en_US", country.getAlpha_2());
                                    item.update();

                                    addedCodeCount++;

                                    result = itemHandle + ": added " + addedCodeCount + " country code(s)";
                                    status = Curator.CURATE_SUCCESS;
                                } catch (SQLException | AuthorizeException sqle) {
                                    log.debug(sqle.getMessage());
                                    result = itemHandle + ": error";
                                    status = Curator.CURATE_ERROR;
                                }
                            }
                        }
                        //check CGSpace countries
                        for (CountriesVocabulary.Country country : cgspaceCountriesJson.countries) {
                            if (itemCountry.value.equalsIgnoreCase(country.getCgspace_name())) {
                                System.out.println(itemHandle + ": adding country code " + country.getAlpha_2());

                                try {
                                    // we have the field as a string, so we need to split/tokenize it here actually
                                    item.addMetadata(iso3166Alpha2FieldParts[0], iso3166Alpha2FieldParts[1], iso3166Alpha2FieldParts[2], "en_US", country.getAlpha_2());
                                    item.update();

                                    addedCodeCount++;

                                    result = itemHandle + ": added " + addedCodeCount + " country code(s)";
                                    status = Curator.CURATE_SUCCESS;
                                } catch (SQLException | AuthorizeException sqle) {
                                    log.debug(sqle.getMessage());
                                    result = itemHandle + ": error";
                                    status = Curator.CURATE_ERROR;
                                }
                            }
                        }
                    }
                } else {
                    result = itemHandle + ": item has country codes, skipping";
                    status = Curator.CURATE_SKIP;
                }
            }

            setResult(result);
            report(result);
		}

        return status;
    }
}