Arvo - Native Avro serialization in Scala
======================

## Motivation
The current Avro ecosystem miss a library that model the Avro AST. In my opinion it is source of all sort of problems as
all the libraries are based on the Java SDK that lacking extensibility nd type checking.

The Avro4s library, even if providing a quite sensible typeclasses it has some drawbacks as well. It depends on shapeless
even if you don't want to generate the typeclasses and does not provide a common type for the Avro type.

With this library I want to have something very similar to Circe where the core module will provide the AST and typeclasses
definition. An additional module will give you the generic transformation. Will be good also to provide some compilation 
time checking on the Schema.

When the AST will be fully complete we can apply the Avro migration rules to the AST. So given a existing AST and a target 
schema, we may be able to obtain another AST compliant with the target schema.

I would like to have the set of rule expandable, but is it an advanced topic. The first step will be to be able to 
serialize and de-serialialize the Avro AST using binary and Json encoding.

## About this README

The code samples in this README file are checked using [tut](https://github.com/tpolecat/tut).

This means that the `README.md` file is generated from `src/main/tut/README.md`. If you want to make any changes to the README, you should:

1. Edit `src/main/tut/README.md`
2. Run `sbt tut` to regenerate `./README.md`
3. Commit both files to git
