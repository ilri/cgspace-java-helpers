/*
* Copyright (C) 2020 Alan Orth
*
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package io.github.ilri.cgspace.ctasks;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ISO3166CountriesVocabulary extends CountriesVocabulary {
    // support reading iso_3166-1.json from Debian's iso-codes package using SerializedName since our class needs to match the JSON exactly
    @SerializedName("3166-1") List<Country> countries;
}