# Replication Package: Branching and Merging in a Consistency-Preserving Multi-View Modelling Environment

This repository is the replication package for the paper

> *TODO: authors.* **TODO: title.** Submitted to ECMFA 2027 (Journal of Object Technology).

It contains a scenario-based case study that evaluates a Git-based branching and semantic merging extension of the
[Vitruvius](https://vitruv.tools) framework on a SysML/CAD virtual single underlying model (V-SUM).
The 14 merge scenarios are executable JUnit tests; their descriptions and conflict classification are in
[`documents/cases.md`](documents/cases.md).

## Contents

| Path | Content |
|------|---------|
| `case-study/` | The case study: metamodels, consistency preservation rules (Reactions), V-SUM setup and scenario tests |
| `Vitruv-Branching-Tool/` | Vitruv 3.2.4-SNAPSHOT including the branching tool under study, **with** the patches below applied |
| `tool-patches/` | The individual changes made to the branching tool during the study, as plain diffs against the original tool version |
| `documents/cases.md` | Description and classification of all scenarios |

## Anonymisation

The models were derived from a real-world vehicle development project. The project partner's data must not be published,
so the following was masked before release; the scenario logic is unchanged:

- vehicle generations are called *V1* and *V2*;
- all CAD part identifiers are replaced by pseudonyms (`P0001`, `P0001_MIR_ASM`, ...), consistently across both CAD models, correspondence sets and tests;
- all CAD geometry (volume, surface area, bounding box) is synthetic;
- all parameter values in the SysML model and in the scenarios are fictitious;
- domain-specific component names (e.g. of safety switches and sensors) are replaced by generic names.

The original STEP files are not included; `step_parser.py` is provided to show how the JSON files were produced.

## Important Note on the Tool Patches

The branching tool was developed independently of this case study. While implementing the scenarios, several defects were found.
The patches in `tool-patches/` were produced with the help of an AI coding agent to locate the defects and make the scenarios executable.
They have **not** been validated by the tool developers and are meant to document *where* the defects are, not as verified fixes.
To run the scenarios against the original tool, reverse-apply the `fix(Vitruv)` patches (`git apply -R`).
The scenario tests themselves and their expected outcomes were written and checked manually.

## Expected Results

Running `./mvnw clean verify -U` in `case-study/` (JDK 17) executes 16 reaction tests and the 14 scenario tests.

| Tool version | Scenarios handled as expected | Failing scenarios |
|---|---|---|
| Patched (as included in this repository) | 12 / 14 | A6 (`CaseActivityDiagramTest.scenario6`), B5 (`CaseGenerationChangesTest.scenario5`): indirect conflicts reported as `SUCCESS` |
| Original (patches `02`-`04` and `06`-`10` reverse-applied) | 2 / 14 | all except B1 and B4; A2-A6 fail as a consequence of A1 (chained scenarios) |

Scenario IDs: A*n* = `CaseActivityDiagramTest.scenario`*n* (Case 1 in `documents/cases.md`),
B*n* = `CaseGenerationChangesTest.scenario`*n* (Case 2). The Maven build reports test failures for the failing scenarios;
this is the expected outcome, not a build problem.

To run against the original tool:

```sh
for p in $(ls tool-patches/*.diff | grep -v -e '/01-' -e '/05-' | sort -r); do git apply -R "$p"; done
```

## How to Build and Run

This chapter shows how to build and run the case study.

### Prerequisites

To install the custom Vitruv version, its prerequisites need to be met:

- JDK 17 (newer JDKs may fail with `release version 17 not supported`)
- Maven 3.9 or later
- Git

### Install Vitruv

To install Vitruv, run:

```sh
cd Vitruv-Branching-Tool/Vitruv/
./mvnw clean install -DskipTests
```

### Run the Case Study

To run the case study, run:

```sh
cd case-study/
./mvnw clean verify -U
```

### Maven Build Stability Issues

The Maven build process has proven to be very unstable during the development of this project.
When building or re-building the project, there were frequent failures to resolve certain dependencies, often those from the auto-generated code related to the Ecore models, for example:

```java
[ERROR] ClassName.method:XY » Unresolved compilation problems: 
        cad cannot be resolved to a variable
        sysml cannot be resolved to a variable
```

To avoid these problems, it is advised to use the above commands to build the project, especially the `-U` parameter for `mvn clean verify`.
This forces Maven to re-download all dependencies, avoiding cached dependencies interfering with the build process and ensuring a working build.

Should dependency resolution errors occur, it is strongly advised to delete all cached dependencies.
To do this, run (command for Unix-like systems):

```sh
cd ~/.m2/
rm -rf repository
```

This deletes all downloaded and locally installed dependencies and artifacts, allowing a clean re-install of the project. 
Note that this also deletes the Vitruv artifact, so a re-build of the custom Vitruv version is necessary.
After re-building Vitruv, the case study can be run using the above command.

## Project Structure

This project consists of two sub-projects: The actual case study (`case-study`) and a modified version of Vitruv 3.2.4-SNAPSHOT (`Vitruv-Branching-Tool`) with some minor bug fixes.

Additional documents can be found in the `documents/` directory. These include the precise descriptions for the cases and scenarios (`cases.md`).

### Case Study

The `case-study` project is based on the [Methodologist Template](https://github.com/vitruv-tools/Methodologist-Template).
The used SysML and CAD Ecore models are located in the `model/` directory, the reactions between them in `consistency/`.

The cases and scenarios of this case study are implemented via JUnit unit tests in `vsum/src/test/`.
In the `resources/` directory, the SysML and CAD documents used for creating the models as well as the "*correspondence sets*" (CSV files that contain corresponding elements for certain models) between them are located.
The `java/tools/vitruv/branchingcasestudy/vsum/` directory contains the actual source code used to implement the case study.
In this directory:

- `cases/` contains the scenario unit tests for each case,
- `reactions/` contains a test file for the defined reactions between the two models,
- `util/base/` contains a base class and user interactor for the unit tests, and
- `util/parser/` contains parsers for both types of model documents.

### Modified Vitruv Version

The used Vitruv version which introduces the branching tool (`3.2.4-SNAPSHOT`) was developed outside of this case study.

During the implementation of the cases, some issues with the branching tool appeared.
This resulted in changes to the source code of the used Vitruv version.
To allow the individual changes to be traced and the project to be built, the Vitruv version with changes is included in this project.

The tool changes have been generated by an AI coding agent.
They could not be validated in the scope of this case study, so for now they only serve two purposes: To show the sources of the encountered errors and to aid in the fixing of them by proposing possible fixes.
Future work building on the modified tool will need to keep this in mind.
The actual content of the case study was not AI generated and has been humanly verified.

The changes made to the branching tool can be seen by looking at the "*fix(Vitruv)*"-annotated commits in the commit history.

## V-SUM Setup

This chapter describes the used models and CPRs of the V-SUM in this project.

### Used Models

In this case study, two different models are used: A **SysML** (Systems Markup Language) model and a **CAD** (Computer-Aided Design) model.

The SysML model aims to represent the SysML specification as completely as possible.
It keeps hold of the various SysML elements (such as blocks, associations, properties etc.) and also keeps the structure of the represented SysML model intact.

The CAD model treats each independent part as a so-called *namespace*.
Each namespace can have an arbitrary amount of parameters (numerical, textual etc.) and shapes (geometric information; not used in this project).
The CAD model is then just a collection of all namespaces with no further hierarchical or positional information.

### Consistency Preservation

Consistency between the two models in the V-SUM is mainly kept in two ways:

- One-to-many correspondence of blocks and namespaces
- Correspondence of equally named properties and parameters in corresponding blocks and namespaces

#### Blocks and Namespaces

Every SysML block must correspond to at least one, but possibly more, CAD namespaces.
When a new block is created, a corresponding namespace will automatically be created for it.

A CAD Namespace may, but is not required to, correspond to some SysML blocks.
This is because there may be parts in the CAD model which are simply not represented in the SysML model (e.g. individual screws).

#### Properties and Parameters

In SysML, parameters are represented as *ValueProperties*, which contain an *EString* as their value.
For CAD namespaces, there are three different types of parameters that differentiate between the contained data type:

- *NumericParameter* (contains an *EFloat*)
- *StringParameter* (contains an *EString*)
- *BooleanParameter* (contains an *EBoolean*)

There is also a *Coordinate* parameter available in the CAD model, but this one is ignored here since it doesn’t have a fitting SysML counterpart.

Parameters of corresponding blocks and namespaces are also made corresponding if they have an equal *name* attribute.
A name change (renaming) is then also propagated to the other model, as well as changes to the value of the property or parameter.

When creating a property or parameter in any of the two models and there is a property or parameter with the same name present in the other model, the two elements are automatically made corresponding.
Else, the user gets the option to create a corresponding property or parameter holding the same value in the other model.
If the user created a SysML *ValueProperty*, they also get to choose which type of parameter (Numeric / String / Boolean) to create in the CAD model; the CAD parameters always create a *ValueProperty*.


## Implementation

This chapter describes the implementation of the various components in this project, beginning with the source document reading and ending with the final scenario implementation.

### Source Documents

A case study evaluates within a real-world context.
To ensure this in the case study, real documents are used to instantiate the V-SUM.

#### SysML Model

The SysML model for this case study was not contained in the real dataset, but rather carefully created based on the development documents and the CAD model.
The model consists of two packages:

- The *structure* package, which contains a Block Definition Diagram (BDD) for the brake system.
This diagram is primarily based on the hierarchy of objects in the CAD files.
- The *activities* package, which contains the "*ApplyBrake*" activity.

#### CAD Files

In the original study, the CAD files came directly from the partner's dataset. **They are not part of this artifact.** Instead, `vsum/src/test/resources/cad/json/` contains anonymised JSON files with the same assembly structure, pseudonymised part identifiers and synthetic geometry.


### Model Parsing

In order to import the models from the source documents into the V-SUM, the existing documents need to be parsed into their respective Ecore model format.
For this purpose, there are two parsers for each model type.

The created parsers are not general-purpose parsers into the Ecore model format.
They are specifically tailored to the needs of this case study and therefore can only parse specific model elements in certain ways.

#### SysML

The SysML 1.6 standard is based on the UML standard.
Therefore, the SysML model file, which is just a `.uml` file, can be parsed using an existing UML parser.
Due to the used elements in this project, no further parsing logic (e.g. stereotype handling) needs to be implemented for the UML parsing.
The only place where a SysML element must be recognized is when parsing a UML class into a SysML block; however, the SysML block is the only element which uses the UML class as its representation in the UML model and can therefore be directly parsed.

The `SysMLParser` uses the `UMLSwitch` from the `uml2` package to parse the UML elements.
This allows a dedicated parsing logic to be written for each element.
The `UMLSwitch` then calls the most specific parsing method for each element.
To avoid parsing elements multiple times or being stuck in an infinite parsing loop, the parser remembers which SysML element was parsed for which UML element.

To make the parsing logic reusable, each element type from the SysML Ecore model gets one or two dedicated parsing methods, depending on whether or not is it instantiable (e.g. not abstract).

Every element type gets an `initX()` method.
This method type fills an already created element with data.
If the element type inherits attributes from another element type (e.g. a *Model* inherits the attributes of a *Package*), then the initialization method of the super type is also called, leading to recursively filling the element with data until a *root type* without a super type is initialized.

To actually create the elements, each instantiable element type also gets a `caseX()` method.
These methods are called by the `UMLSwitch` and create the desired element by instantiating it and calling the respective initialization method to fill the object with data.
The created element is then cached and returned.

#### CAD

The CAD files that were used for this project are in the *STEP* format.
This format does not directly contain geometrical information, but rather a set of instructions on how to create the model inside of a CAD modelling program.
Therefore, parsing the model directly from these files is not really practicable since all model building logic would need to be implemented.
Also, there are no good ways to programmatically work on a STEP-encoded CAD model inside of Java.

For this reason, the CAD parsing process has been split into two steps.
The first step consists of extracting the desired information from the STEP file and putting it into a generic interchange format.
In the second step, the extracted data can then be directly used to create the CAD Ecore model.

Nevertheless, for the first step there needs to be some sort of tooling to retrieve information from the STEP file.
For this, the open-source CAD modelling program [FreeCAD](https://www.freecad.org/index.php) was used.
This program exposes Python libraries that can be imported into a script and be used to perform actions on a loaded CAD model.

To extract the data from the STEP file, the `step_parser.py` Python script was made.
It programmatically opens the STEP file and extracts generic information, e.g. volume or surface area, from all found geometric objects inside of the model.
Each of those objects is then stored in a dictionary, together with an identifier, other metadata and the extracted geometric information.

The created dictionary can then be saved into a `.json` file.
This file in turn can be opened by the Java `CADParser`.
Using the [Jackson](https://github.com/fasterxml/jackson) library, the JSON file can be parsed into a set of *Record* objects.
Those objects can then be used to create the Ecore model.

### Test Base Class

To be able to work with the branch-aware V-SUM, a lot of helper methods need to be defined to allow a higher-level interaction with the tool without needing to reimplement the same logic in all test methods.
For this reason, the `BranchAwareVSUMTestBase` serves as the abstract base class for all case implementation classes.
All case-independent, reusable logic is offloaded into this class.

#### Base Class Features

The base class is intended to be extended by the case implementation classes.
This way, it can also define own JUnit tests that are then executed by the extending classes.
The `setup()` method makes use of this and registers the correct resource factory for XMI documents as well as triggering the generation of the aforementioned JSON files for CAD models, should they not yet be present.

`setupBranching()` and `tearDown()` can be used to create and delete the V-SUM.
Since the activity diagram case with connected scenarios and the V2 changes case with independent scenarios need to call these methods at different times (to keep or delete the V-SUM state after scenarios), these methods are not implemented as JUnit tests and need to be explicitly called by the extending classes.

`loadAndConsistModels()` can be used to load existing models using the parsers and inserting them into the V-SUM in a way that certain elements are made corresponding by the reactions.
In which way the elements are consisted is defined by the respective correspondence set, which is a CSV file containing the identifiers of model elements that are to be consisted.
The correct correspondence set is identified by the file names of the given models.
Currently, the CSV file contains a SysML element identifier followed by a CAD element identifier in each row.
For simplicity, no input validation is done here.
Due to previous attempts at importing models with certain correspondences failing, the actual model insertion is done in a hardcoded way for one SysML and one CAD model.

The `modifyView()` method, which allows writing modification methods on views of the V-SUM, was copied from the Methodologist Template.
To conveniently get such a view, the `getView()` method is used with a fitting element selection condition.
For better readability, this method is only able to be accessed by methods that return a certain view on the V-SUM.
Those methods are:

- `getSysMLView()`, which returns a view on the SysML model
- `getCADView()`, which returns a view on the CAD model
- `getGlobalView()`, which returns a view on the entire V-SUM

To conveniently access specific elements in the V-SUM without needing to manually traverse the contained models, the `findElement()` method allows directly accessing elements from a view by their identifier.

Performing a commit on the current model status can be done with the `commitAndWaitForChangelog()` method, which was copied from the *Vitruvius-Branching-Test* sub-project from the branching tool repository.

#### User Interactor

Since some reactions make use of responses given by the user of Vitruvius, these results must be given in some way.
This is done with the `UserInteractor`, which is an `InteractionResultProvider` that allows directly setting the answers of certain user interactions.
The interaction results are only reset to their default values when rebuilding the V-SUM, so user interactions should be explicitly set before performing actions that may trigger user interacting reactions.

### Reactions Tests

To test the reactions made in this case study and verify their actions on the models, a dedicated `ReactionsTest` class has been written.
This class also extends the test base class, but does not make use of loading existing models.
Rather, custom models are build in-memory to test the effects of specific reactions on the V-SUM.

### Case Implementation

The actual implementation of the two cases happens in the classes `CaseActivityDiagramTest` and `CaseGenerationChangesTest`.
The "*Test*" suffix allows both classes to be run by the Surefire test plugin without needing to explicitly register them as test classes.

Each scenario is implemented as a separate test method.
In those methods, first the two branches for the stakeholders are created.
Then, each branch applies the changes of its respective stakeholder to the V-SUM.
Finally, the two branches are merged together (the first one into the second one).
Should conflicts occur, the resolution is also applied in the second branch.
In the activity diagram case, the result of the second branch is also merged onto the "main" branch with a fast-forward merge.


## Findings

This chapter shows what the case study has found out about the branching tool.

### Branching Tool Issues

During the original study, an intermediate version of the scenario implementations (partly with mocked results of preceding scenarios) was assessed. In that assessment, 6 of the 14 scenarios ran without problems, while 8 initially could not be completed (1.1, 1.3, 1.5, 1.6, 2.2, 2.5, 2.7, 2.8). With the final, unmocked scenario implementations in this repository, the original tool handles only 2 of 14 scenarios as expected (see *Expected Results* above).

In the process of fixing those scenarios, it became apparent that the branching tool is not able to perform the given scenarios without issues, which leads to the failures of some of the test methods.
The problem sources have been discovered through a trial-and-error process using an AI coding agent.
Since those changes could not be evaluated in the scope of this project, the list of issues does not contain precise error sources, but rather broader regions of where the actual error lies.

Through this process, the following regions of error could be identified:

- **EReference conflict detection and management**: The branching tool is not able to detect delete-modify and modify-modify conflicts on references to other model elements (Ecore *EReference*s).
This detection step could quickly be added to the tool with minor modifications.
Still, actual conflicts on EReferences are not able to be resolved correctly.
This affects scenarios 1.1, 1.5, 2.8.
- **Indirect conflict management**: Indirect conflicts are not able to be resolved correctly, as original changes always override consequential changes.
At least in some cases, the tool also only warns about indirect conflicts while returning the *SUCCESS* merge result, despite there being a conflict.
This affects scenarios 1.6, 2.2, 2.5.
- **Fast-forward merges don't commit metadata**: Merges that return the *FAST_FORWARD* merge result do not commit the V-SUM metadata, leading to it being deleted when switching to a different branch.
This affects scenarios 1.1, 1.3.
- **Element resolution failures**: Various errors occurred when trying to reference elements in the engine replay, since the position-based element resolution was sometimes not able to find the right element or an element at all.
This was especially a problem when trying to delete elements from the V-SUM, since the wrong element or a *null* entry was tried to be deleted.
The missing EReference handling did also lead to element resolution failures.
This affects scenarios 1.5, 2.7, 2.8.

Furthermore, some minor inconveniences were found while working with the tool that are not direct issues of the tool, but should still be addressed in future:

- **Misleading `createBranch()` syntax**: The branch creation method takes in two arguments: The new branch name and the name of the source branch which the new branch should be based on.
This suggests that the V-SUM must not necessarily be checked out on the source branch, since this can be freely chosen.
If this is not the case, the new branch cannot be created correctly since the tool tries using the currently checked out branch - and not the given method parameter - as the source branch for the new branch.
The easy fix for this is simply checking out to the desired source branch before creating the new branch.
However, for usability reasons this should be changed by either performing a branch switch in the branch creation method or by removing the second parameter from the method.
- **EClass name collisions in the EPackage registry**: In this project, the Eclipse *uml2* framework was used to parse the SysML model.
This uses the same global *EPackage* registry as the Ecore framework to register its UML types.
When the `ChangeDtoDeserializer.resolveEClass()` method now tries to resolve the *EClass* of a model object from the V-SUM, it does so only by name, leading to the registered UML classes being found instead of the correct Ecore elements.
This can be solved by removing the UML classes from the registry before performing actions on the V-SUM.
Should this registry be needed in a future project, the affected method must be rewritten to use unique class identifiers instead of only the class name.

### Limitations

Due to the real-life context limitation of a case study, the scenarios had to be as realistic as possible.
It was not able to include all combinations of conflict types, origins and detection strategies in this study, since for some of these combinations no plausible scenarios could be found.

The found issues for the branching tool are not exhaustive, so there may be further issues in parts of the tool that have not been discovered.
Furthermore, the changes to the branching tool done in this project have been done without extensive knowledge of the tool and are therefore not to be seen as direct fixes to the tool, as they only prevent the error symptoms from showing.

### Conclusion

The study shows that the branching tool is not yet ready to be built into the Vitruvius framework as several issues still need to be addressed.
Those issues prevent working with the branching tool in a productive manner since they can be experienced in many common situations.
Further testing and error fixing needs to be done in order to be able to safely add the branching tool into Vitruvius.
