# CGSpace Java Helpers [![GitHub Actions](https://github.com/ilri/cgspace-java-helpers/workflows/Build/badge.svg)](https://github.com/ilri/cgspace-java-helpers/actions)
DSpace curation tasks and other Java-based helpers used on the [CGSpace](https://cgspace.cgiar.org) institutional repository:

- **CountryCodeTagger**: add ISO 3166-1 Alpha2 country codes to items based on their existing country metadata
- **FixJpgJpgThumbnails**: fix low-quality ".jpg.jpg" thumbnails by replacing them with their originals
- **FixLowQualityThumbnails**: remove low-quality thumbnails when PDF bitstreams are present

Tested on DSpace 6.3. Read more about the [DSpace curation system](https://wiki.lyrasis.org/display/DSDOC6x/Curation+System).

## Build and Install

### Integrate into DSpace Build
To use these curation tasks in a DSpace project add the following dependency to `dspace/modules/additions/pom.xml`:

```
<dependency>
  <groupId>io.github.ilri.cgspace</groupId>
  <artifactId>cgspace-java-helpers</artifactId>
  <version>6.1-SNAPSHOT</version>
</dependency>
```

The jar will be copied to all DSpace applications.

### Manual Build and Install
To build the standalone jar:

```console
$ mvn package
```

Copy the resulting jar to the DSpace `lib` directory:

```console
$ cp target/cgspace-java-helpers-6.1-SNAPSHOT.jar ~/dspace/lib/
```

## Configuration
Please refer to the appropriate README.md file:

- Curation Tasks: [src/main/java/io/github/ilri/cgspace/ctasks/README.md](https://github.com/ilri/cgspace-java-helpers/blob/dspace6/src/main/java/io/github/ilri/cgspace/ctasks/README.md)
- Scripts: [src/main/java/io/github/ilri/cgspace/scripts/README.md](https://github.com/ilri/cgspace-java-helpers/blob/dspace6/src/main/java/io/github/ilri/cgspace/scripts/README.md)

## TODO

- Add a curation task to normalize DOIs to "https://doi.org" format

## Notes
This project was initially created according to the [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/):

```console
$ mvn -B archetype:generate -DgroupId=io.github.ilri.cgspace -DartifactId=cgspace-java-helpers -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4
```

To deploy a new `-SNAPSHOT` release to Maven Central (make sure OSSHRH credentials are in `~/.m2/settings.xml`):

```console
$ mvn clean deploy
```

See: <a href="https://central.sonatype.org/publish/publish-maven/#performing-a-snapshot-deployment">Performing a Snapshot Deployment</a>

## License
This work is licensed under the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).

This repository contains data from the [Debian iso-codes project](https://salsa.debian.org/iso-codes-team/iso-codes) project, which is licensed under the [GNU Lesser General Public License v2.1](https://salsa.debian.org/iso-codes-team/iso-codes/-/blob/main/COPYING).
