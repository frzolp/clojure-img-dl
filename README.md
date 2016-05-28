# clojure-img-dl

A multi-threaded Imgur album downloader written in Clojure.

## Installation

### From release

Pre-compiled JAR files are available at https://github.com/frzolp/clojure-img-dl/releases

### From source

You will need [Leiningen](http://leiningen.org/), at least version 2.6.1 to compile the source.

Clone the repository, then sign up for an API key at https://api.imgur.com

When you have your API key, save it to the file resources/apikey.properties:

    apikey=(your key here)

In the repository root, build the JAR with

    $ lein uberjar

The generated JAR will be located in the target/uberjar directory.

## Usage

    $ java -jar clojure-imgur-0.1.0-standalone.jar [album_url [album_url ...]]

Albums will be saved to the current working directory with the name

    Imgur_(album title)

If an album title is not present, the album is saved as

    Imgur_(album ID)

Album titles are trimmed to a length of 30 characters with invalid path characters removed.
Images are saved within the folders with an index with leading zeroes, followed by the
image title.

    Imgur_(album)/(index)_(image title).(ext)

The image description is used when a title is missing.

    Imgur_(album)/(index)_(image descr).(ext)

Finally, if there is no description, the image ID is used.

    Imgur_(album)/(index)_(image ID).(ext)

## Examples

### One album

    $ java -jar clojure-imgur-0.1.0-standalone.jar http://imgur.com/a/KSz6k

Saves images to

    Imgur_World's oldest examples of ord/001_Oldest socks.png
    Imgur_World's oldest examples of ord/002_Oldest written recipe.png
    ...

### Two albums

    $ java -jar clojure-imgur-0.1.0-standalone.jar \
                http://imgur.com/a/3wkhb \
                http://imgur.com/a/XXeq7

Saves images to

    Imgur_Space Wallpapers/001_CrHYfTG.jpg
    Imgur_Space Wallpapers/002_gQp3VSW.jpg
    ...
    Imgur_Rather large wallpaper dump/001_xAEryhE.jpg
    Imgur_Rather large wallpaper dump/002_zgwBXjW.jpg
    ...

## License

Copyright © 2016 Francis Zolp

Distributed under the GNU General Public License.
