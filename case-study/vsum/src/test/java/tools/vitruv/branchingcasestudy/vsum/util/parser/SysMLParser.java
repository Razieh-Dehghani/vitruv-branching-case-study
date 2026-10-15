package tools.vitruv.branchingcasestudy.vsum.util.parser;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Action;
import org.eclipse.uml2.uml.Activity;
import org.eclipse.uml2.uml.ActivityEdge;
import org.eclipse.uml2.uml.ActivityFinalNode;
import org.eclipse.uml2.uml.ActivityNode;
import org.eclipse.uml2.uml.AggregationKind;
import org.eclipse.uml2.uml.Association;
import org.eclipse.uml2.uml.CallBehaviorAction;
import org.eclipse.uml2.uml.Class;
import org.eclipse.uml2.uml.ControlFlow;
import org.eclipse.uml2.uml.ControlNode;
import org.eclipse.uml2.uml.DecisionNode;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.InitialNode;
import org.eclipse.uml2.uml.JoinNode;
import org.eclipse.uml2.uml.LiteralUnlimitedNatural;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.Package;
import org.eclipse.uml2.uml.PackageableElement;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Relationship;
import org.eclipse.uml2.uml.VisibilityKind;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;
import org.eclipse.uml2.uml.util.UMLSwitch;

import sysml.SysmlFactory;

/**
 * A parser reading SysML models from .uml files. The read model is then parsed into a model complying with the SysML metamodel used in
 * this project using the {@link sysml.SysmlFactory}.
 * 
 * This parser is:
 * - Incomplete: It does not cover the full SysML specification, but rather the specific subset used by this case study.
 * - Unsafe: It is not resistant to non-well formed SysML models and may throw errors if unexpected elements are queried.
 */
public class SysMLParser extends UMLSwitch<EObject> {
    // The UML resource this parser works on
    private final Resource resource;

    // A cache that stores all visited UML `Elements` and their respective `sysml.Elements`; used to prevent infinite parsing cycles
    private final Map<Element, sysml.Element> cache;

    /**
     * Creates a SysML parser for the .uml file located at the specified resource path.
     * 
     * @param resourcePath the relative file path of the .uml file as seen from the `src/test/resources/sysml/` directory
     */
    public SysMLParser(String umlFilePath) throws Exception {
        super();

        this.resource = initializeResource(umlFilePath);
        this.cache = new IdentityHashMap<>();
    }

    /**
     * Initializes the Eclipse UML2 package and then loads the SysML model from the .uml file at the given resource path.
     * 
     * @param umlFilePath the relative file path of the .uml file as seen from the `src/test/resources/sysml/` directory
     * @return a {@link Resource} that was created from the model in the referenced file
     */
    private Resource initializeResource(String umlFilePath) throws Exception {
        // Register UML2 to the resource set registry
        ResourceSet resourceSet = new ResourceSetImpl();

        UMLResourcesUtil.init(resourceSet);

        // Load UML resource from file
        URI uri = URI.createFileURI(new File("src/test/resources/sysml/" + umlFilePath).getAbsolutePath());
        Resource resource = resourceSet.getResource(uri, true);

        if (resource.getContents().isEmpty()) throw new Exception("Resource has no contents");
        if (!(resource.getContents().iterator().next() instanceof Model)) throw new Exception("Root is not a Model");

        return resource;
    }

    /**
     * Parses the SysML model from the previously loaded resource.
     * 
     * @return a {@link sysml.Model} that can be used inside of Vitruvius
     */
    public sysml.Model parse() {
        Model umlModel = (Model) this.resource.getContents().get(0);

        return (sysml.Model) convert(umlModel);
    }

    /**
     * Converts an UML object into a SysML object. If the UML object has already been converted, it is directly returned from the `cache`.
     * 
     * @param umlObject the UML object to parse
     * @return the cached or parsed SysML object
     */
    private EObject convert(EObject umlObject) {
        if (umlObject == null) return null;

        EObject cached = cache.get(umlObject);

        return cached != null ? cached : doSwitch(umlObject);
    }

    /**
     * CASE METHODS
     * 
     * The case methods are used by the `UMLSwitch` to instantiate model objects. The most concrete object type will be chosen if multiple
     * possible types are available.
     * 
     * To expand this section with new element types, create the respective method and use the object creation schema as seen in the other
     * case methods.
     */

