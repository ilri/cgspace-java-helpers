# Scripts
Java-based helpers used on the [CGSpace](https://cgspace.cgiar.org) institutional repository:

- **FixJpgJpgThumbnails**: Fix low-quality ".jpg.jpg" thumbnails by replacing them with their originals

Tested on DSpace 5.8. Read more about the [DSpace curation system](https://wiki.lyrasis.org/display/DSDOC5x/Curation+System).

## Build and Install

### Integrate into DSpace Build
To use these curation tasks in a DSpace project add the following dependency to `dspace/modules/additions/pom.xml`:

```
<dependency>
  <groupId>io.github.ilri.cgspace</groupId>
  <artifactId>cgspace-java-helpers</artifactId>
  <version>5.3</version>
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
$ cp target/cgspace-java-helpers-5.3.jar ~/dspace/lib
```

### Invocation
The script only takes one argument, which is a community, collection, or item:

```
$ dspace dsrun io.github.ilri.cgspace.scripts.FixJpgJpgThumbnails 10568/83389
```
