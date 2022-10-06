/*
 * Copyright (C) 2020 Alan Orth
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.ilri.cgspace.ctasks;

import com.google.gson.Gson;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CountryCodeTagger extends AbstractCurationTask {
    public class CountryCodeTaggerConfig {
        private final String isocodesJsonPath = "/io/github/ilri/cgspace/ctasks/iso_3166-1.json";
        private final String cgspaceCountriesJsonPath =
                "/io/github/ilri/cgspace/ctasks/cgspace-countries.json";
        private final String iso3166Field = taskProperty("iso3166.field");
        private final String iso3166Alpha2Field = taskProperty("iso3166-alpha2.field");
        private final boolean forceupdate = taskBooleanProperty("forceupdate", false);

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
    public int perform(DSpaceObject dso) throws IOException {
        // gotta define this here so we can access it after the if context...
        CountryCodeTaggerResult alpha2Result = new CountryCodeTaggerResult();

        if (dso.getType() == Constants.ITEM) {
            // Load configuration
            CountryCodeTaggerConfig config = new CountryCodeTaggerConfig();

            Item item = (Item) dso;

            try {
                alpha2Result = performAlpha2(item, config);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            setResult(alpha2Result.getResult());
            report(alpha2Result.getResult());
        }

        return alpha2Result.getStatus();
    }

    public CountryCodeTaggerResult performAlpha2(Item item, CountryCodeTaggerConfig config)
            throws IOException, SQLException {
        CountryCodeTaggerResult alpha2Result = new CountryCodeTaggerResult();
        String itemHandle = item.getHandle();

        List<MetadataValue> itemCountries =
                itemService.getMetadataByMetadataString(item, config.iso3166Field);

        // skip items that don't have country metadata
        if (itemCountries.size() == 0) {
            alpha2Result.setResult(itemHandle + ": no countries, skipping.");
            alpha2Result.setStatus(Curator.CURATE_SKIP);
        } else {
            Gson gson = new Gson();

            // TODO: convert to try:
            // https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    this.getClass().getResourceAsStream(config.isocodesJsonPath)));
            ISO3166CountriesVocabulary isocodesCountriesJson =
                    gson.fromJson(reader, ISO3166CountriesVocabulary.class);
            reader.close();

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    this.getClass()
                                            .getResourceAsStream(config.cgspaceCountriesJsonPath)));
            CGSpaceCountriesVocabulary cgspaceCountriesJson =
                    gson.fromJson(reader, CGSpaceCountriesVocabulary.class);
            reader.close();

            // split the alpha2 country code field into schema, element, and qualifier so we can use
            // it with item.addMetadata()
            String[] iso3166Alpha2FieldParts = config.iso3166Alpha2Field.split("\\.");

            if (config.forceupdate) {
                itemService.clearMetadata(
                        Curator.curationContext(),
                        item,
                        iso3166Alpha2FieldParts[0],
                        iso3166Alpha2FieldParts[1],
                        iso3166Alpha2FieldParts[2],
                        Item.ANY);
            }

            // check the item's country codes, if any
            List<MetadataValue> itemAlpha2CountryCodes =
                    itemService.getMetadataByMetadataString(item, config.iso3166Alpha2Field);

            if (itemAlpha2CountryCodes.size() == 0) {
                List<String> newAlpha2Codes = new ArrayList<String>();
                for (MetadataValue itemCountry : itemCountries) {
                    // check ISO 3166-1 countries
                    for (CountriesVocabulary.Country country : isocodesCountriesJson.countries) {
                        if (itemCountry.getValue().equalsIgnoreCase(country.getName())
                                || itemCountry
                                        .getValue()
                                        .equalsIgnoreCase(country.get_official_name())
                                || itemCountry
                                        .getValue()
                                        .equalsIgnoreCase(country.get_common_name())) {
                            newAlpha2Codes.add(country.getAlpha_2());
                        }
                    }

                    // check CGSpace countries
                    for (CountriesVocabulary.Country country : cgspaceCountriesJson.countries) {
                        if (itemCountry.getValue().equalsIgnoreCase(country.getCgspace_name())) {
                            newAlpha2Codes.add(country.getAlpha_2());
                        }
                    }
                }

                if (newAlpha2Codes.size() > 0) {
                    try {
                        itemService.addMetadata(
                                Curator.curationContext(),
                                item,
                                iso3166Alpha2FieldParts[0],
                                iso3166Alpha2FieldParts[1],
                                iso3166Alpha2FieldParts[2],
                                "en_US",
                                newAlpha2Codes);
                        itemService.update(Curator.curationContext(), item);
                    } catch (SQLException | AuthorizeException sqle) {
                        config.log.debug(sqle.getMessage());
                        alpha2Result.setResult(itemHandle + ": error");
                        alpha2Result.setStatus(Curator.CURATE_ERROR);
                    }

                    alpha2Result.setResult(
                            itemHandle
                                    + ": added "
                                    + newAlpha2Codes.size()
                                    + " alpha2 country code(s)");
                } else {
                    alpha2Result.setResult(itemHandle + ": no matching countries found");
                }
                alpha2Result.setStatus(Curator.CURATE_SUCCESS);
            } else {
                alpha2Result.setResult(itemHandle + ": item has country codes, skipping");
                alpha2Result.setStatus(Curator.CURATE_SKIP);
            }
        }

        return alpha2Result;
    }
}
