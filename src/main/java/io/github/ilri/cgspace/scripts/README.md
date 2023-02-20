# Scripts
Java-based helpers used on the [CGSpace](https://cgspace.cgiar.org) institutional repository:

- **FixJpgJpgThumbnails**: fix low-quality ".jpg.jpg" thumbnails by replacing them with their originals
- **FixLowQualityThumbnails**: remove low-quality thumbnails when PDF bitstreams are present

Tested on DSpace 6.3. Read more about the [DSpace curation system](https://wiki.lyrasis.org/display/DSDOC6x/Curation+System).

## Build and Install

### Integrate into DSpace Build
To use these curation tasks in a DSpace project add the following dependency to `dspace/modules/additions/pom.xml`:

```xml
<dependency>
  <groupId>io.github.ilri.cgspace</groupId>
  <artifactId>cgspace-java-helpers</artifactId>
  <version>6.2</version>
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
$ cp target/cgspace-java-helpers-6.2.jar ~/dspace/lib/
```

## Invocation
The scripts take only one argument, which is a community, collection, or item:

```console
$ dspace dsrun io.github.ilri.cgspace.scripts.FixJpgJpgThumbnails 10568/83389
```
