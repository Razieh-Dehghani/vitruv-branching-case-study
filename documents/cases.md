# Cases

Here, the different cases and scenarios which make up this case study are presented.

> **Note on anonymisation.** The models were derived from a real vehicle development project. All partner data has been masked:
> vehicle generations are called *V1* and *V2*, CAD part identifiers are pseudonymised (`P0001`, ...), CAD geometry is synthetic,
> and parameter values in the SysML model and scenarios are fictitious.

This case study consists of two cases:

- The continuous creation and modification of a SysML activity diagram
- Changes between two vehicle generations (V1 → V2)

In each scenario, there are two "*stakeholders*" performing changes to the same base model.
After their modifications, the two branches are merged together.
The scenario descriptions contain a taxonomy of what kind of conflict happens (if one happens).

## Case 1: Activity Diagram

This case focuses on a SysML activity diagram and subsequent changes made to it.
A CAD model corresponding to the SysML model is also present, but no assumptions of its content (in particular which version of the car it is from) are made here.

*The scenarios in this case **build upon each other**: The result of the previous scenario defines the initial state for the following scenario.*

### Initial Scenario

The activity diagram, which is contained in the used SysML model, depicts the activity of applying the brake pedal.
The diagram can be seen as two loops which come together again at one decision node (“*center node*”).
One loop will be used when the brake pressure increases (compared to the previous loop iteration), the other when the pressure decreases.
The brake pressure itself is only implicitly modeled and has no structural impact.
Each loop triggers an action that changes the position of the brake pads.
When the brake pressure reaches zero, the activity is considered to be finished.

### Scenario 1

**Description**: Two individual Electrical Engineers want to add actions for the brake light and the safety switch to the activity diagram.

**Involved stakeholders**: Electrical Engineer 1 (EE1), Electrical Engineer 2 (EE2)

**Changes in branch 1**: EE1 adds a decision node “*Brake light on?*” after the action “*Push brake pads together*”.
The edge from that action to the center node is now redirected into the new decision node.
If “*[Yes]*”, go back to the center node; if “*[No]*”, go to the new action “*Turn on brake light*” and then go back to the center node.
EE1 also adds a “*Turn off brake light*” node between the “*pedalTravel == 0%?*” decision node and the activity final node, connected by the “*[Yes]*” edge of the decision node.

**Changes in branch 2**: EE2 adds a decision node “*Safety switch pressed?*” after the action “*Push brake pads together*”.
The edge from that action to the center node is now redirected into the new decision node.
If “*[Yes]*”, perform the new action “*Shut off engine*” and go to the existing activity final node; if “*[No]*”, go back to the center node.

**Result of merge operation**: Conflict due to the same edge being redirected into different nodes.

**Conflict type**: Modify-modify

**Conflict origin**: Original-original

**Conflict detection**: Structural

**Conflict resolution strategy**: Manually put the safety-switch decision node and action “after” the brake light decision node and action.
Re-route the edge labeled “*[Yes]*” that goes from the brake light decision node to the center node to the safety-switch decision node instead.

### Scenario 2

**Description**: Cover the edge case where there is no brake application after starting the activity (“*pedalTravel*” stays at zero).

**Involved stakeholders**: Project Manager (PM), SysML Engineer (SE)

**Changes in branch 1**: PM changes the label of the edge between the center node and the action “*Pull brake pads apart*” from “*[Decrease] (brakePressure -= Δp)*” to “*[Decrease or equal] (brakePressure -= Δp)*”.

**Changes in branch 2**: SE puts the action “*Pull brake pads apart*” after the “*[No]*” edge that goes out of the decision node “*brakePressure == 0?*”.

**Result of merge operation**: Conflict-free merge due to compatible changes to related elements.

### Scenario 3

**Description**: It is decided that the brake system activity diagram should not have a direct reference to the engine.

**Involved stakeholders**: SysML Engineer (SE), Electrical Engineer (EE)

**Changes in branch 1**: SE deletes the action “*Shut off engine*” and does not change any edges.

**Changes in branch 2**: EE deletes the action “*Shut off engine*” and adds a new action “*Send safety switch pressed signal to ECU*” in its place.
The edges that were previously connected to the deleted action are now reconnected to the new action.

**Result of merge operation**: Conflict due to conflicting relationship endpoints.

**Conflict type**: Delete-modify

**Conflict origin**: Original-original

**Conflict detection**: Structural

**Conflict resolution strategy**: Accept the changes of the EE.

### Scenario 4

**Description**: Take energy recuperation on braking into account in the activity diagram.

**Involved stakeholders**: SysML Engineer 1 (SE1), SysML Engineer 2 (SE2)

