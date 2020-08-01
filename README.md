# DSpace Curation Tasks
Metadata curation tasks used on the [CGSpace](https://cgspace.cgiar.org) institutional repository:

- CountryCodeTagger: tag items with appropriate country codes based on their existing country metadata

Tested on DSpace 5.8. Read more about the [DSpace curation system](https://wiki.lyrasis.org/display/DSDOC5x/Curation+System).

## Notes
This project was initially created according to the [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/):

```console
$ mvn -B archetype:generate -DgroupId=org.cgiar.cgspace.ctasks -DartifactId=dspace-curation-tasks -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4
```

## Todo

- Integrate with main DSpace build
- Make sure this doesn't work on items in the workflow
- Port to DSpace 6
  - Remember to bump Gson version!

## License
This work is licensed under the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).

This repository contains data from the [Debian iso-codes project](https://salsa.debian.org/iso-codes-team/iso-codes) project, which is licensed under the [GNU Lesser General Public License v2.1](https://salsa.debian.org/iso-codes-team/iso-codes/-/blob/main/COPYING).
