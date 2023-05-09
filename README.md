# Candidate SATD Extractor

The goal of this project is to create a dataset containing potential Self-Admitted Technical Debt (
SATD) by extracting Java source-code comments from commit diffs of open-source
java projects.

This project extracts commit diffs as hunks from the "main" branch of every project
in the SmartSHARK
database version 2.2, saves them in an intermediate MongoDB database, and then processes these hunks
to extract source code comments from them that were added in a commit and were not filtered out
using the 5 heuristic filters
discussed by Maldonado et al. (2017). These filters remove comments that are very unlikely to be
Self-Admitted Technical Debt (SATD). The non-filtered comments are then stored in a separate comment
collection in the intermediate database.

## Requirements

- your own (locally or remotely) hosted SmartSHARK database version 2.2 (available
  at https://smartshark.github.io/dbreleases/)
- your own (locally or remotely) hosted intermediate MongoDB database
- Ability to build and run a Maven project
- application.properties config file in the root of the project (see example below)

## How to Run

This project is built using Maven. In addition, you need to have an
application.properties file in the root of the project that contains key-value pairs. Here is an
example of an application.properties file:

```
[DatabaseSection]
mongodb.uri=mongodb://%s:%s@%s:%s/%s
mongodb.user=<user_name>
mongodb.password=<your_password>
mongodb.hostname=<your_host_name>
mongodb.database=<name_of_smartshark_database>
mongodb.database.comments=<name_of_intermediate_database>
mongodb.port=<pick_27017_if_running_locally>
mongodb.options=?authSource=admin&readPreference=primary&appname=MongoDB%20Compass&directConnection=true&ssl=false
```

You should change every \<value>\ to your own database settings. Additionally, the SmartSHARK and
the
intermediate database should be running on the same server.

## CLI

We are working on adding a CLI to the application to set the earliest committer date from which we
start extracting hunks from projects. If you want to extract all the hunks first from the SmartSHARK
database and persist them in the intermediate database before we start processing them for comments,
you can set the flag to true. If the flag is set to false, we will start processing hunks from the
intermediate hunks collection, assuming the initial hunk extraction has already happened.

There is also an argument that lets you process extracted hunks from
the last processed hunk. All these settings currently have to be set manually with the
committer_date having to be set in the aggregation query in the ProjectDAO class.

## Known Issues

## References

Everton da Silva Maldonado, Emad Shihab, and Nikolaos Tsantalis. Using natural language processing
to automatically detect self-admitted technical debt. IEEE Transactions on Software Engineering,
43(11):1044–1062, 2017. doi:10.1109/TSE.2017.2654244.
