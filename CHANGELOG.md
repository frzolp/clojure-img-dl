# Change Log

## [0.2.5]
### Added
- Imgur galleries are now supported.
- Imgur GIFs can be saved as MP4s with the `-mp4` command line option.

### Changed
- Restructured `clojure-img-dl.dl.imgur`. Some string operations were made
generic and broken out into separate functions.

## [0.2.0]
### Added
- Support for Vidble albums with `clojure-img-dl.dl.vidble`. Since Vidble has
no public API, this functionality could break if they change their page layout.

### Changed
- Broke out some shared functionality from `clojure-img-dl.dl.imgur` and placed
it in `clojure-img-dl.util.download`.
- Miscellaneous tweaks and cleanup.

## [0.1.1] - 2016-05-29
### Changed
- Made image indexes variable length in `clojure-img-dl.dl.imgur/get-file-name`.
- Displays the length of time taken by the save operation.

## [0.1.0] - 2016-04-26
Initial Release

[Unreleased]: https://github.com/frzolp/clojure-img-dl/compare/0.1.1...HEAD
[0.2.5]: https://github.com/frzolp/clojure-img-dl/compare/0.2.0...0.2.5
[0.2.0]: https://github.com/frzolp/clojure-img-dl/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/frzolp/clojure-img-dl/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/frzolp/clojure-img-dl/releases/tag/0.1.0
