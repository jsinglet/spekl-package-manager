# The Spekl Package Manager

The number of systems for specifying program behavior grows every year; while significant enhancements are made in these systems, both in terms of their capability and usability, little progress has been made in the way of making the act of specification authoring and usage easier. 

Spekl is a system designed to make the specification authoring lifecycle easier. 

## Installation

Download an installer from over on the [releases](https://github.com/jsinglet/spekl-package-manager/releases) page. 


## License

Copyright (c) 2015, John L. Singleton <jls@cs.ucf.edu>

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.


## Some Commands for Spekl Developers

# Build the Classpath

     $ ls -m target\deps  | tr ',' ';' | tr '\\n' ' ' | tr -d ' ' > target/deps-classpath.txt

# Get Ready For Packing

     $ lein with-profile dist jar
