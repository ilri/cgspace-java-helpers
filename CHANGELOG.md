# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased
### Updated
- Update dependencies in `pom.xml`

### Changed
- Java compiler and target from JDK 7 to JDK 8

### Added
- New `FixLowQualityThumbnails` script to detect and remove more low-quality thumbnails

### Fixed
- `FixJpgJpgThumbnails` and `FixLowQualityThumbnails` scripts not commiting changes when operating on a site, community, or collection
