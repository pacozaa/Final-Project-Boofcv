# Final Project BoofCV

A collection of computer vision applications built with BoofCV library, focusing on corn plant image analysis and processing.

## Overview

This repository contains multiple Java-based computer vision applications that utilize the BoofCV (Boofcv Computer Vision) library for image processing tasks. The primary focus is on agricultural applications, specifically corn plant detection, segmentation, and analysis.

## Project Structure

The repository is organized into several independent modules:

### Main Applications

- **CornPlantColorSegment** - Color segmentation for corn plant images
- **CornPlantLevel1** - Basic corn plant image processing
- **CornPlantTunnelGuideColorSegmentationV1/V2/V3** - Progressive versions of tunnel guide color segmentation for corn plants
- **WebCamColorSegmentation** - Real-time color segmentation using webcam
- **WebCamColorSegmentation-CornPlant** - Specialized webcam segmentation for corn plants with linear regression analysis

### Computer Vision Examples

- **DetectLine** - Line detection algorithms
- **ExamplePlanarImages** - Planar image processing examples
- **FiducialSquareBinary** - Fiducial marker detection
- **ImageBasic** - Basic image processing operations
- **ImageSegmentation** - Image segmentation techniques
- **InterestPoint** - Interest point detection and feature extraction
- **SuperPixcel** - Superpixel segmentation
- **WatershedWithSeeds** - Watershed segmentation with seed points

### Utilities

- **Webcam** - Webcam capture utilities
- **WebcamViewerExample** - Example webcam viewer implementation
- **TakeAPicture** - Simple image capture application

## Prerequisites

- Java Development Kit (JDK) 8 or higher
- Apache Ant (for building projects)
- Webcam (for webcam-based applications)

## Dependencies

The project uses the following libraries (included in `lib_018` and `lib_023` directories):

- **BoofCV** - Computer vision library (versions 0.18 and 0.23)
  - Core modules: ip, feature, geo, calibration, io, recognition, sfm, visualize
- **Webcam Capture** - Version 0.3.10
- **DDogleg** - Dense matrix operations
- **GeoRegression** - Geometric operations
- **XStream** - XML serialization

## Building the Projects

Each module is a separate NetBeans project with its own build configuration. To build a specific module:

```bash
cd <module-name>
ant clean
ant build
```

For example, to build the CornPlantColorSegment module:

```bash
cd CornPlantColorSegment
ant clean
ant build
```

## Running the Applications

After building, you can run the applications using:

```bash
cd <module-name>
ant run
```

Or execute the JAR files directly from the `dist` directory of each module.

## Key Features

- **Color Segmentation**: HSV-based color segmentation for plant detection
- **Real-time Processing**: Webcam integration for live image analysis
- **Image Processing**: Various algorithms including watershed, superpixels, and interest point detection
- **Agricultural Focus**: Specialized tools for corn plant analysis and tunnel guide detection
- **Multiple Versions**: Iterative improvements in corn plant detection algorithms (V1, V2, V3)

## Sample Images

Several modules include sample images (PNG and JPG format) for testing and demonstration purposes.

## License

This is a final project repository. Please refer to the original project documentation for license information.

## Author

pacozaa

## Acknowledgments

This project is built using:
- [BoofCV](http://boofcv.org/) - Open source computer vision library
- [Webcam Capture](https://github.com/sarxos/webcam-capture) - Library for webcam access
