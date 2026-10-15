package tools.vitruv.branchingcasestudy.vsum.util.parser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import cad.CadFactory;

/**
 * A parser reading JSON files created by the `step_parser.py` Python script and converting them into an instance of the CAD metamodel
 * using the {@link cad.CadFactory} that can be used inside of Vitruvius.
 * 
 * This parser works very closely with the Python script. Changes to the parsing process will likely have to be made in both this class and
 * the script.
 */
public class CADParser {
    // Relative path to the CAD test resource directory
    final static String CAD_RESOURCE_DIRECTORY = "src/test/resources/cad/";

    /**
     * Creates a process calling the `step_parser.py` Python script to create a JSON file from a STEP file. This way, missing JSON files
     * can be created inside of Java tests instead of manually having to execute the Python script.
     * 
     * JSON files that already exist are not regenerated due to the script running for a long time (multiple minutes per STEP file). To
     * regenerate a JSON file, simply delete the existing file.
     * 
     * @param stepFileName the name of the input STEP file (including '.stp' or '.step' file extension)
     * @param jsonFileName the name of the output JSON file (including 'json' file extension)
     * @throws Exception on parsing error
     */
    private static void generateJsonFile(String stepFileName, String jsonFileName) throws Exception {
        File step = new File(CAD_RESOURCE_DIRECTORY + "stp/" + stepFileName);
        File json = new File(CAD_RESOURCE_DIRECTORY + "json/" + jsonFileName);

        // Return if the STEP file is missing or the JSON file already exists
        if (!step.exists() || step.isDirectory() || json.exists()) return;

        // Start a process that calls the JSON file generation Python script
        ProcessBuilder pb = new ProcessBuilder(
            "python3",
            Paths.get(CAD_RESOURCE_DIRECTORY + "step_parser.py").toAbsolutePath().toString(),
            step.toPath().toAbsolutePath().toString(),
            json.toPath().toAbsolutePath().toString()
        );

        Process p = pb.start();
        int exitCode = p.waitFor();

        if (exitCode != 0) throw new Exception("Task parsing " + stepFileName + " into " + jsonFileName + " failed!");
    }

    /**
     * Calls all JSON generation processes. If new JSON files should be generated, register them here.
     * 
     * @throws Exception on JSON generation error
     */
    public static void generateJsonFiles() throws Exception {
        generateJsonFile("vehicle-v1.stp", "V1.json");
        generateJsonFile("vehicle-v2.stp", "V2.json");
    }

    /**
     * Parses a CAD model from a generated JSON file.
     * 
     * @param jsonName the name of the corresponding JSON file (without 'json' file extension)
     * @return a CAD model that can be used inside of Vitruvius
     * @throws Exception on JSON generation error
     */
    public cad.CAD_Model parse(String jsonName) throws Exception {
        // Read JSON file and parse content into record objects
        String jsonString = Files.readString(new File(CAD_RESOURCE_DIRECTORY + "json/" + jsonName + ".json").toPath(), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        List<CADObject> cadObjects = mapper.readValue(jsonString, new TypeReference<List<CADObject>>(){});

        // Create CAD model instance
        cad.CAD_Model cadModel = CadFactory.eINSTANCE.createCAD_Model();
        cadModel.setName(jsonName);
        cadModel.setId("vitruvCadModel"); // static id; assume no same label object in the cad model
        for (CADObject obj : cadObjects) cadModel.getNamespaces().add(obj.toNamespace());

        return cadModel;
    }

    /**
     * Record representing a CAD object in the JSON file.
     * 
     * The record attributes are equivalent to the ones defined in the JSON struct.
     */
    public record CADObject(
        String name,
        String label,
        String path,

        float volume,
        float surfaceArea,
        CADObjectSize size
    ) {
        /**
         * Parses this record into a CAD namespace.
         * 
         * @return the parsed namespace
         */
        public cad.Namespace toNamespace() {
            cad.Namespace namespace = CadFactory.eINSTANCE.createNamespace();
            namespace.setName(name);
            namespace.setId(label); // use label as id since it identifies an object

            // CAD object parameters
            cad.StringParameter path = CadFactory.eINSTANCE.createStringParameter();
            path.setName("path");
            path.setId(label + ".path");
            path.setValue(this.path);
            namespace.getParameters().add(path);

            cad.NumericParameter volume = CadFactory.eINSTANCE.createNumericParameter();
            volume.setName("volume");
            volume.setId(label + ".volume");
            volume.setValue(this.volume);
            volume.setUnit(cad.Unit.MM);
            namespace.getParameters().add(volume);

            cad.NumericParameter surfaceArea = CadFactory.eINSTANCE.createNumericParameter();
            surfaceArea.setName("surfaceArea");
            surfaceArea.setId(label + ".surfaceArea");
            surfaceArea.setValue(this.surfaceArea);
            surfaceArea.setUnit(cad.Unit.MM);
            namespace.getParameters().add(surfaceArea);

            namespace.getParameters().add(size.toCoordinate(label));

            return namespace;
        }
    }

    /**
     * Record representing the size parameter of a CAD object in the JSON file. This separate record is needed for JSON parsing.
     * 
     * The record attributes are equivalent to the ones defined in the JSON struct.
     */
    public record CADObjectSize(
        float x,
        float y,
        float z
    ) {
        /**
         * Parses this record into a CAD coordinate which is to be interpreted as a 3D vector with dimensions.
         * 
         * @param cadObjectLabel the label of the CAD object; used to set the ID of this object
         * @return the parsed coordinate
         */
        public cad.Coordinate toCoordinate(String cadObjectLabel) {
            cad.Coordinate coordinate = CadFactory.eINSTANCE.createCoordinate();
            coordinate.setName("size");
            coordinate.setId(cadObjectLabel + ".size");
            coordinate.setX(this.x);
            coordinate.setY(this.y);
            coordinate.setZ(this.z);
            coordinate.setUnit(cad.Unit.MM);

            return coordinate;
        }
    }
}
