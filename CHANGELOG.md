# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [7.6] - 2024-01-02
### Updated
- `iso_3166-1.json` from iso-codes 4.13.0-SNAPSHOT, which [adds common names for Iran, Laos, and Syria](https://salsa.debian.org/iso-codes-team/iso-codes/-/merge_requests/32)
- DSpace 7.6 compatibility

## [6.2] - 2023-02-20
### Updated
- `iso_3166-1.json` from iso-codes 4.12.0, which updates the name for TR to "Türkiye"

## [6.1] - 2022-10-31
### Updated
- Update dependencies in `pom.xml`
- `iso_3166-1.json` from iso-codes 4.11.0

### Changed
- Java compiler and target from JDK 7 to JDK 8

### Added
- New `FixLowQualityThumbnails` script to detect and remove more low-quality thumbnails

### Fixed
- `FixJpgJpgThumbnails` and `FixLowQualityThumbnails` scripts not commiting changes when operating on a site, community, or collection
