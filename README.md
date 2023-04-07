# Face Recognition Demo Android

The demo can detect and recognize human faces from the camera.
First of all, application detects faces on images and creates bounding box for each detected face.
Then second model recognize all detected faces based on `face_gallery` specified by user.
The following pretrained models can be used:

* `face_detection_short_range.tflite`, to detect faces and predict their bounding boxes. Download model from [source](https://storage.googleapis.com/mediapipe-assets/face_detection_short_range.tflite).
* `mobilefacenet.tflite` to recognize persons. Download model from [source](https://github.com/MCarlomagno/FaceRecognitionAuth/raw/master/assets/mobilefacenet.tflite).

## Prerequisites

* OMZ
* OpenCV-android-sdk

## Build

For **Windows** use `gradlew` to build demo from application root:

* Debug version

```
  .\gradlew assembleDebug -POMZ_DIR="path_to_OMZ" -POpenCV_DIR="path_to_opencv_sdk\OpenCV-android-sdk\sdk\native\jni\abi-arm64-v8a\"
```

* Release version signed

1. First of all, generate key for your application, using next command:

    ```
    keytool -genkey -v -keystore <key_name>.jks -keyalg RSA -keysize 2048 -validity 10000 -alias <alias_name>
    ```

2. Add key to `build.gradle`:

    ```
     signingConfigs {
            release {
                // You need to specify either an absolute path or include the
                // keystore file in the same directory as the build.gradle file.
                storeFile file("my-release-key.jks")
                storePassword "password"
                keyAlias "my-alias"
                keyPassword "password"
            }
     }
     buildTypes {
         release {
             signingConfig signingConfigs.release
             ...
         }
     }
    ```

3. Build application by next command:

    ```
      .\gradlew assembleRelease -POMZ_DIR="path_to_OMZ" -POpenCV_DIR="path_to_opencv_sdk\OpenCV-android-sdk\sdk\native\jni\abi-arm64-v8a\"
    ```

## Example

![](./face_example.gif)
