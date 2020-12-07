# Overview
This program will find the Maven coordinates for all the Java archive
(e.g., jar) files in a specified directory.

# Rationale
I inherited a Java program that was packaged using a custom build utility.
I wanted to move it over to Maven or Gradle, so I could automate the build
using a tool like Jenkins or Travis CI.

This program had over 200 libraries it used, and I did not want to search
Maven Central manually for all of these libraries.

That's when I found [make-pom](https://github.com/sjbotha/make-pom). While
I really loved `make-pom`, I ran into some challenges getting it set up on
my laptop.  That, and I didn't like the fact that it required Bourne shell,
Python 2, and a few other external binaries like `sha1sum`.

I decided to write a program that did much the same thing, but entirely
in Java.  I also introduced caching, external configuration, logging,
output options, and multiple Maven repository searching.

# Algorithm
1. Look for each Java archive (e.g., .jar) file in a specified directory.
2. Calculate the hash value for the given file.
3. Determine if we already have the Maven coordinates for this file.
4. If not, see if the file includes a [pom.properties](https://maven.apache.org/shared/maven-archiver/#pom-properties-content) file.
5. If not, search [Maven Central](http://search.maven.org/) for the given hash value.
6. Store the Maven coordinates in an internal database for future reference.
7. Output the Maven coordinates as requested (e.g., POM, JSON, not at all).

# Configuration
See [`find-dependency.json`](src/main/resources/find-dependency.json).  This program
will look for this file in the class path.

# Usage
```shell script
$ java -jar find-dependency-1.0.jar -d /path/to/archives
```

# Command Line Options
Option | Description | Default
------ | ----------- | -------
-d, --directory | The directory to look for Java archive files. | None. This option is required.
-f, --format | The output file format. One of `JSON` or `POM`. Only valid when used with `-o` or `--output`. | JSON
-o, --output | The output file. | None. If not specified, nothing is output, just loaded in the database.

# Dependencies
This program makes use of features that require Java 11 or newer
(e.g., [HTTP Client](https://openjdk.java.net/groups/net/httpclient/intro.html)).

This program also makes use of the following external libraries.

* [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/index.html)
* [Apache Log4j2](https://logging.apache.org/log4j/2.x/)
* [Google Guava](https://github.com/google/guava)
* [Google Gson](https://github.com/google/gson)
* [H2 Database](http://h2database.com/html/main.html)

# Credits
This program was heavily-inspired by [make-pom](https://github.com/sjbotha/make-pom).
Thanks, @sjbotha!