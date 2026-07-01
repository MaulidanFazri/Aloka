<!-- Improved compatibility of back to top link: See: https://github.com/othneildrew/Best-README-Template/pull/73 -->
<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/MaulidanFazri/Aloka">
    <img src="https://raw.githubusercontent.com/MaulidanFazri/Aloka/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="Logo" width="80" height="80">
  </a>

  <h3 align="center">Aloka</h3>

  <p align="center">
    An AI-powered assistive navigation mobile app for the visually impaired.
    <br />
    <a href="https://github.com/MaulidanFazri/Aloka"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://github.com/MaulidanFazri/Aloka">View Demo</a>
    &middot;
    <a href="https://github.com/MaulidanFazri/Aloka/issues">Report Bug</a>
    &middot;
    <a href="https://github.com/MaulidanFazri/Aloka/issues">Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

Aloka is an assistive technology project designed to empower the visually impaired by providing real-time information about their surroundings. By leveraging modern AI and mobile technology, Aloka turns a smartphone into a powerful navigation tool.

Key Objectives:
* **Independence**: Helping users navigate indoor and outdoor environments with less reliance on others.
* **Safety**: Detecting hazards and dangerous objects in the path.
* **Awareness**: Providing descriptive audio feedback about nearby objects.

The app uses a highly optimized YOLO-TFLite model to ensure fast detection even on mid-range mobile devices, with a UI built entirely in Jetpack Compose for maximum accessibility.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

* [![Kotlin][Kotlin-shield]][Kotlin-url]
* [![Jetpack Compose][Compose-shield]][Compose-url]
* [![TensorFlow Lite][TFLite-shield]][TFLite-url]
* [![Android SDK][Android-shield]][Android-url]
* [![CameraX][CameraX-shield]][CameraX-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started

To get a local copy up and running, follow these steps.

### Prerequisites

* Android Studio (Ladybug 2024.2.1 or newer recommended)
* Android Device with API Level 26 (Android 8.0) or higher
* Physical device is recommended for CameraX and TFLite performance testing

### Installation

1. Clone the repo
   ```sh
   git clone https://github.com/MaulidanFazri/Aloka.git
   ```
2. Open the project in Android Studio.
3. Wait for Gradle Sync to complete.
4. Build and run the `app` module on your device.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

1. **Permission**: Grant Camera permission upon first launch.
2. **Detection**: Point the camera towards the environment. Aloka will automatically detect objects.
3. **Audio Feedback**: The app will announce detected objects and their positions (left, front, right).
4. **Battery Saver**: Double-tap the screen to enter "Black Screen Mode". The AI continues to run and provide audio feedback while saving significant battery.
5. **Danger Alerts**: If a hazardous object (like a vehicle or staircase) is detected nearby, the app will provide urgent vibration and audio warnings.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->
## Roadmap

- [x] Initial Project Setup & Renaming
- [x] YOLO-TFLite Model Integration
- [x] Real-time Object Detection UI
- [x] Battery Saver (Black Screen) Mode
- [x] Audio Guidance (TTS)
- [ ] Multi-language Support (English & Indonesian)
- [ ] Distance Estimation Improvements
- [ ] Integration with External Wearable Camera

See the [open issues](https://github.com/MaulidanFazri/Aloka/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact

Maulidan Fazri - [GitHub Profile](https://github.com/MaulidanFazri)

Project Link: [https://github.com/MaulidanFazri/Aloka](https://github.com/MaulidanFazri/Aloka)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* [Best-README-Template](https://github.com/othneildrew/Best-README-Template) for the inspiration.
* [TensorFlow Lite](https://www.tensorflow.org/lite) for the ML inference engine.
* [Android CameraX](https://developer.android.com/training/camerax) for the robust camera implementation.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[contributors-shield]: https://img.shields.io/github/contributors/MaulidanFazri/Aloka.svg?style=for-the-badge
[contributors-url]: https://github.com/MaulidanFazri/Aloka/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/MaulidanFazri/Aloka.svg?style=for-the-badge
[forks-url]: https://github.com/MaulidanFazri/Aloka/network/members
[stars-shield]: https://img.shields.io/github/stars/MaulidanFazri/Aloka.svg?style=for-the-badge
[stars-url]: https://github.com/MaulidanFazri/Aloka/stargazers
[issues-shield]: https://img.shields.io/github/issues/MaulidanFazri/Aloka.svg?style=for-the-badge
[issues-url]: https://github.com/MaulidanFazri/Aloka/issues
[license-shield]: https://img.shields.io/github/license/MaulidanFazri/Aloka.svg?style=for-the-badge
[license-url]: https://github.com/MaulidanFazri/Aloka/blob/master/LICENSE
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://linkedin.com/in/maulidanfazri
[Kotlin-shield]: https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white
[Kotlin-url]: https://kotlinlang.org/
[Compose-shield]: https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white
[Compose-url]: https://developer.android.com/jetpack/compose
[TFLite-shield]: https://img.shields.io/badge/TensorFlow%20Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white
[TFLite-url]: https://www.tensorflow.org/lite
[Android-shield]: https://img.shields.io/badge/Android%20SDK-3DDC84?style=for-the-badge&logo=android&logoColor=white
[Android-url]: https://developer.android.com/sdk
[CameraX-shield]: https://img.shields.io/badge/CameraX-4285F4?style=for-the-badge&logo=google&logoColor=white
[CameraX-url]: https://developer.android.com/training/camerax
