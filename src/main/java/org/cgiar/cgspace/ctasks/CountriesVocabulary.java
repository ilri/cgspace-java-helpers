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

import javax.annotation.Nullable;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class CountriesVocabulary {
    @SerializedName("3166-1") List<Country> countries;

    class Country {
        private String name;            //required
        private String commonName;      //optional
        private String officialName;    //optional
        private String numeric;         //required Hmmmm need to cast this...
        private String alpha2;          //required
        private String alpha3;          //required

        public Country(String name,
                       @Nullable String commonName,
                       @Nullable String officialName,
                       String numeric,
                       String alpha2,
                       String alpha3) {
            this.name = name;
            this.commonName = commonName;
            this.officialName = officialName;
            this.numeric = numeric; // fuuuuu this is a string and we can't cast to Integer because some values are zeropadded like "004"
            this.alpha2 = alpha2;
            this.alpha3 = alpha3;
        }

        public String getName() {
            return name;
        }

        public String getNumeric() {
            return numeric;
        }

        public String getCommonName() {
            return commonName;
        }

        public String getOfficialName() {
            return officialName;
        }

        public String getAlpha2() {
            return alpha2;
        }

        public String getAlpha3() {
            return alpha3;
        }
    }
}
