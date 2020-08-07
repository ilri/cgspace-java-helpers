# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.3] - 2020-08-07
### Changed
- Make sure `FixJpgJpgThumbnails` only replaces thumbnails where the original is less than ~100KiB
- Make sure `FixJpgJpgThumbnails` only replaces thumbnails if the item type is not `Infographic` (because the JPG in the ORIGINAL bundle is the "real" file and it's OK that the thumbnail is ".jpg.jpg")

## [5.2] - 2020-08-06
### Changed
- Make `FixJpgJpgThumbnails` helper check for files named "JPG" as well as "jpg" (case insensitive)
- Make `FixJpgJpgThumbnails` helper replace thumbnails with description `IM Thumbnail` as well as `Generated Thumbnail`

## [5.1] - 2020-08-06
### Added
- Add `FixJpgJpgThumbnails` helper to replace ".jpg.jpg" thumbnails with their originals
