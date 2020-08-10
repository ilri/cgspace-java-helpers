# CGSpace Java Helpers [![Build Status](https://travis-ci.org/ilri/cgspace-java-helpers.svg?branch=dspace5)](https://travis-ci.org/ilri/dspace-curation-tasks)
DSpace curation tasks and other Java-based helpers used on the [CGSpace](https://cgspace.cgiar.org) institutional repository:

- **CountryCodeTagger**: add ISO 3166-1 Alpha2 country codes to items based on their existing country metadata
- **FixJpgJpgThumbnails**: Fix low-quality ".jpg.jpg" thumbnails by replacing them with their originals

Tested on DSpace 5.8. Read more about the [DSpace curation system](https://wiki.lyrasis.org/display/DSDOC5x/Curation+System).

## Build and Install

### Integrate into DSpace Build
To use these curation tasks in a DSpace project add the following dependency to `dspace/modules/additions/pom.xml`:

```
<dependency>
  <groupId>io.github.ilri.cgspace</groupId>
  <artifactId>cgspace-java-helpers</artifactId>
  <version>5.4-SNAPSHOT</version>
</dependency>
```

The jar will be copied to all DSpace applications.

### Manual Build and Install
To build the standalone jar:

```
$ mvn package
```

Copy the resulting jar to the DSpace `lib` directory:

```
$ cp target/cgspace-java-helpers-5.4-SNAPSHOT.jar ~/dspace/lib
```

## Configuration
Please refer to the appropriate README.md file:

- Curation Tasks: [src/main/java/io/github/ilri/cgspace/ctasks/README.md](https://github.com/ilri/cgspace-java-helpers/blob/dspace5/src/main/java/io/github/ilri/cgspace/ctasks/README.md)
- Scripts: [src/main/java/io/github/ilri/cgspace/scripts/README.md](https://github.com/ilri/cgspace-java-helpers/blob/dspace5/src/main/java/io/github/ilri/cgspace/scripts/README.md)

## Notes
This project was initially created according to the [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/):

```console
$ mvn -B archetype:generate -DgroupId=io.github.ilri.cgspace -DartifactId=cgspace-java-helpers -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4
```

## TODO

- Make sure this doesn't work on items in the workflow
- Check for existence of metadata field before trying to add metadata
- Add tests

## License
This work is licensed under the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).

This repository contains data from the [Debian iso-codes project](https://salsa.debian.org/iso-codes-team/iso-codes) project, which is licensed under the [GNU Lesser General Public License v2.1](https://salsa.debian.org/iso-codes-team/iso-codes/-/blob/main/COPYING).