**Changes in branch 1**: SE1 creates the action “*Recuperate according to brake pressure*” once and redirects the edges going from the two loops into the center node into this action instead.
The action then gets a new edge into the center node.

**Changes in branch 2**: SE2 creates the action “*Recuperate according to brake pressure*” twice and redirects both outgoing edges of the center node into their respective new action. The action then gets a new edge which completes the loop again.

**Result of merge operation**: Conflict-free merge, but there is an order conflict with the new actions now: Recuperation happens twice instead of once in the diagram.

**Conflict type**: Add-add

**Conflict origin**: Original-original

**Conflict detection**: Semantical

**Conflict resolution strategy**: Accept the changes of SE1.

### Scenario 5

**Description**: It is decided that the brake light is not modeled as a part of the brake system anymore.

**Involved stakeholders**: SysML Engineer (SE), Electrical Engineer (EE)

**Changes in branch 1**: SE deletes all brake light nodes from the activity diagram and relocates the edges to restore the loop.

**Changes in branch 2**: EE renames both actions “*Turn on brake light*” and “*Turn off brake light*” to “*Send brake light [on/off] signal to ECU*”.

**Result of merge operation**: Conflict in the activity diagram.

**Conflict type**: Delete-modify

**Conflict origin**: Original-original

**Conflict detection**: Structural

**Conflict resolution strategy**: Accept the changes of the EE.

### Scenario 6

**Description**: It is decided that no other components should be included in the brake system at all, including the safety switch.
Before branching, a SysML block called “*safety switch*” and a corresponding “dummy” CAD namespace are created and the “*Send safety switch pressed signal to ECU*” action is given an activity which references the “*safety switch*” block.

**Involved stakeholders**: CAD Engineer (CE), Electrical Engineer (EE)

**Changes in branch 1**: CE deletes the safety switch namespace.
As a reaction, the “*safety switch*” SysML block is also deleted since there are no more correspondences for it.

**Changes in branch 2**: EE changes the referenced block of the “*Send safety switch pressed signal to ECU*” action from the “*safety switch*” block to the “*BrakeSystem*” block, then renames the action “*Send safety switch pressed signal to ECU*” to “*Send car shut-off signal to ECU*”.

**Result of merge operation**: Conflict in the activity diagram.

**Conflict type**: Modify-modify

**Conflict origin**: Original-consequential

**Conflict detection**: Structural

**Conflict resolution strategy**: Accept the changes of the EE.


## Case 2: Generation Changes (V1 → V2)

This case focuses on the development changes that were made between the brake systems of the V1 and V2.
There were three major changes between those two versions of the vehicle:

- Size reduction of the front brake discs
- Replacement of the front brake caliper: Change from a two-block caliper design (two parts that are screwed together) to a monobloc caliper design (one single piece)
- Inclusion of a third pressure sensor in the hydraulic system

*The scenarios in this case are **independent of prior scenarios** and all assume the initial scenario as their starting point.*

### Initial Scenario

Given are the SysML model as well as the CAD models of the V1 and V2.
In the beginning, the SysML model corresponds to the V1 CAD model.
The V2 CAD model is only used to replace some of the namespaces by their updated versions.

### Scenario 1

**Description**: Directly switch out the brake discs in both models.

**Involved stakeholders**: SysML Engineer (SE), CAD Engineer (CE)

**Changes in branch 1**: SE changes the “*diameter*” attribute of the front disc block.

**Changes in branch 2**: CE exchanges the namespaces of the front discs for the respective V2 namespaces.

**Result of merge operation**: Conflict-free merge due to independent changes.

### Scenario 2

**Description**: The SysML and CAD Engineers replace the front brake caliper independently in both models.
Assumption made here: The two stakeholders only look at their own model and don’t pay attention to what the CPRs exactly do.

**Involved stakeholders**: SysML Engineer (SE), CAD Engineer (CE)

**Changes in branch 1**: SE renames the front disc to “*VentedFrontDisc*”.

**Changes in branch 2**: CE deletes the namespaces of the front discs.
As a reaction, this deletes the SysML block for them since there are no more corresponding namespaces.
After this, the new front disc namespaces are added, but no SysML block is created for them.

**Result of merge operation**: Conflict in the SysML model.

**Conflict type**: Delete-modify

**Conflict origin**: Original-consequential

**Conflict detection**: Structural

**Conflict resolution strategy**: Keep the renamed SysML block and ensure that the new CAD namespaces correspond to it.

### Scenario 3

**Description**: The Project Manager and Electrical Engineer both want to represent the pressure sensors in the SysML block definition diagram (BDD).

**Involved stakeholders**: Project Manager (PM), Electrical Engineer (EE)

