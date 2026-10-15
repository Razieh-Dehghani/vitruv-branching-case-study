package tools.vitruv.branchingcasestudy.vsum.cases;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import sysml.SysmlFactory;
import tools.vitruv.framework.vsum.branch.MergeManager;
import tools.vitruv.framework.vsum.branch.data.ModelMergeResult;
import tools.vitruv.framework.vsum.branch.merge.ConflictResolutionProvider;
import tools.vitruv.branchingcasestudy.vsum.util.base.BranchAwareVSUMTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This case focuses on a SysML activity diagram and subsequent changes made to it. A CAD model corresponding to the SysML model is also
 * present, but no assumptions of its content (in particular which version of the car it is from) are made here.
 * 
 * The scenarios in this case build upon each other: The result of the previous scenario defines the initial state for the following
 * scenario.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class CaseActivityDiagramTest extends BranchAwareVSUMTestBase {
    /**
     * Seed the used models and set up the branching tool once before any test is run. This keeps the state after a scenario and uses it as
     * the initial state for the next one.
     * 
     * @throws Exception on branching tool initialization errors
     */
    @BeforeAll
    void beforeAll(@TempDir Path tempDir, @TempDir Path vsumSeedingDir) throws Exception {
        setupBranching(tempDir);
        loadAndConsistModels("BrakeSystemModel/BrakeSystemModel.uml", "V1");
    }

    /**
     * Clean up only after all tests are done.
     */
    @AfterAll
    void AfterAll() {
        tearDown();
    }

    @Test
    @Order(1)
    void scenario1() throws Exception {
        // Electrical Engineer 1 (EE1) adds brake light action in the first branch
        branchManager.createBranch("ee1-add-brakelight-action", "main");
        branchManager.switchBranch("ee1-add-brakelight-action");

        modifyView(getSysMLView(), v -> {
            // Create the two new nodes for the 'Turn on brake light' action
            sysml.DecisionNode brakeLightOn = SysmlFactory.eINSTANCE.createDecisionNode();
            brakeLightOn.setName("Brake light on?");
            brakeLightOn.setId("brakeLightOn");

            sysml.CallBehaviorAction turnOnBrakeLight = SysmlFactory.eINSTANCE.createCallBehaviorAction();
            turnOnBrakeLight.setName("Turn on brake light");
            turnOnBrakeLight.setId("turnOnBrakeLight");

            // Insert actions into the activity
            sysml.Activity activity = (sysml.Activity) findElement(v, "_oCPuYJGVEfGICs4ezpB-dw");
            activity.getNodes().addAll(List.of(brakeLightOn, turnOnBrakeLight));

            // Re-route edge outgoing from 'Push brake pads together' into the new decision node
            sysml.ControlFlow outgoingEdge = (sysml.ControlFlow) findElement(v, "_URUKYJJtEfGICs4ezpB-dw");
            outgoingEdge.setTarget(brakeLightOn);

            // Create and add the edges for the new nodes
            sysml.JoinNode joinNode = (sysml.JoinNode) findElement(v, "_F8PiQJJtEfGICs4ezpB-dw");
            sysml.ActivityFinalNode finalNode = (sysml.ActivityFinalNode) findElement(v, "_zkPQMJJsEfGICs4ezpB-dw");

            sysml.ControlFlow yesEdge = SysmlFactory.eINSTANCE.createControlFlow();
            yesEdge.setName("[Yes]");
            yesEdge.setId("brakeLightOnYesEdge");
            yesEdge.setSource(brakeLightOn);
            yesEdge.setTarget(joinNode);

            sysml.ControlFlow noEdge = SysmlFactory.eINSTANCE.createControlFlow();
            noEdge.setName("[No]");
            noEdge.setId("brakeLightOnNoEdge");
            noEdge.setSource(brakeLightOn);
            noEdge.setTarget(turnOnBrakeLight);

            sysml.ControlFlow returningEdge = SysmlFactory.eINSTANCE.createControlFlow();
            returningEdge.setId("brakeLightOnReturningEdge");
            returningEdge.setSource(turnOnBrakeLight);
            returningEdge.setTarget(joinNode);

            activity.getEdges().addAll(List.of(yesEdge, noEdge, returningEdge));

            // Create and insert the 'Turn off brake light' action
            sysml.CallBehaviorAction turnOffBrakeLight = SysmlFactory.eINSTANCE.createCallBehaviorAction();
            turnOffBrakeLight.setName("Turn off brake light");
            turnOffBrakeLight.setId("turnOffBrakeLight");

            sysml.ControlFlow pedalTravelZeroYesEdge = (sysml.ControlFlow) findElement(v, "_DVpTcJJtEfGICs4ezpB-dw");
            pedalTravelZeroYesEdge.setTarget(turnOffBrakeLight);

            sysml.ControlFlow turnOffBrakeLightToFinalNodeEdge = SysmlFactory.eINSTANCE.createControlFlow();
            turnOffBrakeLightToFinalNodeEdge.setId("turnOffBrakeLightToFinalNodeEdge");
            turnOffBrakeLightToFinalNodeEdge.setSource(turnOffBrakeLight);
            turnOffBrakeLightToFinalNodeEdge.setTarget(finalNode);

            activity.getNodes().add(turnOffBrakeLight);
            activity.getEdges().add(turnOffBrakeLightToFinalNodeEdge);
        });
        commitAndWaitForChangelog("feat: add brake light action", "ee1-add-brakelight-action");

        // To be able to create the new branch with the correct base, we need to switch to the 'main' branch first before creating the new
        // branch. The reason for this to be necessary seems to be the method `newLayout.inheritFromBranchIfEmpty(oldBranch)` from the 
        // `PostCheckoutHandler`: Here, `oldBranch` is the branch that was last active, not the branch that the new branch originates from.
        // When this extra switch to 'main' is not made, there will likely be no root models registered to the VSUM.
        branchManager.switchBranch("main");

        // Electrical Engineer 2 (EE2) adds SafetySwitch action in the second branch
        branchManager.createBranch("ee2-add-safetySwitch-action", "main");
        branchManager.switchBranch("ee2-add-safetySwitch-action");

        modifyView(getSysMLView(), v -> {
            // Create the two new nodes for the SafetySwitch action
            sysml.DecisionNode safetySwitchPressed = SysmlFactory.eINSTANCE.createDecisionNode();
            safetySwitchPressed.setName("SafetySwitch pressed?");
            safetySwitchPressed.setId("safetySwitchPressed");

            sysml.CallBehaviorAction shutOffEngine = SysmlFactory.eINSTANCE.createCallBehaviorAction();
            shutOffEngine.setName("Shut off engine");
            shutOffEngine.setId("shutOffEngine");

            // Insert actions into the activity
            sysml.Activity activity = (sysml.Activity) findElement(v, "_oCPuYJGVEfGICs4ezpB-dw");
            activity.getNodes().addAll(List.of(safetySwitchPressed, shutOffEngine));

            // Re-route edge outgoing from 'push brake pads together' into the new decision node
            sysml.ControlFlow outgoingEdge = (sysml.ControlFlow) findElement(v, "_URUKYJJtEfGICs4ezpB-dw");
            outgoingEdge.setTarget(safetySwitchPressed);

            // Create and add the edges for the new nodes
            sysml.JoinNode joinNode = (sysml.JoinNode) findElement(v, "_F8PiQJJtEfGICs4ezpB-dw");
            sysml.ActivityFinalNode finalNode = (sysml.ActivityFinalNode) findElement(v, "_zkPQMJJsEfGICs4ezpB-dw");

            sysml.ControlFlow yesEdge = SysmlFactory.eINSTANCE.createControlFlow();
            yesEdge.setName("[Yes]");
            yesEdge.setId("safetySwitchPressedYesEdge");
            yesEdge.setSource(safetySwitchPressed);
            yesEdge.setTarget(shutOffEngine);

            sysml.ControlFlow noEdge = SysmlFactory.eINSTANCE.createControlFlow();
            noEdge.setName("[No]");
            noEdge.setId("safetySwitchPressedNoEdge");
            noEdge.setSource(safetySwitchPressed);
            noEdge.setTarget(joinNode);

            sysml.ControlFlow finishingEdge = SysmlFactory.eINSTANCE.createControlFlow();
            finishingEdge.setId("safetySwitchPressedReturningEdge");
            finishingEdge.setSource(shutOffEngine);
            finishingEdge.setTarget(finalNode);

            activity.getEdges().addAll(List.of(yesEdge, noEdge, finishingEdge));
        });
        commitAndWaitForChangelog("feat: add SafetySwitch action", "ee2-add-safetySwitch-action");

        // Merge the two branches; there is now a merge conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("ee1-add-brakelight-action");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the merge conflict by selecting the change from the 'add-brakelight-action' branch for the conflicting edge; semantical
        // conflict resolution happens later
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllTheirs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("ee1-add-brakelight-action");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Re-connect the edges so that the activity semantically makes sense again; this is done by setting the `safetySwitchPressed` decision
        // node as the target of both the `brakeLightOnYesEdge` and the `brakeLightOnReturningEdge`
        modifyView(getSysMLView(), v -> {
            sysml.DecisionNode safetySwitchPressed = (sysml.DecisionNode) findElement(v, "safetySwitchPressed");

            sysml.ControlFlow brakeLightOnYesEdge = (sysml.ControlFlow) findElement(v, "brakeLightOnYesEdge");
            brakeLightOnYesEdge.setTarget(safetySwitchPressed);

            sysml.ControlFlow brakeLightOnReturningEdge = (sysml.ControlFlow) findElement(v, "brakeLightOnReturningEdge");
            brakeLightOnReturningEdge.setId("brakeLightOnToSafetySwitchPressedEdge"); // change id since the meaning of this edge has changed
            brakeLightOnReturningEdge.setTarget(safetySwitchPressed);
        });
        commitAndWaitForChangelog("fix: connect brake light and SafetySwitch activities in a semantically correct way", "ee2-add-safetySwitch-action");

        // Check whether the model elements are now connected in the expected way
        sysml.CallBehaviorAction pushBrakePadsTogether = (sysml.CallBehaviorAction) findElement(getSysMLView(), "_z543cJJpEfGICs4ezpB-dw");
        sysml.DecisionNode brakeLightOn = (sysml.DecisionNode) findElement(getSysMLView(), "brakeLightOn");
        sysml.CallBehaviorAction turnOnBrakeLight = (sysml.CallBehaviorAction) findElement(getSysMLView(), "turnOnBrakeLight");
        sysml.DecisionNode safetySwitchPressed = (sysml.DecisionNode) findElement(getSysMLView(), "safetySwitchPressed");
        sysml.CallBehaviorAction shutOffEngine = (sysml.CallBehaviorAction) findElement(getSysMLView(), "shutOffEngine");
        sysml.JoinNode joinNode = (sysml.JoinNode) findElement(getSysMLView(), "_F8PiQJJtEfGICs4ezpB-dw");
        sysml.ActivityFinalNode finalNode = (sysml.ActivityFinalNode) findElement(getSysMLView(), "_zkPQMJJsEfGICs4ezpB-dw");
        sysml.DecisionNode pedalTravelIsZero = (sysml.DecisionNode) findElement(getSysMLView(), "_kH1MkJJsEfGICs4ezpB-dw");
        sysml.CallBehaviorAction turnOffBrakeLight = (sysml.CallBehaviorAction) findElement(getSysMLView(), "turnOffBrakeLight");
        assertNotNull(pushBrakePadsTogether);
        assertNotNull(brakeLightOn);
        assertNotNull(turnOnBrakeLight);
        assertNotNull(safetySwitchPressed);
        assertNotNull(shutOffEngine);
        assertNotNull(joinNode);
        assertNotNull(finalNode);
        assertNotNull(pedalTravelIsZero);
        assertNotNull(turnOffBrakeLight);

        sysml.ControlFlow outgoingEdge = (sysml.ControlFlow) findElement(getSysMLView(), "_URUKYJJtEfGICs4ezpB-dw");
        assertNotNull(outgoingEdge);
        assertTrue(EcoreUtil.equals(pushBrakePadsTogether, outgoingEdge.getSource()));
        assertTrue(EcoreUtil.equals(brakeLightOn, outgoingEdge.getTarget()));

        sysml.ControlFlow brakeLightOnYesEdge = (sysml.ControlFlow) findElement(getSysMLView(), "brakeLightOnYesEdge");
        assertNotNull(brakeLightOnYesEdge);
        assertTrue(EcoreUtil.equals(brakeLightOn, brakeLightOnYesEdge.getSource()));
        assertTrue(EcoreUtil.equals(safetySwitchPressed, brakeLightOnYesEdge.getTarget()));

        sysml.ControlFlow brakeLightOnNoEdge = (sysml.ControlFlow) findElement(getSysMLView(), "brakeLightOnNoEdge");
        assertNotNull(brakeLightOnNoEdge);
        assertTrue(EcoreUtil.equals(brakeLightOn, brakeLightOnNoEdge.getSource()));
        assertTrue(EcoreUtil.equals(turnOnBrakeLight, brakeLightOnNoEdge.getTarget()));

        sysml.ControlFlow brakeLightOnReturningEdge = (sysml.ControlFlow) findElement(getSysMLView(), "brakeLightOnToSafetySwitchPressedEdge");
        assertNotNull(brakeLightOnReturningEdge);
        assertTrue(EcoreUtil.equals(turnOnBrakeLight, brakeLightOnReturningEdge.getSource()));
        assertTrue(EcoreUtil.equals(safetySwitchPressed, brakeLightOnReturningEdge.getTarget()));

        sysml.ControlFlow safetySwitchPressedYesEdge = (sysml.ControlFlow) findElement(getSysMLView(), "safetySwitchPressedYesEdge");
        assertNotNull(safetySwitchPressedYesEdge);
        assertTrue(EcoreUtil.equals(safetySwitchPressed, safetySwitchPressedYesEdge.getSource()));
        assertTrue(EcoreUtil.equals(shutOffEngine, safetySwitchPressedYesEdge.getTarget()));

        sysml.ControlFlow safetySwitchPressedNoEdge = (sysml.ControlFlow) findElement(getSysMLView(), "safetySwitchPressedNoEdge");
        assertNotNull(safetySwitchPressedNoEdge);
        assertTrue(EcoreUtil.equals(safetySwitchPressed, safetySwitchPressedNoEdge.getSource()));
        assertTrue(EcoreUtil.equals(joinNode, safetySwitchPressedNoEdge.getTarget()));

        sysml.ControlFlow safetySwitchPressedReturningEdge = (sysml.ControlFlow) findElement(getSysMLView(), "safetySwitchPressedReturningEdge");
        assertNotNull(safetySwitchPressedReturningEdge);
        assertTrue(EcoreUtil.equals(shutOffEngine, safetySwitchPressedReturningEdge.getSource()));
        assertTrue(EcoreUtil.equals(finalNode, safetySwitchPressedReturningEdge.getTarget()));

        sysml.ControlFlow pedalTravelZeroYesEdge = (sysml.ControlFlow) findElement(getSysMLView(), "_DVpTcJJtEfGICs4ezpB-dw");
        assertNotNull(pedalTravelZeroYesEdge);
        assertTrue(EcoreUtil.equals(pedalTravelIsZero, pedalTravelZeroYesEdge.getSource()));
        assertTrue(EcoreUtil.equals(turnOffBrakeLight, pedalTravelZeroYesEdge.getTarget()));

        sysml.ControlFlow turnOffBrakeLightToFinalNodeEdge = (sysml.ControlFlow) findElement(getSysMLView(), "turnOffBrakeLightToFinalNodeEdge");
        assertNotNull(turnOffBrakeLightToFinalNodeEdge);
        assertTrue(EcoreUtil.equals(turnOffBrakeLight, turnOffBrakeLightToFinalNodeEdge.getSource()));
        assertTrue(EcoreUtil.equals(finalNode, turnOffBrakeLightToFinalNodeEdge.getTarget()));

        // Merge the result into the "main" branch for further use in following scenarios
        mergeManager.clearConflictResolutionProvider();
        mergeManager.setMergeEngine(createMergeEngine(null)); // don't resolve conflicts since none are expected

        branchManager.switchBranch("main");
        mergeResult = mergeManager.merge("ee2-add-safetySwitch-action");

        assertEquals(ModelMergeResult.MergeStatus.FAST_FORWARD, mergeResult.getStatus());
    }

    @Test
    @Order(2)
    void scenario2() throws Exception {
        // Project Manager (PM) renames the edge label
        branchManager.createBranch("pm-rename-edge", "main");
        branchManager.switchBranch("pm-rename-edge");

        modifyView(getSysMLView(), v -> {
            sysml.ControlFlow pushLessEdge = (sysml.ControlFlow) findElement(v, "_W6GnAJJsEfGICs4ezpB-dw");
            pushLessEdge.setName("[Push less or equal pressure] (pedalTravel -= Δp)");
        });
        commitAndWaitForChangelog("feat: clarify 'push less' edge condition", "pm-rename-edge");
        
        // SysML Engineer (SE) reconnects the 'Pull brake pads apart' node
        branchManager.switchBranch("main");
        branchManager.createBranch("se-reconnect-node", "main");
        branchManager.switchBranch("se-reconnect-node");

        modifyView(getSysMLView(), v -> {
            sysml.CallBehaviorAction pullBrakePadsApart = (sysml.CallBehaviorAction) findElement(v, "_H6bisJJsEfGICs4ezpB-dw");
            sysml.DecisionNode pedalTravelIsZero = (sysml.DecisionNode) findElement(v, "_kH1MkJJsEfGICs4ezpB-dw");
            sysml.JoinNode joinNode = (sysml.JoinNode) findElement(getSysMLView(), "_F8PiQJJtEfGICs4ezpB-dw");

            sysml.ControlFlow pushLessEdge = (sysml.ControlFlow) findElement(v, "_W6GnAJJsEfGICs4ezpB-dw");
            sysml.ControlFlow actionToDecisionNodeEdge = (sysml.ControlFlow) findElement(v, "_mzCOkJJsEfGICs4ezpB-dw");
            sysml.ControlFlow noEdge = (sysml.ControlFlow) findElement(v, "_W6GnAJJsEfGICs4ezpB-dw");

            pushLessEdge.setTarget(pedalTravelIsZero);
            noEdge.setTarget(pullBrakePadsApart);
            actionToDecisionNodeEdge.setTarget(joinNode);
        });
        commitAndWaitForChangelog("feat: move 'pull brake pads apart' node after decision node", "se-reconnect-node");

        // Merge the two branches; no conflict expected
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("pm-rename-edge");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Merge the result into "main" for further use
        branchManager.switchBranch("main");
        mergeResult = mergeManager.merge("se-reconnect-node");

        assertEquals(ModelMergeResult.MergeStatus.FAST_FORWARD, mergeResult.getStatus());
    }

    @Test
    @Order(3)
    void scenario3() throws Exception {
        // SysML Engineer (SE) deletes the 'Shut off engine' node
        branchManager.createBranch("se-delete-node", "main");
        branchManager.switchBranch("se-delete-node");

        modifyView(getSysMLView(), v -> {
            EcoreUtil.delete(findElement(v, "shutOffEngine"), true);
        });
        commitAndWaitForChangelog("feat: delete 'shut off engine' node", "se-delete-node");
        
        // Electrical Engineer (EE) deletes the 'Shut off engine' node and adds the new 'Send SafetySwitch pressed signal to ECU' node in its place
        branchManager.switchBranch("main");
        branchManager.createBranch("ee-replace-node", "main");
        branchManager.switchBranch("ee-replace-node");

        modifyView(getSysMLView(), v -> {
            // Exact same action like the SE
            EcoreUtil.delete(findElement(v, "shutOffEngine"), true);

            // Create and add the new node
            sysml.CallBehaviorAction sendSafetySwitchPressedSignalToEcu = SysmlFactory.eINSTANCE.createCallBehaviorAction();
            sendSafetySwitchPressedSignalToEcu.setName("Send SafetySwitch pressed signal to ECU");
            sendSafetySwitchPressedSignalToEcu.setId("sendSafetySwitchPressedSignalToEcu");

            sysml.Activity activity = (sysml.Activity) findElement(v, "_oCPuYJGVEfGICs4ezpB-dw");
            activity.getNodes().add(sendSafetySwitchPressedSignalToEcu);

            // Connect the new node by re-using the existing edges
            sysml.ControlFlow safetySwitchPressedYesEdge = (sysml.ControlFlow) findElement(v, "safetySwitchPressedYesEdge");
            sysml.ControlFlow safetySwitchPressedReturningEdge = (sysml.ControlFlow) findElement(v, "safetySwitchPressedReturningEdge");

            safetySwitchPressedYesEdge.setTarget(sendSafetySwitchPressedSignalToEcu);
            safetySwitchPressedReturningEdge.setSource(sendSafetySwitchPressedSignalToEcu);
        });
        commitAndWaitForChangelog("feat: replace 'shut off engine' node by a generic 'send to ECU' node", "ee-replace-node");

        // Merge the two branches; there is now a conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se-delete-node");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the merge conflict by accepting the changes from the EE
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllOurs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("se-delete-node");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Merge the result into "main" for further use
        branchManager.switchBranch("main");
        mergeResult = mergeManager.merge("ee-replace-node");

        assertEquals(ModelMergeResult.MergeStatus.FAST_FORWARD, mergeResult.getStatus());
    }

    @Test
    @Order(4)
    void scenario4() throws Exception {
        // SysML Engineer 1 (SE1) adds one recuperation node between the join node and 'center' decision node ('center node') where the
        // cycles split
        branchManager.createBranch("se1-recuperation", "main");
        branchManager.switchBranch("se1-recuperation");

        modifyView(getSysMLView(), v -> {
            // Create recuperation action node
            sysml.CallBehaviorAction recuperateEnergy = SysmlFactory.eINSTANCE.createCallBehaviorAction();
            recuperateEnergy.setName("Recuperate according to brake pressure");
            recuperateEnergy.setId("recuperateEnergy");

            // Insert the node between the join node and the center node
            sysml.ControlFlow joinNodeToCenterNodeEdge = (sysml.ControlFlow) findElement(v, "_PfQJkJJtEfGICs4ezpB-dw");
            joinNodeToCenterNodeEdge.setTarget(recuperateEnergy);

            sysml.DecisionNode centerNode = (sysml.DecisionNode) findElement(v, "_d_EUAJJrEfGICs4ezpB-dw");

            sysml.ControlFlow recuperateEnergyToCenterNodeEdge = SysmlFactory.eINSTANCE.createControlFlow();
            recuperateEnergyToCenterNodeEdge.setId("recuperateEnergyToCenterNodeEdge");
            recuperateEnergyToCenterNodeEdge.setSource(recuperateEnergy);
            recuperateEnergyToCenterNodeEdge.setTarget(centerNode);

            sysml.Activity activity = (sysml.Activity) findElement(v, "_oCPuYJGVEfGICs4ezpB-dw");
            activity.getNodes().add(recuperateEnergy);
            activity.getEdges().add(recuperateEnergyToCenterNodeEdge);
        });
        commitAndWaitForChangelog("feat: add energy recuperation action", "se1-recuperation");

        // SysML Engineer 2 (SE2) adds two recuperation nodes; one for each cycle right after the decision node and before the first other
        // action of the cycle
        branchManager.switchBranch("main");
        branchManager.createBranch("se2-recuperation", "main");
        branchManager.switchBranch("se2-recuperation");

        modifyView(getSysMLView(), v -> {
            // Create two recuperation action nodes
            sysml.CallBehaviorAction recuperateEnergyOnPressureIncrease = SysmlFactory.eINSTANCE.createCallBehaviorAction();
            recuperateEnergyOnPressureIncrease.setName("Recuperate according to brake pressure");
            recuperateEnergyOnPressureIncrease.setId("recuperateEnergyOnPressureIncrease");

            sysml.CallBehaviorAction recuperateEnergyOnPressureDecrease = SysmlFactory.eINSTANCE.createCallBehaviorAction();
            recuperateEnergyOnPressureDecrease.setName("Recuperate according to brake pressure");
            recuperateEnergyOnPressureDecrease.setId("recuperateEnergyOnPressureDecrease");

            // Connect pressure increase recuperation node
            sysml.ControlFlow pushMoreEdge = (sysml.ControlFlow) findElement(v, "_sL_FsJJrEfGICs4ezpB-dw");
            pushMoreEdge.setTarget(recuperateEnergyOnPressureIncrease);

            sysml.CallBehaviorAction pushBrakePadsTogether = (sysml.CallBehaviorAction) findElement(getSysMLView(), "_z543cJJpEfGICs4ezpB-dw");

            sysml.ControlFlow recuperateEnergyToPushBrakePadsTogetherEdge = SysmlFactory.eINSTANCE.createControlFlow();
            recuperateEnergyToPushBrakePadsTogetherEdge.setId("recuperateEnergyToPushBrakePadsTogetherEdge");
            recuperateEnergyToPushBrakePadsTogetherEdge.setSource(recuperateEnergyOnPressureIncrease);
            recuperateEnergyToPushBrakePadsTogetherEdge.setTarget(pushBrakePadsTogether);

            // Connect pressure decrease recuperation node
            sysml.ControlFlow pushLessEdge = (sysml.ControlFlow) findElement(v, "_W6GnAJJsEfGICs4ezpB-dw");
            pushLessEdge.setTarget(recuperateEnergyOnPressureDecrease);

            sysml.DecisionNode pedalTravelIsZero = (sysml.DecisionNode) findElement(v, "_kH1MkJJsEfGICs4ezpB-dw");

            sysml.ControlFlow recuperateEnergyToPedalTravelIsZeroEdge = SysmlFactory.eINSTANCE.createControlFlow();
            recuperateEnergyToPedalTravelIsZeroEdge.setId("recuperateEnergyToPedalTravelIsZeroEdge");
            recuperateEnergyToPedalTravelIsZeroEdge.setSource(recuperateEnergyOnPressureDecrease);
            recuperateEnergyToPedalTravelIsZeroEdge.setTarget(pedalTravelIsZero);

            // Add nodes and edges to the activity
            sysml.Activity activity = (sysml.Activity) findElement(v, "_oCPuYJGVEfGICs4ezpB-dw");
            activity.getNodes().addAll(List.of(recuperateEnergyOnPressureIncrease, recuperateEnergyOnPressureDecrease));
            activity.getEdges().addAll(List.of(recuperateEnergyToPushBrakePadsTogetherEdge, recuperateEnergyToPedalTravelIsZeroEdge));
        });
        commitAndWaitForChangelog("feat: add energy recuperation actions", "se2-recuperation");

        // Merge the two branches; no conflict expected, but three recuperation nodes are now present in the activity
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se1-recuperation");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Resolve the semantical conflict by undoing the changes made by SE1
        modifyView(getSysMLView(), v -> {
            // Re-connect the 'Push more' edge
            sysml.CallBehaviorAction pushBrakePadsTogether = (sysml.CallBehaviorAction) findElement(getSysMLView(), "_z543cJJpEfGICs4ezpB-dw");

            sysml.ControlFlow pushMoreEdge = (sysml.ControlFlow) findElement(v, "_sL_FsJJrEfGICs4ezpB-dw");
            pushMoreEdge.setTarget(pushBrakePadsTogether);

            // Re-connect the 'Push less or equal pressure' edge
            sysml.DecisionNode pedalTravelIsZero = (sysml.DecisionNode) findElement(v, "_kH1MkJJsEfGICs4ezpB-dw");

            sysml.ControlFlow pushLessEdge = (sysml.ControlFlow) findElement(v, "_W6GnAJJsEfGICs4ezpB-dw");
            pushLessEdge.setTarget(pedalTravelIsZero);

            // Delete the now unused nodes and edges
            EcoreUtil.delete(findElement(v, "recuperateEnergyOnPressureIncrease"), true);
            EcoreUtil.delete(findElement(v, "recuperateEnergyOnPressureDecrease"), true);
            EcoreUtil.delete(findElement(v, "recuperateEnergyToPushBrakePadsTogetherEdge"), true);
            EcoreUtil.delete(findElement(v, "recuperateEnergyToPedalTravelIsZeroEdge"), true);
        });
        commitAndWaitForChangelog("fix: remove double energy recuperation", "se2-recuperation");

        // Merge the result into "main" for further use
        branchManager.switchBranch("main");
        mergeResult = mergeManager.merge("se2-recuperation");

        assertEquals(ModelMergeResult.MergeStatus.FAST_FORWARD, mergeResult.getStatus());
    }

    @Test
    @Order(5)
    void scenario5() throws Exception {
        // SysML Engineer (SE) deletes all brake light nodes and reconnects the edges
        branchManager.createBranch("se-delete-brake-light", "main");
        branchManager.switchBranch("se-delete-brake-light");

        modifyView(getSysMLView(), v -> {
            // Delete nodes
            EcoreUtil.delete(findElement(v, "turnOnBrakeLight"), true);
            EcoreUtil.delete(findElement(v, "turnOffBrakeLight"), true);
            EcoreUtil.delete(findElement(v, "brakeLightOn"), true);

            // Reconnect and delete edges
            sysml.ControlFlow outgoingEdge = (sysml.ControlFlow) findElement(v, "_URUKYJJtEfGICs4ezpB-dw");
            outgoingEdge.setTarget((sysml.DecisionNode) findElement(v, "safetySwitchPressed"));

            sysml.ControlFlow pedalTravelZeroYesEdge = (sysml.ControlFlow) findElement(v, "_DVpTcJJtEfGICs4ezpB-dw");
            pedalTravelZeroYesEdge.setTarget((sysml.ActivityFinalNode) findElement(v, "_zkPQMJJsEfGICs4ezpB-dw")); // final node

            EcoreUtil.delete(findElement(v, "brakeLightOnYesEdge"), true);
            EcoreUtil.delete(findElement(v, "brakeLightOnNoEdge"), true);
            EcoreUtil.delete(findElement(v, "brakeLightOnToSafetySwitchPressedEdge"), true);
            EcoreUtil.delete(findElement(v, "turnOffBrakeLightToFinalNodeEdge"), true);
        });
        commitAndWaitForChangelog("feat: delete brake light nodes", "se-delete-brake-light");

        // Electrical Engineer (EE) renames the brake light actions
        branchManager.switchBranch("main");
        branchManager.createBranch("ee-rename-brake-light", "main");
        branchManager.switchBranch("ee-rename-brake-light");

        modifyView(getSysMLView(), v -> {
            sysml.CallBehaviorAction turnOnBrakeLight = (sysml.CallBehaviorAction) findElement(v, "turnOnBrakeLight");
            turnOnBrakeLight.setName("Send brake light on signal to ECU");
            turnOnBrakeLight.setId("sendBrakeLightOnSignalToEcu");

            sysml.CallBehaviorAction turnOffBrakeLight = (sysml.CallBehaviorAction) findElement(v, "turnOffBrakeLight");
            turnOffBrakeLight.setName("Send brake light off signal to ECU");
            turnOffBrakeLight.setId("sendBrakeLightOffSignalToEcu");
        });
        commitAndWaitForChangelog("feat: rename brake light nodes", "ee-rename-brake-light");

        // Merge the two branches; there is now a conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se-delete-brake-light");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the conflict by keeping the renamed nodes
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllOurs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("se-delete-brake-light");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Reconnect the kept nodes with the rest of the diagram
        modifyView(getSysMLView(), v -> {
            // Reconnect 'send on signal' node
            sysml.CallBehaviorAction sendBrakeLightOnSignalToEcu = (sysml.CallBehaviorAction) findElement(v, "sendBrakeLightOnSignalToEcu");
            sysml.DecisionNode safetySwitchPressed = (sysml.DecisionNode) findElement(v, "safetySwitchPressed");

            sysml.ControlFlow outgoingEdge = (sysml.ControlFlow) findElement(v, "_URUKYJJtEfGICs4ezpB-dw");
            outgoingEdge.setTarget(sendBrakeLightOnSignalToEcu);

            sysml.ControlFlow sendOnSignalToSafetySwitchPressedEdge = SysmlFactory.eINSTANCE.createControlFlow();
            sendOnSignalToSafetySwitchPressedEdge.setId("sendOnSignalToSafetySwitchPressedEdge");
            sendOnSignalToSafetySwitchPressedEdge.setSource(sendBrakeLightOnSignalToEcu);
            sendOnSignalToSafetySwitchPressedEdge.setTarget(safetySwitchPressed);

            sysml.Activity activity = (sysml.Activity) findElement(v, "_oCPuYJGVEfGICs4ezpB-dw");
            activity.getNodes().add(sendBrakeLightOnSignalToEcu);
        });
        commitAndWaitForChangelog("feat: reconnect renamed nodes", "ee-rename-brake-light");

        // Merge the result into "main" for further use
        branchManager.switchBranch("main");
        mergeResult = mergeManager.merge("ee-rename-brake-light");

        assertEquals(ModelMergeResult.MergeStatus.FAST_FORWARD, mergeResult.getStatus());
    }

    @Test
    @Order(6)
    void scenario6() throws Exception {
        // Prepare scenario: Add SafetySwitch block and give the "Send SafetySwitch pressed signal to ECU" action an activity which references this block
        modifyView(getSysMLView(), v -> {
            // Add SafetySwitch block; this creates a corresponding SafetySwitch namespace
            sysml.Block safetySwitch = SysmlFactory.eINSTANCE.createBlock();
            safetySwitch.setName("SafetySwitch");
            safetySwitch.setId("safetySwitch");

            sysml.Package structure = (sysml.Package) findElement(v, "_Hx7osJGVEfGICs4ezpB-dw");
            structure.getOwnedElements().add(safetySwitch);

            // Create the activity and give it to the action
            sysml.Activity sendEcuSignal = SysmlFactory.eINSTANCE.createActivity();
            sendEcuSignal.setName("SendEcuSignal");
            sendEcuSignal.setId("sendEcuSignal");
            sendEcuSignal.setContext(safetySwitch);

            sysml.Package activities = (sysml.Package) findElement(v, "_UUejQJGVEfGICs4ezpB-dw");
            activities.getOwnedElements().add(sendEcuSignal);

            sysml.CallBehaviorAction sendSafetySwitchPressedSignalToEcu = (sysml.CallBehaviorAction) findElement(v, "sendSafetySwitchPressedSignalToEcu");
            sendSafetySwitchPressedSignalToEcu.setBehavior(sendEcuSignal);
        });
        commitAndWaitForChangelog("feat: add SafetySwitch block and action reference", "main");

        // CAD Engineer (CE) deletes the SafetySwitch namespace
        branchManager.createBranch("ce-delete-safetySwitch-namespace", "main");
        branchManager.switchBranch("ce-delete-safetySwitch-namespace");

        modifyView(getCADView(), v -> {
            EcoreUtil.delete(findElement(v, "safetySwitch_reaction"), true);
        });
        commitAndWaitForChangelog("feat: remove SafetySwitch namespace", "ce-delete-safetySwitch-namespace");

        // Electrical Engineer (EE) changes the activity block reference and renames the action
        branchManager.switchBranch("main");
        branchManager.createBranch("ee-change-action", "main");
        branchManager.switchBranch("ee-change-action");

        modifyView(getSysMLView(), v -> {
            sysml.CallBehaviorAction sendSafetySwitchPressedSignalToEcu = (sysml.CallBehaviorAction) findElement(v, "sendSafetySwitchPressedSignalToEcu");

            sysml.Block brakeSystem = (sysml.Block) (sysml.Block) findElement(v, "_7tn3YJGWEfGICs4ezpB-dw");
            sendSafetySwitchPressedSignalToEcu.getBehavior().setContext(brakeSystem);

            sendSafetySwitchPressedSignalToEcu.setName("Send car shut-off signal to ECU");
        });
        commitAndWaitForChangelog("feat: change 'Send SafetySwitch pressed signal to ECU' action", "ee-change-action");

        // Merge the two branches; there is now a conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("ce-delete-safetySwitch-namespace");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the conflict by accepting the changes of the EE
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllOurs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("ce-delete-safetySwitch-namespace");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());
    }
}
