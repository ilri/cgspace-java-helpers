/*
* Copyright (C) 2020 Alan Orth
*
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package io.github.ilri.cgspace.ctasks;

import javax.annotation.Nullable;

public class CountriesVocabulary {

    class Country {
        private String name;            //required
        private String common_name;     //optional
        private String official_name;   //optional
        private String cgspace_name;    //optional
        private String numeric;         //required Hmmmm need to cast this...
        private String alpha_2;         //required
        private String alpha_3;         //required

        public Country(String name,
                       @Nullable String common_name,
                       @Nullable String official_name,
                       @Nullable String cgspace_name,
                       String numeric,
                       String alpha_2,
                       String alpha_3) {
            this.name = name;
            this.common_name = common_name;
            this.official_name = official_name;
            this.cgspace_name = cgspace_name;
            this.numeric = numeric; // fuuuuu this is a string and we can't cast to Integer because some values are zeropadded like "004"
            this.alpha_2 = alpha_2;
            this.alpha_3 = alpha_3;
        }

        public String getName() {
            return name;
        }

        public String getNumeric() {
            return numeric;
        }

        public String get_common_name() {
            return common_name;
        }

        public String get_official_name() {
            return official_name;
        }

        public String getAlpha_2() {
            return alpha_2;
        }

        public String getAlpha_3() {
            return alpha_3;
        }

        public String getCgspace_name() {
            return cgspace_name;
        }
    }
}