**Changes in branch 1**: PM adds a block representing all sensors (“collection block”) called “*Sensors*”.
Then, they add one block for each of the three sensors and a composition between the collection block and each sensor.

**Changes in branch 2**: EE adds a collection block called “*PressureSensors*”.
Then, they add one block for the sensors that have already been in the car (whose functionality has already been used) and one block for the newly added “validation” sensor, as well as compositions between the collection block and each sensor type.

**Result of merge operation**: Conflict-free merge, but the sensors are now represented twice in the SysML diagram.

**Conflict type**: Add-add

**Conflict origin**: Original-original

**Conflict detection**: Semantical

**Conflict resolution strategy**: Perform another commit on the branch which was merged into: “Unify” the collection block under the name “*BrakePressureSensors*” and choose the sensor block design from the EE (two blocks).

### Scenario 4

**Description**: Two stakeholders rename the same element differently.

**Involved stakeholders**: Project Manager (PM), SysML Engineer (SE)

**Changes in branch 1**: PM renames the “*BrakeSystem*” block to “*V2BrakeSystem*”.

**Changes in branch 2**: SE renames the “*BrakeSystem*” block to “*BrakeSystemV2*”.

**Result of merge operation**: Conflict in the SysML model (renaming conflict).

**Conflict type**: Modify-modify

**Conflict origin**: Original-original

**Conflict detection**: Structural

**Conflict resolution strategy**: Choose the name “*V2BrakeSystem*”.

### Scenario 5

**Description**: The SysML and CAD Engineers change the value of a corresponding parameter in their model to two different values.
The subsequent parameter value propagation leads to a consequential-consequential conflict.

**Involved stakeholders**: SysML Engineer (SE), CAD Engineer (CE)

**Changes in branch 1**: SE incorrectly sets the value of the “*diameter*” parameter to a too high value.
As a reaction, this also changes the value of the corresponding parameter in the CAD model.

**Changes in branch 2**: CE sets the value of the “*diameter*” numeric parameter to the correct value.
As a reaction, this also changes the value of the corresponding parameter in the SysML model.

**Result of merge operation**: Conflicts in both models.

**Conflict type**: Modify-modify

**Conflict origin**: Consequential-consequential

**Conflict detection**: Structural

**Conflict resolution strategy**: Accept the changes from CE.

### Scenario 6

**Description**: The SysML and CAD Engineers independently add the new pressure sensor to their model.
When merging, both the manually created and automatically consisted elements are present in each model.

**Involved stakeholders**: SysML Engineer (SE), CAD Engineer (CE)

**Changes in branch 1**: SE creates a block for the new pressure sensor.
This automatically creates a corresponding CAD namespace.

**Changes in branch 2**: CE creates a namespace for the new pressure sensor and chooses to create a corresponding SysML block.

**Result of merge operation**: Conflict-free merge, but now the pressure sensor is present twice in each model.

**Conflict type**: Add-add

**Conflict origin**: Consequential-consequential

**Conflict detection**: Semantical

**Conflict resolution strategy**: Keep the manually created elements and make them corresponding while deleting the automatically created ones.

### Scenario 7

**Description**: Individually valid changes in two branches lead to wrong correspondences after merging.

**Involved stakeholders**: SysML Engineer (SE), CAD Engineer (CE)

**Changes in branch 1**: SE deletes the “*BrakeCaliper*” block and adds two separate “*FrontBrakeCaliper*” and “*RearBrakeCaliper*” blocks.
Then, they make each of the two new blocks corresponding to the respective CAD namespaces.

**Changes in branch 2**: CE replaces the front caliper namespaces with the respective namespaces of the V2 and makes them corresponding to the “*BrakeCaliper*” SysML block.

**Result of merge operation**: Conflict due to correspondence conflicts between the two models.

**Conflict type**: Modify-modify

**Conflict origin**: Consequential-consequential

**Conflict detection**: Structural

**Conflict resolution strategy**: Make the new namespaces corresponding to the two new blocks.

### Scenario 8

**Description**: After switching to a monobloc front brake caliper design, two individual SysML Engineers represent this change differently in the SysML model.
This leads to a reference to a deleted element.

**Involved stakeholders**: SysML Engineer 1 (SE1), SysML Engineer 2 (SE2)

**Changes in branch 1**: SE1 deletes the “*CaliperBlock*” block.

**Changes in branch 2**: SE2 adds a value property called “*type*” with the value “*MONOBLOC*” to the “*BrakeCaliperBlock*” block.

**Result of merge operation**: Conflict in the SysML model.

**Conflict type**: Delete-modify

**Conflict origin**: Original-original

**Conflict detection**: Structural

**Conflict resolution strategy**: Accept the changes from SE2.