    @Override
    public EObject caseActivity(Activity src) {
        sysml.Activity tgt = SysmlFactory.eINSTANCE.createActivity();
        cache.put(src, tgt);
        initActivity(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseActivityFinalNode(ActivityFinalNode src) {
        sysml.ActivityFinalNode tgt = SysmlFactory.eINSTANCE.createActivityFinalNode();
        cache.put(src, tgt);
        initActivityFinalNode(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseAssociation(Association src) {
        // Parse either a `Composition` or an `Association` depending on whether the member ends of the UML `Association` have the
        // `aggregation` field equal to 'composite'
        if ((src.getMemberEnds().size() == 2) 
            && src.getMemberEnds().get(0).getAggregation() == AggregationKind.COMPOSITE_LITERAL 
            && src.getMemberEnds().get(1).getAggregation() == AggregationKind.COMPOSITE_LITERAL) {
            sysml.Composition tgt = SysmlFactory.eINSTANCE.createComposition();
            cache.put(src, tgt);
            initComposition(src, tgt);
            return tgt;
        } else {
            sysml.Association tgt = SysmlFactory.eINSTANCE.createAssociation();
            cache.put(src, tgt);
            initAssociation(src, tgt);
            return tgt;
        }
    }

    @Override
    public EObject caseCallBehaviorAction(CallBehaviorAction src) {
        sysml.CallBehaviorAction tgt = SysmlFactory.eINSTANCE.createCallBehaviorAction();
        cache.put(src, tgt);
        initCallBehaviorAction(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseClass(Class src) {
        sysml.Block tgt = SysmlFactory.eINSTANCE.createBlock();
        cache.put(src, tgt);
        initBlock(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseControlFlow(ControlFlow src) {
        sysml.ControlFlow tgt = SysmlFactory.eINSTANCE.createControlFlow();
        cache.put(src, tgt);
        initControlFlow(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseDecisionNode(DecisionNode src) {
        sysml.DecisionNode tgt = SysmlFactory.eINSTANCE.createDecisionNode();
        cache.put(src, tgt);
        initDecisionNode(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseInitialNode(InitialNode src) {
        sysml.InitialNode tgt = SysmlFactory.eINSTANCE.createInitialNode();
        cache.put(src, tgt);
        initInitialNode(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseJoinNode(JoinNode src) {
        sysml.JoinNode tgt = SysmlFactory.eINSTANCE.createJoinNode();
        cache.put(src, tgt);
        initJoinNode(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseModel(Model src) {
        sysml.Model tgt = SysmlFactory.eINSTANCE.createModel();
        cache.put(src, tgt);
        initModel(src, tgt);
        return tgt;
    }

    @Override
    public EObject casePackage(Package src) {
        sysml.Package tgt = SysmlFactory.eINSTANCE.createPackage();
        cache.put(src, tgt);
        initPackage(src, tgt);
        return tgt;
    }

    @Override
    public EObject caseProperty(Property src) {
        // Parse either a `PartProperty` or a `ValueProperty` depending on whether the UML `Property` has the `association` field
        if (src.getAssociation() != null) {
            sysml.PartProperty tgt = SysmlFactory.eINSTANCE.createPartProperty();
            cache.put(src, tgt);
            initPartProperty(src, tgt);
            return tgt;
        } else {
            sysml.ValueProperty tgt = SysmlFactory.eINSTANCE.createValueProperty();
            cache.put(src, tgt);
            initValueProperty(src, tgt);
            return tgt;
        }
    }

    /**
     * INIT METHODS
     * 
     * The init methods fill an object with data. These methods make use of inheritance so that the same fields don't have to be filled in
     * multiple methods.
     * 
     * To expand this section with new element types, create the respective init method and (if applicable) use the existing init method of
     * the super type.
     */

    private void initAction(Action src, sysml.Action tgt) {
        initActivityNode(src, tgt);
    }

    private void initActivity(Activity src, sysml.Activity tgt) {
        initNamedElement(src, tgt);

        for (ActivityNode n : src.getNodes()) {
            tgt.getNodes().add((sysml.ActivityNode) convert(n));
        }

        for (ActivityEdge e : src.getEdges()) {
            tgt.getEdges().add((sysml.ActivityEdge) convert(e));
        }
    }

    private void initActivityEdge(ActivityEdge src, sysml.ActivityEdge tgt) {
        initNamedElement(src, tgt);

        tgt.setSource((sysml.ActivityNode) convert(src.getSource()));

        tgt.setTarget((sysml.ActivityNode) convert(src.getTarget()));
    }

    private void initActivityFinalNode(ActivityFinalNode src, sysml.ActivityFinalNode tgt) {
        initControlNode(src, tgt);
    }

    private void initActivityNode(ActivityNode src, sysml.ActivityNode tgt) {
        initNamedElement(src, tgt);
    }

    private void initAssociation(Association src, sysml.Association tgt) {
        initRelationship(src, tgt);

        tgt.setIsDerived(src.isDerived());
    }

    private void initBlock(Class src, sysml.Block tgt) {
        initNamedElement(src, tgt);

        for (Property p : src.getOwnedAttributes()) {
            if (p.getAssociation() != null) {
                // Parse associations as `PartProperties`
                tgt.getParts().add((sysml.PartProperty) convert(p));
            } else {
                // Parse remaining properties as `ValueProperties`
                tgt.getValues().add((sysml.ValueProperty) convert(p));
            }
        }
    }

    private void initCallBehaviorAction(CallBehaviorAction src, sysml.CallBehaviorAction tgt) {
        initAction(src, tgt);
    }

    private void initComposition(Association src, sysml.Composition tgt) {
        initAssociation(src, tgt);
    }

    private void initControlFlow(ControlFlow src, sysml.ControlFlow tgt) {
        initActivityEdge(src, tgt);

        if (src.getGuard() != null) tgt.setGuard(src.getGuard().toString());
    }

    private void initControlNode(ControlNode src, sysml.ControlNode tgt) {
        initActivityNode(src, tgt);
    }

    private void initDecisionNode(DecisionNode src, sysml.DecisionNode tgt) {
        initControlNode(src, tgt);
    }

    private void initElement(Element src, sysml.Element tgt) {
        // A `sysml.Element` will not have its name set since the name field is not part of a UML `Element`
        tgt.setId(src.eResource().getURIFragment(src));
    }

    private void initInitialNode(InitialNode src, sysml.InitialNode tgt) {
        initControlNode(src, tgt);
    }

    private void initJoinNode(JoinNode src, sysml.JoinNode tgt) {
        initControlNode(src, tgt);
    }

    private void initModel(Model src, sysml.Model tgt) {
        initPackage(src, tgt);
    }

    private void initNamedElement(NamedElement src, sysml.NamedElement tgt) {
        initElement(src, tgt);

        tgt.setName(src.getName());

        sysml.VisibilityKind visibility = null;
        if (src.getVisibility() == VisibilityKind.PUBLIC_LITERAL) visibility = sysml.VisibilityKind.PUBLIC;
        else if (src.getVisibility() == VisibilityKind.PROTECTED_LITERAL) visibility = sysml.VisibilityKind.PROTECTED;
        else if (src.getVisibility() == VisibilityKind.PRIVATE_LITERAL) visibility = sysml.VisibilityKind.PRIVATE;
        else if (src.getVisibility() == VisibilityKind.PACKAGE_LITERAL) visibility = sysml.VisibilityKind.PACKAGE;
        if (visibility != null) tgt.setVisibility(visibility);
    }

    private void initPackage(Package src, sysml.Package tgt) {
        initNamedElement(src, tgt);

        for (PackageableElement e : src.getPackagedElements()) {
            EObject converted = convert(e);
            if (converted == null) continue;

            if (converted instanceof sysml.Package) tgt.getNestedPackages().add((sysml.Package) converted);
            else tgt.getOwnedElements().add((sysml.Element) converted);
        }
    }

    private void initPartProperty(Property src, sysml.PartProperty tgt) {
        initProperty(src, tgt);
    }

    private void initProperty(Property src, sysml.Property tgt) {
        initNamedElement(src, tgt);

        tgt.setType((sysml.Block) convert(src.getType()));

        String multiplicity = null;
        if (src.getUpper() == LiteralUnlimitedNatural.UNLIMITED || src.getLower() != src.getUpper()) multiplicity = src.getLowerValue() + ".." + src.getUpperValue();
        else if (src.getLowerValue() != null) multiplicity = src.getLowerValue().toString();
        if (multiplicity != null) tgt.setMultiplicity(multiplicity);

        if (src.getDefaultValue() != null) tgt.setDefaultValue(src.getDefaultValue().toString());

        tgt.setIsDerived(src.isDerived());

        tgt.setIsReadOnly(src.isReadOnly());
    }

    private void initRelationship(Relationship src, sysml.Relationship tgt) {
        initElement(src, tgt);

        if (src.getRelatedElements() == null || src.getRelatedElements().size() != 2) throw new Error("Invalid relationship");

        Element source = src.getRelatedElements().get(0);
        tgt.setSource((sysml.Element) convert(source));

        Element target = src.getRelatedElements().get(1);
        tgt.setTarget((sysml.Element) convert(target));
    }

    private void initValueProperty(Property src, sysml.ValueProperty tgt) {
        initProperty(src, tgt);
    }
}
