# @String(label="Input path (.tif or folder with .tifs)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script') input
# @String(label="Output path (.tif or folder)", required=false, value='/home/random/Development/imagej/project/CSBDeep/script/out') output
# @String(label="Model file", required=false, value='/home/random/Development/imagej/project/CSBDeep/data/Tobias Boothe/models/phago_C2_no_transform_model.zip') modelFile
# @String(label="Model file", required=false, value='phago_C2_no_transform_model') _modelName
# @Integer(label="Number of tiles", required=false, value=8) nTiles
# @Integer(label="Tile overlap", required=false, value=32) overlap
# @Boolean(label="Normalize input", required=false, value=true) normalizeInput
# @Float(label="Bottom percentile", required=false, value=3.0, stepSize=0.1) percentileBottom
# @Float(label="Top percentile", required=false, value=99.8, stepSize=0.1) percentileTop
# @Boolean(label="Clip", required=false, value=false) clip
# @Boolean(label="Show progress dialog", required=false, value=true) showProgressDialog
# @DatasetIOService io
# @CommandService command
# @ModuleService module

from java.io import File
import sys
from de.csbdresden.csbdeep.commands import GenericNetwork

def getFileName(path):
	fileparts = path.split("/")
	return fileparts[len(fileparts)-1]

def runNetwork(inputPath, outputPath):
	print("input: " + inputPath + ", output: " + outputPath)
	imp = io.open(inputPath)
	mymod = (command.run(GenericNetwork, False,
		"input", imp,
		"nTiles", nTiles,
		"overlap", overlap,
		"normalizeInput", normalizeInput,
		"percentileBottom", percentileBottom,
		"percentileTop", percentileTop,
		"clip", clip,
		"showProgressDialog", showProgressDialog,
		"modelFile", modelFile)).get()
	myoutput = mymod.getOutput("output")
	print(myoutput)
	io.save(myoutput, outputPath)

if input.endswith(".tif"):
	if output.endswith(".tif"):
		runNetwork(input, output)
	else:
		if not(output.endswith("/")):
			output += "/"
		runNetwork(input, output + getFileName(input))
else:
	if output.endswith(".tif"):
		print("ERROR: please provide a directory as output, because your input is also a directory")
		sys.exit()
	if not(output.endswith("/")):
		output += "/"
	if not(input.endswith("/")):
		input += "/"
	if(output == input):
		print("ERROR: please provide an output directory that is not the same as the input directory")
		sys.exit()
	directory = File(input);
	listOfFilesInFolder = directory.listFiles();

	for file in listOfFilesInFolder:
		if file.toString().endswith(".tif"):
			runNetwork(file.toString(), output + getFileName(file.toString()))
