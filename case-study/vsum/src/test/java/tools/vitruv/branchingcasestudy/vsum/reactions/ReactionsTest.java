package tools.vitruv.branchingcasestudy.vsum.reactions;

import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cad.CadFactory;
import sysml.SysmlFactory;

import tools.vitruv.branchingcasestudy.vsum.util.base.BranchAwareVSUMTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class contains tests for the different reactions between the SysML and CAD models. The reactions are tested to ensure that they
 * have the desired effect on the VSUM structure.
 */
public final class ReactionsTest extends BranchAwareVSUMTestBase {
    /**
     * Set up the branching tool before each test. This deletes the (possibly) manipulated state from previous scenarios and allows
     * subsequent ones to use a freshly initialized branching tool.
     * 
     * @throws Exception on branching tool initialization errors
     */
    @BeforeEach
    void beforeEach(@TempDir Path tempDir) throws Exception {
        setupBranching(tempDir);
    }

    /**
     * Clean up the branching tool and all object fields after each test to reset the test class state.
     */
    @AfterEach
    void afterEach() {
        tearDown();
    }

    /**
     * A SysML `Model` is created in an empty VSUM.
     * 
     * As a reaction, a corresponding `CAD_Model` is created.
     */
    @Test
    void sysmlModelCreated() {
        // There are no SysML and CAD models present
        assertEquals(0, getSysMLView().getRootObjects(sysml.Model.class).size());
        assertEquals(0, getCADView().getRootObjects(cad.CAD_Model.class).size());

        // Create the SysML model and register it as root
        modifyView(getSysMLView(), v -> {
            v.registerRoot(SysmlFactory.eINSTANCE.createModel(), URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // A corresponding CAD model has been created
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class).size());
    }

    /**
     * A `CAD_Model` is created in an empty VSUM.
     * 
     * As a reaction, a corresponding SysML `Model` is created.
     */
    @Test
    void cadModelCreated() {
        // There are no SysML and CAD models present
        assertEquals(0, getSysMLView().getRootObjects(sysml.Model.class).size());
        assertEquals(0, getCADView().getRootObjects(cad.CAD_Model.class).size());

        // Create the CAD model and register it as root
        modifyView(getCADView(), v -> {
            v.registerRoot(CadFactory.eINSTANCE.createCAD_Model(), URI.createFileURI(tempDir.toString() + "/cad.model"));
        });

        // A corresponding SysML model has been created
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class).size());
    }

    /**
     * A SysML `Block` is created, but not made corresponding to a CAD `Namespace`.
     * 
     * As a reaction, a corresponding CAD `Namespace` is created.
     */
    @Test
    void nonCorrespondingSysmlBlockCreated() {
        // Create model roots (by adding a SysML model, the CAD model will also be created)
        modifyView(getSysMLView(), v -> {
            v.registerRoot(SysmlFactory.eINSTANCE.createModel(), URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // Create non-corresponding block
        modifyView(getSysMLView(), v -> {
            v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .add(SysmlFactory.eINSTANCE.createBlock());
        });

        // A corresponding CAD namespace has been created
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
    }

    /**
     * A CAD `Namespace` is created, but not made corresponding to a SysML `Block`.
     * 
     * As a reaction, the user is asked whether to create a corresponding SysML `Block`.
     */
    @Test
    void nonCorrespondingCadNamespaceCreated() {
        // Create model roots (by adding a CAD model, the SysML model will also be created)
        modifyView(getCADView(), v -> {
            v.registerRoot(CadFactory.eINSTANCE.createCAD_Model(), URI.createFileURI(tempDir.toString() + "/cad.model"));
        });

        // Create non-corresponding namespace
        modifyView(getCADView(), v -> {
            userInteractor.setMCSSAnswer(0); // create corresponding block: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .add(CadFactory.eINSTANCE.createNamespace());
        });

        // A corresponding SysML block has been created
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
    }

    /**
     * A SysML `Block` corresponding to some CAD `Namespaces` is deleted.
     * 
     * As a reaction, the user is asked whether to delete the corresponding CAD `Namespaces`.
     */
    @Test
    void sysmlBlockDeleted() {
        // Create the model roots and fill the SysML model with two blocks (creates one corresponding namespace for each block)
        modifyView(getSysMLView(), v -> {
            sysml.Model sysmlModel = SysmlFactory.eINSTANCE.createModel();
            sysmlModel.getOwnedElements().add(SysmlFactory.eINSTANCE.createBlock());
            sysmlModel.getOwnedElements().add(SysmlFactory.eINSTANCE.createBlock());

            v.registerRoot(sysmlModel, URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // The corresponding CAD model and namespaces have been created
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class).size());
        assertEquals(2, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
        
        // Delete the second namespace and delete the corresponding SysML block
        modifyView(getCADView(), v -> {
            userInteractor.setMCSSAnswer(0); // delete corresponding block: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .remove(0);
        });

        // There are now 1 SysML block and 1 CAD namespace present
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
        
        // Delete the first namespace, but do not delete the corresponding SysML block; this still deletes the SysML block since there are
        // no more corresponding namespaces for it
        modifyView(getCADView(), v -> {
            userInteractor.setMCSSAnswer(1); // delete corresponding block: no
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .remove(0);
        });

        // There are now no SysML blocks and no CAD namespaces present
        assertEquals(0, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(0, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
    }

    /**
     * A CAD `Namespace` corresponding to a SysML `Block` is deleted.
     * 
     * As a reaction, the corresponding SysML `Block` is checked for the number of correspondences and, if zero, deleted.
     */
    @Test
    void cadNamespaceDeleted() {
        // Create the model roots and fill the SysML model with a block (creates a corresponding namespace)
        modifyView(getSysMLView(), v -> {
            sysml.Model sysmlModel = SysmlFactory.eINSTANCE.createModel();

            sysml.Block block = SysmlFactory.eINSTANCE.createBlock();
            block.setId("block");
            sysmlModel.getOwnedElements().add(block);

            v.registerRoot(sysmlModel, URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // The corresponding CAD model and namespace have been created
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class).size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());

        // Create another namespace and make it corresponding to the existing SysML block
        modifyView(getGlobalView(), v -> {
            userInteractor.setMCSSAnswer(1); // create corresponding block: no
            userInteractor.setTextAnswer("block"); // correspond with block with id "block"

            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .add(CadFactory.eINSTANCE.createNamespace());
        });
        
        // There are now 1 SysML block and 2 CAD namespaces present
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(2, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
        
        // Delete the CAD namespaces; this also deletes the SysML block since there are no more namespaces corresponding to it
        modifyView(getCADView(), v -> {
            userInteractor.setMCSSAnswer(0); // delete corresponding namespace: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .remove(1);
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .remove(0);
        });

        // The SysML block and CAD namespace are now both deleted
        assertEquals(0, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(0, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
    }

    /**
     * A SysML `ValueProperty` is created in a SysML `Block`, but there is no CAD Parameter with the same name in the CAD `Namespace`
     * corresponding to the `Block`.
     * 
     * As a reaction, the user is asked whether to create a corresponding CAD Parameter.
     */
    @Test
    void valuePropertyCreatedNoSameNameCadProperty() {
        // Create model roots and add three value properties (one for each CAD parameter type)
        modifyView(getSysMLView(), v -> {
            sysml.Model sysmlModel = SysmlFactory.eINSTANCE.createModel();
            sysmlModel.getOwnedElements().add(SysmlFactory.eINSTANCE.createBlock());

            v.registerRoot(sysmlModel, URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // Add numeric property
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty numericProperty = SysmlFactory.eINSTANCE.createValueProperty();
            numericProperty.setName("numeric");
            numericProperty.setDefaultValue("1.0");

            userInteractor.setMCSSAnswer(0); // create a parameter: numeric
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(numericProperty);
        });

        // Add string property
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty stringProperty = SysmlFactory.eINSTANCE.createValueProperty();
            stringProperty.setName("string");
            stringProperty.setDefaultValue("example string");

            userInteractor.setMCSSAnswer(1); // create a parameter: string
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(stringProperty);
        });

        // Add boolean property
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty booleanProperty = SysmlFactory.eINSTANCE.createValueProperty();
            booleanProperty.setName("boolean");
            booleanProperty.setDefaultValue("true");

            userInteractor.setMCSSAnswer(2); // create a parameter: boolean
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(booleanProperty);
        });

        // There are now 1 CAD namespace and 3 CAD parameters present in that namespace
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
        assertEquals(3, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());

        // All three types of parameters are present in the CAD namespace
        assertNotNull(getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .stream().filter(p -> p instanceof cad.NumericParameter)
            .findFirst().orElse(null));
        assertNotNull(getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .stream().filter(p -> p instanceof cad.StringParameter)
            .findFirst().orElse(null));
        assertNotNull(getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .stream().filter(p -> p instanceof cad.BooleanParameter)
            .findFirst().orElse(null));
    }

    /**
     * A SysML `ValueProperty` is created in a SysML `Block` and there is a CAD Parameter with the same name in the CAD `Namespace`
     * corresponding to the `Block`.
     * 
     * As a reaction, the `ValueProperty` is made corresponding to the CAD Parameter.
     */
    @Test
    void valuePropertyCreatedWithSameNameCadProperty() {
        // Create model roots and SysML block corresponding to a CAD namespace
        modifyView(getSysMLView(), v -> {
            sysml.Model sysmlModel = SysmlFactory.eINSTANCE.createModel();
            sysmlModel.getOwnedElements().add(SysmlFactory.eINSTANCE.createBlock());

            v.registerRoot(sysmlModel, URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // Add a CAD `NumericParameter` to the CAD namespace
        modifyView(getCADView(), v -> {
            cad.NumericParameter numericParameter = CadFactory.eINSTANCE.createNumericParameter();
            numericParameter.setName("numeric");
            numericParameter.setValue(1.f);
            
            userInteractor.setMCSSAnswer(1); // create a value property: no
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(numericParameter);
        });

        // There is now 1 CAD parameter, but no SysML value property present
        assertEquals(0, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
        
        // Create a SysML value property with the same name as the CAD numeric parameter
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty valueProperty = SysmlFactory.eINSTANCE.createValueProperty();
            valueProperty.setName("numeric");

            userInteractor.setMCSSAnswer(0); // create a parameter: numeric (this should not trigger)
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(valueProperty);
        });

        // There are now 1 SysML value property and 1 CAD parameter present
        assertEquals(1, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
    }

    /**
     * A CAD Parameter is created in a CAD `Namespace`, but there is no SysML `ValueProperty` with the same name in the SysML `Block`
     * corresponding to the `Namespace`.
     * 
     * As a reaction, the user is asked whether to create a corresponding SysML `ValueProperty`.
     */
    @Test
    void cadParameterCreatedNoSameNameValueProperty() {
        // Create model roots and add three CAD parameters (one of each type)
        modifyView(getCADView(), v -> {
            cad.CAD_Model cadModel = CadFactory.eINSTANCE.createCAD_Model();
            cadModel.getNamespaces().add(CadFactory.eINSTANCE.createNamespace());

            v.registerRoot(cadModel, URI.createFileURI(tempDir.toString() + "/cad.model"));
        });

        // Add numeric parameter
        modifyView(getCADView(), v -> {
            cad.NumericParameter numericParameter = CadFactory.eINSTANCE.createNumericParameter();
            numericParameter.setName("numeric");
            numericParameter.setValue(1.f);

            userInteractor.setMCSSAnswer(0); // create a value property: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(numericParameter);
        });

        // Add string parameter
        modifyView(getCADView(), v -> {
            cad.StringParameter stringParameter = CadFactory.eINSTANCE.createStringParameter();
            stringParameter.setName("string");
            stringParameter.setValue("example string");

            userInteractor.setMCSSAnswer(0); // create a value property: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(stringParameter);
        });

        // Add boolean parameter
        modifyView(getCADView(), v -> {
            cad.BooleanParameter booleanParameter = CadFactory.eINSTANCE.createBooleanParameter();
            booleanParameter.setName("boolean");
            booleanParameter.setValue(true);

            userInteractor.setMCSSAnswer(0); // create a value property: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(booleanParameter);
        });

        // There are now 1 SysML block and 3 SysML value properties present in that block
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(3, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
    }

    /**
     * A CAD Property is created in a CAD `Namespace` and there is a SysML `ValueProperty` with the same name in the SysML `Block`
     * corresponding to the `Namespace`.
     * 
     * As a reaction, the CAD Parameter is made corresponding to the SysML `ValueProperty`.
     */
    @Test
    void cadParameterCreatedWithSameNameValueProperty() {
        // Create model roots and CAD namespace corresponding to a SysML block
        modifyView(getCADView(), v -> {
            cad.CAD_Model cadModel = CadFactory.eINSTANCE.createCAD_Model();
            cadModel.getNamespaces().add(CadFactory.eINSTANCE.createNamespace());

            v.registerRoot(cadModel, URI.createFileURI(tempDir.toString() + "/cad.model"));
        });

        // Add a SysML value property to the SysML block
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty valueProperty = SysmlFactory.eINSTANCE.createValueProperty();
            valueProperty.setName("numeric");
            valueProperty.setDefaultValue("1.0");

            userInteractor.setMCSSAnswer(3); // create a parameter: no
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(valueProperty);
        });

        // There is now 1 SysML value property, but no CAD parameter present
        assertEquals(1, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
        assertEquals(0, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
        
        // Create a CAD parameter with the same name as the SysML value property
        modifyView(getCADView(), v -> {
            cad.NumericParameter numericParameter = CadFactory.eINSTANCE.createNumericParameter();
            numericParameter.setName("numeric");
            
            userInteractor.setMCSSAnswer(0); // create a value property: yes (this should not trigger)
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(numericParameter);
        });

        // There are now 1 SysML value property and 1 CAD parameter present
        assertEquals(1, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
    }

    /**
     * A SysML `ValueProperty` is renamed.
     * 
     * As a reaction, the corresponding CAD Parameter is also renamed to the new name.
     */
    @Test
    void valuePropertyRenamed() {
        // Create the model roots and a SysML block
        modifyView(getSysMLView(), v -> {
            sysml.Model sysmlModel = SysmlFactory.eINSTANCE.createModel();
            sysmlModel.getOwnedElements().add(SysmlFactory.eINSTANCE.createBlock());

            v.registerRoot(sysmlModel, URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // Create a value property and a corresponding CAD parameter
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty valueProperty = SysmlFactory.eINSTANCE.createValueProperty();
            valueProperty.setName("numeric");
            valueProperty.setDefaultValue("1.0");

            userInteractor.setMCSSAnswer(0); // create a parameter: numeric
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(valueProperty);
        });

        // There are now 1 CAD namespace and 1 CAD parameter present in that namespace
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
        
        // Rename the value property
        modifyView(getSysMLView(), v -> {
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .get(0)
                .setName("newName");
        });

        // The CAD parameter is now also renamed
        assertEquals("newName", getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .get(0)
            .getName());
    }

    /**
     * A CAD Parameter is renamed.
     * 
     * As a reaction, the corresponding SysML `ValueProperty` is also renamed to the new name.
     */
    @Test
    void cadParameterRenamed() {
        // Create the model roots and a CAD namespace
        modifyView(getCADView(), v -> {
            cad.CAD_Model cadModel = CadFactory.eINSTANCE.createCAD_Model();
            cadModel.getNamespaces().add(CadFactory.eINSTANCE.createNamespace());

            v.registerRoot(cadModel, URI.createFileURI(tempDir.toString() + "/cad.model"));
        });

        // Create a parameter and a corresponding SysML value property
        modifyView(getCADView(), v -> {
            cad.NumericParameter numericParameter = CadFactory.eINSTANCE.createNumericParameter();
            numericParameter.setName("numeric");
            numericParameter.setValue(1.f);
            
            userInteractor.setMCSSAnswer(0); // create a value property: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(numericParameter);
        });

        // There are now 1 SysML block and 1 SysML value property present in that block
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(1, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
        
        // Rename the parameter
        modifyView(getCADView(), v -> {
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .get(0)
                .setName("newName");
        });

        // The SysML value property is now also renamed
        assertEquals("newName", ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .get(0)
            .getName());
    }

    /**
     * A SysML `ValueProperty` has its value changed.
     * 
     * As a reaction, the corresponding CAD Parameter also changes its value.
     */
    @Test
    void valuePropertyValueChanged() {
        // Create the model roots and a SysML block
        modifyView(getSysMLView(), v -> {
            sysml.Model sysmlModel = SysmlFactory.eINSTANCE.createModel();
            sysmlModel.getOwnedElements().add(SysmlFactory.eINSTANCE.createBlock());

            v.registerRoot(sysmlModel, URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // Create a value property and a corresponding CAD parameter
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty valueProperty = SysmlFactory.eINSTANCE.createValueProperty();
            valueProperty.setName("numeric");
            valueProperty.setDefaultValue("1.0");

            userInteractor.setMCSSAnswer(0); // create a parameter: numeric
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(valueProperty);
        });

        // There are now 1 CAD namespace and 1 CAD parameter present in that namespace
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
        
        // Change the value of the value property
        modifyView(getSysMLView(), v -> {
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .get(0)
                .setDefaultValue("2.0");
        });

        // The CAD parameter is now also renamed
        assertEquals(2.f, ((cad.NumericParameter) getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .get(0))
            .getValue());
    }

    /**
     * A CAD Parameter has its value changed.
     * 
     * As a reaction, the corresponding SysML `ValueProperty` also changes its value.
     */
    @Test
    void cadParameterValueChanged() {
        // Create the model roots and a CAD namespace
        modifyView(getCADView(), v -> {
            cad.CAD_Model cadModel = CadFactory.eINSTANCE.createCAD_Model();
            cadModel.getNamespaces().add(CadFactory.eINSTANCE.createNamespace());

            v.registerRoot(cadModel, URI.createFileURI(tempDir.toString() + "/cad.model"));
        });

        // Create a parameter and a corresponding SysML value property
        modifyView(getCADView(), v -> {
            cad.NumericParameter numericParameter = CadFactory.eINSTANCE.createNumericParameter();
            numericParameter.setName("numeric");
            numericParameter.setValue(1.f);
            
            userInteractor.setMCSSAnswer(0); // create a value property: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(numericParameter);
        });

        // There are now 1 SysML block and 1 SysML value property present in that block
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(1, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
        
        // Change the value of the parameter
        modifyView(getCADView(), v -> {
            ((cad.NumericParameter) v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .get(0))
                .setValue(2.f);
        });

        // The SysML value property is now also renamed
        assertEquals("2.0", ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .get(0)
            .getDefaultValue());
    }

    /**
     * A SysML `ValueProperty` is deleted.
     * 
     * As a reaction, the user is asked whether to delete the corresponding CAD Parameter.
     */
    @Test
    void valuePropertyDeleted() {
        // Create the model roots and a SysML block
        modifyView(getSysMLView(), v -> {
            sysml.Model sysmlModel = SysmlFactory.eINSTANCE.createModel();
            sysmlModel.getOwnedElements().add(SysmlFactory.eINSTANCE.createBlock());

            v.registerRoot(sysmlModel, URI.createFileURI(tempDir.toString() + "/sysml.model"));
        });

        // Create a value property and a corresponding CAD parameter
        modifyView(getSysMLView(), v -> {
            sysml.ValueProperty valueProperty = SysmlFactory.eINSTANCE.createValueProperty();
            valueProperty.setName("numeric");
            valueProperty.setDefaultValue("1.0");

            userInteractor.setMCSSAnswer(0); // create a parameter: numeric
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .add(valueProperty);
        });

        // There are now 1 CAD namespace and 1 CAD parameter present in that namespace
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .size());
        assertEquals(1, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
        
        // Delete the value property
        modifyView(getSysMLView(), v -> {
            userInteractor.setMCSSAnswer(0); // delete the parameter: yes
            ((sysml.Block) v.getRootObjects(sysml.Model.class)
                .iterator().next()
                .getOwnedElements()
                .get(0))
                .getValues()
                .remove(0);
        });

        // The CAD parameter is now deleted
        assertEquals(0, getCADView().getRootObjects(cad.CAD_Model.class)
            .iterator().next()
            .getNamespaces()
            .get(0)
            .getParameters()
            .size());
    }

    /**
     * A CAD Parameter is deleted.
     * 
     * As a reaction, the user is asked whether to delete the corresponding SysML `ValueProperty`.
     */
    @Test
    void cadParameterDeleted() {
        // Create the model roots and a CAD namespace
        modifyView(getCADView(), v -> {
            cad.CAD_Model cadModel = CadFactory.eINSTANCE.createCAD_Model();
            cadModel.getNamespaces().add(CadFactory.eINSTANCE.createNamespace());

            v.registerRoot(cadModel, URI.createFileURI(tempDir.toString() + "/cad.model"));
        });

        // Create a parameter and a corresponding SysML value property
        modifyView(getCADView(), v -> {
            cad.NumericParameter numericParameter = CadFactory.eINSTANCE.createNumericParameter();
            numericParameter.setName("numeric");
            numericParameter.setValue(1.f);
            
            userInteractor.setMCSSAnswer(0); // create a value property: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .add(numericParameter);
        });

        // There are now 1 SysML block and 1 SysML value property present in that block
        assertEquals(1, getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .size());
        assertEquals(1, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
        
        // Delete the parameter
        modifyView(getCADView(), v -> {
            userInteractor.setMCSSAnswer(0); // delete the value property: yes
            v.getRootObjects(cad.CAD_Model.class)
                .iterator().next()
                .getNamespaces()
                .get(0)
                .getParameters()
                .remove(0);
        });

        // The SysML value property is now also deleted
        assertEquals(0, ((sysml.Block) getSysMLView().getRootObjects(sysml.Model.class)
            .iterator().next()
            .getOwnedElements()
            .get(0))
            .getValues()
            .size());
    }
}
