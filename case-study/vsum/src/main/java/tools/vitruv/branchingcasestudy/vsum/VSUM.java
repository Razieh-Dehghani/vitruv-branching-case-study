package tools.vitruv.branchingcasestudy.vsum;

import tools.vitruv.framework.vsum.VirtualModelBuilder;
import sysml.SysmlFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import mir.reactions.cad2Sysml.Cad2SysmlChangePropagationSpecification;
import mir.reactions.sysml2Cad.Sysml2CadChangePropagationSpecification;
import tools.vitruv.change.testutils.TestUserInteraction;
import tools.vitruv.framework.views.CommittableView;
import tools.vitruv.framework.views.View;
import tools.vitruv.framework.views.ViewTypeFactory;
import tools.vitruv.framework.vsum.VirtualModel;

public class VSUM {
    public static void main(String[] args) throws IOException {
        VirtualModel vsum = createDefaultVirtualModel();
        CommittableView view = getDefaultView(vsum).withChangeDerivingTrait();

        modifyView(view, (CommittableView v) -> {
            v.getRootObjects().add(SysmlFactory.eINSTANCE.createModel());
        });
    }

    private static VirtualModel createDefaultVirtualModel() throws IOException {
        return new VirtualModelBuilder()
            .withStorageFolder(Path.of("vsum"))
            .withUserInteractorForResultProvider(new TestUserInteraction.ResultProvider(new TestUserInteraction()))
            .withChangePropagationSpecifications(new Cad2SysmlChangePropagationSpecification(), new Sysml2CadChangePropagationSpecification())
            .buildAndInitialize();
    }

    private static View getDefaultView(VirtualModel vsum) {
        var selector = vsum.createSelector(ViewTypeFactory.createIdentityMappingViewType("default"));
        selector.getSelectableElements().forEach(it -> selector.setSelected(it, true));

        return selector.createView();
    }

    private static void modifyView(CommittableView view, Consumer<CommittableView> modificationFunction) {
        modificationFunction.accept(view);
        view.commitChanges();
    }
}
