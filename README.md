# DSpace Curation Tasks
Metadata curation tasks used on the [CGSpace](https://cgspace.cgiar.org) institutional repository:

- **CountryCodeTagger**: add ISO 3166-1 Alpha2 country codes to items based on their existing country metadata

Tested on DSpace 5.8. Read more about the [DSpace curation system](https://wiki.lyrasis.org/display/DSDOC5x/Curation+System).

## Build and Install
To build the standalone jar:

```
$ mvn package
```

Copy the resulting jar to the DSpace `lib` directory:

```
$ cp target/dspace-curation-tasks-1.0-SNAPSHOT.jar ~/dspace/lib/dspace-curation-tasks-1.0-SNAPSHOT.jar
```

## Invocation
Once the jar is installed and you have added appropriate configuration in `~/dspace/config/modules`:

```
$ ~/dspace/bin/dspace curate -t countrycodetagger -i 10568/3 -r - -l 500 -s object
```

_Note_: it is very important to set the cache limit (`-l`) and the database transaction scope to something sensible (`object`) if you're curating a community or collection with more than a few hundred items.

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
- Check for existence of metadata field before trying to add metadata
- Add tests

## License
This work is licensed under the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).

This repository contains data from the [Debian iso-codes project](https://salsa.debian.org/iso-codes-team/iso-codes) project, which is licensed under the [GNU Lesser General Public License v2.1](https://salsa.debian.org/iso-codes-team/iso-codes/-/blob/main/COPYING).
