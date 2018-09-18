
package mpicbg.csbd.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import mpicbg.csbd.task.Task;
import mpicbg.csbd.util.ArrayHelper;
import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;

// TODO rename
public class ImageTensor {

	// I do not use the following line because it was returning the axes in a
	// different order in different setups
	// AxisType[] axes = Axes.knownTypes();
	AxisType[] availableAxes = { Axes.X, Axes.Y, Axes.Z, Axes.TIME,
		Axes.CHANNEL };

	private String name;
	private final List<AxisType> nodeAxes = new ArrayList<>();
	private long[] nodeShape;
	private final List<Integer> finalMapping = new ArrayList<>();
	private Dataset dataset;
	private boolean mappingInitialized;
	private boolean reducedAxis = false;
	private boolean generatingMapping = false;

	public ImageTensor() {
		mappingInitialized = false;
	}

	public void initialize(final Dataset dataset) {
		this.dataset = dataset;
	}

	public void setNodeShape(final long[] shape) {

		nodeShape = shape;
//		while (nodeAxes.size() > nodeShape.length) {
//			nodeAxes.remove(nodeAxes.size() - 1);
//		}
	}

	public void initializeNodeMapping() {
		initializeNodeMapping(nodeShape);
	}

	public void initializeNodeMapping(final long[] shape) {
		nodeAxes.clear();
		for (int i = 0; i < shape.length; i++) {
			nodeAxes.add(null);
		}
	}

	public long[] getNodeShape() {
		return nodeShape;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int[] getMappingIndices() {
		return ArrayHelper.toIntArray(finalMapping);
	}

	public AxisType[] getMapping() {
		return nodeAxes.toArray(new AxisType[0]);
	}

	public void generateMapping() {

		generatingMapping = true;

		finalMapping.clear();

		for (int i = 0; i < nodeShape.length; i++) {
			finalMapping.add(getNodeDimByDatasetDim(i));
		}

		// if a dimension is not set, assign an unused dimension
		ArrayHelper.replaceNegativeIndicesWithUnusedIndices(finalMapping);

		generatingMapping = false;

	}

	public AxisType getDimType(final int dim) {
		if (dataset.numDimensions() > dim) {
			return dataset.axis(dim).type();
		}
		return null;
	}

	public Dataset getDataset() {
		return dataset;
	}

	public void setMappingDefaults() {
		final int tensorDimCount = nodeShape.length;
		if (tensorDimCount == 5) {
			setNodeAxis(0, Axes.TIME);
			setNodeAxis(1, Axes.Z);
			setNodeAxis(2, Axes.Y);
			setNodeAxis(3, Axes.X);
			setNodeAxis(4, Axes.CHANNEL);
			if(dataset != null && dataset.dimension(Axes.Z) <= 1 && dataset.dimension(Axes.TIME) > 1) {
				setNodeAxis(1, Axes.TIME);
				setNodeAxis(0, Axes.Z);
			}
		}
		else {
			if (tensorDimCount == 4) {
				setNodeAxis(1, Axes.Y);
				setNodeAxis(2, Axes.X);
				setNodeAxis(3, Axes.CHANNEL);
				if (dataset != null && dataset.dimension(Axes.Z) > 1) {
					setNodeAxis(0, Axes.Z);
				}
				else {
					setNodeAxis(0, Axes.TIME);
				}
			}
		}
		// printMapping();
	}

	public void resetMapping() {
		nodeAxes.clear();
	}

	public int numDimensions() {
		return dataset.numDimensions();
	}

	public String getDatasetDimName(final AxisType axis) {
		return axis.getLabel().substring(0, 1);
	}

	public String getDatasetDimName(final int datasetAxisIndex) {
		if (dataset.numDimensions() > datasetAxisIndex) {
			return getDatasetDimName(dataset.axis(datasetAxisIndex).type());
		}
		return "not found";
	}

	public boolean removeAxisFromMapping(final AxisType axisToRemove) {
		System.out.println("REMOVING " + axisToRemove.getLabel());
		if (!reducedAxis) {
			if (nodeAxes.contains(axisToRemove)) {
				nodeAxes.remove(axisToRemove);
				reducedAxis = true;
			}
			printMapping();
		}
		return reducedAxis;
	}

	public void printMapping() {
		printMapping(null);
	}

	public void printMapping(Task task) {
		Consumer<String> logFunction = System.out::println;
		if(task != null) logFunction = task::log;
		logFunction.accept("Mapping of tensor " + getName() + ": ");
		if (dataset != null) {
			final AxisType[] axes = new AxisType[dataset.numDimensions()];
			for (int i = 0; i < dataset.numDimensions(); i++) {
				axes[i] = dataset.axis(i).type();
			}
			logFunction.accept("   datasetAxes:" + Arrays.toString(axes));
		}
		logFunction.accept("   nodeAxes:" + nodeAxes.toString());
		logFunction.accept("   mapping:" + finalMapping.toString());
	}

	public Long getDatasetDimSizeByNodeDim(final int nodeDim) {
		final Integer index = getDatasetDimIndexByTFIndex(nodeDim);
		if (index != null) {
			return dataset.dimension(index);
		}
		return (long) 1;
	}

	public Integer getDatasetDimIndexByTFIndex(final int nodeDim) {
		if (nodeAxes.size() > nodeDim) {
			final AxisType axis = nodeAxes.get(nodeDim);
			if (dataset.axis(axis) != null) {
				return dataset.dimensionIndex(axis);
			}
		}
		return null;
	}

	public String getDatasetDimNameByNodeDim(final int nodeDim) {
		if (nodeAxes.size() > nodeDim) {
			return getDatasetDimName(nodeAxes.get(nodeDim));
		}
		return null;
	}

	public Integer getNodeDimByDatasetDim(final int datasetDim) {
		if (dataset.numDimensions() > datasetDim) {
			return nodeAxes.indexOf(dataset.axis(datasetDim).type());
		}
		return -1;
	}

	public AxisType getAxisByDatasetDim(final int datasetDim) {
		if (dataset.numDimensions() > datasetDim) {
			return dataset.axis(datasetDim).type();
		}
		return null;
	}

	public void setNodeAxis(final int index, final AxisType axisType) {
		if (nodeAxes.size() > index) {
			nodeAxes.set(index, axisType);
		}
	}

	public AxisType getNodeAxis(final int index) {
		return nodeAxes.get(index);
	}

	public void setNodeAxisByKnownAxesIndex(final int nodeDim,
		final int knownAxesIndex)
	{
		if (knownAxesIndex < availableAxes.length && nodeDim < nodeAxes.size()) {
			nodeAxes.set(nodeDim, availableAxes[knownAxesIndex]);
		}
	}

	public void setMapping(final AxisType[] newmapping) {
		nodeAxes.clear();
		for (int i = 0; i < newmapping.length; i++) {
			nodeAxes.add(newmapping[i]);
		}
		printMapping();
	}

	public List<AxisType> getNodeAxes() {
		return nodeAxes;
	}

}
