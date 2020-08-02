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
    public class CountryCodeTaggerConfig {
        private final String isocodesJsonPath = "/org/cgiar/cgspace/ctasks/iso_3166-1.json";
        private final String cgspaceCountriesJsonPath = "/org/cgiar/cgspace/ctasks/cgspace-countries.json";
        private final String iso3166Field = taskProperty("iso3166.field");
        private final String iso3166Alpha2Field = taskProperty("iso3166-alpha2.field");
        private final boolean forceupdate = taskBooleanProperty("forceupdate", false);

        private List<String> results = new ArrayList<String>();

        private Logger log = Logger.getLogger(CountryCodeTagger.class);
    }

    public class CountryCodeTaggerResult {
        private int status = Curator.CURATE_UNSET;
        private String result = null;

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        // gotta define this here so we can access it after the if context...
        CountryCodeTaggerResult alpha2Result = new CountryCodeTaggerResult();

		if (dso.getType() == Constants.ITEM)
        {
            // Load configuration
            CountryCodeTaggerConfig config = new CountryCodeTaggerConfig();

            Item item = (Item)dso;

            alpha2Result = performAlpha2(item, config);

            setResult(alpha2Result.getResult());
            report(alpha2Result.getResult());
		}

		return alpha2Result.getStatus();
    }

    public CountryCodeTaggerResult performAlpha2(Item item, CountryCodeTaggerConfig config) throws IOException
    {
        CountryCodeTaggerResult alpha2Result = new CountryCodeTaggerResult();
        String itemHandle = item.getHandle();

        Metadatum[] itemCountries = item.getMetadataByMetadataString(config.iso3166Field);

        // skip items that don't have country metadata
        if (itemCountries.length == 0) {
            alpha2Result.setResult(itemHandle + ": no countries, skipping.");
            alpha2Result.setStatus(Curator.CURATE_SKIP);
        } else {
            Gson gson = new Gson();

            // TODO: convert to try: https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(config.isocodesJsonPath)));
            ISO3166CountriesVocabulary isocodesCountriesJson = gson.fromJson(reader, ISO3166CountriesVocabulary.class);
            reader.close();

            reader = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream(config.cgspaceCountriesJsonPath)));
            CGSpaceCountriesVocabulary cgspaceCountriesJson = gson.fromJson(reader, CGSpaceCountriesVocabulary.class);
            reader.close();

            // split the alpha2 country code field into schema, element, and qualifier so we can use it with item.addMetadata()
            String[] iso3166Alpha2FieldParts = config.iso3166Alpha2Field.split("\\.");

            if (config.forceupdate) {
                item.clearMetadata(iso3166Alpha2FieldParts[0], iso3166Alpha2FieldParts[1], iso3166Alpha2FieldParts[2], Item.ANY);
            }

            // check the item's country codes, if any
            Metadatum[] itemAlpha2CountryCodes = item.getMetadataByMetadataString(config.iso3166Alpha2Field);

            if (itemAlpha2CountryCodes.length == 0) {
                int addedCodeCount = 0;
                for (Metadatum itemCountry : itemCountries) {
                    //check ISO 3166-1 countries
                    for (CountriesVocabulary.Country country : isocodesCountriesJson.countries) {
                        if (itemCountry.value.equalsIgnoreCase(country.getName()) || itemCountry.value.equalsIgnoreCase(country.get_official_name()) || itemCountry.value.equalsIgnoreCase(country.get_common_name())) {
                            try {
                                item.addMetadata(iso3166Alpha2FieldParts[0], iso3166Alpha2FieldParts[1], iso3166Alpha2FieldParts[2], "en_US", country.getAlpha_2());
                                item.update();

                                addedCodeCount++;

                                alpha2Result.setResult(itemHandle + ": added " + addedCodeCount + " country code(s)");
                                alpha2Result.setStatus(Curator.CURATE_SUCCESS);
                            } catch (SQLException | AuthorizeException sqle) {
                                config.log.debug(sqle.getMessage());
                                alpha2Result.setResult(itemHandle + ": error");
                                alpha2Result.setStatus(Curator.CURATE_ERROR);
                            }
                        }
                    }
                    //check CGSpace countries
                    for (CountriesVocabulary.Country country : cgspaceCountriesJson.countries) {
                        if (itemCountry.value.equalsIgnoreCase(country.getCgspace_name())) {
                            try {
                                // we have the field as a string, so we need to split/tokenize it here actually
                                item.addMetadata(iso3166Alpha2FieldParts[0], iso3166Alpha2FieldParts[1], iso3166Alpha2FieldParts[2], "en_US", country.getAlpha_2());
                                item.update();

                                addedCodeCount++;

                                alpha2Result.setResult(itemHandle + ": added " + addedCodeCount + " country code(s)");
                                alpha2Result.setStatus(Curator.CURATE_SUCCESS);
                            } catch (SQLException | AuthorizeException sqle) {
                                config.log.debug(sqle.getMessage());
                                alpha2Result.setResult(itemHandle + ": error");
                                alpha2Result.setStatus(Curator.CURATE_ERROR);
                            }
                        }
                    }
                }
            } else {
                alpha2Result.setResult(itemHandle + ": item has country codes, skipping");
                alpha2Result.setStatus(Curator.CURATE_SKIP);
            }
        }

        return alpha2Result;
    }
}