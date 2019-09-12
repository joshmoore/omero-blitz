5.5.4 (Sep 2019)
----------------

- Add deprecation warning to Blitz objects (#62)
- Increase VerifyDepthMax for websocket login (#66)
- Prepare Blitz objects for Python 3 support (#69, #70)

5.5.3 (July 2019)
-----------------

- Bump omero-server and omero-common-test version.
- Fix Ice SSL ciphers (#58, #59)
- Properly close table services (#57, #60)
- Add -T Project:name:x/Dataset:name:y support (#61)

5.5.2 (June 2019)
-----------------

- Bump omero-server and omero-common-test version.

5.5.1 (May 2019)
----------------

- Update omero-server dependency version.
- Support websocket and url style connection strings.

5.5.0 (May 2019)
----------------

- Move clients' graph requests onto background threads.
- Generate Ice doc.
- Move repository location properties.
- Fix remaining Javadoc warnings.
- Reduce use of Guava.
- Remove findbugs pulled in by subethamail.
- Remove the Java Gateway code.
- Partially migrate Properties file from the openmicroscopy repository.
- Fix Javadoc warnings.
- Run units test in Travis.
- Deprecate setCaseSentivice in favor of new setCaseSensitive.
- Deprecate vestigial data provider for deleted test.
- Add DEBUG logging for successful repository file deletions.
- Safely order repository file deletions.
- Move OMEROImportFixture into test directory.
- LegalGraphTargets can work for any GraphQuery instance.
- Java SSL fixes for clients (rebased from metadata54).
- Remove Repository.pixels and deprecate BfPixelsStoreI.
- Fix "cannot link" bug with import target containers.
- Adjust some Blitz API responses to signal "OK".
- Remove from comments confusing references to OMERO 4.x code.
- Update Hibernate DTD URL to current recommendation.
- Version loading tweak.
- Fix comments in FS Blitz API.
- Include map annotations in image metadata export.
- Add License file.
- Use new Gradle build system.
- Extract omero-blitz from the openmicroscopy repository.
