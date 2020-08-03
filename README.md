# DSpace Curation Tasks [![Build Status](https://travis-ci.org/ilri/dspace-curation-tasks.svg?branch=master)](https://travis-ci.org/ilri/dspace-curation-tasks)
Metadata curation tasks used on the [CGSpace](https://cgspace.cgiar.org) institutional repository:

- **CountryCodeTagger**: add ISO 3166-1 Alpha2 country codes to items based on their existing country metadata

Tested on DSpace 5.8. Read more about the [DSpace curation system](https://wiki.lyrasis.org/display/DSDOC5x/Curation+System).

## Build and Install

### Integrate into DSpace Build
To use these curation tasks in a DSpace project add the following dependency to `dspace/modules/additions/pom.xml`:

```
<dependency>
  <groupId>io.github.ilri.cgspace</groupId>
  <artifactId>dspace-curation-tasks</artifactId>
  <version>1.0-SNAPSHOT</version>
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
$ cp target/dspace-curation-tasks-1.0-SNAPSHOT.jar ~/dspace/lib/dspace-curation-tasks-1.0-SNAPSHOT.jar
```

## Configuration
Add the curation task to DSpace's `config/modules/curate.cfg`:

```
plugin.named.org.dspace.curate.CurationTask = \
...
    io.github.ilri.cgspace.ctasks.CountryCodeTagger = countrycodetagger \
    io.github.ilri.cgspace.ctasks.CountryCodeTagger = countrycodetagger.force
```

And then add a configuration file for the task in `config/modules/countrycodetagger.cfg`:

```
# name of the field containing ISO 3166-1 country names
iso3166.field = cg.coverage.country

# name of the field containing ISO 3166-1 Alpha2 country codes
iso3166-alpha2.field = cg.coverage.iso3166-alpha2

# only add country codes if an item doesn't have any (default false)
#forceupdate = false
```

*Note*: DSpace's curation system supports "profiles" where you can use the same task with different options, for example above I have a normal country code tagger and a "force" variant. To use the "force" variant you create a new configuration file with the overridden options in `config/modules/countrycodetagger.force.cfg`. The "force" profile clears all existing country codes and updates everything.

## Invocation
Once the jar is installed and you have added appropriate configuration in `~/dspace/config/modules`:

```
$ ~/dspace/bin/dspace curate -t countrycodetagger -i 10568/3 -r - -l 500 -s object
```

*Note*: it is very important to set the cache limit (`-l`) and the database transaction scope to something sensible (`object`) if you're curating a community or collection with more than a few hundred items.

## Notes
This project was initially created according to the [Maven Getting Started Guide](https://maven.apache.org/guides/getting-started/):

```console
$ mvn -B archetype:generate -DgroupId=io.github.ilri.cgspace -DartifactId=dspace-curation-tasks -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4
```

## TODO

- Make sure this doesn't work on items in the workflow
- Port to DSpace 6
  - Remember to bump Gson version!
- Check for existence of metadata field before trying to add metadata
- Add tests

## License
This work is licensed under the [GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html).

This repository contains data from the [Debian iso-codes project](https://salsa.debian.org/iso-codes-team/iso-codes) project, which is licensed under the [GNU Lesser General Public License v2.1](https://salsa.debian.org/iso-codes-team/iso-codes/-/blob/main/COPYING).
