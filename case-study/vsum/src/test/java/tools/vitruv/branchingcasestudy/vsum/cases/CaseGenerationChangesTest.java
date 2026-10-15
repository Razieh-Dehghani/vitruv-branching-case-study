package tools.vitruv.branchingcasestudy.vsum.cases;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cad.CadFactory;
import sysml.SysmlFactory;
import tools.vitruv.framework.vsum.branch.MergeManager;
import tools.vitruv.framework.vsum.branch.data.ModelMergeResult;
import tools.vitruv.framework.vsum.branch.merge.ConflictResolutionProvider;
import tools.vitruv.branchingcasestudy.vsum.util.base.BranchAwareVSUMTestBase;
import tools.vitruv.branchingcasestudy.vsum.util.parser.CADParser;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This case focuses on the development changes that were made between the brake systems of the V1 and V2. There were three major
 * changes between those two versions of the car:
 * 
 * - Size reduction of the front brake discs
 * - Replacement of the front brake caliper: Change from a two-block caliper design (two parts that are screwed together) to a monobloc
 *   caliper design (one single piece)
 * - Inclusion of a third pressure sensor in the hydraulic system
 * 
 * The scenarios in this case are independent of prior scenarios and all assume the initial scenario as their starting point.

 */
public final class CaseGenerationChangesTest extends BranchAwareVSUMTestBase {
    /**
     * Set up the branching tool before each test. This deletes the (possibly) manipulated state from previous scenarios and allows
     * subsequent ones to use a freshly initialized branching tool.
     * 
     * @throws Exception on branching tool initialization errors
     */
    @BeforeEach
    void beforeEach(@TempDir Path tempDir, @TempDir Path vsumSeedingDir) throws Exception {
        setupBranching(tempDir);
        loadAndConsistModels("BrakeSystemModel/BrakeSystemModel.uml", "V1");
    }

    /**
     * Clean up after each test to reset the test class state.
     */
    @AfterEach
    void afterEach() {
        tearDown();
    }

