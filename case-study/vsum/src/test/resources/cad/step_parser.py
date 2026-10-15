# STEP PARSER
# 
# A Python script that uses FreeCAD to extract the necessary information from .stp (STEP) CAD files
# in order to be able to import them into Vitruvius.
# 
# This script generates a JSON-formatted output file which contains all necessary information to be
# able to construct a model according to the CAD metamodel defined in Vitruvius.
# To acutally create this model instance for use inside of Vitruvius, you need to read the
# generated JSON file in Java and create the instance accordingly.
# 
# Using a Python script to extract data from the STEP files is necessary because there is no
# suitable approach in plain Java to do this.
# 
# How to run:
# - You need to have FreeCAD installed on your system (direct download or via package manager)
# - You must set `FREECADPATH` to the path of your FreeCAD installation (see below)
# - Run this script and pass the STEP file you want to parse and the location of the JSON output
#   file as arguments. Example:
#   > "/path/to/this/step_parser.py" "/path/to/file.stp" "output.json"
# - You can also call this script by using the headless FreeCAD command line tool. This way, you
#   can skip setting the `FREECADPATH`. Simply call:
#   > "/path/to/FreeCAD" -c "/path/to/this/step_parser.py" "/path/to/file.stp" "output.json"

# Python imports
import json
import os
import sys

# Add the path to your FreeCAD installation to Python's system paths. This allows Python to locate
# the FreeCAD library file.
# Change the below value to point to the FreeCAD installation on your device.
FREECADPATH = '/Applications/FreeCAD.app/Contents/Resources/lib'
sys.path.append(FREECADPATH)

# FreeCAD imports
import FreeCAD
import Import

# Validate command line arguments
try:
    input_file_location = sys.argv[1]

    if not input_file_location.lower().endswith(('.stp', '.step')):
        raise Exception(input_file_location)
    
    input_file_location = os.path.abspath(input_file_location)
except Exception as e:
    print(f'Error: Please supply a STEP file as the first command line argument: {e}')
    sys.exit(2)

try:
    output_file_location = sys.argv[2]

    if not output_file_location.lower().endswith(('.json')):
        raise Exception(output_file_location)
    
    output_file_location = os.path.abspath(output_file_location)
except Exception as e:
    print(f'Error: Please supply a JSON file location as the second command line argument: {e}')
    sys.exit(2)

# Open and read STEP file
document = FreeCAD.newDocument('temp', temp=True)

try:
    Import.insert(input_file_location, document.Name)
    document.recompute()
except OSError as e:
    print(f'Error: Could not access STEP file at {sys.argv[1]} (does your IDE / terminal have read access to the file?): {e}')
    sys.exit(1)
except Exception as e:
    print(f'Error: Could not read STEP file at {sys.argv[1]}: {e}')
    sys.exit(1)

# Store parsed objects in a list
result = []

# Parse visited CAD objects and append them to the result list
def visit(obj, path):
    # Location of the object in the object hierarchy
    path = path + '/' + obj.Label

    # Don't parse shapeless objects (e.g. containers in the hierarchy)
    if is_solid_object(obj):
        shape = obj.Shape

        bbox = shape.BoundBox

        # Parse the object into a dictionary and append it to the result list
        result.append({
            # Main object attributes
            "name": obj.Name,
            "label": obj.Label,
            "path": path,

            # Geometric features
            "volume": shape.Volume,
            "surfaceArea": shape.Area,
            "size": {
                "x": bbox.XLength,
                "y": bbox.YLength,
                "z": bbox.ZLength
            }
        })

    # Recursively visit children of this object
    if hasattr(obj, 'OutList'):
        for child in obj.OutList:
            visit(child, path)

# Returns true if an object is a real, solid object with a non-zero volume. Returns false for e.g.
# axis and plane objects which are not needed for the CAD model.
def is_solid_object(obj):
    return (
        hasattr(obj, 'Shape')
        and not obj.Shape.isNull()
        and obj.Shape.Solids
        and obj.Shape.Volume > 0
    )

# Visit root objects in the hierarchy
root_objects = [obj for obj in document.Objects if len(obj.InList) == 0]

for obj in root_objects:
    visit(obj, '')

# Store result in the output JSON file
with open(output_file_location, 'w', encoding='utf-8') as f:
    json.dump(result, f, ensure_ascii=False, indent=4)
    f.close()

# Cleanup and exit
FreeCAD.closeDocument(document.Name)

sys.exit(0)