    @Test
    void scenario1() throws Exception {
        // SysML Engineer (SE) changes the value of the 'diameter' attribute in the front disc block
        branchManager.createBranch("se-change-diameter", "main");
        branchManager.switchBranch("se-change-diameter");

        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty frontDiscDiameter = (sysml.ValueProperty) findElement(v, "_LFwdAJGtEfGICs4ezpB-dw");
            frontDiscDiameter.setDefaultValue("240.0");
        });
        commitAndWaitForChangelog("feat: change front brake disc diameter to new value", "se-change-diameter");

        // CAD Engineer (CE) exchanges the namespaces of the current front brake discs
        branchManager.switchBranch("main");
        branchManager.createBranch("ce-exchange-namespaces", "main");
        branchManager.switchBranch("ce-exchange-namespaces");

        // Load V2 CAD model for the namespaces of the new front brake discs
        cad.CAD_Model v2CadModel = new CADParser().parse("V2");

        modifyView(getGlobalView(), v -> {
            cad.Namespace flNew = v2CadModel.getNamespaces().stream().filter(n -> "P0196".equals(n.getId())).findFirst().get();
            cad.Namespace frNew = v2CadModel.getNamespaces().stream().filter(n -> "P0196_MIR".equals(n.getId())).findFirst().get();

            String frontDiscId = ((sysml.Block) findElement(v, "_7nNMsJGeEfGICs4ezpB-dw")).getId();

            userInteractor.setMCSSAnswer(1); // create / delete corresponding SysML block: no
            userInteractor.setTextAnswer(frontDiscId); // correspond added namespaces with the front disc block

            // Add the new namespaces and insert the correspondences
            cad.CAD_Model cadModel = (cad.CAD_Model) findElement(v, "vitruvCadModel");
            cadModel.getNamespaces().addAll(List.of(flNew, frNew));

            // Remove both old front discs
            EcoreUtil.delete(findElement(v, "P0035_ASM"), true);
            EcoreUtil.delete(findElement(v, "P0035_MIR_ASM"), true);
        });
        commitAndWaitForChangelog("feat: exchange CAD namespaces for front brake discs", "ce-exchange-namespaces");

        // Merge the two branches; no conflict expected
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se-change-diameter");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Check if the SysML block and the correct namespaces are there
        assertNotNull(findElement(getSysMLView(), "_7nNMsJGeEfGICs4ezpB-dw"));
        assertNotNull(findElement(getCADView(), "P0196"));
        assertNotNull(findElement(getCADView(), "P0196_MIR"));
        assertNull(findElement(getCADView(), "P0035_ASM"));
        assertNull(findElement(getCADView(), "P0035_MIR_ASM"));
    }

    @Test
    void scenario2() throws Exception {
        // SysML Engineer (SE) renamed the front disc block
        branchManager.createBranch("se-rename", "main");
        branchManager.switchBranch("se-rename");

        modifyView(getSysMLView(), v -> {
            sysml.Block frontDisc = (sysml.Block) findElement(v, "_7nNMsJGeEfGICs4ezpB-dw");
            frontDisc.setName("VentedFrontDisc");
        });
        commitAndWaitForChangelog("feat: rename front caliper block", "se-rename");

        // CAD Engineer (CE) deletes the namespaces of the front discs, then adds the new ones
        branchManager.switchBranch("main");
        branchManager.createBranch("ce-change-namespaces", "main");
        branchManager.switchBranch("ce-change-namespaces");

        // Load V2 CAD model for the namespaces of the new front brake discs
        cad.CAD_Model v2CadModel = new CADParser().parse("V2");

        modifyView(getCADView(), v -> {
            // Add the two new front discs
            cad.CAD_Model cadModel = (cad.CAD_Model) findElement(v, "vitruvCadModel");

            cad.Namespace flNew = v2CadModel.getNamespaces().stream().filter(n -> "P0196".equals(n.getId())).findFirst().get();
            cad.Namespace frNew = v2CadModel.getNamespaces().stream().filter(n -> "P0196_MIR".equals(n.getId())).findFirst().get();
            cadModel.getNamespaces().addAll(List.of(flNew, frNew));

            // Remove both old front discs (but also delete the SysML block by not setting the user interactor)
            EcoreUtil.delete(findElement(v, "P0035_ASM"), true);
            EcoreUtil.delete(findElement(v, "P0035_MIR_ASM"), true);
        });
        commitAndWaitForChangelog("feat: exchange front disc namespaces", "ce-change-namespaces");

        // Merge the two branches; there is now a merge conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se-rename");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the conflict by keeping the renamed front disc
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllTheirs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("se-rename");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());
    }

    @Test
    void scenario3() throws Exception {
        // Project manager (PM) adds collection block and three sensor blocks to SysML model
        branchManager.createBranch("pm-add-sensors", "main");
        branchManager.switchBranch("pm-add-sensors");

        modifyView(getSysMLView(), v -> {
            // Create sensor blocks
            sysml.Block sensors = SysmlFactory.eINSTANCE.createBlock();
            sensors.setName("Sensors");
            sensors.setId("sensors");

            // Assumption here: The functional pressure sensors are both at the front located near the front axle, the validation sensor is in the
            // center of the chassis; differentiate between the functional sensors (A and B)
            sysml.Block frontSensorA = SysmlFactory.eINSTANCE.createBlock();
            frontSensorA.setName("FrontSensorA");
            frontSensorA.setId("frontSensorA");

            sysml.Block frontSensorB = SysmlFactory.eINSTANCE.createBlock();
            frontSensorB.setName("FrontSensorB");
            frontSensorB.setId("frontSensorB");

            sysml.Block chassisCenterSensor = SysmlFactory.eINSTANCE.createBlock();
            chassisCenterSensor.setName("ChassisCenterSensor");
            chassisCenterSensor.setId("chassisCenterSensor");

            // Link up the sensor blocks
            sysml.Composition sensors2FrontSensorA = SysmlFactory.eINSTANCE.createComposition();
            sensors2FrontSensorA.setName("frontSensorA");
            sensors2FrontSensorA.setId("sensors2FrontSensorA");
            sensors2FrontSensorA.setSource(sensors);
            sensors2FrontSensorA.setTarget(frontSensorA);

            sysml.Composition sensors2FrontSensorB = SysmlFactory.eINSTANCE.createComposition();
            sensors2FrontSensorB.setName("frontSensorB");
            sensors2FrontSensorB.setId("sensors2FrontSensorB");
            sensors2FrontSensorB.setSource(sensors);
            sensors2FrontSensorB.setTarget(frontSensorB);

            sysml.Composition sensors2ChassisCenterSensor = SysmlFactory.eINSTANCE.createComposition();
            sensors2ChassisCenterSensor.setName("chassisCenterSensor");
            sensors2ChassisCenterSensor.setId("sensors2ChassisCenterSensor");
            sensors2ChassisCenterSensor.setSource(sensors);
            sensors2ChassisCenterSensor.setTarget(chassisCenterSensor);

            // Add the elements to the structure package
            sysml.Package structure = (sysml.Package) findElement(v, "_Hx7osJGVEfGICs4ezpB-dw");
            structure.getOwnedElements().addAll(List.of(
                sensors,
                frontSensorA,
                frontSensorB,
                chassisCenterSensor,
                sensors2FrontSensorA,
                sensors2FrontSensorB,
                sensors2ChassisCenterSensor
            ));

            // Add a composition from the brake system block to the sensors block
            sysml.Block brakeSystem = (sysml.Block) findElement(v, "_7tn3YJGWEfGICs4ezpB-dw");

            sysml.Composition brakeSystem2Sensors = SysmlFactory.eINSTANCE.createComposition();
            brakeSystem2Sensors.setName("sensors");
            brakeSystem2Sensors.setId("brakeSystem2Sensors");
            brakeSystem2Sensors.setSource(brakeSystem);
            brakeSystem2Sensors.setTarget(sensors);

            structure.getOwnedElements().add(brakeSystem2Sensors);
        });
        commitAndWaitForChangelog("feat: add blocks for sensors", "pm-add-sensors");

        // Electrical Engineer (EE) adds collection block and two sensor blocks to SysML model
        branchManager.switchBranch("main");
        branchManager.createBranch("ee-add-sensors", "main");
        branchManager.switchBranch("ee-add-sensors");

        modifyView(getSysMLView(), v -> {
            // Create sensor blocks
            sysml.Block pressureSensors = SysmlFactory.eINSTANCE.createBlock();
            pressureSensors.setName("PressureSensors");
            pressureSensors.setId("pressureSensors");

            sysml.Block functionalSensor = SysmlFactory.eINSTANCE.createBlock();
            functionalSensor.setName("FunctionalSensor");
            functionalSensor.setId("functionalSensor");

            sysml.Block validationSensor = SysmlFactory.eINSTANCE.createBlock();
            validationSensor.setName("ValidationSensor");
            validationSensor.setId("validationSensor");

            // Link up the sensor blocks
            sysml.Composition pressureSensors2FunctionalSensor = SysmlFactory.eINSTANCE.createComposition();
            pressureSensors2FunctionalSensor.setName("functionalSensor");
            pressureSensors2FunctionalSensor.setId("pressureSensors2FunctionalSensor");
            pressureSensors2FunctionalSensor.setSource(pressureSensors);
            pressureSensors2FunctionalSensor.setTarget(functionalSensor);

            sysml.Composition pressureSensors2ValidationSensor = SysmlFactory.eINSTANCE.createComposition();
            pressureSensors2ValidationSensor.setName("validationSensor");
            pressureSensors2ValidationSensor.setId("pressureSensors2ValidationSensor");
            pressureSensors2ValidationSensor.setSource(pressureSensors);
            pressureSensors2ValidationSensor.setTarget(validationSensor);

            // Add the elements to the structure package
            sysml.Package structure = (sysml.Package) findElement(v, "_Hx7osJGVEfGICs4ezpB-dw");
            structure.getOwnedElements().addAll(List.of(
                pressureSensors,
                functionalSensor,
                validationSensor,
                pressureSensors2FunctionalSensor,
                pressureSensors2ValidationSensor
            ));

            // Add a composition from the brake system block to the pressure sensors block
            sysml.Block brakeSystem = (sysml.Block) findElement(v, "_7tn3YJGWEfGICs4ezpB-dw");

            sysml.Composition brakeSystem2PressureSensors = SysmlFactory.eINSTANCE.createComposition();
            brakeSystem2PressureSensors.setName("pressureSensors");
            brakeSystem2PressureSensors.setId("brakeSystem2PressureSensors");
            brakeSystem2PressureSensors.setSource(brakeSystem);
            brakeSystem2PressureSensors.setTarget(pressureSensors);

            structure.getOwnedElements().add(brakeSystem2PressureSensors);
        });
        commitAndWaitForChangelog("feat: add sensor blocks", "ee-add-sensors");

        // Merge the two branches; no conflict expected, but now there are 7 different sensor blocks in the model
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("pm-add-sensors");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        assertNotNull(findElement(getSysMLView(), "sensors"));
        assertNotNull(findElement(getSysMLView(), "frontSensorA"));
        assertNotNull(findElement(getSysMLView(), "frontSensorB"));
        assertNotNull(findElement(getSysMLView(), "chassisCenterSensor"));
        assertNotNull(findElement(getSysMLView(), "pressureSensors"));
        assertNotNull(findElement(getSysMLView(), "functionalSensor"));
        assertNotNull(findElement(getSysMLView(), "validationSensor"));

        // Fix the semantical conflict by choosing the block design by the EE and renaming the collection block
        modifyView(getSysMLView(), v -> {
            // Remove PM sensor blocks
            EcoreUtil.delete(findElement(v, "sensors"), true);
            EcoreUtil.delete(findElement(v, "frontSensorA"), true);
            EcoreUtil.delete(findElement(v, "frontSensorB"), true);
            EcoreUtil.delete(findElement(v, "chassisCenterSensor"), true);
            EcoreUtil.delete(findElement(v, "sensors2FrontSensorA"), true);
            EcoreUtil.delete(findElement(v, "sensors2FrontSensorB"), true);
            EcoreUtil.delete(findElement(v, "sensors2ChassisCenterSensor"), true);
            EcoreUtil.delete(findElement(v, "brakeSystem2Sensors"), true);

            // Rename collection block and according composition
            sysml.Block pressureSensors = (sysml.Block) findElement(v, "pressureSensors");
            pressureSensors.setName("BrakePressureSensors");
            pressureSensors.setId("brakePressureSensors");

            sysml.Composition brakeSystem2PressureSensors = (sysml.Composition) findElement(v, "brakeSystem2PressureSensors");
            brakeSystem2PressureSensors.setName("brakePressureSensors");
            brakeSystem2PressureSensors.setId("brakeSystem2BrakePressureSensors");

            sysml.Composition pressureSensors2FunctionalSensor = (sysml.Composition) findElement(v, "pressureSensors2FunctionalSensor");
            pressureSensors2FunctionalSensor.setId("brakePressureSensors2FunctionalSensor");

            sysml.Composition pressureSensors2ValidationSensor = (sysml.Composition) findElement(v, "pressureSensors2ValidationSensor");
            pressureSensors2ValidationSensor.setId("brakePressureSensors2ValidationSensor");
        });
        commitAndWaitForChangelog("fix: resolve semantical sensor block conflict", "ee-add-sensors");

        // Now, there are only 3 types of sensor blocks in the desired form
        assertNull(findElement(getSysMLView(), "sensors"));
        assertNull(findElement(getSysMLView(), "frontSensorA"));
        assertNull(findElement(getSysMLView(), "frontSensorB"));
        assertNull(findElement(getSysMLView(), "chassisCenterSensor"));

        assertNull(findElement(getSysMLView(), "brakeSystem2Sensors"));
        assertNull(findElement(getSysMLView(), "sensors2FrontSensorA"));
        assertNull(findElement(getSysMLView(), "sensors2FrontSensorB"));
        assertNull(findElement(getSysMLView(), "sensors2ChassisCenterSensor"));

        assertNull(findElement(getSysMLView(), "pressureSensors"));
        assertNotNull(findElement(getSysMLView(), "brakePressureSensors"));

        assertNotNull(findElement(getSysMLView(), "functionalSensor"));
        assertNotNull(findElement(getSysMLView(), "validationSensor"));

        assertNull(findElement(getSysMLView(), "brakeSystem2PressureSensors"));
        assertNotNull(findElement(getSysMLView(), "brakeSystem2BrakePressureSensors"));
        assertNull(findElement(getSysMLView(), "pressureSensors2FunctionalSensor"));
        assertNotNull(findElement(getSysMLView(), "brakePressureSensors2FunctionalSensor"));
        assertNull(findElement(getSysMLView(), "pressureSensors2ValidationSensor"));
        assertNotNull(findElement(getSysMLView(), "brakePressureSensors2ValidationSensor"));
    }

    @Test
    void scenario4() throws Exception {
        // Project manager (PM) changes the name of the 'BrakeSystem' block
        branchManager.createBranch("pm-rename", "main");
        branchManager.switchBranch("pm-rename");

        modifyView(getSysMLView(), v -> {
            sysml.Block brakeSystem = (sysml.Block) findElement(v, "_7tn3YJGWEfGICs4ezpB-dw");
            brakeSystem.setName("V2BrakeSystem");
        });
        commitAndWaitForChangelog("feat: rename brake system", "pm-rename");

        // SysML Engineer (SE) changes the name of the 'BrakeSystem' block
        branchManager.switchBranch("main");
        branchManager.createBranch("se-rename", "main");
        branchManager.switchBranch("se-rename");

        modifyView(getSysMLView(), v -> {
            sysml.Block brakeSystem = (sysml.Block) findElement(v, "_7tn3YJGWEfGICs4ezpB-dw");
            brakeSystem.setName("BrakeSystemV2");
        });
        commitAndWaitForChangelog("feat: rename brake system", "se-rename");

        // Merge the two branches; there is now a merge conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("pm-rename");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the merge conflict by selecting the name change from the 'pm-rename' branch
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllTheirs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("pm-rename");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Check whether the name is now 'V2BrakeSystem'
        assertEquals("V2BrakeSystem", ((sysml.Block) findElement(getSysMLView(), "_7tn3YJGWEfGICs4ezpB-dw")).getName());
    }

    @Test
    void scenario5() throws Exception {
        // Add 'diameter' numeric parameter to the CAD namespace corresponding to the front disc SysML blocks
        modifyView(getCADView(), v -> {
            cad.Namespace flDisc = (cad.Namespace) findElement(v, "P0035_ASM");
            cad.Namespace frDisc = (cad.Namespace) findElement(v, "P0035_MIR_ASM");

            cad.NumericParameter flDiscDiameter = CadFactory.eINSTANCE.createNumericParameter();
            flDiscDiameter.setName("diameter");
            flDiscDiameter.setId("P0035_ASM.diameter");
            flDisc.getParameters().add(flDiscDiameter);

            cad.NumericParameter frDiscDiameter = CadFactory.eINSTANCE.createNumericParameter();
            frDiscDiameter.setName("diameter");
            frDiscDiameter.setId("P0035_MIR_ASM.diameter");
            frDisc.getParameters().add(frDiscDiameter);
        });
        commitAndWaitForChangelog("feat: add CAD parameters corresponding to SysML 'diameter' value property", "main");

        // SysML Engineer (SE) changes the diameter value property to 200.0 (the wrong value)
        branchManager.createBranch("se-change-diameter", "main");
        branchManager.switchBranch("se-change-diameter");

        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty diameter = (sysml.ValueProperty) findElement(v, "_LFwdAJGtEfGICs4ezpB-dw");
            diameter.setDefaultValue("245.0");
        });
        commitAndWaitForChangelog("feat: change diameter in front disc block", "se-change-diameter");

        // CAD Engineer (CE) changes the diameter parameter of one of the front discs to 199.0 (the correct value)
        branchManager.switchBranch("main");
        branchManager.createBranch("ce-change-diameter", "main");
        branchManager.switchBranch("ce-change-diameter");

        modifyView(getCADView(), v -> {
            // Arbitrarily choose the front left disc parameter; the change should propagate through to the other diameter parameter
            cad.NumericParameter diameter = (cad.NumericParameter) findElement(v, "P0035_ASM.diameter");
            diameter.setValue(240.f);
        });
        commitAndWaitForChangelog("feat: change diameter in front disc namespaces", "ce-change-diameter");

        // Merge the two branches; there is now a merge conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se-change-diameter");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the conflict by accepting the CE changes
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllOurs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("se-change-diameter");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // All parameters now have the correct value
        assertEquals("240.0", ((sysml.ValueProperty) findElement(getSysMLView(), "_LFwdAJGtEfGICs4ezpB-dw")).getDefaultValue());
        assertEquals(240.f, ((cad.NumericParameter) findElement(getCADView(), "P0035_ASM.diameter")).getValue());
        assertEquals(240.f, ((cad.NumericParameter) findElement(getCADView(), "P0035_MIR_ASM.diameter")).getValue());
    }

    @Test
    void scenario6() throws Exception {
        // SysML Engineer (SE) adds a pressure sensor block
        branchManager.createBranch("se-add-pressure-sensor", "main");
        branchManager.switchBranch("se-add-pressure-sensor");

        modifyView(getSysMLView(), v -> {
            // Create the pressure sensor
            sysml.Block pressureSensor = SysmlFactory.eINSTANCE.createBlock();
            pressureSensor.setName("PressureSensor");
            pressureSensor.setId("pressureSensor");

            // Add the sensor to the structure package
            sysml.Package structure = (sysml.Package) findElement(v, "_Hx7osJGVEfGICs4ezpB-dw");
            structure.getOwnedElements().add(pressureSensor);

            // Add a composition between the brake system block and the pressure sensor block
            sysml.Block brakeSystem = (sysml.Block) findElement(v, "_7tn3YJGWEfGICs4ezpB-dw");

            sysml.Composition brakeSystem2PressureSensor = SysmlFactory.eINSTANCE.createComposition();
            brakeSystem2PressureSensor.setName("pressureSensor");
            brakeSystem2PressureSensor.setId("brakeSystem2PressureSensor");
            brakeSystem2PressureSensor.setSource(brakeSystem);
            brakeSystem2PressureSensor.setTarget(pressureSensor);

            structure.getOwnedElements().add(brakeSystem2PressureSensor);
        });
        commitAndWaitForChangelog("feat: add pressure sensor block", "se-add-pressure-sensor");

        // CAD Engineer (CE) adds a pressure sensor name and chooses to create a corresponding SysML block
        branchManager.switchBranch("main");
        branchManager.createBranch("ce-add-pressure-sensor", "main");
        branchManager.switchBranch("ce-add-pressure-sensor");

        modifyView(getCADView(), v -> {
            // Create pressure sensor namespace; skip creation of attribute parameters (size, surfaceArea etc.) since it is not needed for
            // this scenario and would be made up data
            cad.Namespace pressureSensor = CadFactory.eINSTANCE.createNamespace();
            pressureSensor.setName("PRESSURE_SENSOR_ASM");
            pressureSensor.setId("PRESSURE_SENSOR_ASM");

            cad.CAD_Model cadModel = (cad.CAD_Model) findElement(v, "vitruvCadModel");
            userInteractor.setMCSSAnswer(0); // create corresponding SysML block: yes
            cadModel.getNamespaces().add(pressureSensor);
        });
        commitAndWaitForChangelog("feat: add pressure sensor namespace", "ce-add-pressure-sensor");

        // Merge the two branches; no conflict expected, but there are now 2 sensor elements in each model
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se-add-pressure-sensor");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        assertNotNull(findElement(getSysMLView(), "pressureSensor"));
        assertNotNull(findElement(getCADView(), "pressureSensor_reaction"));
        assertNotNull(findElement(getCADView(), "PRESSURE_SENSOR_ASM"));
        assertNotNull(findElement(getSysMLView(), "PRESSURE_SENSOR_ASM_reaction"));

        // Resolve the conflict by deleting the created elements and adding new ones with the data from the respective engineer
        modifyView(getGlobalView(), v -> {
            cad.Namespace createdSensor = EcoreUtil.copy((cad.Namespace) findElement(v, "PRESSURE_SENSOR_ASM"));

            // Modify the namespace created by inserting the pressure sensor block
            cad.Namespace reactionSensor = (cad.Namespace) findElement(v, "pressureSensor_reaction");
            reactionSensor.setName(createdSensor.getName());
            reactionSensor.getParameters().addAll(createdSensor.getParameters());

            // Delete the elements inserted by the CE
            EcoreUtil.delete(findElement(v, "PRESSURE_SENSOR_ASM"), true);
            EcoreUtil.delete(findElement(v, "PRESSURE_SENSOR_ASM_reaction"), true);

            // Change ID here to not accidentally delete the wrong namespace
            reactionSensor.setId(createdSensor.getId());
        });

        // Now, there is only one set of pressure sensors left
        assertNotNull(findElement(getSysMLView(), "pressureSensor"));
        assertNull(findElement(getCADView(), "pressureSensor_reaction"));
        assertNotNull(findElement(getCADView(), "PRESSURE_SENSOR_ASM"));
        assertNull(findElement(getSysMLView(), "PRESSURE_SENSOR_ASM_reaction"));
    }

    @Test
    void scenario7() throws Exception {
        // SysML Engineer (SE) deletes the caliper block and adds the new front and rear caliper blocks
        branchManager.createBranch("se-replace-caliper-blocks", "main");
        branchManager.switchBranch("se-replace-caliper-blocks");

        modifyView(getGlobalView(), v -> {
            // Create and insert new caliper blocks
            sysml.Block frontCaliper = SysmlFactory.eINSTANCE.createBlock();
            frontCaliper.setName("FrontCaliper");
            frontCaliper.setId("frontCaliper");

            sysml.Block rearCaliper = SysmlFactory.eINSTANCE.createBlock();
            rearCaliper.setName("RearCaliper");
            rearCaliper.setId("rearCaliper");

            sysml.Package structure = (sysml.Package) findElement(v, "_Hx7osJGVEfGICs4ezpB-dw");
            structure.getOwnedElements().addAll(List.of(frontCaliper, rearCaliper));

            // Reconnect compositions in the BDD
            sysml.Association frontBrakeAssembly2Caliper = (sysml.Association) findElement(v, "_DvEOMJGrEfGICs4ezpB-dw");
            frontBrakeAssembly2Caliper.setTarget(frontCaliper);

            sysml.Association rearBrakeAssembly2Caliper = (sysml.Association) findElement(v, "_EhHWMJGrEfGICs4ezpB-dw");
            rearBrakeAssembly2Caliper.setTarget(rearCaliper);

            sysml.Association frontCaliper2CaliperBlock = (sysml.Association) findElement(v, "_QkIcIJGrEfGICs4ezpB-dw"); // reuse existing edge
            frontCaliper2CaliperBlock.setSource(frontCaliper);

            sysml.Association rearCaliper2CaliperBlock = SysmlFactory.eINSTANCE.createAssociation();
            rearCaliper2CaliperBlock.setName("caliperBlock");
            rearCaliper2CaliperBlock.setId("rearCaliper2CaliperBlock");
            rearCaliper2CaliperBlock.setSource(rearCaliper);
            rearCaliper2CaliperBlock.setTarget((sysml.Block) findElement(v, "_AebaUJGfEfGICs4ezpB-dw")); // to 'CaliperBlock'

            structure.getOwnedElements().add(rearCaliper2CaliperBlock);

            // Correspond new blocks with the existing namespaces by re-inserting them into the CAD model
            cad.CAD_Model cadModel = (cad.CAD_Model) findElement(v, "vitruvCadModel");
            for (String id : List.of("P0003_ASM", "P0003_MIR_ASM", "P0038_ASM", "P0038_MIR_ASM")) {
                cad.Namespace n = (cad.Namespace) findElement(v, id);
                cad.Namespace copy = EcoreUtil.copy(n);

                EcoreUtil.delete(n, true);

                userInteractor.setTextAnswer(id.contains("P0003") ? "frontCaliper" : "rearCaliper");
                cadModel.getNamespaces().add(copy);
            }

            // Delete the existing caliper block
            userInteractor.setMCSSAnswer(1); // delete corresponding namespaces: no
            EcoreUtil.delete(findElement(v, "__RlMsJGeEfGICs4ezpB-dw"), true);
        });
        commitAndWaitForChangelog("feat: replace caliper block by front and rear caliper blocks", "se-replace-caliper-blocks");

        // CAD Engineer (CE) replaces the caliper namespaces by those of the V2
        branchManager.switchBranch("main");
        branchManager.createBranch("ce-replace-calipers", "main");
        branchManager.switchBranch("ce-replace-calipers");

        // Load V2 CAD model for the namespaces of the new calipers
        cad.CAD_Model v2CadModel = new CADParser().parse("V2");

        modifyView(getCADView(), v -> {
            // Insert and correspond new namespaces
            cad.Namespace flNew = v2CadModel.getNamespaces().stream().filter(n -> "P0197_ASM".equals(n.getId())).findFirst().get();
            cad.Namespace frNew = v2CadModel.getNamespaces().stream().filter(n -> "P0197_MIR_ASM".equals(n.getId())).findFirst().get();
            cad.Namespace rlNew = v2CadModel.getNamespaces().stream().filter(n -> "P0246_ASM".equals(n.getId())).findFirst().get();
            cad.Namespace rrNew = v2CadModel.getNamespaces().stream().filter(n -> "P0246_MIR_ASM".equals(n.getId())).findFirst().get();

            cad.CAD_Model cadModel = (cad.CAD_Model) findElement(v, "vitruvCadModel");

            userInteractor.setTextAnswer("__RlMsJGeEfGICs4ezpB-dw"); // correspond with: caliper block
            cadModel.getNamespaces().addAll(List.of(flNew, frNew, rlNew, rrNew));

            // Delete existing namespaces
            userInteractor.setMCSSAnswer(1); // delete corresponding block: no
            for (String id : List.of("P0003_ASM", "P0003_MIR_ASM", "P0038_ASM", "P0038_MIR_ASM")) {
                EcoreUtil.delete(findElement(v, id), true);
            }
        });
        commitAndWaitForChangelog("feat: exchange caliper namespaces", "ce-replace-calipers");

        // Merge the two branches; There is now a conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se-replace-caliper-blocks");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the conflict by using the namespaces from the CE
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllOurs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("se-replace-caliper-blocks");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Delete the old namespaces; due to the re-insertion, those are now also present in the model
        modifyView(getCADView(), v -> {
            userInteractor.setMCSSAnswer(1); // delete corresponding block: no
            for (String id : List.of("P0003_ASM", "P0003_MIR_ASM", "P0038_ASM", "P0038_MIR_ASM")) {
                EcoreUtil.delete(findElement(v, id), true);
            }
        });

        // There are now only the correct elements in the VSUM
        assertNull(findElement(getCADView(), "P0003_ASM"));
        assertNull(findElement(getCADView(), "P0003_MIR_ASM"));
        assertNull(findElement(getCADView(), "P0038_ASM"));
        assertNull(findElement(getCADView(), "P0038_MIR_ASM"));

        assertNotNull(findElement(getCADView(), "P0197_ASM"));
        assertNotNull(findElement(getCADView(), "P0197_MIR_ASM"));
        assertNotNull(findElement(getCADView(), "P0246_ASM"));
        assertNotNull(findElement(getCADView(), "P0246_MIR_ASM"));

        assertNull(findElement(getSysMLView(), "__RlMsJGeEfGICs4ezpB-dw"));
        assertNotNull(findElement(getSysMLView(), "frontCaliper"));
        assertNotNull(findElement(getSysMLView(), "rearCaliper"));
    }

    @Test
    void scenario8() throws Exception {
        // SysML Engineer 1 (SE1) deletes the caliper block SysML block
        branchManager.createBranch("se1-delete-block", "main");
        branchManager.switchBranch("se1-delete-block");

        modifyView(getSysMLView(), v -> {
            // Remove the block and the composition towards it
            userInteractor.setMCSSAnswer(1); // delete corresponding namespaces: no (makes things easier, also CAD is not relevant here)
            EcoreUtil.delete(findElement(v, "_AebaUJGfEfGICs4ezpB-dw"), true);
            EcoreUtil.delete(findElement(v, "_QkJDMpGrEfGICs4ezpB-dw"), true);
        });
        commitAndWaitForChangelog("feat: delete caliper block SysML block", "se1-delete-block");

        // SysML Engineer 2 (SE2) adds a value property to the caliper block SysML block
        branchManager.switchBranch("main");
        branchManager.createBranch("se2-add-property", "main");
        branchManager.switchBranch("se2-add-property");

        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty type = SysmlFactory.eINSTANCE.createValueProperty();
            type.setName("type");
            type.setId("caliperBlockType");
            type.setDefaultValue("MONOBLOC");

            sysml.Block caliperBlock = (sysml.Block) findElement(v, "_AebaUJGfEfGICs4ezpB-dw");
            caliperBlock.getValues().add(type);
        });
        commitAndWaitForChangelog("feat: add monobloc value property", "se2-add-property");

        // Merge the two branches; there is now a conflict
        MergeManager mergeManager = new MergeManager(tempDir);
        mergeManager.suppressTriggerFile();
        mergeManager.setPostMergeReload(() -> vsum.reload());
        mergeManager.setMergeEngine(createMergeEngine(null));
        ModelMergeResult mergeResult = mergeManager.merge("se1-delete-block");

        assertEquals(ModelMergeResult.MergeStatus.CONFLICTING, mergeResult.getStatus());

        // Resolve the conflict by accepting the changes from SE2
        ConflictResolutionProvider conflictResolutionProvider = ConflictResolutionProvider.chooseAllOurs();
        mergeManager.setMergeEngine(createMergeEngine(conflictResolutionProvider));
        mergeManager.setConflictResolutionProvider(conflictResolutionProvider);

        mergeResult = mergeManager.merge("se1-delete-block");

        assertEquals(ModelMergeResult.MergeStatus.SUCCESS, mergeResult.getStatus());

        // Re-insert the composition from 'Caliper' to 'CaliperBlock' since this was deleted without creating a conflict
        modifyView(getSysMLView(), v -> {
            sysml.Block caliper = (sysml.Block) findElement(v, "__RlMsJGeEfGICs4ezpB-dw");
            sysml.Block caliperBlock = (sysml.Block) findElement(v, "_AebaUJGfEfGICs4ezpB-dw");

            sysml.Composition caliper2CaliperBlock = SysmlFactory.eINSTANCE.createComposition();
            caliper2CaliperBlock.setName("caliperBlock");
            caliper2CaliperBlock.setId("caliper2CaliperBlock");
            caliper2CaliperBlock.setSource(caliper);
            caliper2CaliperBlock.setTarget(caliperBlock);

            sysml.Package structure = (sysml.Package) findElement(v, "_Hx7osJGVEfGICs4ezpB-dw");
            structure.getOwnedElements().add(caliper2CaliperBlock);
        });

        // The caliper block SysML block is there and now has the value property
        sysml.Block caliperBlock = (sysml.Block) findElement(getSysMLView(), "_AebaUJGfEfGICs4ezpB-dw");
        assertNotNull(caliperBlock);

        sysml.ValueProperty caliperBlockType = caliperBlock.getValues().get(0);
        assertEquals("type", caliperBlockType.getName());
        assertEquals("caliperBlockType", caliperBlockType.getId());
        assertEquals("MONOBLOC", caliperBlockType.getDefaultValue());
    }
}
